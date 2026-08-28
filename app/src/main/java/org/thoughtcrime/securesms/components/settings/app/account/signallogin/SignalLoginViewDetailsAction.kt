/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.signallogin

/**
 * One-shot side effects that need an Activity or the nav graph, and therefore have to be carried out by
 * [SignalLoginViewDetailsFragment] rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface SignalLoginViewDetailsAction {

  /** Leave the screen. */
  data object NavigateBack : SignalLoginViewDetailsAction

  /** Launch the system document picker so the user can choose where to save the login PDF. */
  data object LaunchSaveAsPdf : SignalLoginViewDetailsAction
}
