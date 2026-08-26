/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorcodeentry

/**
 * One-shot side effects that need the nav graph, and therefore have to be carried out by the fragment hosting
 * [AuthenticatorCodeEntryScreen] rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface AuthenticatorCodeEntryAction {

  /** Leave the screen. */
  data object NavigateBack : AuthenticatorCodeEntryAction

  /** The new authenticator app is confirmed, so go name it. */
  data object NavigateToNaming : AuthenticatorCodeEntryAction

  /** The removal is done, so go back to the list of authenticator apps. */
  data object NavigateToAuthenticatorApps : AuthenticatorCodeEntryAction

  /** Tell the user their authenticator app was removed. */
  data object ShowAuthenticatorAppRemoved : AuthenticatorCodeEntryAction
}
