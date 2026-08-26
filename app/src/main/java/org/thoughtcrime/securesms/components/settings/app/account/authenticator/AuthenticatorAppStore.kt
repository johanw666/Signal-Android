/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import org.signal.appsettings.authenticatorapps.AuthenticatorApp

/**
 * Stand-in for wherever authenticator app state will eventually live. Nothing is persisted or sent to the service yet,
 * so all of this is mocked up and lasts only as long as the process does.
 */
object AuthenticatorAppStore {

  /** The key we'd hand off to an authenticator app, which the service will supply for real later on. */
  const val MOCK_SETUP_KEY = "KVZ7WL3FDDWJZMTOB7PLZPKVRFD4LYSX"

  /** How many authenticator apps an account is allowed, which the service will decide for real later on. */
  const val MAX_APPS = 2

  private val lock = Any()
  private val apps = mutableListOf<AuthenticatorApp>()
  private var nextId = 1L

  fun getApps(): List<AuthenticatorApp> = synchronized(lock) { apps.toList() }

  fun addApp(name: String, createdAt: Long): Long = synchronized(lock) {
    val id = nextId++
    apps += AuthenticatorApp(id = id, name = name, createdAt = createdAt)
    id
  }

  fun renameApp(id: Long, name: String) = synchronized(lock) {
    val index = apps.indexOfFirst { it.id == id }
    if (index >= 0) {
      apps[index] = apps[index].copy(name = name)
    }
  }

  fun removeApp(id: Long) = synchronized(lock) {
    apps.removeAll { it.id == id }
  }

  fun getApp(id: Long): AuthenticatorApp? = synchronized(lock) { apps.firstOrNull { it.id == id } }
}
