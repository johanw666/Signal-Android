/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
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
import org.junit.Rule
import org.junit.Test
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryAction
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryEvent
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryState.Purpose
import org.thoughtcrime.securesms.testing.CoroutineDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorCodeEntryViewModelTest {

  companion object {
    private const val FULL_CODE = "123456"
  }

  private val testDispatcher = UnconfinedTestDispatcher()

  @get:Rule
  val dispatcherRule = CoroutineDispatcherRule(testDispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    clearApps()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearApps()
  }

  @Test
  fun `non-digits are dropped and the code is capped at six digits`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(AuthenticatorCodeEntryEvent.CodeChanged("12a34 5678"))

    assertThat(viewModel.state.value.code).isEqualTo(FULL_CODE)
  }

  @Test
  fun `a partial code can't be submitted`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorCodeEntryEvent.CodeChanged("123"))

    assertThat(viewModel.state.value.canSubmit).isFalse()

    viewModel.onEvent(AuthenticatorCodeEntryEvent.DoneClicked)

    assertThat(actions).isEmpty()
  }

  @Test
  fun `a full code entered while adding sends the user on to name the app`() = runTest(testDispatcher) {
    val viewModel = createViewModel(purpose = Purpose.Add)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorCodeEntryEvent.CodeChanged(FULL_CODE))
    viewModel.onEvent(AuthenticatorCodeEntryEvent.DoneClicked)

    assertThat(actions.last()).isEqualTo(AuthenticatorCodeEntryAction.NavigateToNaming)
  }

  @Test
  fun `a full code entered while removing removes the app and goes back to the list`() = runTest(testDispatcher) {
    val appId = AuthenticatorAppStore.addApp(name = "Bitwarden Authenticator", createdAt = 0)
    val viewModel = createViewModel(purpose = Purpose.Remove(appId))
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorCodeEntryEvent.CodeChanged(FULL_CODE))
    viewModel.onEvent(AuthenticatorCodeEntryEvent.DoneClicked)

    assertThat(AuthenticatorAppStore.getApps()).isEmpty()
    assertThat(actions).contains(AuthenticatorCodeEntryAction.ShowAuthenticatorAppRemoved)
    assertThat(actions.last()).isEqualTo(AuthenticatorCodeEntryAction.NavigateToAuthenticatorApps)
  }

  @Test
  fun `NavigateBackClicked leaves the screen`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorCodeEntryEvent.NavigateBackClicked)

    assertThat(actions.last()).isEqualTo(AuthenticatorCodeEntryAction.NavigateBack)
  }

  private fun createViewModel(purpose: Purpose = Purpose.Add) = AuthenticatorCodeEntryViewModel(purpose = purpose)

  private fun clearApps() {
    AuthenticatorAppStore.getApps().forEach { AuthenticatorAppStore.removeApp(it.id) }
  }

  private fun TestScope.collectActions(actions: Flow<AuthenticatorCodeEntryAction>): List<AuthenticatorCodeEntryAction> {
    val collected = mutableListOf<AuthenticatorCodeEntryAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
