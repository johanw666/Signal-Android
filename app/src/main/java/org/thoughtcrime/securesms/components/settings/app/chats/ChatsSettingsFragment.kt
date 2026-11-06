package org.thoughtcrime.securesms.components.settings.app.chats

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.routes.AppSettingsNavigationEvent
import org.signal.appsettings.routes.AppSettingsRoute
import org.signal.appsettings.routes.AppSettingsRouter
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.util.collectFlowOnViewLifecycle
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.util.navigation.safeNavigate
// JW: added
import android.app.Activity
import android.content.Intent
import android.os.Build
import org.thoughtcrime.securesms.backup.BackupDialog
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.service.LocalBackupListener
import org.signal.appsettings.chats.ChatsSettingsEvents

/**
 * Fragment host for [ChatsSettingsEntry] until app settings moves to Nav3. Only translates routes into the nav graph.
 */
class ChatsSettingsFragment : ComposeFragment() {

  // JW: added
  override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
    super.onActivityResult(requestCode, resultCode, intent)
    val context = AppDependencies.application

    if (intent != null && intent.data != null) {
      if (resultCode == Activity.RESULT_OK) {
        if (requestCode == CHOOSE_BACKUPS_LOCATION_REQUEST_CODE) {
          val backupUri = intent.data
          val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          SignalStore.settings.signalBackupDirectory = backupUri!!
          context?.getContentResolver()?.takePersistableUriPermission(backupUri, takeFlags)
          SignalStore.settings.localBackupNextTime = 0
          LocalBackupListener.schedule(context)
          SignalStore.settings.signalBackupDirectory = backupUri
        }
      }
    }
  }

  // JW: added
  fun setChatBackupLocationApi30() {
    val backupUri = SignalStore.settings.signalBackupDirectory
    if (Build.VERSION.SDK_INT >= 30) {
      BackupDialog.showChooseBackupLocationDialog(this@ChatsSettingsFragment, CHOOSE_BACKUPS_LOCATION_REQUEST_CODE)
      SignalStore.settings.signalBackupDirectory = backupUri
    }
  }

  private val appSettingsRouter by viewModels<AppSettingsRouter>()
  val CHOOSE_BACKUPS_LOCATION_REQUEST_CODE = 1201 // JW: added

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    collectFlowOnViewLifecycle(appSettingsRouter.events, Lifecycle.State.RESUMED) { event ->
      when (event) {
        AppSettingsNavigationEvent.NavigateBack -> requireActivity().onBackPressedDispatcher.onBackPressed()
        is AppSettingsNavigationEvent.NavigateToScreen -> navigateToScreen(event.route)
      }
    }
  }

  private fun navigateToScreen(route: AppSettingsRoute) {
    when (route) {
      AppSettingsRoute.ChatFoldersRoute.ChatFolders -> findNavController().safeNavigate(R.id.action_chatsSettingsFragment_to_chatFoldersFragment)
      AppSettingsRoute.BackupsRoute.Backups -> findNavController().safeNavigate(R.id.action_direct_to_backupsSettingsFragment) // JW: added
      else -> error("Unexpected route: ${route.javaClass.name}")
    }
  }

  @Composable
  override fun FragmentContent() {
    ChatsSettingsEntry(parentEventEmitter = appSettingsRouter::onEvent)
    // JW: TODO: see if the line above works
    //ChatsSettingsEvents.ChatBackupsLocationApi30Clicked -> setChatBackupLocationApi30() // JW: added TODO: label with directory is not refreshed
  }
}
