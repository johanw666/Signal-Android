/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

/**
 * One-shot side effects that need an Activity or the nav graph, and therefore have to be carried out by the fragment
 * hosting [PasskeysScreen] rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface PasskeysAction {

  /** Leave the screen. */
  data object NavigateBack : PasskeysAction

  /** Kick off creating a new passkey. */
  data object LaunchPasskeyCreation : PasskeysAction

  /** Send the user to a support article about passkeys. */
  data object OpenLearnMore : PasskeysAction
}
