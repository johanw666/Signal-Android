/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

import org.signal.core.util.censor

sealed class SignalLoginScreenEvents {
  /** The user tapped the back arrow. */
  data object BackClicked : SignalLoginScreenEvents()

  /** The user edited the account key field. Carries the raw text, formatting and all. */
  data class AccountKeyChanged(val value: String) : SignalLoginScreenEvents() {
    override fun toString(): String = "AccountKeyChanged(value=${value.censor()})"
  }

  /** The user tapped "Need help?". */
  data object NeedHelpClicked : SignalLoginScreenEvents()

  /** The user submitted the account key, either with the next button or the keyboard's next action. */
  data object NextClicked : SignalLoginScreenEvents()

  /** The user dismissed the network error dialog. */
  data object NetworkErrorDialogDismissed : SignalLoginScreenEvents()

  /** The user dismissed the unknown error dialog. */
  data object UnknownErrorDialogDismissed : SignalLoginScreenEvents()
}
