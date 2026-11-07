/*
 * Copyright 2026 JW
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.chats

/**
 * One-shot side effects that need an Activity, and therefore have to be carried out by the host rather than the screen
 * itself. Navigation doesn't come through here: the view model sends it straight to its parent.
 *
 * Actions are logged, so be sure `toString()` contains nothing sensitive.
 */
sealed interface ChatsSettingsAction {

  /** Open the flow to change the legacy passphrase. */
  data object LaunchChatBackupsLocationApi30 : ChatsSettingsAction
}
