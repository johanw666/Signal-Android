/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.passkeys

import org.signal.appsettings.passkeys.Passkey
import org.signal.appsettings.passkeys.PasskeysRepository

/**
 * Stand-in for wherever passkeys will eventually be read from. Nothing is fetched from the service yet, so the
 * passkeys are mocked.
 */
class AppPasskeysRepository : PasskeysRepository {

  companion object {
    private val MOCK_PASSKEYS = listOf(
      Passkey(id = 1, name = "My Security Key", createdAt = System.currentTimeMillis()),
      Passkey(id = 2, name = "My Pixel Phone", createdAt = System.currentTimeMillis())
    )
  }

  override fun getPasskeys(): List<Passkey> = MOCK_PASSKEYS
}
