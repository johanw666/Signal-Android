/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpapplist

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
import org.signal.appsettings.totpapplist.TotpAppListState.Dialog
import org.signal.appsettings.totpapplist.TotpAppListState.LoadState
import org.signal.core.ui.compose.Dialogs

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TotpAppListScreenTest {

  companion object {
    private val APPS = listOf(
      TotpApp(id = 1, name = "Bitwarden Authenticator", createdAt = System.currentTimeMillis()),
      TotpApp(id = 2, name = "Twilio Authy", createdAt = System.currentTimeMillis())
    )
  }

  @get:Rule
  val composeTestRule = createComposeRule()

  private val events = mutableListOf<TotpAppListEvent>()

  @Test
  fun givenNoApps_whenIDisplayScreen_thenIExpectTheEmptyMessage() {
    setContent(TotpAppListState(loadState = LoadState.LOADED))

    scrollTo(TotpAppListTestTags.EMPTY_MESSAGE)
    composeTestRule.onNodeWithTag(TotpAppListTestTags.EMPTY_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun givenApps_whenIDisplayScreen_thenIExpectARowPerApp() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED))

    for (app in APPS) {
      composeTestRule.onNodeWithTag(TotpAppListTestTags.SCROLLER).performScrollToNode(hasText(app.name))
      composeTestRule.onNodeWithText(app.name).assertIsDisplayed()
    }
  }

  @Test
  fun whenIClickAddTotpApp_thenIExpectAddTotpAppClickedEvent() {
    setContent(TotpAppListState(loadState = LoadState.LOADED))

    composeTestRule.onNodeWithTag(TotpAppListTestTags.BUTTON_ADD)
      .assertIsDisplayed()
      .performClick()

    assertThat(events).contains(TotpAppListEvent.AddTotpAppClicked)
  }

  @Test
  fun givenApps_whenIClickRenameInTheMenu_thenIExpectRenameAppClickedEvent() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED))

    scrollTo(TotpAppListTestTags.ROW_APP)
    composeTestRule.onAllNodesWithTag(TotpAppListTestTags.BUTTON_APP_MENU)[0].performClick()
    composeTestRule.onNodeWithTag(TotpAppListTestTags.MENU_ITEM_RENAME).performClick()

    assertThat(events).contains(TotpAppListEvent.RenameAppClicked(appId = APPS[0].id))
  }

  @Test
  fun givenApps_whenIClickRemoveInTheMenu_thenIExpectRemoveAppClickedEvent() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED))

    scrollTo(TotpAppListTestTags.ROW_APP)
    composeTestRule.onAllNodesWithTag(TotpAppListTestTags.BUTTON_APP_MENU)[0].performClick()
    composeTestRule.onNodeWithTag(TotpAppListTestTags.MENU_ITEM_REMOVE).performClick()

    assertThat(events).contains(TotpAppListEvent.RemoveAppClicked(appId = APPS[0].id))
  }

  @Test
  fun givenTheConfirmRemoveDialog_whenIDisplayScreen_thenIExpectTheDialog() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED, dialog = Dialog.ConfirmRemove(APPS[0].id)))

    composeTestRule.onNodeWithTag(TotpAppListTestTags.DIALOG_CONFIRM_REMOVE).assertIsDisplayed()
  }

  /** The event has to carry the id, since the dialog holding it is dismissed before the confirmation is reported. */
  @Test
  fun givenTheConfirmRemoveDialog_whenIConfirm_thenIExpectRemoveAppConfirmedForThatApp() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED, dialog = Dialog.ConfirmRemove(APPS[1].id)))

    composeTestRule.onNodeWithTag(Dialogs.TEST_TAG_ALERT_DIALOG_CONFIRM_BUTTON).performClick()

    assertThat(events).contains(TotpAppListEvent.RemoveAppConfirmed(APPS[1].id))
  }

  @Test
  fun givenTheConfirmRemoveDialog_whenICancel_thenIExpectDialogDismissedEvent() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED, dialog = Dialog.ConfirmRemove(APPS[0].id)))

    composeTestRule.onNodeWithTag(Dialogs.TEST_TAG_ALERT_DIALOG_DISMISS_BUTTON).performClick()

    assertThat(events).contains(TotpAppListEvent.DialogDismissed)
  }

  @Test
  fun givenTheMaxAppsDialog_whenIDisplayScreen_thenIExpectTheDialog() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED, dialog = Dialog.MaxAppsReached))

    composeTestRule.onNodeWithTag(TotpAppListTestTags.DIALOG_MAX_APPS_REACHED).assertIsDisplayed()
  }

  @Test
  fun givenTheMaxAppsDialog_whenIClickLearnMore_thenIExpectLearnMoreAndDismissEvents() {
    setContent(TotpAppListState(apps = APPS, loadState = LoadState.LOADED, dialog = Dialog.MaxAppsReached))

    composeTestRule.onNodeWithTag(Dialogs.TEST_TAG_ALERT_DIALOG_DISMISS_BUTTON).performClick()

    assertThat(events).contains(TotpAppListEvent.LearnMoreClicked)
    assertThat(events).contains(TotpAppListEvent.DialogDismissed)
  }

  @Test
  fun whenTheListHasntArrived_thenIExpectASpinnerRatherThanNoTotpApps() {
    setContent(TotpAppListState(loadState = LoadState.LOADING))

    scrollTo(TotpAppListTestTags.LOADING)
    composeTestRule.onNodeWithTag(TotpAppListTestTags.LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TotpAppListTestTags.EMPTY_MESSAGE).assertDoesNotExist()
  }

  /** An account we couldn't ask about is not an account with no authenticator apps. */
  @Test
  fun givenTheListCouldntBeLoaded_whenIDisplayScreen_thenIExpectTheFailureMessageRatherThanTheEmptyOne() {
    setContent(TotpAppListState(loadState = LoadState.NETWORK_FAILURE))

    scrollTo(TotpAppListTestTags.LOAD_FAILED_MESSAGE)
    composeTestRule.onNodeWithTag(TotpAppListTestTags.LOAD_FAILED_MESSAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TotpAppListTestTags.EMPTY_MESSAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TotpAppListTestTags.LOADING).assertDoesNotExist()
  }

  private fun setContent(state: TotpAppListState) {
    composeTestRule.setContent {
      TotpAppListScreen(
        state = state,
        onEvent = { events += it }
      )
    }
  }

  private fun scrollTo(testTag: String) {
    composeTestRule.onNodeWithTag(TotpAppListTestTags.SCROLLER)
      .performScrollToNode(hasTestTag(testTag))
  }
}
