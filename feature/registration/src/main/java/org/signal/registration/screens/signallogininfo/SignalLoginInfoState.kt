/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogininfo

import org.signal.core.models.AccountEntropyPool
import org.signal.core.models.ServiceId
import org.signal.core.util.censor

/**
 * State for the screen that hands the user their newly-purchased Signal Login and asks them to save it.
 *
 * The credentials are shown masked on the card, with only a few trailing characters revealed, until the user opts
 * into seeing the full values.
 */
data class SignalLoginInfoState(
  val aci: ServiceId.ACI? = null,
  val recoveryKey: AccountEntropyPool? = null,
  val isPasswordManagerAvailable: Boolean = false,
  val showSpinner: Boolean = false,
  val dialogs: Dialogs = Dialogs()
) {
  /** Full account identifier, as it should be displayed to the user. */
  val accountDisplay: String
    get() = aci?.toString()?.uppercase().orEmpty()

  /** Full recovery key, as it should be displayed to the user. */
  val recoveryDisplay: String
    get() = recoveryKey?.displayValue.orEmpty()

  override fun toString(): String = "SignalLoginInfoState(aci=${aci?.logString()}, recoveryKey=${recoveryKey?.value?.censor()}, " +
    "isPasswordManagerAvailable=$isPasswordManagerAvailable, showSpinner=$showSpinner, dialogs=$dialogs)"

  data class Dialogs(
    /** Shows the full, unmasked credentials. */
    val credentialDetails: Boolean = false,
    val saveFailed: Boolean = false,
    val unknownError: Boolean = false
  )
}
