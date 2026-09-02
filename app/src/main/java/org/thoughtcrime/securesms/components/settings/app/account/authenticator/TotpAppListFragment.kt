/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.totpapplist.TotpAppListAction
import org.signal.appsettings.totpapplist.TotpAppListEvent
import org.signal.appsettings.totpapplist.TotpAppListScreen
import org.signal.core.ui.compose.CollectActions
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.signal.appsettings.R as AppSettingsR

/**
 * Lists the authenticator apps on the account. Carries out the [TotpAppListAction]s that need the nav graph.
 */
class TotpAppListFragment : ComposeFragment() {

  companion object {
    private val TAG = Log.tag(TotpAppListFragment::class)
  }

  private val viewModel: TotpAppListViewModel by viewModels()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectActions(viewModel.actions) { action -> handleAction(action) }

    TotpAppListScreen(
      state = state,
      onEvent = viewModel::onEvent
    )
  }

  override fun onResume() {
    super.onResume()
    viewModel.onEvent(TotpAppListEvent.ScreenResumed)
  }

  private fun handleAction(action: TotpAppListAction) {
    when (action) {
      TotpAppListAction.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
      TotpAppListAction.NavigateToSetup -> {
        findNavController().safeNavigate(R.id.action_authenticatorAppsFragment_to_authenticatorSetupFragment)
      }
      is TotpAppListAction.NavigateToRename -> {
        findNavController().safeNavigate(
          R.id.action_authenticatorAppsFragment_to_authenticatorNameFragment,
          Bundle().apply { TotpNavArgs.putRenamedApp(this, action.app) }
        )
      }
      TotpAppListAction.ShowTotpAppRemoved -> toast(AppSettingsR.string.TotpAppListScreen__authenticator_app_removed)
      TotpAppListAction.ShowRemovalFailed -> toast(AppSettingsR.string.TotpAppListScreen__couldnt_remove_authenticator_app)
      TotpAppListAction.OpenLearnMore -> Log.w(TAG, "There's no support article to open yet.")
    }
  }

  private fun toast(@StringRes message: Int) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
  }
}
