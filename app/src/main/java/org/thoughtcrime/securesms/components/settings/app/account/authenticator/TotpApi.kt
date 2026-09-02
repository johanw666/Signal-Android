/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import org.signal.core.util.censor
import org.signal.libsignal.net.BadRequestError
import org.signal.libsignal.net.RequestResult
import java.time.Instant

/**
 * All TOTP operations.
 */
interface TotpApi {

  companion object {
    /** How many confirmed keys an account may have, which the service enforces. */
    const val MAX_KEYS = 2

    /** The ids the service will assign, which fit in a byte with the sign bit clear. */
    val KEY_ID_RANGE = 0..127
  }

  /**
   * Generates a new pending key, replacing any pending key already on the account. The key doesn't take effect, or show
   * up in [listKeys], until [confirmKey] proves the caller kept a copy of it.
   */
  suspend fun generateKey(): RequestResult<GeneratedKey, GenerateKeyError>

  /**
   * Confirms the pending key by proving a one-time password can be derived from it, and attaches [metadata] to it,
   * returning the id the service assigned.
   */
  suspend fun confirmKey(oneTimePassword: Int, metadata: Metadata): RequestResult<Int, ConfirmKeyError>

  /** The confirmed keys on the account. Key material is never returned, only metadata and parameters. */
  suspend fun listKeys(): RequestResult<List<RemoteKey>, Nothing>

  /** Replaces the metadata attached to a confirmed key. */
  suspend fun setKeyMetadata(keyId: Int, metadata: Metadata): RequestResult<Unit, SetKeyMetadataError>

  /** Removes a key, which also succeeds when there's no key with that id, so retries look the same as the first try. */
  suspend fun removeKey(keyId: Int): RequestResult<Unit, Nothing>

  data class Metadata(val name: String, val createdAt: Instant) {
    override fun toString(): String = "Metadata(name=${name.censor()}, createdAt=$createdAt)"

    companion object {
      /** How long [name] may be, in bytes of UTF-8, which is the room the service's metadata blob leaves for it. */
      const val NAME_MAX_LENGTH = 98
    }
  }

  data class GeneratedKey(val key: ByteArray) {
    override fun equals(other: Any?): Boolean = other is GeneratedKey && key.contentEquals(other.key)
    override fun hashCode(): Int = key.contentHashCode()
    override fun toString(): String = "GeneratedKey()"
  }

  data class RemoteKey(
    /** The account-specific id, in [KEY_ID_RANGE]. */
    val keyId: Int,
    val metadata: Metadata
  ) {
    override fun toString(): String = "RemoteKey(keyId=$keyId)"
  }

  sealed interface GenerateKeyError : BadRequestError {
    /** The account already has [MAX_KEYS] keys. */
    data object TooManyKeys : GenerateKeyError
  }

  sealed interface ConfirmKeyError : BadRequestError {
    /** The password was wrong, the clocks are too far apart, or there was no pending key. The service can't tell us which. */
    data object NotVerified : ConfirmKeyError

    /** The account filled up with keys between generating this one and confirming it. */
    data object TooManyKeys : ConfirmKeyError
  }

  sealed interface SetKeyMetadataError : BadRequestError {
    data object KeyNotFound : SetKeyMetadataError
  }
}
