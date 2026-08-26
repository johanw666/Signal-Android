/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorapps

data class AuthenticatorAppsState(
  /** The authenticator apps configured on the account. When empty, the list section says so instead of listing rows. */
  val apps: List<AuthenticatorApp> = emptyList(),
  /** How many authenticator apps the account is allowed to have at once. */
  val maxApps: Int = 0,
  val dialog: Dialog = Dialog.None
) {

  val atMaxApps: Boolean
    get() = apps.size >= maxApps

  /** Whichever dialog the screen is showing, if any. Only one is ever up at a time. */
  sealed interface Dialog {
    data object None : Dialog

    /** Confirms removing [appId], which still has to be backed up by a code from the app itself. */
    data class ConfirmRemove(val appId: Long) : Dialog

    /** Explains that the account already has as many authenticator apps as it's allowed. */
    data object MaxAppsReached : Dialog
  }
}
