/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.signallogin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.signal.core.ui.compose.CollectActions
import org.signal.core.ui.compose.ComposeFragment
import org.signal.signallogin.viewdetails.SignalLoginViewDetailsScreen

/**
 * Shows the account and recovery keys that make up the user's Signal Login, the same way registration does.
 */
class SignalLoginViewDetailsFragment : ComposeFragment() {

  private val viewModel: SignalLoginViewDetailsViewModel by viewModels()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectActions(viewModel.actions) { action -> handleAction(action) }

    SignalLoginViewDetailsScreen(
      state = state,
      onEvent = viewModel::onEvent
    )
  }

  private fun handleAction(action: SignalLoginViewDetailsAction) {
    when (action) {
      SignalLoginViewDetailsAction.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
    }
  }
}
