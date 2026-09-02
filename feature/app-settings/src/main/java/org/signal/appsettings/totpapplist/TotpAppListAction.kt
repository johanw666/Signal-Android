/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpapplist

/**
 * One-shot side effects that need the nav graph, and therefore have to be carried out by the fragment hosting
 * [TotpAppListScreen] rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface TotpAppListAction {

  /** Leave the screen. */
  data object NavigateBack : TotpAppListAction

  /** Open the flow that pairs a new authenticator app. */
  data object NavigateToSetup : TotpAppListAction

  /** Open the screen that renames [app]. */
  data class NavigateToRename(val app: TotpApp) : TotpAppListAction

  /** Tell the user their authenticator app was removed. */
  data object ShowTotpAppRemoved : TotpAppListAction

  /** Tell the user the removal didn't go through, so they know the app is still on the account. */
  data object ShowRemovalFailed : TotpAppListAction

  /** Send the user to a support article about authenticator apps. */
  data object OpenLearnMore : TotpAppListAction
}
