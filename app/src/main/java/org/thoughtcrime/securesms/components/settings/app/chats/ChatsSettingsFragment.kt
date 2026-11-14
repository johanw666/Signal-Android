package org.thoughtcrime.securesms.components.settings.app.chats

// JW: added
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.navigation.fragment.findNavController
import org.signal.appsettings.routes.AppSettingsRoute
import org.signal.core.ui.compose.ComposeFragment
import org.signal.core.util.logging.Log
import org.signal.core.util.logging.Log.tag
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.backup.BackupDialog
import org.thoughtcrime.securesms.components.settings.app.appSettingsRouter
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.preferences.BackupsPreferenceFragment
import org.thoughtcrime.securesms.service.LocalBackupListener
import org.thoughtcrime.securesms.util.navigation.safeNavigate

/**
 * Fragment host for [ChatsSettingsEntry] until app settings moves to Nav3. Only translates routes into the nav graph.
 */
public class ChatsSettingsFragment : ComposeFragment() {

  private val appSettingsRouter by appSettingsRouter(onNavigateToScreen = ::navigateToScreen)

  // JW: added
  private val TAG = tag(ChatsSettingsFragment::class.java)

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    if (requestCode == CHOOSE_BACKUPS_LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
      val backupUri = data.data
      val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      SignalStore.settings.signalBackupDirectory = backupUri!!
      context?.getContentResolver()?.takePersistableUriPermission(backupUri, takeFlags)
      SignalStore.settings.localBackupNextTime = 0
      LocalBackupListener.schedule(context)
      SignalStore.settings.signalBackupDirectory = backupUri
    } else {
      Log.w(TAG, "Unknown activity result. code: " + requestCode + " resultCode: " + resultCode + " data present: " + (data != null))
    }
  }

/*
  // https://developer.android.com/training/basics/intents/result
  val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
      val intent = result.data
      if (intent != null && intent.data != null) {
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
*/
/*
  override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
    super.onActivityResult(requestCode, resultCode, intent)
  //startActivityForResult(requestCode: Int, resultCode: Int, intent: Intent?) {
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
*/
  companion object {
    val CHOOSE_BACKUPS_LOCATION_REQUEST_CODE = 26212 // JW: added

    @JvmStatic
    fun setChatBackupLocationApi30() {
      val backupUri = SignalStore.settings.signalBackupDirectory
      val fragment = ChatsSettingsFragment()
      if (Build.VERSION.SDK_INT >= 30) {
        BackupDialog.showChooseBackupLocationDialog(fragment, CHOOSE_BACKUPS_LOCATION_REQUEST_CODE)
        //BackupDialog.showChooseBackupLocationDialog(this@ChatsSettingsFragment, CHOOSE_BACKUPS_LOCATION_REQUEST_CODE)
        SignalStore.settings.signalBackupDirectory = backupUri
      }
    }
  }
  // JW end

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
  }
}
