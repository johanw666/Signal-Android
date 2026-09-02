/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.totpentry

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.signal.core.ui.navigation.ResultEventBus
import org.signal.registration.RegistrationFlowEvent
import org.signal.registration.RegistrationRoute

@OptIn(ExperimentalCoroutinesApi::class)
class TotpEntryViewModelTest {

  companion object {
    private const val RESULT_KEY = "totp_code_result"
  }

  private val testDispatcher = UnconfinedTestDispatcher()

  private val emittedParentEvents = mutableListOf<RegistrationFlowEvent>()
  private val resultBus = ResultEventBus()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is empty with focus on the first digit`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    assertThat(viewModel.state.value.digits).isEqualTo(TotpEntryState.emptyDigits())
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(0)
    assertThat(viewModel.state.value.isComplete).isFalse()
  }

  @Test
  fun `entering a digit records it and advances focus`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4"))

    assertThat(viewModel.state.value.digits[0]).isEqualTo("4")
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(1)
  }

  @Test
  fun `entering the final digit emits the code and pops back to the login screen`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    "41837".forEachIndexed { index, digit ->
      viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(index, digit.toString()))
    }
    assertThat(sentCode()).isNull()
    assertThat(emittedParentEvents).isEmpty()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(5, "2"))

    assertThat(viewModel.state.value.isComplete).isTrue()
    assertThat(sentCode()).isEqualTo("418372")
    assertThat(emittedParentEvents).containsExactly(RegistrationFlowEvent.NavigateBackToScreen(RegistrationRoute.SignalLoginCredentialEntry()))
  }

  @Test
  fun `pasting a full code populates every field and emits the code`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "418372"))

    assertThat(viewModel.state.value.digits).isEqualTo(listOf("4", "1", "8", "3", "7", "2"))
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(5)
    assertThat(sentCode()).isEqualTo("418372")
    assertThat(emittedParentEvents).containsExactly(RegistrationFlowEvent.NavigateBackToScreen(RegistrationRoute.SignalLoginCredentialEntry()))
  }

  @Test
  fun `pasting an incomplete code is ignored`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4183"))

    assertThat(viewModel.state.value.digits).isEqualTo(TotpEntryState.emptyDigits())
    assertThat(sentCode()).isNull()
    assertThat(emittedParentEvents).isEmpty()
  }

  @Test
  fun `a backspace deletes the digit and shifts the following ones left`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4"))
    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(1, "1"))
    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(2, "8"))

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(1, ""))

    assertThat(viewModel.state.value.digits).isEqualTo(listOf("4", "8", "", "", "", ""))
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(0)
  }

  @Test
  fun `a backspace on an empty field deletes the previous digit`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4"))
    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(1, ""))

    assertThat(viewModel.state.value.digits).isEqualTo(TotpEntryState.emptyDigits())
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(0)
  }

  @Test
  fun `CancelClicked navigates back`() = runTest(testDispatcher) {
    val viewModel = createViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.CancelClicked)

    assertThat(emittedParentEvents).containsExactly(RegistrationFlowEvent.NavigateBack)
  }

  private fun createViewModel(): TotpEntryViewModel {
    return TotpEntryViewModel(
      parentEventEmitter = { emittedParentEvents.add(it) },
      resultBus = resultBus,
      resultKey = RESULT_KEY
    )
  }

  private fun sentCode(): String? {
    return resultBus.channelMap[RESULT_KEY]?.tryReceive()?.getOrNull() as String?
  }
}
