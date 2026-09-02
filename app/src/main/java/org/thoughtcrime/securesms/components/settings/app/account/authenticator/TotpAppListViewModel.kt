/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.signal.appsettings.totpapplist.TotpAppListAction
import org.signal.appsettings.totpapplist.TotpAppListEvent
import org.signal.appsettings.totpapplist.TotpAppListState
import org.signal.appsettings.totpapplist.TotpAppListState.Dialog
import org.signal.appsettings.totpapplist.TotpAppListState.LoadState
import org.signal.core.ui.compose.EventDrivenViewModel
import org.signal.core.util.logging.Log

/**
 * Drives the screen that lists the authenticator apps on the account.
 */
class TotpAppListViewModel(
  private val repository: TotpRepository = TotpRepository()
) : EventDrivenViewModel<TotpAppListEvent>(TAG) {

  companion object {
    private val TAG = Log.tag(TotpAppListViewModel::class)
  }

  private val _state = MutableStateFlow(TotpAppListState(maxApps = repository.getMaxApps()))
  private val _actions = Channel<TotpAppListAction>(Channel.BUFFERED)

  val state: StateFlow<TotpAppListState> = _state.asStateFlow()
  val actions: Flow<TotpAppListAction> = _actions.receiveAsFlow()

  init {
    viewModelScope.launch { refresh() }
  }

  override suspend fun processEvent(event: TotpAppListEvent) {
    when (event) {
      TotpAppListEvent.ScreenResumed -> {
        refresh()
      }
      TotpAppListEvent.NavigateBackClicked -> {
        _actions.send(TotpAppListAction.NavigateBack)
      }
      TotpAppListEvent.AddTotpAppClicked -> {
        if (_state.value.atMaxApps) {
          _state.update { it.copy(dialog = Dialog.MaxAppsReached) }
        } else {
          _actions.send(TotpAppListAction.NavigateToSetup)
        }
      }
      TotpAppListEvent.LearnMoreClicked -> {
        _actions.send(TotpAppListAction.OpenLearnMore)
      }
      is TotpAppListEvent.RenameAppClicked -> {
        val app = _state.value.apps.firstOrNull { it.id == event.appId }
        if (app == null) {
          Log.w(TAG, "Asked to rename an app that isn't in the list.")
        } else {
          _actions.send(TotpAppListAction.NavigateToRename(app))
        }
      }
      is TotpAppListEvent.RemoveAppClicked -> {
        _state.update { it.copy(dialog = Dialog.ConfirmRemove(event.appId)) }
      }
      is TotpAppListEvent.RemoveAppConfirmed -> {
        _state.update { it.copy(dialog = Dialog.None) }
        removeApp(event.appId)
      }
      TotpAppListEvent.DialogDismissed -> {
        _state.update { it.copy(dialog = Dialog.None) }
      }
    }
  }

  private suspend fun removeApp(appId: Long) {
    when (repository.removeTotpApp(appId)) {
      TotpRepository.UpdateResult.Success, TotpRepository.UpdateResult.AppNotFound -> {
        _actions.send(TotpAppListAction.ShowTotpAppRemoved)
        refresh()
      }
      TotpRepository.UpdateResult.NetworkFailure -> {
        Log.w(TAG, "Couldn't remove the authenticator app. Leaving it in the list, where it still is.")
        _actions.send(TotpAppListAction.ShowRemovalFailed)
      }
    }
  }

  private suspend fun refresh() {
    when (val result = repository.getTotpApps()) {
      is TotpRepository.AppsResult.Success -> {
        _state.update { it.copy(apps = result.apps, loadState = LoadState.LOADED) }
      }
      TotpRepository.AppsResult.NetworkFailure -> {
        Log.w(TAG, "Couldn't reach the service to list authenticator apps.")
        _state.update { it.copy(apps = emptyList(), loadState = LoadState.NETWORK_FAILURE) }
      }
    }
  }
}
