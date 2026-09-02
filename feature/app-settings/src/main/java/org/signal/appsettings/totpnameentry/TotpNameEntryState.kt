/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpnameentry

data class TotpNameEntryState(
  /**
   * The name so far, already capped in grapheme clusters and in UTF-8 bytes by whoever fed it in. Nothing here needs to
   * know either limit: an over-length name is never a state this screen has to render or explain, because the field
   * simply stops accepting one.
   */
  val name: String = "",
  /** True when an already-configured app is being renamed, false when one is being named for the first time. */
  val renaming: Boolean = false,
  val submitting: Boolean = false
) {

  val canSubmit: Boolean
    get() = name.isNotBlank() && !submitting

  override fun toString(): String = "TotpNameEntryState(nameLength=${name.length}, renaming=$renaming, submitting=$submitting)"
}
