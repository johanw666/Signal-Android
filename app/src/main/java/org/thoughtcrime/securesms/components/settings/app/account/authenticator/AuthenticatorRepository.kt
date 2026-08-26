/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import org.signal.appsettings.authenticatorapps.AuthenticatorApp

class AuthenticatorRepository {

  fun getSetupKey(): String = AuthenticatorAppStore.MOCK_SETUP_KEY

  fun getMaxApps(): Int = AuthenticatorAppStore.MAX_APPS

  fun getAuthenticatorApps(): List<AuthenticatorApp> = AuthenticatorAppStore.getApps()

  fun getAuthenticatorApp(id: Long): AuthenticatorApp? = AuthenticatorAppStore.getApp(id)

  fun addAuthenticatorApp(name: String): Long = AuthenticatorAppStore.addApp(name, System.currentTimeMillis())

  fun renameAuthenticatorApp(id: Long, name: String) = AuthenticatorAppStore.renameApp(id, name)

  fun removeAuthenticatorApp(id: Long) = AuthenticatorAppStore.removeApp(id)
}
