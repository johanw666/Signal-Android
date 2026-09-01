/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.twofactorselection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import org.signal.core.ui.compose.EventDrivenViewModel
import org.signal.core.util.logging.Log

/**
 * Drives [TwoFactorSelectionScreen]. Surfaces the method the user picked (and cancellation) to the host as a
 * [TwoFactorSelectionAction].
 */
class TwoFactorSelectionViewModel(
  methods: List<TwoFactorMethod>
) : EventDrivenViewModel<TwoFactorSelectionScreenEvents>(TAG) {

  companion object {
    private val TAG = Log.tag(TwoFactorSelectionViewModel::class)
  }

  private val _state = MutableStateFlow(TwoFactorSelectionState(methods = methods))
  private val _actions = Channel<TwoFactorSelectionAction>(Channel.BUFFERED)

  val state: StateFlow<TwoFactorSelectionState> = _state.asStateFlow()
  val actions: Flow<TwoFactorSelectionAction> = _actions.receiveAsFlow()

  init {
    _state
      .onEach { Log.d(TAG, "[State] $it") }
      .launchIn(viewModelScope)
  }

  override suspend fun processEvent(event: TwoFactorSelectionScreenEvents) {
    when (event) {
      is TwoFactorSelectionScreenEvents.MethodSelected -> {
        val action = when (event.method) {
          TwoFactorMethod.Passkey -> TwoFactorSelectionAction.AuthenticateWithPasskey
          TwoFactorMethod.AuthenticatorApp -> TwoFactorSelectionAction.NavigateToTotpEntry
        }

        _actions.send(action)
      }
      TwoFactorSelectionScreenEvents.CancelClicked -> {
        _actions.send(TwoFactorSelectionAction.NavigateBack)
      }
    }
  }

  class Factory(private val methods: List<TwoFactorMethod>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return TwoFactorSelectionViewModel(methods) as T
    }
  }
}
