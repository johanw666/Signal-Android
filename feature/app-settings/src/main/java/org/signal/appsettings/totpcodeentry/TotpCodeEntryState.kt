/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpcodeentry

data class TotpCodeEntryState(
  val code: String = "",
  val submitting: Boolean = false,
  /** Why the last submission didn't work, shown under the code field and cleared as soon as the user types. */
  val error: Error = Error.None
) {

  val canSubmit: Boolean
    get() = code.length == CODE_LENGTH && !submitting

  override fun toString(): String = "TotpCodeEntryState(codeLength=${code.length}, submitting=$submitting, error=$error)"

  sealed interface Error {
    data object None : Error

    /** The service rejected the code, and we have no reason to think it was anything but a wrong code. */
    data object IncorrectCode : Error

    data object NetworkFailure : Error
  }

  companion object {
    const val CODE_LENGTH = 6
  }
}
