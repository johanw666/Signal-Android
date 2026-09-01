/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.twofactorselection

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TwoFactorSelectionViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state offers the methods it was constructed with`() = runTest(testDispatcher) {
    val viewModel = createViewModel(listOf(TwoFactorMethod.Passkey, TwoFactorMethod.AuthenticatorApp))

    assertThat(viewModel.state.value.methods).containsExactly(TwoFactorMethod.Passkey, TwoFactorMethod.AuthenticatorApp)
  }

  @Test
  fun `selecting passkey starts the passkey flow`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TwoFactorSelectionScreenEvents.MethodSelected(TwoFactorMethod.Passkey))

    assertThat(actions).containsExactly(TwoFactorSelectionAction.AuthenticateWithPasskey)
  }

  @Test
  fun `selecting the authenticator app moves on to code entry`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TwoFactorSelectionScreenEvents.MethodSelected(TwoFactorMethod.AuthenticatorApp))

    assertThat(actions).containsExactly(TwoFactorSelectionAction.NavigateToTotpEntry)
  }

  @Test
  fun `CancelClicked navigates back`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TwoFactorSelectionScreenEvents.CancelClicked)

    assertThat(actions).containsExactly(TwoFactorSelectionAction.NavigateBack)
  }

  @Test
  fun `selecting a method leaves the offered methods alone`() = runTest(testDispatcher) {
    val methods = listOf(TwoFactorMethod.Passkey, TwoFactorMethod.AuthenticatorApp)
    val viewModel = createViewModel(methods)

    viewModel.onEvent(TwoFactorSelectionScreenEvents.MethodSelected(TwoFactorMethod.Passkey))

    assertThat(viewModel.state.value).isEqualTo(TwoFactorSelectionState(methods = methods))
  }

  private fun createViewModel(
    methods: List<TwoFactorMethod> = listOf(TwoFactorMethod.Passkey, TwoFactorMethod.AuthenticatorApp)
  ): TwoFactorSelectionViewModel {
    return TwoFactorSelectionViewModel(methods)
  }

  private fun TestScope.collectActions(actions: Flow<TwoFactorSelectionAction>): List<TwoFactorSelectionAction> {
    val collected = mutableListOf<TwoFactorSelectionAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
