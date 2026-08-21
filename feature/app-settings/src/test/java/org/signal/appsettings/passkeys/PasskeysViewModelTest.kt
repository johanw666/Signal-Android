/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

import assertk.assertThat
import assertk.assertions.isEmpty
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
class PasskeysViewModelTest {

  companion object {
    private val PASSKEYS = listOf(
      Passkey(id = 1, name = "My Security Key", createdAt = System.currentTimeMillis()),
      Passkey(id = 2, name = "My Pixel Phone", createdAt = System.currentTimeMillis())
    )
  }

  private val testDispatcher = UnconfinedTestDispatcher()

  private val repository = object : PasskeysRepository {
    override fun getPasskeys(): List<Passkey> = PASSKEYS
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `the passkeys are available as soon as the screen opens`() = runTest(testDispatcher) {
    val viewModel = PasskeysViewModel(repository)

    assertThat(viewModel.state.value.passkeys).isEqualTo(PASSKEYS)
  }

  @Test
  fun `SetUpPasskeyClicked launches passkey creation`() = runTest(testDispatcher) {
    val viewModel = PasskeysViewModel(repository)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(PasskeysEvent.SetUpPasskeyClicked)

    assertThat(actions.last()).isEqualTo(PasskeysAction.LaunchPasskeyCreation)
  }

  @Test
  fun `LearnMoreClicked opens the learn more article`() = runTest(testDispatcher) {
    val viewModel = PasskeysViewModel(repository)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(PasskeysEvent.LearnMoreClicked)

    assertThat(actions.last()).isEqualTo(PasskeysAction.OpenLearnMore)
  }

  @Test
  fun `NavigateBackClicked leaves the screen`() = runTest(testDispatcher) {
    val viewModel = PasskeysViewModel(repository)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(PasskeysEvent.NavigateBackClicked)

    assertThat(actions.last()).isEqualTo(PasskeysAction.NavigateBack)
  }

  @Test
  fun `rename and remove produce no actions yet`() = runTest(testDispatcher) {
    val viewModel = PasskeysViewModel(repository)
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(PasskeysEvent.RenamePasskeyClicked(passkeyId = 1))
    viewModel.onEvent(PasskeysEvent.RemovePasskeyClicked(passkeyId = 1))

    assertThat(actions).isEmpty()
  }

  private fun TestScope.collectActions(actions: Flow<PasskeysAction>): List<PasskeysAction> {
    val collected = mutableListOf<PasskeysAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
