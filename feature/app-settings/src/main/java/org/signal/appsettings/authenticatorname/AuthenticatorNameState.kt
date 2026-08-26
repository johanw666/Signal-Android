/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorname

data class AuthenticatorNameState(
  val name: String = "",
  /** True when an already-configured app is being renamed, false when one is being named for the first time. */
  val renaming: Boolean = false,
  val submitting: Boolean = false
) {

  val canSubmit: Boolean
    get() = name.isNotBlank() && !submitting

  override fun toString(): String = "AuthenticatorNameState(nameLength=${name.length}, renaming=$renaming, submitting=$submitting)"
}
