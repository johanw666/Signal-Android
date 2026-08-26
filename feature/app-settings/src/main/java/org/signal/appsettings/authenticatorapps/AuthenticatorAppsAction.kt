/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorapps

/**
 * One-shot side effects that need the nav graph, and therefore have to be carried out by the fragment hosting
 * [AuthenticatorAppsScreen] rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface AuthenticatorAppsAction {

  /** Leave the screen. */
  data object NavigateBack : AuthenticatorAppsAction

  /** Open the flow that pairs a new authenticator app. */
  data object NavigateToSetup : AuthenticatorAppsAction

  /** Open the screen that renames [appId]. */
  data class NavigateToRename(val appId: Long) : AuthenticatorAppsAction

  /** Collect a code from [appId] before it's removed. */
  data class NavigateToRemovalCodeEntry(val appId: Long) : AuthenticatorAppsAction

  /** Send the user to a support article about authenticator apps. */
  data object OpenLearnMore : AuthenticatorAppsAction
}
