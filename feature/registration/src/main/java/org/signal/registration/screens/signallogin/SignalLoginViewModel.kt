/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

import androidx.annotation.VisibleForTesting
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
import org.signal.registration.RegistrationFlowEvent
import org.signal.registration.RegistrationRepository
import org.signal.registration.screens.util.navigateBack

/**
 * View model for [SignalLoginScreen].
 *
 * Logging in with an account key requires an endpoint that doesn't exist yet, so [SignalLoginScreenEvents.NextClicked]
 * is deliberately left as a stub. Everything the screen needs to validate and format what the user types is here.
 */
class SignalLoginViewModel(
  private val repository: RegistrationRepository,
  private val parentEventEmitter: (RegistrationFlowEvent) -> Unit
) : EventDrivenViewModel<SignalLoginScreenEvents>(TAG) {

  companion object {
    private val TAG = Log.tag(SignalLoginViewModel::class)

    /** Formatting the user may have pasted along with the key, which we accept and discard. */
    private val FORMATTING_CHARACTERS = Regex("""[\s-]""")

    private fun Char.isAccountKeyCharacter(): Boolean = this in '0'..'9' || this in 'a'..'f'
  }

  private val _state = MutableStateFlow(SignalLoginState())
  val state: StateFlow<SignalLoginState> = _state.asStateFlow()

  private val _actions = Channel<SignalLoginScreenActions>(Channel.BUFFERED)
  val actions: Flow<SignalLoginScreenActions> = _actions.receiveAsFlow()

  init {
    _state
      .onEach { Log.d(TAG, "[State] $it") }
      .launchIn(viewModelScope)
  }

  override suspend fun processEvent(event: SignalLoginScreenEvents) {
    applyEvent(_state.value, event, parentEventEmitter) { _state.value = it }
  }

  @VisibleForTesting
  suspend fun applyEvent(
    state: SignalLoginState,
    event: SignalLoginScreenEvents,
    parentEventEmitter: (RegistrationFlowEvent) -> Unit,
    stateEmitter: (SignalLoginState) -> Unit
  ) {
    when (event) {
      is SignalLoginScreenEvents.BackClicked -> {
        parentEventEmitter.navigateBack()
      }

      is SignalLoginScreenEvents.AccountKeyChanged -> {
        val accountKey = event.value.replace(FORMATTING_CHARACTERS, "").lowercase()
        stateEmitter(state.copy(accountKey = accountKey, accountKeyError = validate(accountKey)))
      }

      is SignalLoginScreenEvents.NeedHelpClicked -> {
        _actions.trySend(SignalLoginScreenActions.OpenNeedHelpArticle)
      }

      is SignalLoginScreenEvents.NextClicked -> {
        Log.i(TAG, "Next clicked, but logging in with an account key isn't implemented yet.")
      }

      is SignalLoginScreenEvents.NetworkErrorDialogDismissed -> {
        stateEmitter(state.copy(dialogs = state.dialogs.copy(networkError = false)))
      }

      is SignalLoginScreenEvents.UnknownErrorDialogDismissed -> {
        stateEmitter(state.copy(dialogs = state.dialogs.copy(unknownError = false)))
      }
    }
  }

  /**
   * Checks an already-normalized [accountKey]. A key that is merely incomplete isn't an error — the next button stays
   * disabled until it is the right length, without nagging the user as they type.
   */
  private fun validate(accountKey: String): AccountKeyError? {
    return when {
      accountKey.length > SignalLoginState.ACCOUNT_KEY_LENGTH -> AccountKeyError.TooLong(accountKey.length)
      accountKey.any { !it.isAccountKeyCharacter() } -> AccountKeyError.Invalid
      else -> null
    }
  }

  class Factory(
    private val repository: RegistrationRepository,
    private val parentEventEmitter: (RegistrationFlowEvent) -> Unit
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return SignalLoginViewModel(repository, parentEventEmitter) as T
    }
  }
}
