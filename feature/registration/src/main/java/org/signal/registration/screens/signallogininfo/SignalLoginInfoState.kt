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
  val aep: AccountEntropyPool? = null,
  val isPasswordManagerAvailable: Boolean = false,
  val showSpinner: Boolean = false,
  val dialogs: Dialogs = Dialogs()
) {
  override fun toString(): String = "SignalLoginInfoState(aci=${aci?.logString()}, aep=${aep?.value?.censor()}, " +
    "isPasswordManagerAvailable=$isPasswordManagerAvailable, showSpinner=$showSpinner, dialogs=$dialogs)"

  data class Dialogs(
    val saveFailed: Boolean = false,
    val unknownError: Boolean = false
  )
}
