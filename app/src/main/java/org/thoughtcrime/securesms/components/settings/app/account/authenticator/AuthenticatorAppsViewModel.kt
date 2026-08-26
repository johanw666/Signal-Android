/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsAction
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsEvent
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsState
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsState.Dialog
import org.signal.core.ui.compose.EventDrivenViewModel
import org.signal.core.util.logging.Log

/**
 * Drives the screen that lists the authenticator apps on the account.
 */
class AuthenticatorAppsViewModel(
  private val repository: AuthenticatorRepository = AuthenticatorRepository()
) : EventDrivenViewModel<AuthenticatorAppsEvent>(TAG) {

  companion object {
    private val TAG = Log.tag(AuthenticatorAppsViewModel::class)
  }

  private val _state = MutableStateFlow(AuthenticatorAppsState(maxApps = repository.getMaxApps()))
  private val _actions = Channel<AuthenticatorAppsAction>(Channel.BUFFERED)

  val state: StateFlow<AuthenticatorAppsState> = _state.asStateFlow()
  val actions: Flow<AuthenticatorAppsAction> = _actions.receiveAsFlow()

  init {
    refresh()
  }

  override suspend fun processEvent(event: AuthenticatorAppsEvent) {
    when (event) {
      AuthenticatorAppsEvent.ScreenResumed -> {
        refresh()
      }
      AuthenticatorAppsEvent.NavigateBackClicked -> {
        _actions.send(AuthenticatorAppsAction.NavigateBack)
      }
      AuthenticatorAppsEvent.AddAuthenticatorAppClicked -> {
        if (_state.value.atMaxApps) {
          _state.update { it.copy(dialog = Dialog.MaxAppsReached) }
        } else {
          _actions.send(AuthenticatorAppsAction.NavigateToSetup)
        }
      }
      AuthenticatorAppsEvent.LearnMoreClicked -> {
        _actions.send(AuthenticatorAppsAction.OpenLearnMore)
      }
      is AuthenticatorAppsEvent.RenameAppClicked -> {
        _actions.send(AuthenticatorAppsAction.NavigateToRename(event.appId))
      }
      is AuthenticatorAppsEvent.RemoveAppClicked -> {
        _state.update { it.copy(dialog = Dialog.ConfirmRemove(event.appId)) }
      }
      AuthenticatorAppsEvent.RemoveAppConfirmed -> {
        val dialog = _state.value.dialog as? Dialog.ConfirmRemove ?: return
        _state.update { it.copy(dialog = Dialog.None) }
        _actions.send(AuthenticatorAppsAction.NavigateToRemovalCodeEntry(dialog.appId))
      }
      AuthenticatorAppsEvent.DialogDismissed -> {
        _state.update { it.copy(dialog = Dialog.None) }
      }
    }
  }

  private fun refresh() {
    _state.update { it.copy(apps = repository.getAuthenticatorApps()) }
  }
}
