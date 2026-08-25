/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.signal.registration.RegistrationFlowEvent
import org.signal.registration.RegistrationRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SignalLoginViewModelTest {

  companion object {
    private const val VALID_ACCOUNT_KEY = "a6b284822e3283d07f2391360a4c2b91"
  }

  private val testDispatcher = UnconfinedTestDispatcher()

  private lateinit var mockRepository: RegistrationRepository
  private lateinit var viewModel: SignalLoginViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    viewModel = SignalLoginViewModel(repository = mockRepository, parentEventEmitter = {})
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `BackClicked navigates back`() = runTest(testDispatcher) {
    val parentEvents = mutableListOf<RegistrationFlowEvent>()

    viewModel.applyEvent(SignalLoginState(), SignalLoginScreenEvents.BackClicked, { parentEvents.add(it) }) {}

    assertThat(parentEvents).containsExactly(RegistrationFlowEvent.NavigateBack)
  }

  @Test
  fun `AccountKeyChanged strips formatting and lowercases the entered key`() = runTest(testDispatcher) {
    val state = applyAccountKey("A6B28482-2E32-83D0-7F23 91360A4C2B91")

    assertThat(state.accountKey).isEqualTo(VALID_ACCOUNT_KEY)
    assertThat(state.accountKeyError).isNull()
    assertThat(state.isNextEnabled).isTrue()
  }

  @Test
  fun `AccountKeyChanged does not report an error for a partially typed key`() = runTest(testDispatcher) {
    val state = applyAccountKey("a6b28482")

    assertThat(state.accountKeyError).isNull()
    assertThat(state.isNextEnabled).isFalse()
  }

  @Test
  fun `AccountKeyChanged reports non-hex characters as invalid`() = runTest(testDispatcher) {
    val state = applyAccountKey(VALID_ACCOUNT_KEY.dropLast(1) + "z")

    assertThat(state.accountKeyError).isEqualTo(AccountKeyError.Invalid)
    assertThat(state.isNextEnabled).isFalse()
  }

  @Test
  fun `AccountKeyChanged reports an over-long key as too long`() = runTest(testDispatcher) {
    val state = applyAccountKey(VALID_ACCOUNT_KEY + "ab")

    assertThat(state.accountKeyError).isEqualTo(AccountKeyError.TooLong(34))
    assertThat(state.isNextEnabled).isFalse()
  }

  @Test
  fun `NeedHelpClicked opens the help article`() = runTest(testDispatcher) {
    val actions = mutableListOf<SignalLoginScreenActions>()
    backgroundScope.launch { viewModel.actions.toList(actions) }

    viewModel.applyEvent(SignalLoginState(), SignalLoginScreenEvents.NeedHelpClicked, {}) {}

    assertThat(actions).containsExactly(SignalLoginScreenActions.OpenNeedHelpArticle)
  }

  @Test
  fun `NextClicked does nothing yet because logging in is not implemented`() = runTest(testDispatcher) {
    val parentEvents = mutableListOf<RegistrationFlowEvent>()
    val states = mutableListOf<SignalLoginState>()

    viewModel.applyEvent(SignalLoginState(accountKey = VALID_ACCOUNT_KEY), SignalLoginScreenEvents.NextClicked, { parentEvents.add(it) }) { states.add(it) }

    assertThat(parentEvents).isEmpty()
    assertThat(states).isEmpty()
  }

  @Test
  fun `NetworkErrorDialogDismissed clears the dialog`() = runTest(testDispatcher) {
    var state: SignalLoginState? = null

    viewModel.applyEvent(
      SignalLoginState(dialogs = SignalLoginState.Dialogs(networkError = true)),
      SignalLoginScreenEvents.NetworkErrorDialogDismissed,
      {}
    ) { state = it }

    assertThat(state!!.dialogs.networkError).isFalse()
  }

  @Test
  fun `UnknownErrorDialogDismissed clears the dialog`() = runTest(testDispatcher) {
    var state: SignalLoginState? = null

    viewModel.applyEvent(
      SignalLoginState(dialogs = SignalLoginState.Dialogs(unknownError = true)),
      SignalLoginScreenEvents.UnknownErrorDialogDismissed,
      {}
    ) { state = it }

    assertThat(state!!.dialogs.unknownError).isFalse()
  }

  private suspend fun applyAccountKey(value: String): SignalLoginState {
    var state = SignalLoginState()
    viewModel.applyEvent(state, SignalLoginScreenEvents.AccountKeyChanged(value), {}) { state = it }
    return state
  }
}
