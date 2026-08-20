/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.signallogin.viewdetails

import org.signal.core.util.censor

/**
 * State for the screen that shows the user the full keys that make up their Signal Login.
 */
data class SignalLoginViewDetailsState(
  val accountKey: String = "",
  val recoveryKey: String = ""
) {
  companion object {
    private const val GROUP_SIZE = 4
  }

  /** The recovery key broken into character groups, in display order. */
  val recoveryKeyGroups: List<String>
    get() = recoveryKey.chunked(GROUP_SIZE)

  override fun toString(): String = "SignalLoginViewDetailsState(accountKey=${accountKey.censor()}, recoveryKey=${recoveryKey.censor()})"
}
