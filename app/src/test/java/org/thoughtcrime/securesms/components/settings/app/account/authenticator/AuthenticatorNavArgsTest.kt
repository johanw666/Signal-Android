/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import android.app.Application
import android.os.Bundle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryState.Purpose

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AuthenticatorNavArgsTest {

  @Test
  fun `no arguments means adding a new app`() {
    assertThat(AuthenticatorNavArgs.purpose(null)).isEqualTo(Purpose.Add)
    assertThat(AuthenticatorNavArgs.appId(null)).isNull()
  }

  @Test
  fun `an unset app id reads as null`() {
    val arguments = Bundle().apply { putLong(AuthenticatorNavArgs.ARG_APP_ID, AuthenticatorNavArgs.NO_APP_ID) }

    assertThat(AuthenticatorNavArgs.appId(arguments)).isNull()
  }

  @Test
  fun `a removal carries the app id it names`() {
    val arguments = Bundle().apply {
      putString(AuthenticatorNavArgs.ARG_PURPOSE, AuthenticatorNavArgs.PURPOSE_REMOVE)
      putLong(AuthenticatorNavArgs.ARG_APP_ID, 7)
    }

    assertThat(AuthenticatorNavArgs.appId(arguments)).isEqualTo(7L)
    assertThat(AuthenticatorNavArgs.purpose(arguments)).isEqualTo(Purpose.Remove(7))
  }

  @Test
  fun `a removal without an app id falls back to adding rather than removing something unidentified`() {
    val arguments = Bundle().apply { putString(AuthenticatorNavArgs.ARG_PURPOSE, AuthenticatorNavArgs.PURPOSE_REMOVE) }

    assertThat(AuthenticatorNavArgs.purpose(arguments)).isEqualTo(Purpose.Add)
  }

  @Test
  fun `an unrecognized purpose falls back to adding instead of throwing`() {
    val arguments = Bundle().apply { putString(AuthenticatorNavArgs.ARG_PURPOSE, "nonsense") }

    assertThat(AuthenticatorNavArgs.purpose(arguments)).isEqualTo(Purpose.Add)
  }
}
