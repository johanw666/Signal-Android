/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

data class PasskeysState(
  /** The passkeys on the account. When empty, the screen explains passkeys instead of listing them. */
  val passkeys: List<Passkey> = emptyList()
)
