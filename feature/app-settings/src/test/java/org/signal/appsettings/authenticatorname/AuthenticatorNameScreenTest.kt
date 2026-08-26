/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorname

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import assertk.assertThat
import assertk.assertions.contains
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AuthenticatorNameScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val events = mutableListOf<AuthenticatorNameEvent>()

  @Test
  fun whenITypeAName_thenIExpectNameChangedEvent() {
    setContent(AuthenticatorNameState())

    composeTestRule.onNodeWithTag(AuthenticatorNameTestTags.NAME_INPUT)
      .assertIsDisplayed()
      .performTextReplacement("Bitwarden Authenticator")

    assertThat(events).contains(AuthenticatorNameEvent.NameChanged("Bitwarden Authenticator"))
  }

  @Test
  fun givenABlankName_whenIDisplayScreen_thenIExpectNextDisabled() {
    setContent(AuthenticatorNameState())

    composeTestRule.onNodeWithTag(AuthenticatorNameTestTags.BUTTON_NEXT).assertIsNotEnabled()
  }

  @Test
  fun givenASubmittingState_whenIDisplayScreen_thenIExpectNextDisabled() {
    setContent(AuthenticatorNameState(name = "Twilio Authy", submitting = true))

    composeTestRule.onNodeWithTag(AuthenticatorNameTestTags.BUTTON_NEXT).assertIsNotEnabled()
  }

  @Test
  fun givenAName_whenIClickNext_thenIExpectNextClickedEvent() {
    setContent(AuthenticatorNameState(name = "Twilio Authy"))

    composeTestRule.onNodeWithTag(AuthenticatorNameTestTags.BUTTON_NEXT)
      .assertIsEnabled()
      .performClick()

    assertThat(events).contains(AuthenticatorNameEvent.NextClicked)
  }

  private fun setContent(state: AuthenticatorNameState) {
    composeTestRule.setContent {
      AuthenticatorNameScreen(
        state = state,
        onEvent = { events += it }
      )
    }
  }
}
