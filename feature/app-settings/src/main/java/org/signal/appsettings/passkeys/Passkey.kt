/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

/**
 * A single passkey on the user's account, as shown on [PasskeysScreen].
 */
data class Passkey(
  val id: Long,
  val name: String,
  /** When the passkey was added, in epoch milliseconds. */
  val createdAt: Long
) {
  override fun toString(): String = "Passkey(id=$id)"
}
