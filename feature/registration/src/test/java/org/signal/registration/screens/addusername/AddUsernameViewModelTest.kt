/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.addusername

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.signal.libsignal.net.RequestResult
import org.signal.libsignal.usernames.Username
import org.signal.network.service.UsernameService.ConfirmUsernameError
import org.signal.network.service.UsernameService.ReserveUsernameError
import org.signal.registration.RegistrationFlowEvent
import org.signal.registration.RegistrationRepository
import org.signal.registration.RegistrationRoute
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddUsernameViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private lateinit var mockRepository: RegistrationRepository
  private lateinit var parentEvents: MutableList<RegistrationFlowEvent>
  private lateinit var parentEventEmitter: (RegistrationFlowEvent) -> Unit
  private lateinit var viewModel: AddUsernameViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepository = mockk(relaxed = true)
    parentEvents = mutableListOf()
    parentEventEmitter = { parentEvents.add(it) }
    viewModel = AddUsernameViewModel(
      repository = mockRepository,
      parentEventEmitter = parentEventEmitter
    )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun TestScope.collectActions(): List<AddUsernameScreenActions> {
    val actions = mutableListOf<AddUsernameScreenActions>()
    backgroundScope.launch(testDispatcher) { viewModel.actions.collect { actions.add(it) } }
    return actions
  }

  @Test
  fun `LearnMoreClicked emits an action to open the learn more article`() = runTest(testDispatcher) {
    val actions = collectActions()

    viewModel.applyEvent(AddUsernameState(), AddUsernameScreenEvents.LearnMoreClicked, parentEventEmitter) {}

    assertThat(actions).containsExactly(AddUsernameScreenActions.OpenLearnMoreArticle)
  }

  @Test
  fun `typing a valid nickname reserves a username after the debounce`() = runTest(testDispatcher) {
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(Username("maya.45"))

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.reservation).isEqualTo(Username("maya.45"))
    assertThat(viewModel.state.value.isReserving).isFalse()
    assertThat(viewModel.state.value.isSubmittable).isTrue()
  }

  @Test
  fun `editing the nickname clears any existing reservation`() = runTest(testDispatcher) {
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(Username("maya.45"))

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("mayab"))

    assertThat(viewModel.state.value.reservation).isNull()
    assertThat(viewModel.state.value.isSubmittable).isFalse()
  }

  @Test
  fun `a too-short nickname produces a validation error`() = runTest(testDispatcher) {
    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("ma"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.validationError).isEqualTo(AddUsernameState.ValidationError.TOO_SHORT)
    assertThat(viewModel.state.value.isSubmittable).isFalse()
  }

  @Test
  fun `a nickname starting with a digit produces a validation error`() = runTest(testDispatcher) {
    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("1maya"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.validationError).isEqualTo(AddUsernameState.ValidationError.CANNOT_START_WITH_DIGIT)
  }

  @Test
  fun `a nickname with invalid characters produces a validation error`() = runTest(testDispatcher) {
    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("ma!a"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.validationError).isEqualTo(AddUsernameState.ValidationError.INVALID_CHARACTERS)
  }

  @Test
  fun `an unavailable nickname produces a validation error`() = runTest(testDispatcher) {
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.NonSuccess(ReserveUsernameError.NotAvailable)

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.validationError).isEqualTo(AddUsernameState.ValidationError.NOT_AVAILABLE)
    assertThat(viewModel.state.value.isSubmittable).isFalse()
  }

  @Test
  fun `a network error while reserving shows the network error dialog`() = runTest(testDispatcher) {
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.RetryableNetworkError(IOException())

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.dialogs.networkError).isTrue()
  }

  @Test
  fun `NextClicked confirms the reservation and advances to the profile screen`() = runTest(testDispatcher) {
    val reservation = Username("maya.45")
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(reservation)
    coEvery { mockRepository.confirmUsername(reservation) } returns RequestResult.Success(Unit)

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    viewModel.onEvent(AddUsernameScreenEvents.NextClicked)
    advanceUntilIdle()

    assertThat(parentEvents).contains(RegistrationFlowEvent.NavigateToScreen(RegistrationRoute.Profile, popCurrent = true))
  }

  @Test
  fun `NextClicked does nothing without a reservation`() = runTest(testDispatcher) {
    viewModel.onEvent(AddUsernameScreenEvents.NextClicked)
    advanceUntilIdle()

    assertThat(parentEvents).isEmpty()
    assertThat(viewModel.state.value.showSpinner).isFalse()
  }

  @Test
  fun `a taken username at confirmation shows the unavailable dialog and clears the reservation`() = runTest(testDispatcher) {
    val reservation = Username("maya.45")
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(reservation)
    coEvery { mockRepository.confirmUsername(reservation) } returns RequestResult.NonSuccess(ConfirmUsernameError.NotAvailable)

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    viewModel.onEvent(AddUsernameScreenEvents.NextClicked)
    advanceUntilIdle()

    assertThat(viewModel.state.value.dialogs.usernameUnavailable).isTrue()
    assertThat(viewModel.state.value.showSpinner).isFalse()
    assertThat(viewModel.state.value.reservation).isNull()
    assertThat(parentEvents).isEmpty()
  }

  @Test
  fun `a lapsed reservation at confirmation shows the lapsed dialog and clears the reservation`() = runTest(testDispatcher) {
    val reservation = Username("maya.45")
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(reservation)
    coEvery { mockRepository.confirmUsername(reservation) } returns RequestResult.NonSuccess(ConfirmUsernameError.ReservationInvalid)

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    viewModel.onEvent(AddUsernameScreenEvents.NextClicked)
    advanceUntilIdle()

    assertThat(viewModel.state.value.dialogs.reservationLapsed).isTrue()
    assertThat(viewModel.state.value.reservation).isNull()
    assertThat(parentEvents).isEmpty()
  }

  @Test
  fun `dismissing an error dialog retries the reservation`() = runTest(testDispatcher) {
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.RetryableNetworkError(IOException()) andThen RequestResult.Success(Username("maya.45"))

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    assertThat(viewModel.state.value.dialogs.networkError).isTrue()

    viewModel.onEvent(AddUsernameScreenEvents.NetworkErrorDialogDismissed)
    advanceUntilIdle()

    assertThat(viewModel.state.value.dialogs.networkError).isFalse()
    assertThat(viewModel.state.value.reservation).isEqualTo(Username("maya.45"))
  }

  @Test
  fun `an unchanged username keeps the reservation`() = runTest(testDispatcher) {
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(Username("maya.45"))

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()

    assertThat(viewModel.state.value.reservation).isEqualTo(Username("maya.45"))
    assertThat(viewModel.state.value.isSubmittable).isTrue()
  }

  @Test
  fun `a network error at confirmation shows the network error dialog and keeps the reservation`() = runTest(testDispatcher) {
    val reservation = Username("maya.45")
    coEvery { mockRepository.reserveUsername("maya") } returns RequestResult.Success(reservation)
    coEvery { mockRepository.confirmUsername(reservation) } returns RequestResult.RetryableNetworkError(IOException())

    viewModel.onEvent(AddUsernameScreenEvents.UsernameChanged("maya"))
    advanceUntilIdle()
    viewModel.onEvent(AddUsernameScreenEvents.NextClicked)
    advanceUntilIdle()

    assertThat(viewModel.state.value.dialogs.networkError).isTrue()
    assertThat(viewModel.state.value.reservation).isEqualTo(reservation)
  }

  @Test
  fun `SkipClicked advances to the profile screen`() = runTest(testDispatcher) {
    viewModel.onEvent(AddUsernameScreenEvents.SkipClicked)
    advanceUntilIdle()

    assertThat(parentEvents).containsExactly(RegistrationFlowEvent.NavigateToScreen(RegistrationRoute.Profile, popCurrent = true))
  }
}
