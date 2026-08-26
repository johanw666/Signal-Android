/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.authenticatorname.AuthenticatorNameAction
import org.signal.appsettings.authenticatorname.AuthenticatorNameScreen
import org.signal.core.ui.compose.CollectActions
import org.signal.core.ui.compose.ComposeFragment
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.viewModel
import org.signal.appsettings.R as AppSettingsR

/**
 * Names an authenticator app, either a newly paired one or one being renamed. Carries out the
 * [AuthenticatorNameAction]s that need the nav graph.
 */
class AuthenticatorNameFragment : ComposeFragment() {

  private val viewModel: AuthenticatorNameViewModel by viewModel {
    AuthenticatorNameViewModel(AuthenticatorNavArgs.appId(arguments))
  }

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectActions(viewModel.actions) { action -> handleAction(action) }

    AuthenticatorNameScreen(
      state = state,
      onEvent = viewModel::onEvent
    )
  }

  private fun handleAction(action: AuthenticatorNameAction) {
    when (action) {
      AuthenticatorNameAction.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
      AuthenticatorNameAction.NavigateToAuthenticatorApps -> findNavController().popBackStack(R.id.authenticatorAppsFragment, false)
      AuthenticatorNameAction.ShowAuthenticatorAppSetUp -> toast(AppSettingsR.string.AuthenticatorNameScreen__authenticator_app_set_up)
      AuthenticatorNameAction.ShowAuthenticatorAppRenamed -> toast(AppSettingsR.string.AuthenticatorNameScreen__authenticator_app_renamed)
    }
  }

  private fun toast(@StringRes message: Int) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
  }
}
