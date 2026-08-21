/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

/**
 * Where [PasskeysScreen] reads passkeys from.
 */
interface PasskeysRepository {

  fun getPasskeys(): List<Passkey>
}
