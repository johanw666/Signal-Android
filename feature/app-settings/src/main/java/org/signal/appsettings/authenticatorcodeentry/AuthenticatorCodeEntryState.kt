/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorcodeentry

data class AuthenticatorCodeEntryState(
  val code: String = "",
  val submitting: Boolean = false,
  /** What the code is being collected for, which decides where the user goes once it's accepted. */
  val purpose: Purpose = Purpose.Add
) {

  val canSubmit: Boolean
    get() = code.length == CODE_LENGTH && !submitting

  override fun toString(): String = "AuthenticatorCodeEntryState(codeLength=${code.length}, submitting=$submitting, purpose=$purpose)"

  sealed interface Purpose {
    /** Confirming a newly paired authenticator app, which is then named. */
    data object Add : Purpose

    /** Confirming removal of the already-configured app identified by [appId]. */
    data class Remove(val appId: Long) : Purpose
  }

  companion object {
    const val CODE_LENGTH = 6
  }
}
