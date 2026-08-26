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
import org.signal.appsettings.authenticatorname.AuthenticatorNameAction
import org.signal.appsettings.authenticatorname.AuthenticatorNameEvent
import org.signal.appsettings.authenticatorname.AuthenticatorNameState
import org.signal.core.ui.compose.EventDrivenViewModel
import org.signal.core.util.logging.Log

/**
 * Drives the screen that names an authenticator app. [appId] is null when a newly paired app is being named for the
 * first time, and set when an existing one is being renamed.
 */
class AuthenticatorNameViewModel(
  private val appId: Long?,
  private val repository: AuthenticatorRepository = AuthenticatorRepository()
) : EventDrivenViewModel<AuthenticatorNameEvent>(TAG) {

  companion object {
    private val TAG = Log.tag(AuthenticatorNameViewModel::class)
  }

  private val _state = MutableStateFlow(
    AuthenticatorNameState(
      name = appId?.let { repository.getAuthenticatorApp(it)?.name } ?: "",
      renaming = appId != null
    )
  )
  private val _actions = Channel<AuthenticatorNameAction>(Channel.BUFFERED)

  val state: StateFlow<AuthenticatorNameState> = _state.asStateFlow()
  val actions: Flow<AuthenticatorNameAction> = _actions.receiveAsFlow()

  override suspend fun processEvent(event: AuthenticatorNameEvent) {
    when (event) {
      AuthenticatorNameEvent.NavigateBackClicked -> {
        _actions.send(AuthenticatorNameAction.NavigateBack)
      }
      is AuthenticatorNameEvent.NameChanged -> {
        _state.update { it.copy(name = event.name) }
      }
      AuthenticatorNameEvent.NextClicked -> {
        if (!_state.value.canSubmit) {
          return
        }

        val name = _state.value.name.trim()
        _state.update { it.copy(submitting = true) }

        if (appId != null) {
          repository.renameAuthenticatorApp(appId, name)
          _actions.send(AuthenticatorNameAction.ShowAuthenticatorAppRenamed)
        } else {
          repository.addAuthenticatorApp(name)
          _actions.send(AuthenticatorNameAction.ShowAuthenticatorAppSetUp)
        }

        _actions.send(AuthenticatorNameAction.NavigateToAuthenticatorApps)
      }
    }
  }
}
