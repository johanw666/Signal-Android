/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogincredentials

import org.signal.core.util.censor
import org.signal.registration.screens.aepentry.AepInput
import org.signal.registration.screens.shared.AccountIdError
import org.signal.registration.screens.shared.AccountIdFormat

/**
 * State for the screen where a user who already owns a Signal Login types both halves of it in: the account ID and the
 * recovery key that pairs with it.
 *
 * [accountId] holds the ID without any of the formatting the user sees: the screen renders the dashes and the
 * uppercasing itself, so what is stored here is always the raw lowercase value.
 */
data class SignalLoginCredentialEntryState(
  val accountId: String = "",
  val accountIdError: AccountIdError? = null,
  val recoveryKey: AepInput = AepInput(),
  /** Whether the recovery key is spelled out rather than masked like a password. */
  val isRecoveryKeyRevealed: Boolean = false,
  /** The service rejected the pair. Either half could be at fault, so both fields are flagged rather than just one. */
  val areCredentialsIncorrect: Boolean = false,
  val isLoggingIn: Boolean = false,
  val loginError: SignalLoginError? = null
) {

  /** Whether both halves of the login are complete and well-formed enough to attempt. */
  val isNextEnabled: Boolean
    get() = accountId.length == AccountIdFormat.ACCOUNT_ID_LENGTH &&
      accountIdError == null &&
      recoveryKey.isValid &&
      recoveryKey.error == null &&
      !areCredentialsIncorrect &&
      !isLoggingIn

  override fun toString(): String = "SignalLoginCredentialEntryState(accountId=${accountId.censor()}, accountIdError=$accountIdError, recoveryKey=$recoveryKey, isRecoveryKeyRevealed=$isRecoveryKeyRevealed, areCredentialsIncorrect=$areCredentialsIncorrect, isLoggingIn=$isLoggingIn, loginError=$loginError)"
}

/** A login failure that the text fields can't express, so it gets a dialog instead. */
sealed interface SignalLoginError {
  data object RateLimited : SignalLoginError
  data object NetworkError : SignalLoginError
  data object UnknownError : SignalLoginError
}
