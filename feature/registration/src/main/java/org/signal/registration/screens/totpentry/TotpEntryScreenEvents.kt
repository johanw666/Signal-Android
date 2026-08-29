/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.totpentry

/**
 * Reminder that these events are logged, so don't include anything sensitive in the toString.
 */
sealed class TotpEntryScreenEvents {

  /**
   * The raw [value] of the digit field at [index] changed. The view model interprets it: a single digit is recorded,
   * an empty [value] is a backspace (deleting a digit and shifting the following ones left), and multi-character
   * input (e.g. a pasted code) populates every field at once.
   */
  data class DigitChanged(val index: Int, val value: String) : TotpEntryScreenEvents() {
    override fun toString(): String = "DigitChanged(index=$index)"
  }

  /** The user tapped the cancel button. */
  data object CancelClicked : TotpEntryScreenEvents()
}
