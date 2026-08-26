/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorapps

/**
 * Reminder that these events are logged, so don't include anything sensitive in the toString.
 */
sealed interface AuthenticatorAppsEvent {

  /** The screen came back to the foreground, so the list we read out of storage may be stale. */
  data object ScreenResumed : AuthenticatorAppsEvent

  /** The user tapped the navigation (back) icon. */
  data object NavigateBackClicked : AuthenticatorAppsEvent

  /** The user tapped the button that starts setting up another authenticator app. */
  data object AddAuthenticatorAppClicked : AuthenticatorAppsEvent

  /** The user tapped the learn more link. */
  data object LearnMoreClicked : AuthenticatorAppsEvent

  /** The user tapped the rename option in an app's overflow menu. */
  data class RenameAppClicked(val appId: Long) : AuthenticatorAppsEvent

  /** The user tapped the remove option in an app's overflow menu, which asks them to confirm first. */
  data class RemoveAppClicked(val appId: Long) : AuthenticatorAppsEvent

  /** The user confirmed removing the app named in [AuthenticatorAppsState.Dialog.ConfirmRemove]. */
  data object RemoveAppConfirmed : AuthenticatorAppsEvent

  /** Dismisses whatever is in [AuthenticatorAppsState.dialog]. */
  data object DialogDismissed : AuthenticatorAppsEvent
}
