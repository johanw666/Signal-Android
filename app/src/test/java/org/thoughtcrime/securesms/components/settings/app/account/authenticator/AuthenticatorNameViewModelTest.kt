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
import assertk.assertions.isTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.signal.appsettings.authenticatorname.AuthenticatorNameAction
import org.signal.appsettings.authenticatorname.AuthenticatorNameEvent
import org.thoughtcrime.securesms.testing.CoroutineDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticatorNameViewModelTest {

  companion object {
    private val EXISTING_APP = AuthenticatorApp(id = 7, name = "Twilio Authy", createdAt = 0)
  }

  private val testDispatcher = UnconfinedTestDispatcher()
  private val repository: AuthenticatorRepository = mockk(relaxed = true)

  @get:Rule
  val dispatcherRule = CoroutineDispatcherRule(testDispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    every { repository.getAuthenticatorApp(EXISTING_APP.id) } returns EXISTING_APP
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `naming a new app starts empty and isn't renaming`() = runTest(testDispatcher) {
    val viewModel = createViewModel(appId = null)

    assertThat(viewModel.state.value.name).isEqualTo("")
    assertThat(viewModel.state.value.renaming).isFalse()
  }

  @Test
  fun `renaming starts from the app's current name`() = runTest(testDispatcher) {
    val viewModel = createViewModel(appId = EXISTING_APP.id)

    assertThat(viewModel.state.value.name).isEqualTo(EXISTING_APP.name)
    assertThat(viewModel.state.value.renaming).isTrue()
  }

  @Test
  fun `a blank name can't be submitted`() = runTest(testDispatcher) {
    val viewModel = createViewModel(appId = null)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorNameEvent.NameChanged("   "))

    assertThat(viewModel.state.value.canSubmit).isFalse()

    viewModel.onEvent(AuthenticatorNameEvent.NextClicked)

    assertThat(actions).isEmpty()
  }

  @Test
  fun `NextClicked adds a new app and goes back to the list`() = runTest(testDispatcher) {
    val viewModel = createViewModel(appId = null)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorNameEvent.NameChanged("  Bitwarden Authenticator  "))
    viewModel.onEvent(AuthenticatorNameEvent.NextClicked)

    verify { repository.addAuthenticatorApp("Bitwarden Authenticator") }
    assertThat(actions).contains(AuthenticatorNameAction.ShowAuthenticatorAppSetUp)
    assertThat(actions.last()).isEqualTo(AuthenticatorNameAction.NavigateToAuthenticatorApps)
  }

  @Test
  fun `NextClicked renames an existing app and goes back to the list`() = runTest(testDispatcher) {
    val viewModel = createViewModel(appId = EXISTING_APP.id)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorNameEvent.NameChanged("Work Authenticator"))
    viewModel.onEvent(AuthenticatorNameEvent.NextClicked)

    verify { repository.renameAuthenticatorApp(EXISTING_APP.id, "Work Authenticator") }
    assertThat(actions).contains(AuthenticatorNameAction.ShowAuthenticatorAppRenamed)
    assertThat(actions.last()).isEqualTo(AuthenticatorNameAction.NavigateToAuthenticatorApps)
  }

  @Test
  fun `NavigateBackClicked leaves the screen`() = runTest(testDispatcher) {
    val viewModel = createViewModel(appId = null)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(AuthenticatorNameEvent.NavigateBackClicked)

    assertThat(actions.last()).isEqualTo(AuthenticatorNameAction.NavigateBack)
  }

  private fun createViewModel(appId: Long?) = AuthenticatorNameViewModel(appId, repository)

  private fun TestScope.collectActions(actions: Flow<AuthenticatorNameAction>): List<AuthenticatorNameAction> {
    val collected = mutableListOf<AuthenticatorNameAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
