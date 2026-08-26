/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryAction
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryScreen
import org.signal.core.ui.compose.CollectActions
import org.signal.core.ui.compose.ComposeFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.thoughtcrime.securesms.util.viewModel
import org.signal.appsettings.R as AppSettingsR

/**
 * Collects the code from the user's authenticator app, either to confirm a newly paired one or to authorize removing
 * one. Carries out the [AuthenticatorCodeEntryAction]s that need the nav graph.
 */
class AuthenticatorCodeEntryFragment : ComposeFragment() {

  private val viewModel: AuthenticatorCodeEntryViewModel by viewModel {
    AuthenticatorCodeEntryViewModel(AuthenticatorNavArgs.purpose(arguments))
  }

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectActions(viewModel.actions) { action -> handleAction(action) }

    AuthenticatorCodeEntryScreen(
      state = state,
      onEvent = viewModel::onEvent
    )
  }

  private fun handleAction(action: AuthenticatorCodeEntryAction) {
    when (action) {
      AuthenticatorCodeEntryAction.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
      AuthenticatorCodeEntryAction.NavigateToNaming -> {
        findNavController().safeNavigate(R.id.action_authenticatorCodeEntryFragment_to_authenticatorNameFragment)
      }
      AuthenticatorCodeEntryAction.NavigateToAuthenticatorApps -> findNavController().popBackStack(R.id.authenticatorAppsFragment, false)
      AuthenticatorCodeEntryAction.ShowAuthenticatorAppRemoved -> {
        Toast.makeText(requireContext(), AppSettingsR.string.AuthenticatorAppsScreen__authenticator_app_removed, Toast.LENGTH_SHORT).show()
      }
    }
  }
}
