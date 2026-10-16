package org.thoughtcrime.securesms.components.settings.app.chats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.chats.ChatsSettingsEvents
import org.signal.appsettings.chats.ChatsSettingsScreen
import org.signal.core.ui.compose.ComposeFragment
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

/**
 * Displays a list of chats settings options to the user, including
 * generating link previews and keeping muted chats archived.
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

  private val viewModel: ChatsSettingsViewModel by viewModels()
  val CHOOSE_BACKUPS_LOCATION_REQUEST_CODE = 1201 // JW: added

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChatsSettingsScreen(
      state = state,
      onEvent = { event ->
        when (event) {
          ChatsSettingsEvents.ChatFoldersClicked -> findNavController().safeNavigate(R.id.action_chatsSettingsFragment_to_chatFoldersFragment)
          ChatsSettingsEvents.ChatBackupClicked -> findNavController().safeNavigate(R.id.action_direct_to_backupsSettingsFragment) // JW: added
          ChatsSettingsEvents.ChatBackupsLocationApi30Clicked -> setChatBackupLocationApi30() // JW: added TODO: label with directory is not refreshed
          else -> viewModel.onEvent(event)
        }
      }
    )
  }
}
