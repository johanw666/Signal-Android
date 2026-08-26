/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorapps

/**
 * A single authenticator app configured on the user's account, as shown on [AuthenticatorAppsScreen].
 */
data class AuthenticatorApp(
  val id: Long,
  val name: String,
  /** When the app was configured, in epoch milliseconds. */
  val createdAt: Long
) {
  override fun toString(): String = "AuthenticatorApp(id=$id)"
}
