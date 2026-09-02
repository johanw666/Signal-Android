/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
import org.signal.appsettings.totpapplist.TotpApp
import org.signal.appsettings.totpapplist.TotpAppListAction
import org.signal.appsettings.totpapplist.TotpAppListEvent
import org.signal.appsettings.totpapplist.TotpAppListState.Dialog
import org.signal.appsettings.totpapplist.TotpAppListState.LoadState
import org.thoughtcrime.securesms.testing.CoroutineDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class TotpAppListViewModelTest {

  companion object {
    private val APP_ONE = TotpApp(id = 1, name = "Bitwarden Authenticator", createdAt = 0)
    private val APP_TWO = TotpApp(id = 2, name = "Twilio Authy", createdAt = 0)
  }

  private val testDispatcher = UnconfinedTestDispatcher()
  private val repository: TotpRepository = mockk(relaxed = true)

  @get:Rule
  val dispatcherRule = CoroutineDispatcherRule(testDispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    every { repository.getMaxApps() } returns 2
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(emptyList())
    coEvery { repository.removeTotpApp(any()) } returns TotpRepository.UpdateResult.Success
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `the configured apps are read on creation`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()

    assertThat(viewModel.state.value.apps).containsExactly(APP_ONE)
  }

  /** An empty list says nothing on its own, so the screen leans on [LoadState] to know we haven't heard back yet. */
  @Test
  fun `the state is LOADING until we've heard back about the account`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } coAnswers { awaitCancellation() }

    val viewModel = createViewModel()

    assertThat(viewModel.state.value.loadState).isEqualTo(LoadState.LOADING)
    assertThat(viewModel.state.value.apps).isEmpty()
  }

  @Test
  fun `a service we couldn't reach clears the list and says so`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.NetworkFailure

    val viewModel = createViewModel()

    assertThat(viewModel.state.value.loadState).isEqualTo(LoadState.NETWORK_FAILURE)
    assertThat(viewModel.state.value.apps).isEmpty()
  }

  @Test
  fun `a load that succeeds after one that failed clears the failure`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.NetworkFailure

    val viewModel = createViewModel()

    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))
    viewModel.onEvent(TotpAppListEvent.ScreenResumed)

    assertThat(viewModel.state.value.loadState).isEqualTo(LoadState.LOADED)
    assertThat(viewModel.state.value.apps).containsExactly(APP_ONE)
  }

  @Test
  fun `ScreenResumed picks up apps added elsewhere`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    assertThat(viewModel.state.value.apps).isEmpty()

    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))
    viewModel.onEvent(TotpAppListEvent.ScreenResumed)

    assertThat(viewModel.state.value.apps).containsExactly(APP_ONE)
  }

  @Test
  fun `AddTotpAppClicked opens setup when there's room for another app`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.AddTotpAppClicked)

    assertThat(actions.last()).isEqualTo(TotpAppListAction.NavigateToSetup)
  }

  @Test
  fun `AddTotpAppClicked explains the limit when there's no room for another app`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE, APP_TWO))

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.AddTotpAppClicked)

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.MaxAppsReached)
    assertThat(actions).isEmpty()
  }

  @Test
  fun `RenameAppClicked opens the naming screen for that app`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.RenameAppClicked(APP_ONE.id))

    assertThat(actions.last()).isEqualTo(TotpAppListAction.NavigateToRename(APP_ONE))
  }

  @Test
  fun `RemoveAppClicked asks the user to confirm first`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.RemoveAppClicked(APP_ONE.id))

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.ConfirmRemove(APP_ONE.id))
    assertThat(actions).isEmpty()
  }

  @Test
  fun `RemoveAppConfirmed removes the app and says so`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.RemoveAppClicked(APP_ONE.id))
    viewModel.onEvent(TotpAppListEvent.RemoveAppConfirmed(APP_ONE.id))

    coVerify { repository.removeTotpApp(APP_ONE.id) }
    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.None)
    assertThat(actions.last()).isEqualTo(TotpAppListAction.ShowTotpAppRemoved)
  }

  /** The list is what tells the user the app is gone, so it has to be read again rather than assumed. */
  @Test
  fun `a removal re-reads the list`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()

    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(emptyList())
    viewModel.onEvent(TotpAppListEvent.RemoveAppConfirmed(APP_ONE.id))

    assertThat(viewModel.state.value.apps).isEmpty()
  }

  /** Removing a key the service has already forgotten is the outcome the user wanted, so it isn't an error. */
  @Test
  fun `removing an app the service doesn't have still counts as removed`() = runTest(testDispatcher) {
    coEvery { repository.removeTotpApp(any()) } returns TotpRepository.UpdateResult.AppNotFound

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.RemoveAppConfirmed(APP_ONE.id))

    assertThat(actions.last()).isEqualTo(TotpAppListAction.ShowTotpAppRemoved)
  }

  @Test
  fun `a removal that didn't go through says so rather than pretending the app is gone`() = runTest(testDispatcher) {
    coEvery { repository.removeTotpApp(any()) } returns TotpRepository.UpdateResult.NetworkFailure

    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.RemoveAppConfirmed(APP_ONE.id))

    assertThat(actions.last()).isEqualTo(TotpAppListAction.ShowRemovalFailed)
  }

  /**
   * What the confirm button actually does: the dialog dismisses itself before it reports the confirmation, so the
   * removal has to survive arriving after the dialog is already gone.
   */
  @Test
  fun `RemoveAppConfirmed removes the app even though the dialog dismissed itself first`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()

    viewModel.onEvent(TotpAppListEvent.RemoveAppClicked(APP_ONE.id))
    viewModel.onEvent(TotpAppListEvent.DialogDismissed)
    viewModel.onEvent(TotpAppListEvent.RemoveAppConfirmed(APP_ONE.id))

    coVerify { repository.removeTotpApp(APP_ONE.id) }
  }

  @Test
  fun `DialogDismissed clears the dialog`() = runTest(testDispatcher) {
    coEvery { repository.getTotpApps() } returns TotpRepository.AppsResult.Success(listOf(APP_ONE))

    val viewModel = createViewModel()

    viewModel.onEvent(TotpAppListEvent.RemoveAppClicked(APP_ONE.id))
    viewModel.onEvent(TotpAppListEvent.DialogDismissed)

    assertThat(viewModel.state.value.dialog).isEqualTo(Dialog.None)
  }

  @Test
  fun `NavigateBackClicked leaves the screen`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.NavigateBackClicked)

    assertThat(actions.last()).isEqualTo(TotpAppListAction.NavigateBack)
  }

  @Test
  fun `LearnMoreClicked opens the support article`() = runTest(testDispatcher) {
    val viewModel = createViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpAppListEvent.LearnMoreClicked)

    assertThat(actions.last()).isEqualTo(TotpAppListAction.OpenLearnMore)
  }

  private fun createViewModel() = TotpAppListViewModel(repository)

  private fun TestScope.collectActions(actions: Flow<TotpAppListAction>): List<TotpAppListAction> {
    val collected = mutableListOf<TotpAppListAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
