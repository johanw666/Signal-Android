/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpapplist

/**
 * A single authenticator app configured on the user's account, as shown on [TotpAppListScreen].
 */
data class TotpApp(
  val id: Long,
  val name: String,
  /** When the app was configured, in epoch milliseconds. */
  val createdAt: Long
) {
  override fun toString(): String = "TotpApp(id=$id)"
}
