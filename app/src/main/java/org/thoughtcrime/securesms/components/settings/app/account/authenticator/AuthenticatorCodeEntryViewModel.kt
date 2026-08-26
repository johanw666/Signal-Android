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
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryAction
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryEvent
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryState
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryState.Purpose
import org.signal.core.ui.compose.EventDrivenViewModel
import org.signal.core.util.logging.Log

/**
 * Drives the screen that collects a code from the user's authenticator app, which is required both to confirm a newly
 * paired app and to remove one that already exists. There's nothing to verify the code against yet, so any code of the
 * right length is treated as correct.
 */
class AuthenticatorCodeEntryViewModel(
  purpose: Purpose = Purpose.Add,
  private val repository: AuthenticatorRepository = AuthenticatorRepository()
) : EventDrivenViewModel<AuthenticatorCodeEntryEvent>(TAG) {

  companion object {
    private val TAG = Log.tag(AuthenticatorCodeEntryViewModel::class)
  }

  private val _state = MutableStateFlow(AuthenticatorCodeEntryState(purpose = purpose))
  private val _actions = Channel<AuthenticatorCodeEntryAction>(Channel.BUFFERED)

  val state: StateFlow<AuthenticatorCodeEntryState> = _state.asStateFlow()
  val actions: Flow<AuthenticatorCodeEntryAction> = _actions.receiveAsFlow()

  override suspend fun processEvent(event: AuthenticatorCodeEntryEvent) {
    when (event) {
      AuthenticatorCodeEntryEvent.NavigateBackClicked -> {
        _actions.send(AuthenticatorCodeEntryAction.NavigateBack)
      }
      is AuthenticatorCodeEntryEvent.CodeChanged -> {
        val digits = event.code.filter { it.isDigit() }.take(AuthenticatorCodeEntryState.CODE_LENGTH)
        _state.update { it.copy(code = digits) }
      }
      AuthenticatorCodeEntryEvent.DoneClicked -> {
        if (!_state.value.canSubmit) {
          return
        }

        Log.i(TAG, "Accepting the entered code without verifying it, which is all we can do until this is wired up.")
        _state.update { it.copy(submitting = true) }

        when (val purpose = _state.value.purpose) {
          Purpose.Add -> {
            _actions.send(AuthenticatorCodeEntryAction.NavigateToNaming)
          }
          is Purpose.Remove -> {
            repository.removeAuthenticatorApp(purpose.appId)
            _actions.send(AuthenticatorCodeEntryAction.ShowAuthenticatorAppRemoved)
            _actions.send(AuthenticatorCodeEntryAction.NavigateToAuthenticatorApps)
          }
        }
      }
    }
  }
}
