/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
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
import org.signal.appsettings.authenticatorapps.AuthenticatorApp
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsAction
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsEvent
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsState.Dialog
import org.thoughtcrime.securesms.testing.CoroutineDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorAppsViewModelTest {

  companion object {
    private val APP_ONE = AuthenticatorApp(id = 1, name = "Bitwarden Authenticator", createdAt = 0)
    private val APP_TWO = AuthenticatorApp(id = 2, name = "Twilio Authy", createdAt = 0)
  }

  private val testDispatcher = UnconfinedTestDispatcher()
  private val repository: AuthenticatorRepository = mockk(relaxed = true)

  @get:Rule
  val dispatcherRule = CoroutineDispatcherRule(testDispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    every { repository.getMaxApps() } returns 2
    every { repository.getAuthenticatorApps() } returns emptyList()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `the configured apps are read on creation`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)

    val viewModel = createViewModel()

    assertThat(viewModel.state.value.apps).containsExactly(APP_ONE)
  }

  @Test
  fun `ScreenResumed picks up apps added elsewhere`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    assertThat(viewModel.state.value.apps).isEmpty()

    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)
    viewModel.onEvent(AuthenticatorAppsEvent.ScreenResumed)

    assertThat(viewModel.state.value.apps).containsExactly(APP_ONE)
  }

  @Test
  fun `AddAuthenticatorAppClicked opens setup when there's room for another app`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.AddAuthenticatorAppClicked)

    assertThat(actions.last()).isEqualTo(AuthenticatorAppsAction.NavigateToSetup)
  }

  @Test
  fun `AddAuthenticatorAppClicked explains the limit when there's no room for another app`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE, APP_TWO)

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.AddAuthenticatorAppClicked)

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.MaxAppsReached)
    assertThat(actions).isEmpty()
  }

  @Test
  fun `RenameAppClicked opens the naming screen for that app`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.RenameAppClicked(APP_ONE.id))

    assertThat(actions.last()).isEqualTo(AuthenticatorAppsAction.NavigateToRename(APP_ONE.id))
  }

  @Test
  fun `RemoveAppClicked asks the user to confirm first`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.RemoveAppClicked(APP_ONE.id))

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.ConfirmRemove(APP_ONE.id))
    assertThat(actions).isEmpty()
  }

  @Test
  fun `RemoveAppConfirmed collects a code before removing the app`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.RemoveAppClicked(APP_ONE.id))
    viewModel.onEvent(AuthenticatorAppsEvent.RemoveAppConfirmed)

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.None)
    assertThat(actions.last()).isEqualTo(AuthenticatorAppsAction.NavigateToRemovalCodeEntry(APP_ONE.id))
  }

  @Test
  fun `RemoveAppConfirmed does nothing when no removal is pending`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.RemoveAppConfirmed)

    assertThat(actions).isEmpty()
  }

  @Test
  fun `DialogDismissed clears the dialog`() = runTest(testDispatcher) {
    every { repository.getAuthenticatorApps() } returns listOf(APP_ONE)

    val viewModel = createViewModel()

    viewModel.onEvent(AuthenticatorAppsEvent.RemoveAppClicked(APP_ONE.id))
    viewModel.onEvent(AuthenticatorAppsEvent.DialogDismissed)

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.None)
  }

  @Test
  fun `NavigateBackClicked leaves the screen`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.NavigateBackClicked)

    assertThat(actions.last()).isEqualTo(AuthenticatorAppsAction.NavigateBack)
  }

  @Test
  fun `LearnMoreClicked opens the support article`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorAppsEvent.LearnMoreClicked)

    assertThat(actions.last()).isEqualTo(AuthenticatorAppsAction.OpenLearnMore)
  }

  private fun createViewModel() = AuthenticatorAppsViewModel(repository)

  private fun TestScope.collectActions(actions: Flow<AuthenticatorAppsAction>): List<AuthenticatorAppsAction> {
    val collected = mutableListOf<AuthenticatorAppsAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
