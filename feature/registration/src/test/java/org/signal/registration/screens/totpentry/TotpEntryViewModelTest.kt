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
import assertk.assertions.isTrue
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
class TotpEntryViewModelTest {

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
  fun `initial state is empty with focus on the first digit`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()

    assertThat(viewModel.state.value.digits).isEqualTo(TotpEntryState.emptyDigits())
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(0)
    assertThat(viewModel.state.value.isComplete).isFalse()
  }

  @Test
  fun `entering a digit records it and advances focus`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4"))

    assertThat(viewModel.state.value.digits[0]).isEqualTo("4")
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(1)
  }

  @Test
  fun `entering the final digit emits CodeEntered`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()
    val actions = collectActions(viewModel.actions)

    "41837".forEachIndexed { index, digit ->
      viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(index, digit.toString()))
    }
    assertThat(actions).isEmpty()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(5, "2"))

    assertThat(viewModel.state.value.isComplete).isTrue()
    assertThat(actions).containsExactly(TotpEntryAction.CodeEntered("418372"))
  }

  @Test
  fun `pasting a full code populates every field and emits CodeEntered`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "418372"))

    assertThat(viewModel.state.value.digits).isEqualTo(listOf("4", "1", "8", "3", "7", "2"))
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(5)
    assertThat(actions).containsExactly(TotpEntryAction.CodeEntered("418372"))
  }

  @Test
  fun `pasting an incomplete code is ignored`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4183"))

    assertThat(viewModel.state.value.digits).isEqualTo(TotpEntryState.emptyDigits())
    assertThat(actions).isEmpty()
  }

  @Test
  fun `a backspace deletes the digit and shifts the following ones left`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4"))
    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(1, "1"))
    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(2, "8"))

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(1, ""))

    assertThat(viewModel.state.value.digits).isEqualTo(listOf("4", "8", "", "", "", ""))
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(0)
  }

  @Test
  fun `a backspace on an empty field deletes the previous digit`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()

    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(0, "4"))
    viewModel.onEvent(TotpEntryScreenEvents.DigitChanged(1, ""))

    assertThat(viewModel.state.value.digits).isEqualTo(TotpEntryState.emptyDigits())
    assertThat(viewModel.state.value.focusedDigitIndex).isEqualTo(0)
  }

  @Test
  fun `CancelClicked emits NavigateBack`() = runTest(testDispatcher) {
    val viewModel = TotpEntryViewModel()
    val actions = collectActions(viewModel.actions)

    viewModel.onEvent(TotpEntryScreenEvents.CancelClicked)

    assertThat(actions).containsExactly(TotpEntryAction.NavigateBack)
  }

  private fun TestScope.collectActions(actions: Flow<TotpEntryAction>): List<TotpEntryAction> {
    val collected = mutableListOf<TotpEntryAction>()
    backgroundScope.launch { actions.toList(collected) }
    return collected
  }
}
