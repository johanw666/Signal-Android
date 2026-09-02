/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.signal.libsignal.net.RequestResult
import java.time.Instant

/**
 * Covers the parts of the stand-in that copy behaviour the service is strict about, since those are the parts most
 * likely to be wrong once there's a real service behind [TotpApi].
 */
class InMemoryTotpApiTest {

  companion object {
    private const val NOW = 1_700_000_000_000L
    private const val CODE = 123456
    private val METADATA = TotpApi.Metadata(name = "Aegis", createdAt = Instant.ofEpochMilli(NOW))
    private val OTHER_METADATA = TotpApi.Metadata(name = "Aegis on my tablet", createdAt = Instant.ofEpochMilli(NOW))
  }

  private val api = InMemoryTotpApi()

  @Test
  fun `a generated key is 32 bytes`() = runTest {
    val result = api.generateKey()

    assertThat(result).isInstanceOf(RequestResult.Success::class)
    assertThat((result as RequestResult.Success).result.key.size).isEqualTo(32)
  }

  @Test
  fun `a pending key doesn't show up until it's confirmed`() = runTest {
    api.generateKey()

    assertThat(listedKeys()).hasSize(0)
  }

  @Test
  fun `a confirmed key is assigned the lowest free id`() = runTest {
    val first = confirmNewKey()
    val second = confirmNewKey()

    assertThat(first).isEqualTo(0)
    assertThat(second).isEqualTo(1)
  }

  @Test
  fun `an id freed by a removal is handed out again rather than counting upwards`() = runTest {
    confirmNewKey()
    val second = confirmNewKey()
    api.removeKey(second)

    assertThat(confirmNewKey()).isEqualTo(second)
  }

  @Test
  fun `confirming with no pending key doesn't confirm anything`() = runTest {
    assertThat(api.confirmKey(oneTimePassword = CODE, metadata = METADATA)).isEqualTo(RequestResult.NonSuccess(TotpApi.ConfirmKeyError.NotVerified))
    assertThat(listedKeys()).hasSize(0)
  }

  @Test
  fun `an account at its limit can't generate another key`() = runTest {
    repeat(TotpApi.MAX_KEYS) { confirmNewKey() }

    assertThat(api.generateKey()).isEqualTo(RequestResult.NonSuccess(TotpApi.GenerateKeyError.TooManyKeys))
  }

  @Test
  fun `removing a key makes room for another`() = runTest {
    repeat(TotpApi.MAX_KEYS) { confirmNewKey() }
    api.removeKey(0)

    assertThat(api.generateKey()).isInstanceOf(RequestResult.Success::class)
  }

  @Test
  fun `metadata can be replaced on a confirmed key`() = runTest {
    val keyId = confirmNewKey()

    assertThat(api.setKeyMetadata(keyId, OTHER_METADATA)).isEqualTo(RequestResult.Success(Unit))
    assertThat(listedKeys().first().metadata).isEqualTo(OTHER_METADATA)
  }

  @Test
  fun `metadata for a key that isn't there is reported rather than created`() = runTest {
    assertThat(api.setKeyMetadata(7, METADATA)).isEqualTo(RequestResult.NonSuccess(TotpApi.SetKeyMetadataError.KeyNotFound))
  }

  /** The service leaves a fixed amount of room for the name, so a name that doesn't fit is the caller's bug. */
  @Test
  fun `a name longer than the room the service leaves is refused`() = runTest {
    val keyId = confirmNewKey()
    val tooLong = METADATA.copy(name = "a".repeat(TotpApi.Metadata.NAME_MAX_LENGTH + 1))

    assertFailure { api.setKeyMetadata(keyId, tooLong) }.isInstanceOf(IllegalArgumentException::class)
  }

  /** The service reports success either way, so a retried removal looks like the original. */
  @Test
  fun `removing a key that isn't there still succeeds`() = runTest {
    assertThat(api.removeKey(7)).isEqualTo(RequestResult.Success(Unit))
  }

  @Test
  fun `keys are listed in ascending id order`() = runTest {
    confirmNewKey()
    confirmNewKey()

    assertThat(listedKeys().map { it.keyId }).isEqualTo(listOf(0, 1))
  }

  private suspend fun confirmNewKey(): Int {
    api.generateKey()
    return (api.confirmKey(oneTimePassword = CODE, metadata = METADATA) as RequestResult.Success).result
  }

  private suspend fun listedKeys(): List<TotpApi.RemoteKey> = (api.listKeys() as RequestResult.Success).result
}
