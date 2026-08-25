/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

import org.signal.core.util.censor

/**
 * State for the screen where a user who already owns a Signal Login types in their account key to log in.
 *
 * [accountKey] holds the key without any of the formatting the user sees: the screen renders the dashes and
 * uppercasing itself, so what is stored here is always the raw lowercase value.
 */
data class SignalLoginState(
  val accountKey: String = "",
  val accountKeyError: AccountKeyError? = null,
  val isSubmitting: Boolean = false,
  val dialogs: Dialogs = Dialogs()
) {

  /** Whether the entered key is complete and well-formed enough to send to the service. */
  val isNextEnabled: Boolean
    get() = accountKey.length == ACCOUNT_KEY_LENGTH && accountKeyError == null && !isSubmitting

  override fun toString(): String = "SignalLoginState(accountKey=${accountKey.censor()}, accountKeyError=$accountKeyError, isSubmitting=$isSubmitting, dialogs=$dialogs)"

  data class Dialogs(
    val networkError: Boolean = false,
    val unknownError: Boolean = false
  )

  companion object {
    /** An account key is an ACI with its dashes removed, so it is always this many hex characters. */
    const val ACCOUNT_KEY_LENGTH = 32
  }
}

/** Why the entered account key can't be submitted. Shown beneath the text field rather than in a dialog. */
sealed interface AccountKeyError {
  /** More than [SignalLoginState.ACCOUNT_KEY_LENGTH] characters were entered. */
  data class TooLong(val count: Int) : AccountKeyError

  /** The entered text contains characters that can't appear in an account key. */
  data object Invalid : AccountKeyError

  /** The service didn't recognize the entered account key. */
  data object Incorrect : AccountKeyError
}
