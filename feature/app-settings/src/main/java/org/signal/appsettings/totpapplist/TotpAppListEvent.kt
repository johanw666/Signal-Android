/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpapplist

/**
 * Reminder that these events are logged, so don't include anything sensitive in the toString.
 */
sealed interface TotpAppListEvent {

  /** The screen came back to the foreground, so the list we read out of storage may be stale. */
  data object ScreenResumed : TotpAppListEvent

  /** The user tapped the navigation (back) icon. */
  data object NavigateBackClicked : TotpAppListEvent

  /** The user tapped the button that starts setting up another authenticator app. */
  data object AddTotpAppClicked : TotpAppListEvent

  /** The user tapped the learn more link. */
  data object LearnMoreClicked : TotpAppListEvent

  /** The user tapped the rename option in an app's overflow menu. */
  data class RenameAppClicked(val appId: Long) : TotpAppListEvent

  /** The user tapped the remove option in an app's overflow menu, which asks them to confirm first. */
  data class RemoveAppClicked(val appId: Long) : TotpAppListEvent

  /** The user confirmed removing the app, which removes it. */
  data class RemoveAppConfirmed(val appId: Long) : TotpAppListEvent

  /** Dismisses whatever is in [TotpAppListState.dialog]. */
  data object DialogDismissed : TotpAppListEvent
}
