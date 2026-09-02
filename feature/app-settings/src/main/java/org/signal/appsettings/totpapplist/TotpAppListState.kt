/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpapplist

data class TotpAppListState(
  /** The authenticator apps configured on the account, which only means anything once [loadState] is [LoadState.LOADED]. */
  val apps: List<TotpApp> = emptyList(),
  /** How many authenticator apps the account is allowed to have at once. */
  val maxApps: Int = 0,
  /** How the last look at the account went, which decides what the list section shows in place of rows. */
  val loadState: LoadState = LoadState.LOADING,
  val dialog: Dialog = Dialog.None
) {

  val atMaxApps: Boolean
    get() = apps.size >= maxApps

  /** How the last attempt to read the account's authenticator apps went, since an empty list can't say on its own. */
  enum class LoadState {
    /** We haven't heard back about the account yet. */
    LOADING,

    LOADED,

    /** We couldn't reach the service, which is worth another try. */
    NETWORK_FAILURE
  }

  /** Whichever dialog the screen is showing, if any. Only one is ever up at a time. */
  sealed interface Dialog {
    data object None : Dialog

    /** Confirms removing [appId], which still has to be backed up by a code from the app itself. */
    data class ConfirmRemove(val appId: Long) : Dialog

    /** Explains that the account already has as many authenticator apps as it's allowed. */
    data object MaxAppsReached : Dialog
  }
}
