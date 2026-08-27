/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogininfo

import org.signal.registration.RegistrationFlowState

sealed class SignalLoginInfoScreenEvents {
  /** The parent registration flow state changed. */
  data class ParentStateChanged(val parentState: RegistrationFlowState) : SignalLoginInfoScreenEvents()

  /** The user tapped "view details" on the credential card to reveal the full values. */
  data object ViewDetailsClicked : SignalLoginInfoScreenEvents()

  /** The user chose to store the credentials with the system password manager. */
  data object SaveToPasswordManagerClicked : SignalLoginInfoScreenEvents()

  /** The user chose to record the credentials themselves rather than using a password manager. */
  data object SaveManuallyClicked : SignalLoginInfoScreenEvents()

  /** The user dismissed the failed-save dialog. */
  data object SaveFailedDialogDismissed : SignalLoginInfoScreenEvents()

  /** The user dismissed the unknown error dialog. */
  data object UnknownErrorDialogDismissed : SignalLoginInfoScreenEvents()
}
