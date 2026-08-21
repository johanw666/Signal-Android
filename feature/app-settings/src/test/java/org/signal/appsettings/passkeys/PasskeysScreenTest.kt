/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import assertk.assertThat
import assertk.assertions.contains
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PasskeysScreenTest {

  companion object {
    private val PASSKEYS = listOf(
      Passkey(id = 1, name = "My Security Key", createdAt = System.currentTimeMillis()),
      Passkey(id = 2, name = "My Pixel Phone", createdAt = System.currentTimeMillis())
    )
  }

  @get:Rule
  val composeTestRule = createComposeRule()

  private val events = mutableListOf<PasskeysEvent>()

  @Test
  fun givenNoPasskeys_whenIClickSetUpAPasskey_thenIExpectSetUpPasskeyEvent() {
    setContent(PasskeysState())

    composeTestRule.onNodeWithTag(PasskeysTestTags.BUTTON_SET_UP)
      .assertIsDisplayed()
      .performClick()

    assertThat(events).contains(PasskeysEvent.SetUpPasskeyClicked)
  }

  @Test
  fun givenPasskeys_whenIDisplayScreen_thenIExpectARowPerPasskey() {
    setContent(PasskeysState(passkeys = PASSKEYS))

    for (passkey in PASSKEYS) {
      composeTestRule.onNodeWithTag(PasskeysTestTags.SCROLLER).performScrollToNode(hasText(passkey.name))
      composeTestRule.onNodeWithText(passkey.name).assertIsDisplayed()
    }
  }

  @Test
  fun givenPasskeys_whenIClickAddANewPasskey_thenIExpectSetUpPasskeyEvent() {
    setContent(PasskeysState(passkeys = PASSKEYS))

    composeTestRule.onNodeWithTag(PasskeysTestTags.BUTTON_SET_UP)
      .assertIsDisplayed()
      .performClick()

    assertThat(events).contains(PasskeysEvent.SetUpPasskeyClicked)
  }

  @Test
  fun givenPasskeys_whenIClickRenameInAPasskeysMenu_thenIExpectRenameEvent() {
    setContent(PasskeysState(passkeys = PASSKEYS))

    scrollTo(PasskeysTestTags.BUTTON_PASSKEY_MENU)

    composeTestRule.onAllNodesWithTag(PasskeysTestTags.BUTTON_PASSKEY_MENU)[0].performClick()
    composeTestRule.onNodeWithTag(PasskeysTestTags.MENU_ITEM_RENAME).performClick()

    assertThat(events).contains(PasskeysEvent.RenamePasskeyClicked(passkeyId = PASSKEYS[0].id))
  }

  @Test
  fun givenPasskeys_whenIClickRemoveInAPasskeysMenu_thenIExpectRemoveEvent() {
    setContent(PasskeysState(passkeys = PASSKEYS))

    scrollTo(PasskeysTestTags.BUTTON_PASSKEY_MENU)

    composeTestRule.onAllNodesWithTag(PasskeysTestTags.BUTTON_PASSKEY_MENU)[0].performClick()
    composeTestRule.onNodeWithTag(PasskeysTestTags.MENU_ITEM_REMOVE).performClick()

    assertThat(events).contains(PasskeysEvent.RemovePasskeyClicked(passkeyId = PASSKEYS[0].id))
  }

  private fun setContent(state: PasskeysState) {
    composeTestRule.setContent {
      PasskeysScreen(
        state = state,
        onEvent = { events += it }
      )
    }
  }

  private fun scrollTo(testTag: String) {
    composeTestRule.onNodeWithTag(PasskeysTestTags.SCROLLER)
      .performScrollToNode(hasTestTag(testTag))
  }
}
