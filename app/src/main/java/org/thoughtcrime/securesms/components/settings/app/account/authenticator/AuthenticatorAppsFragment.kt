/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsAction
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsEvent
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsScreen
import org.signal.core.ui.compose.CollectActions
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.navigation.safeNavigate

/**
 * Lists the authenticator apps on the account. Carries out the [AuthenticatorAppsAction]s that need the nav graph.
 */
class AuthenticatorAppsFragment : ComposeFragment() {

  companion object {
    private val TAG = Log.tag(AuthenticatorAppsFragment::class)
  }

  private val viewModel: AuthenticatorAppsViewModel by viewModels()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectActions(viewModel.actions) { action -> handleAction(action) }

    AuthenticatorAppsScreen(
      state = state,
      onEvent = viewModel::onEvent
    )
  }

  override fun onResume() {
    super.onResume()
    viewModel.onEvent(AuthenticatorAppsEvent.ScreenResumed)
  }

  private fun handleAction(action: AuthenticatorAppsAction) {
    when (action) {
      AuthenticatorAppsAction.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
      AuthenticatorAppsAction.NavigateToSetup -> {
        findNavController().safeNavigate(R.id.action_authenticatorAppsFragment_to_authenticatorSetupFragment)
      }
      is AuthenticatorAppsAction.NavigateToRename -> {
        findNavController().safeNavigate(
          R.id.action_authenticatorAppsFragment_to_authenticatorNameFragment,
          Bundle().apply { putLong(AuthenticatorNavArgs.ARG_APP_ID, action.appId) }
        )
      }
      is AuthenticatorAppsAction.NavigateToRemovalCodeEntry -> {
        findNavController().safeNavigate(
          R.id.action_authenticatorAppsFragment_to_authenticatorCodeEntryFragment,
          Bundle().apply {
            putString(AuthenticatorNavArgs.ARG_PURPOSE, AuthenticatorNavArgs.PURPOSE_REMOVE)
            putLong(AuthenticatorNavArgs.ARG_APP_ID, action.appId)
          }
        )
      }
      AuthenticatorAppsAction.OpenLearnMore -> Log.w(TAG, "There's no support article to open yet.")
    }
  }
}
