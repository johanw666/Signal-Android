/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import org.signal.appsettings.totpapplist.TotpApp
import org.signal.core.util.Base32
import org.signal.core.util.logging.Log
import org.signal.libsignal.net.RequestResult
import java.net.URLEncoder
import java.time.Instant

/**
 * Everything the authenticator app screens need, sitting between them and the TOTP operations on [TotpApi].
 */
class TotpRepository(
  private val api: TotpApi = SHARED_API,
  private val clock: () -> Long = System::currentTimeMillis
) {

  companion object {
    private val TAG = Log.tag(TotpRepository::class)

    /** Shared so that every screen in the flow sees the same state until there's a service behind this. */
    private val SHARED_API: TotpApi = InMemoryTotpApi()

    private const val ISSUER = "Signal"

    /** What the service uses, and what every authenticator app supports without reading a single URI parameter. */
    private const val ALGORITHM = "SHA1"
    private const val DIGITS = 6
    private const val PERIOD_SECONDS = 30

    /** How many characters of the display form go between spaces. */
    private const val DISPLAY_GROUP_SIZE = 4

    const val MAX_NAME_LENGTH_BYTES = TotpApi.Metadata.NAME_MAX_LENGTH
    const val MAX_NAME_LENGTH_GRAPHEMES = 30
  }

  fun getMaxApps(): Int {
    return TotpApi.MAX_KEYS
  }

  /**
   * Asks the service for a new key, returning what the setup screen needs to hand it to an authenticator app. The
   * service holds the pending key from here until [confirmPendingApp], so nothing is kept on this side.
   */
  suspend fun beginSetup(accountName: String): BeginSetupResult {
    return when (val result = api.generateKey()) {
      is RequestResult.Success -> {
        val key = result.result.key

        BeginSetupResult.Success(
          setupUri = buildSetupUri(key = key, accountName = accountName),
          displayKey = Base32.encode(key).chunked(DISPLAY_GROUP_SIZE).joinToString(" "),
          clipboardKey = Base32.encode(key)
        )
      }
      is RequestResult.NonSuccess -> {
        when (result.error) {
          TotpApi.GenerateKeyError.TooManyKeys -> BeginSetupResult.TooManyApps
        }
      }
      is RequestResult.RetryableNetworkError -> {
        Log.w(TAG, "Couldn't generate a key.", result.networkError)
        BeginSetupResult.NetworkFailure
      }
      is RequestResult.ApplicationError -> {
        Log.w(TAG, "Couldn't generate a key.", result.cause)
        BeginSetupResult.NetworkFailure
      }
    }
  }

  /**
   * Confirms the pending key with a code from the user's authenticator app.
   *
   * The key is confirmed without a name, because the service wants metadata at confirmation time and the user doesn't
   * name their app until the screen after this one. Naming it later means a brief window where a key has no name, which
   * is a better failure than a window where the second factor isn't active yet.
   */
  suspend fun confirmPendingApp(code: String): ConfirmResult {
    val oneTimePassword = code.toIntOrNull() ?: return ConfirmResult.IncorrectCode

    val metadata = TotpApi.Metadata(name = "", createdAt = Instant.ofEpochMilli(clock()))

    return when (val result = api.confirmKey(oneTimePassword = oneTimePassword, metadata = metadata)) {
      is RequestResult.Success -> {
        ConfirmResult.Success(appId = result.result.toLong())
      }
      is RequestResult.NonSuccess -> when (result.error) {
        TotpApi.ConfirmKeyError.NotVerified -> ConfirmResult.IncorrectCode
        TotpApi.ConfirmKeyError.TooManyKeys -> {
          Log.w(TAG, "The account filled up with keys between generating this one and confirming it.")
          ConfirmResult.TooManyApps
        }
      }
      is RequestResult.RetryableNetworkError -> {
        Log.w(TAG, "Couldn't confirm the pending key.", result.networkError)
        ConfirmResult.NetworkFailure
      }
      is RequestResult.ApplicationError -> {
        Log.w(TAG, "Couldn't confirm the pending key.", result.cause)
        ConfirmResult.NetworkFailure
      }
    }
  }

  /** The authenticator apps on the account, newest id last. */
  suspend fun getTotpApps(): AppsResult {
    return when (val result = api.listKeys()) {
      is RequestResult.Success -> {
        AppsResult.Success(
          result.result.map { key ->
            TotpApp(
              id = key.keyId.toLong(),
              name = key.metadata.name,
              createdAt = key.metadata.createdAt.toEpochMilli()
            )
          }
        )
      }
      is RequestResult.RetryableNetworkError -> {
        Log.w(TAG, "Couldn't list keys.", result.networkError)
        AppsResult.NetworkFailure
      }
      is RequestResult.ApplicationError -> {
        Log.w(TAG, "Couldn't list keys.", result.cause)
        AppsResult.NetworkFailure
      }
      is RequestResult.NonSuccess -> error("Code branch is unreachable")
    }
  }

  /** Renames [app], which means handing the whole metadata blob back to the service. */
  suspend fun renameTotpApp(app: TotpApp, name: String): UpdateResult {
    return setMetadata(app.id, TotpApi.Metadata(name = name, createdAt = Instant.ofEpochMilli(app.createdAt)))
  }

  /** Names a newly confirmed app, which was confirmed without one moments ago. */
  suspend fun nameNewTotpApp(appId: Long, name: String): UpdateResult {
    return setMetadata(appId, TotpApi.Metadata(name = name, createdAt = Instant.ofEpochMilli(clock())))
  }

  suspend fun removeTotpApp(appId: Long): UpdateResult {
    return when (val result = api.removeKey(appId.toInt())) {
      is RequestResult.Success -> UpdateResult.Success
      is RequestResult.RetryableNetworkError -> {
        Log.w(TAG, "Couldn't remove the key.", result.networkError)
        UpdateResult.NetworkFailure
      }
      is RequestResult.ApplicationError -> {
        Log.w(TAG, "Couldn't remove the key.", result.cause)
        UpdateResult.NetworkFailure
      }
      is RequestResult.NonSuccess -> error("Code branch is unreachable")
    }
  }

  private suspend fun setMetadata(appId: Long, metadata: TotpApi.Metadata): UpdateResult {
    return when (val result = api.setKeyMetadata(keyId = appId.toInt(), metadata = metadata)) {
      is RequestResult.Success -> UpdateResult.Success
      is RequestResult.NonSuccess -> when (result.error) {
        TotpApi.SetKeyMetadataError.KeyNotFound -> UpdateResult.AppNotFound
      }
      is RequestResult.RetryableNetworkError -> {
        Log.w(TAG, "Couldn't set key metadata.", result.networkError)
        UpdateResult.NetworkFailure
      }
      is RequestResult.ApplicationError -> {
        Log.w(TAG, "Couldn't set key metadata.", result.cause)
        UpdateResult.NetworkFailure
      }
    }
  }

  /**
   * The `otpauth://` URI that hands the key to an authenticator app, following the de facto Key Uri Format every app
   * implements. Note that a lot of apps ignore params like "algorithm", but we set them just in case.
   */
  private fun buildSetupUri(key: ByteArray, accountName: String): String {
    val label = if (accountName.isBlank()) encode(ISSUER) else "${encode(ISSUER)}:${encode(accountName)}"

    val query = listOf(
      "secret" to Base32.encode(key),
      "issuer" to ISSUER,
      "algorithm" to ALGORITHM,
      "digits" to DIGITS.toString(),
      "period" to PERIOD_SECONDS.toString()
    ).joinToString("&") { (name, value) -> "$name=${encode(value)}" }

    return "otpauth://totp/$label?$query"
  }

  /**
   * [URLEncoder] targets form encoding rather than URIs, so it renders a space as `+` where a URI needs `%20`, and
   * escapes `~` where a URI leaves it alone.
   */
  private fun encode(value: String): String {
    return URLEncoder.encode(value, Charsets.UTF_8.name())
      .replace("+", "%20")
      .replace("%7E", "~")
  }

  sealed interface BeginSetupResult {
    data class Success(val setupUri: String, val displayKey: String, val clipboardKey: String) : BeginSetupResult {
      override fun toString(): String = "Success()"
    }

    /** The account already has as many authenticator apps as it's allowed. */
    data object TooManyApps : BeginSetupResult

    data object NetworkFailure : BeginSetupResult
  }

  sealed interface ConfirmResult {
    data class Success(val appId: Long) : ConfirmResult

    data object IncorrectCode : ConfirmResult

    /** Another device filled the account up between generating the key and confirming it. */
    data object TooManyApps : ConfirmResult

    data object NetworkFailure : ConfirmResult
  }

  sealed interface AppsResult {
    data class Success(val apps: List<TotpApp>) : AppsResult

    data object NetworkFailure : AppsResult
  }

  sealed interface UpdateResult {
    data object Success : UpdateResult

    data object AppNotFound : UpdateResult

    data object NetworkFailure : UpdateResult
  }
}
