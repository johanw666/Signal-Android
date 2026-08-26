/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorname

/**
 * One-shot side effects that need the nav graph, and therefore have to be carried out by the fragment hosting
 * [AuthenticatorNameScreen] rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface AuthenticatorNameAction {

  /** Leave the screen. */
  data object NavigateBack : AuthenticatorNameAction

  /** The app has a name now, so go back to the list of authenticator apps. */
  data object NavigateToAuthenticatorApps : AuthenticatorNameAction

  /** Tell the user their authenticator app was set up. */
  data object ShowAuthenticatorAppSetUp : AuthenticatorNameAction

  /** Tell the user their authenticator app was renamed. */
  data object ShowAuthenticatorAppRenamed : AuthenticatorNameAction
}
