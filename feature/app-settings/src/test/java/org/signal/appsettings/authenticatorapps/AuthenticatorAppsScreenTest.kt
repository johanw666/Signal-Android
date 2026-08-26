/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorapps

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
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsState.Dialog
import org.signal.core.ui.compose.Dialogs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AuthenticatorAppsScreenTest {

  companion object {
    private val APPS = listOf(
      AuthenticatorApp(id = 1, name = "Bitwarden Authenticator", createdAt = System.currentTimeMillis()),
      AuthenticatorApp(id = 2, name = "Twilio Authy", createdAt = System.currentTimeMillis())
    )
  }

  @get:Rule
  val composeTestRule = createComposeRule()

  private val events = mutableListOf<AuthenticatorAppsEvent>()

  @Test
  fun givenNoApps_whenIDisplayScreen_thenIExpectTheEmptyMessage() {
    setContent(AuthenticatorAppsState())

    scrollTo(AuthenticatorAppsTestTags.EMPTY_MESSAGE)
    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun givenApps_whenIDisplayScreen_thenIExpectARowPerApp() {
    setContent(AuthenticatorAppsState(apps = APPS))

    for (app in APPS) {
      composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.SCROLLER).performScrollToNode(hasText(app.name))
      composeTestRule.onNodeWithText(app.name).assertIsDisplayed()
    }
  }

  @Test
  fun whenIClickAddAuthenticatorApp_thenIExpectAddAuthenticatorAppClickedEvent() {
    setContent(AuthenticatorAppsState())

    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.BUTTON_ADD)
      .assertIsDisplayed()
      .performClick()

    assertThat(events).contains(AuthenticatorAppsEvent.AddAuthenticatorAppClicked)
  }

  @Test
  fun givenApps_whenIClickRenameInTheMenu_thenIExpectRenameAppClickedEvent() {
    setContent(AuthenticatorAppsState(apps = APPS))

    scrollTo(AuthenticatorAppsTestTags.ROW_APP)
    composeTestRule.onAllNodesWithTag(AuthenticatorAppsTestTags.BUTTON_APP_MENU)[0].performClick()
    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.MENU_ITEM_RENAME).performClick()

    assertThat(events).contains(AuthenticatorAppsEvent.RenameAppClicked(appId = APPS[0].id))
  }

  @Test
  fun givenApps_whenIClickRemoveInTheMenu_thenIExpectRemoveAppClickedEvent() {
    setContent(AuthenticatorAppsState(apps = APPS))

    scrollTo(AuthenticatorAppsTestTags.ROW_APP)
    composeTestRule.onAllNodesWithTag(AuthenticatorAppsTestTags.BUTTON_APP_MENU)[0].performClick()
    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.MENU_ITEM_REMOVE).performClick()

    assertThat(events).contains(AuthenticatorAppsEvent.RemoveAppClicked(appId = APPS[0].id))
  }

  @Test
  fun givenTheConfirmRemoveDialog_whenIDisplayScreen_thenIExpectTheDialog() {
    setContent(AuthenticatorAppsState(apps = APPS, dialog = Dialog.ConfirmRemove(APPS[0].id)))

    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.DIALOG_CONFIRM_REMOVE).assertIsDisplayed()
  }

  @Test
  fun givenTheConfirmRemoveDialog_whenICancel_thenIExpectDialogDismissedEvent() {
    setContent(AuthenticatorAppsState(apps = APPS, dialog = Dialog.ConfirmRemove(APPS[0].id)))

    composeTestRule.onNodeWithTag(Dialogs.TEST_TAG_ALERT_DIALOG_DISMISS_BUTTON).performClick()

    assertThat(events).contains(AuthenticatorAppsEvent.DialogDismissed)
  }

  @Test
  fun givenTheMaxAppsDialog_whenIDisplayScreen_thenIExpectTheDialog() {
    setContent(AuthenticatorAppsState(apps = APPS, dialog = Dialog.MaxAppsReached))

    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.DIALOG_MAX_APPS_REACHED).assertIsDisplayed()
  }

  @Test
  fun givenTheMaxAppsDialog_whenIClickLearnMore_thenIExpectLearnMoreAndDismissEvents() {
    setContent(AuthenticatorAppsState(apps = APPS, dialog = Dialog.MaxAppsReached))

    composeTestRule.onNodeWithTag(Dialogs.TEST_TAG_ALERT_DIALOG_DISMISS_BUTTON).performClick()

    assertThat(events).contains(AuthenticatorAppsEvent.LearnMoreClicked)
    assertThat(events).contains(AuthenticatorAppsEvent.DialogDismissed)
  }

  private fun setContent(state: AuthenticatorAppsState) {
    composeTestRule.setContent {
      AuthenticatorAppsScreen(
        state = state,
        onEvent = { events += it }
      )
    }
  }

  private fun scrollTo(testTag: String) {
    composeTestRule.onNodeWithTag(AuthenticatorAppsTestTags.SCROLLER)
      .performScrollToNode(hasTestTag(testTag))
  }
}
