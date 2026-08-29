/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.totpentry

/**
 * Everything [TotpEntryScreen] needs to render.
 */
data class TotpEntryState(
  val digits: List<String> = emptyDigits(),
  val focusedDigitIndex: Int = 0
) {

  override fun toString(): String = "TotpEntryState(digitsEntered=${digits.count { it.isNotEmpty() }}, focusedDigitIndex=$focusedDigitIndex)"

  /**
   * The full code as currently entered. Only meaningful when [isComplete] is true.
   */
  val code: String get() = digits.joinToString("")

  val isComplete: Boolean get() = digits.size == CODE_LENGTH && digits.all { it.isNotEmpty() }

  companion object {
    const val CODE_LENGTH = 6

    fun emptyDigits(): List<String> = List(CODE_LENGTH) { "" }
  }
}
