/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogindetails

sealed interface SignalLoginViewDetailsScreenActions {
  /** Launch the system credential manager UI so the user can store the login in their password manager. */
  data object LaunchSaveToPasswordManager : SignalLoginViewDetailsScreenActions

  /** Launch the system document picker so the user can choose where to save the login PDF. */
  data object LaunchSaveAsPdf : SignalLoginViewDetailsScreenActions
}
