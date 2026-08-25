/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.contains
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.signal.core.ui.CoreUiDependenciesRule
import org.signal.core.ui.compose.theme.SignalTheme
import org.signal.registration.test.TestTags

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SignalLoginScreenTest {

  companion object {
    private const val VALID_ACCOUNT_KEY = "a6b284822e3283d07f2391360a4c2b91"
  }

  @get:Rule
  val composeTestRule = createComposeRule()

  @get:Rule
  val coreUiDependenciesRule = CoreUiDependenciesRule(ApplicationProvider.getApplicationContext())

  private val events = mutableListOf<SignalLoginScreenEvents>()

  @Test
  fun `when text is typed into the account key field, AccountKeyChanged is emitted`() {
    setContent(SignalLoginState())

    composeTestRule.onNodeWithTag(TestTags.SIGNAL_LOGIN_ACCOUNT_KEY_FIELD).performTextInput("a6b2")

    assertThat(events).contains(SignalLoginScreenEvents.AccountKeyChanged("a6b2"))
  }

  @Test
  fun `when Need help is clicked, NeedHelpClicked is emitted`() {
    setContent(SignalLoginState())

    composeTestRule.onNodeWithTag(TestTags.SIGNAL_LOGIN_NEED_HELP_BUTTON).performClick()

    assertThat(events).contains(SignalLoginScreenEvents.NeedHelpClicked)
  }

  @Test
  fun `when Next is clicked with a complete key, NextClicked is emitted`() {
    setContent(SignalLoginState(accountKey = VALID_ACCOUNT_KEY))

    composeTestRule.onNodeWithTag(TestTags.SIGNAL_LOGIN_NEXT_BUTTON).performClick()

    assertThat(events).contains(SignalLoginScreenEvents.NextClicked)
  }

  @Test
  fun `given an incomplete key, Next is disabled`() {
    setContent(SignalLoginState(accountKey = "a6b28482"))

    composeTestRule.onNodeWithTag(TestTags.SIGNAL_LOGIN_NEXT_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun `given a complete key, Next is enabled`() {
    setContent(SignalLoginState(accountKey = VALID_ACCOUNT_KEY))

    composeTestRule.onNodeWithTag(TestTags.SIGNAL_LOGIN_NEXT_BUTTON).assertIsEnabled()
  }

  @Test
  fun `given a submission is in flight, Next is disabled`() {
    setContent(SignalLoginState(accountKey = VALID_ACCOUNT_KEY, isSubmitting = true))

    composeTestRule.onNodeWithTag(TestTags.SIGNAL_LOGIN_NEXT_BUTTON).assertIsNotEnabled()
  }

  private fun setContent(state: SignalLoginState) {
    composeTestRule.setContent {
      SignalTheme {
        SignalLoginScreen(
          state = state,
          onEvent = { events += it }
        )
      }
    }
  }
}
