/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import org.signal.libsignal.net.RequestResult
import java.security.SecureRandom

/**
 * Stands in for the service until the gRPC methods land. Nothing here is persisted or sent anywhere, so it lasts only
 * as long as the process does.
 *
 * It doesn't verify one-time passwords -- any code confirms the pending key. It does copy the service's behaviour where
 * that behaviour shapes the screens: one pending key at a time, [TotpApi.MAX_KEYS] confirmed keys, ids drawn from the
 * lowest free slot, and name length enforced.
 */
class InMemoryTotpApi : TotpApi {

  companion object {
    /** What the service generates: a 256-bit key. */
    private const val KEY_LENGTH_BYTES = 32
  }

  private val lock = Any()
  private val secureRandom = SecureRandom()

  private var hasPendingKey = false
  private val confirmedKeys = mutableMapOf<Int, TotpApi.Metadata>()

  override suspend fun generateKey(): RequestResult<TotpApi.GeneratedKey, TotpApi.GenerateKeyError> = synchronized(lock) {
    if (confirmedKeys.size >= TotpApi.MAX_KEYS) {
      return RequestResult.NonSuccess(TotpApi.GenerateKeyError.TooManyKeys)
    }

    hasPendingKey = true

    RequestResult.Success(TotpApi.GeneratedKey(key = ByteArray(KEY_LENGTH_BYTES).also { secureRandom.nextBytes(it) }))
  }

  override suspend fun confirmKey(oneTimePassword: Int, metadata: TotpApi.Metadata): RequestResult<Int, TotpApi.ConfirmKeyError> = synchronized(lock) {
    requireNameFits(metadata)

    if (!hasPendingKey) {
      return RequestResult.NonSuccess(TotpApi.ConfirmKeyError.NotVerified)
    }

    if (confirmedKeys.size >= TotpApi.MAX_KEYS) {
      return RequestResult.NonSuccess(TotpApi.ConfirmKeyError.TooManyKeys)
    }

    val keyId = nextKeyId()
    confirmedKeys[keyId] = metadata
    hasPendingKey = false

    RequestResult.Success(keyId)
  }

  override suspend fun listKeys(): RequestResult<List<TotpApi.RemoteKey>, Nothing> = synchronized(lock) {
    RequestResult.Success(
      confirmedKeys.entries
        .sortedBy { it.key }
        .map { (keyId, metadata) -> TotpApi.RemoteKey(keyId = keyId, metadata = metadata) }
    )
  }

  override suspend fun setKeyMetadata(keyId: Int, metadata: TotpApi.Metadata): RequestResult<Unit, TotpApi.SetKeyMetadataError> = synchronized(lock) {
    requireNameFits(metadata)

    if (keyId !in confirmedKeys) {
      return RequestResult.NonSuccess(TotpApi.SetKeyMetadataError.KeyNotFound)
    }
    confirmedKeys[keyId] = metadata

    RequestResult.Success(Unit)
  }

  override suspend fun removeKey(keyId: Int): RequestResult<Unit, Nothing> = synchronized(lock) {
    confirmedKeys.remove(keyId)
    RequestResult.Success(Unit)
  }

  /** The service hands out the lowest free id rather than counting upwards, so removing a key frees its id for reuse. */
  private fun nextKeyId(): Int = TotpApi.KEY_ID_RANGE.first { it !in confirmedKeys }

  private fun requireNameFits(metadata: TotpApi.Metadata) {
    require(metadata.name.toByteArray(Charsets.UTF_8).size <= TotpApi.Metadata.NAME_MAX_LENGTH) {
      "Name must be at most ${TotpApi.Metadata.NAME_MAX_LENGTH} bytes of UTF-8"
    }
  }
}
