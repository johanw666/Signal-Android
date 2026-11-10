/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.chats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.signal.appsettings.chats.ChatsSettingsScreen
import org.signal.appsettings.routes.AppSettingsNavigationEvent
import org.signal.core.ui.compose.CollectActions
// JW: added
import org.signal.appsettings.chats.ChatsSettingsAction

/**
 * Hosts [ChatsSettingsScreen]. The view model navigates by sending events to [parentEventEmitter], so this works from
 * whichever host owns the app settings back stack.
 */
@Composable
fun ChatsSettingsEntry(
  parentEventEmitter: (AppSettingsNavigationEvent) -> Unit
) {
  val viewModel: ChatsSettingsViewModel = viewModel { ChatsSettingsViewModel(parentEventEmitter) }
  val state by viewModel.state.collectAsStateWithLifecycle()

  // JW: added
  CollectActions(viewModel.actions) { action ->
    when (action) {
      ChatsSettingsAction.LaunchChatBackupsLocationApi30 -> ChatsSettingsFragment.setChatBackupLocationApi30()
    }
  }
  // JW end

  ChatsSettingsScreen(
    state = state,
    onEvent = viewModel::onEvent
  )
}

