/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.totpentry

import org.signal.core.util.censor

/**
 * One-shot side effects that have to be carried out by the host rather than the screen itself.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface TotpEntryAction {

  /** The user cancelled code entry. Leave the screen. */
  data object NavigateBack : TotpEntryAction

  /** All six digits have been entered. The host should verify [code]. */
  data class CodeEntered(val code: String) : TotpEntryAction {
    override fun toString(): String = "CodeEntered(code=${code.censor()})"
  }
}
