/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.startsWith
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.signal.appsettings.totpapplist.TotpApp
import org.thoughtcrime.securesms.components.settings.app.account.authenticator.TotpRepository.AppsResult
import org.thoughtcrime.securesms.components.settings.app.account.authenticator.TotpRepository.BeginSetupResult
import org.thoughtcrime.securesms.components.settings.app.account.authenticator.TotpRepository.ConfirmResult
import org.thoughtcrime.securesms.components.settings.app.account.authenticator.TotpRepository.UpdateResult

class TotpRepositoryTest {

  companion object {
    private const val NOW = 1_700_000_000_000L
    private const val ACCOUNT_NAME = "8B4A1F0C"
    private const val CODE = "123456"
  }

  private var now = NOW
  private val api = InMemoryTotpApi()
  private val repository = TotpRepository(api = api, clock = { now })

  @Test
  fun `beginSetup returns a link and a key in both the forms the screen needs`() = runTest {
    val result = repository.beginSetup(ACCOUNT_NAME) as BeginSetupResult.Success

    assertThat(result.setupUri).startsWith("otpauth://totp/Signal:$ACCOUNT_NAME?")
    assertThat(result.setupUri).contains("secret=${result.clipboardKey}")
    assertThat(result.displayKey).isEqualTo(result.clipboardKey.chunked(4).joinToString(" "))
  }

  /** The issuer and the account name have to differ, or an app that shows both renders "Signal: Signal". */
  @Test
  fun `beginSetup names the entry after the account, under the issuer`() = runTest {
    val result = repository.beginSetup(ACCOUNT_NAME) as BeginSetupResult.Success

    assertThat(result.setupUri).startsWith("otpauth://totp/Signal:$ACCOUNT_NAME?")
    assertThat(result.setupUri).contains("issuer=Signal&")
  }

  /** Nothing should reach this without an ACI, but a bare issuer beats a label ending in a colon if anything does. */
  @Test
  fun `beginSetup falls back to the issuer alone when there's no account name`() = runTest {
    val result = repository.beginSetup("") as BeginSetupResult.Success

    assertThat(result.setupUri).startsWith("otpauth://totp/Signal?")
  }

  @Test
  fun `beginSetup treats an account name of nothing but whitespace as no account name`() = runTest {
    val result = repository.beginSetup("   ") as BeginSetupResult.Success

    assertThat(result.setupUri).startsWith("otpauth://totp/Signal?")
  }

  /** A space has to become %20 rather than the + form encoding would produce, or apps render it literally. */
  @Test
  fun `beginSetup percent-encodes the account name`() = runTest {
    val result = repository.beginSetup("+1 555") as BeginSetupResult.Success

    assertThat(result.setupUri).startsWith("otpauth://totp/Signal:%2B1%20555?")
  }

  @Test
  fun `setup asks for the parameters every authenticator app supports`() = runTest {
    val result = repository.beginSetup(ACCOUNT_NAME) as BeginSetupResult.Success

    assertThat(result.setupUri).contains("algorithm=SHA1")
    assertThat(result.setupUri).contains("digits=6")
    assertThat(result.setupUri).contains("period=30")
  }

  @Test
  fun `an account at its limit is told rather than handed a key`() = runTest {
    repeat(TotpApi.MAX_KEYS) { confirmNewApp() }

    assertThat(repository.beginSetup(ACCOUNT_NAME)).isEqualTo(BeginSetupResult.TooManyApps)
  }

  @Test
  fun `a confirmed app shows up in the list with the time it was confirmed`() = runTest {
    val appId = confirmNewApp()

    val apps = (repository.getTotpApps() as AppsResult.Success).apps

    assertThat(apps).hasSize(1)
    assertThat(apps.first().id).isEqualTo(appId)
    assertThat(apps.first().createdAt).isEqualTo(NOW)
  }

  /** The service wants metadata at confirmation time, and the user hasn't been asked for a name yet. */
  @Test
  fun `a newly confirmed app starts out with no name`() = runTest {
    confirmNewApp()

    val apps = (repository.getTotpApps() as AppsResult.Success).apps

    assertThat(apps.first().name).isEqualTo("")
  }

  @Test
  fun `naming a new app names it`() = runTest {
    val appId = confirmNewApp()

    assertThat(repository.nameNewTotpApp(appId, "Aegis")).isEqualTo(UpdateResult.Success)

    assertThat(listedApp(appId)?.name).isEqualTo("Aegis")
  }

  @Test
  fun `renaming keeps the time the app was confirmed`() = runTest {
    val appId = confirmNewApp()
    repository.nameNewTotpApp(appId, "Aegis")
    now += 60_000

    assertThat(repository.renameTotpApp(listedApp(appId)!!, "Aegis on my tablet")).isEqualTo(UpdateResult.Success)

    val app = listedApp(appId)
    assertThat(app?.name).isEqualTo("Aegis on my tablet")
    assertThat(app?.createdAt).isEqualTo(NOW)
  }

  @Test
  fun `renaming an app that isn't there is reported rather than creating one`() = runTest {
    val gone = TotpApp(id = 7, name = "Aegis", createdAt = NOW)

    assertThat(repository.renameTotpApp(gone, "Aegis on my tablet")).isEqualTo(UpdateResult.AppNotFound)
  }

  @Test
  fun `a removed app leaves the list`() = runTest {
    val appId = confirmNewApp()

    assertThat(repository.removeTotpApp(appId)).isEqualTo(UpdateResult.Success)
    assertThat((repository.getTotpApps() as AppsResult.Success).apps).isEmpty()
  }

  /** The service can't tell a wrong code from a missing pending key, so neither can we. */
  @Test
  fun `confirming with nothing pending is just a wrong code`() = runTest {
    assertThat(repository.confirmPendingApp(CODE)).isEqualTo(ConfirmResult.IncorrectCode)
  }

  @Test
  fun `a code that isn't a number is reported as a wrong code`() = runTest {
    repository.beginSetup(ACCOUNT_NAME)

    assertThat(repository.confirmPendingApp("abcdef")).isEqualTo(ConfirmResult.IncorrectCode)
  }

  @Test
  fun `a code confirms the app`() = runTest {
    repository.beginSetup(ACCOUNT_NAME)

    assertThat(repository.confirmPendingApp(CODE)).isInstanceOf(ConfirmResult.Success::class)
  }

  private suspend fun listedApp(appId: Long): TotpApp? {
    return (repository.getTotpApps() as AppsResult.Success).apps.firstOrNull { it.id == appId }
  }

  private suspend fun confirmNewApp(): Long {
    repository.beginSetup(ACCOUNT_NAME)
    return (repository.confirmPendingApp(CODE) as ConfirmResult.Success).appId
  }
}
