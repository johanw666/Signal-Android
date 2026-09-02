/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpnameentry

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
class TotpNameEntryScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val events = mutableListOf<TotpNameEntryEvent>()

  @Test
  fun whenITypeAName_thenIExpectNameChangedEvent() {
    setContent(TotpNameEntryState())

    composeTestRule.onNodeWithTag(TotpNameEntryTestTags.NAME_INPUT)
      .assertIsDisplayed()
      .performTextReplacement("Bitwarden Authenticator")

    assertThat(events).contains(TotpNameEntryEvent.NameChanged("Bitwarden Authenticator"))
  }

  @Test
  fun givenABlankName_whenIDisplayScreen_thenIExpectNextDisabled() {
    setContent(TotpNameEntryState())

    composeTestRule.onNodeWithTag(TotpNameEntryTestTags.BUTTON_NEXT).assertIsNotEnabled()
  }

  @Test
  fun givenASubmittingState_whenIDisplayScreen_thenIExpectNextDisabled() {
    setContent(TotpNameEntryState(name = "Twilio Authy", submitting = true))

    composeTestRule.onNodeWithTag(TotpNameEntryTestTags.BUTTON_NEXT).assertIsNotEnabled()
  }

  @Test
  fun givenAName_whenIClickNext_thenIExpectNextClickedEvent() {
    setContent(TotpNameEntryState(name = "Twilio Authy"))

    composeTestRule.onNodeWithTag(TotpNameEntryTestTags.BUTTON_NEXT)
      .assertIsEnabled()
      .performClick()

    assertThat(events).contains(TotpNameEntryEvent.NextClicked)
  }

  private fun setContent(state: TotpNameEntryState) {
    composeTestRule.setContent {
      TotpNameEntryScreen(
        state = state,
        onEvent = { events += it }
      )
    }
  }
}
