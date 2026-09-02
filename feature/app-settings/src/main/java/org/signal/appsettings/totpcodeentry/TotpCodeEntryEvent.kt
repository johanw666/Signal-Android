/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpcodeentry

/**
 * Reminder that these events are logged, so don't include anything sensitive in the toString.
 */
sealed interface TotpCodeEntryEvent {

  /** The user tapped the navigation (back) icon. */
  data object NavigateBackClicked : TotpCodeEntryEvent

  /** The user typed in the code field. */
  data class CodeChanged(val code: String) : TotpCodeEntryEvent {
    override fun toString(): String = "CodeChanged(length=${code.length})"
  }

  /** The user submitted the code they entered. */
  data object DoneClicked : TotpCodeEntryEvent
}
