package org.thoughtcrime.securesms.components.settings.app.chats

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import org.signal.core.ui.compose.DayNightPreviews
import org.signal.core.ui.compose.Dividers
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.Texts
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.compose.ComposeFragment
import org.thoughtcrime.securesms.compose.rememberStatusBarColorNestedScrollModifier
import org.thoughtcrime.securesms.util.navigation.safeNavigate
// JW: Added
import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.compose.ui.res.stringArrayResource
import org.thoughtcrime.securesms.backup.BackupDialog
import org.thoughtcrime.securesms.service.LocalBackupListener
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.UriUtils

/**
 * Displays a list of chats settings options to the user, including
 * generating link previews and keeping muted chats archived.
 */
class ChatsSettingsFragment : ComposeFragment() {

  private val viewModel: ChatsSettingsViewModel by viewModels()
  val CHOOSE_BACKUPS_LOCATION_REQUEST_CODE = 1201 // JW: added

  override fun onResume() {
    super.onResume()
    viewModel.refresh()
  }

  // JW: added
  override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
    super.onActivityResult(requestCode, resultCode, intent)

    if (intent != null && intent.data != null) {
      if (resultCode == Activity.RESULT_OK) {
        if (requestCode == CHOOSE_BACKUPS_LOCATION_REQUEST_CODE) {
          val backupUri = intent.data
          val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          SignalStore.settings.setSignalBackupDirectory(backupUri!!)
          context?.getContentResolver()?.takePersistableUriPermission(backupUri, takeFlags)
          TextSecurePreferences.setNextBackupTime(requireContext(), 0)
          LocalBackupListener.schedule(context)
          viewModel.setChatBackupLocationApi30(UriUtils.getFullPathFromTreeUri(context, backupUri))
        }
      }
    }
  }
  
  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val callbacks = remember { Callbacks() }

    ChatsSettingsScreen(
      state = state,
      callbacks = callbacks
    )
  }

  private inner class Callbacks : ChatsSettingsCallbacks {
    override fun onNavigationClick() {
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    override fun onGenerateLinkPreviewsChanged(enabled: Boolean) {
      viewModel.setGenerateLinkPreviewsEnabled(enabled)
    }

    override fun onUseAddressBookChanged(enabled: Boolean) {
      viewModel.setUseAddressBook(enabled)
    }

    override fun onKeepMutedChatsArchivedChanged(enabled: Boolean) {
      viewModel.setKeepMutedChatsArchived(enabled)
    }

    override fun onAddAChatFolderClick() {
      findNavController().safeNavigate(R.id.action_chatsSettingsFragment_to_chatFoldersFragment)
    }

    override fun onAddOrEditFoldersClick() {
      findNavController().safeNavigate(R.id.action_chatsSettingsFragment_to_chatFoldersFragment)
    }

    override fun onUseSystemEmojiChanged(enabled: Boolean) {
      viewModel.setUseSystemEmoji(enabled)
    }

    override fun onEnterKeySendsChanged(enabled: Boolean) {
      viewModel.setEnterKeySends(enabled)
    }

    
    // JW: added --------------------------------------------------------------
    override fun onChatBackupLocationChanged(enabled: Boolean) {
      viewModel.setChatBackupLocation(enabled)
    }
    
    override fun onChatBackupLocationChangedApi30() {
      val backupUri = SignalStore.settings.signalBackupDirectory

      if (Build.VERSION.SDK_INT >= 30) {
        BackupDialog.showChooseBackupLocationDialog(this@ChatsSettingsFragment, CHOOSE_BACKUPS_LOCATION_REQUEST_CODE)
        viewModel.setChatBackupLocationApi30(UriUtils.getFullPathFromTreeUri(context, backupUri))
      }
    }
    
    override fun ontChatBackupZipfileChanged(enabled: Boolean) {
      viewModel.setChatBackupZipfile(enabled)
    }
    
    override fun onChatBackupZipfilePlainChanged(enabled: Boolean) {
      viewModel.setChatBackupZipfilePlain(enabled)
    }
    
    override fun onKeepViewOnceMessagesChanged(enabled: Boolean) {
      viewModel.keepViewOnceMessages(enabled)
    }

    override fun onIgnoreRemoteDeleteChanged(enabled: Boolean) {
      viewModel.setIgnoreRemoteDelete(enabled)
    }
    
    override fun onDeleteMediaOnlyChanged(enabled: Boolean) {
      viewModel.setDeleteMediaOnly(enabled)
    }
    
    override fun onWhoCanAddYouToGroupsClicked(selection: String) {
      viewModel.setWhoCanAddYouToGroups(selection)
    }

    override fun onSetGoogleMapTypeClicked(selection: String) {
      viewModel.setGoogleMapType(selection)
    }  
    //-------------------------------------------------------------------------
  }
}

private interface ChatsSettingsCallbacks {
  fun onNavigationClick() = Unit
  fun onGenerateLinkPreviewsChanged(enabled: Boolean) = Unit
  fun onUseAddressBookChanged(enabled: Boolean) = Unit
  fun onKeepMutedChatsArchivedChanged(enabled: Boolean) = Unit
  fun onAddAChatFolderClick() = Unit
  fun onAddOrEditFoldersClick() = Unit
  fun onUseSystemEmojiChanged(enabled: Boolean) = Unit
  fun onEnterKeySendsChanged(enabled: Boolean) = Unit
  // JW: added
  fun onChatBackupLocationChanged(enabled: Boolean) = Unit
  fun onChatBackupLocationChangedApi30() = Unit
  fun ontChatBackupZipfileChanged(enabled: Boolean) = Unit
  fun onChatBackupZipfilePlainChanged(enabled: Boolean) = Unit
  fun onKeepViewOnceMessagesChanged(enabled: Boolean) = Unit
  fun onIgnoreRemoteDeleteChanged(enabled: Boolean) = Unit
  fun onDeleteMediaOnlyChanged(enabled: Boolean) = Unit
  fun onWhoCanAddYouToGroupsClicked(selection: String) = Unit
  fun onSetGoogleMapTypeClicked(selection: String) = Unit

  object Empty : ChatsSettingsCallbacks
}

@Composable
private fun ChatsSettingsScreen(
  state: ChatsSettingsState,
  callbacks: ChatsSettingsCallbacks
) {
  Scaffolds.Settings(
    title = stringResource(R.string.preferences_chats__chats),
    onNavigationClick = callbacks::onNavigationClick,
    navigationIcon = SignalIcons.ArrowStart.imageVector
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .padding(paddingValues)
        .then(rememberStatusBarColorNestedScrollModifier())
    ) {
      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences__generate_link_previews),
          label = stringResource(R.string.preferences__retrieve_link_previews_from_websites_for_messages),
          enabled = state.isRegisteredAndUpToDate(),
          checked = state.generateLinkPreviews,
          onCheckChanged = callbacks::onGenerateLinkPreviewsChanged
        )
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences__pref_use_address_book_photos),
          label = stringResource(R.string.preferences__display_contact_photos_from_your_address_book_if_available),
          enabled = state.isRegisteredAndUpToDate(),
          checked = state.useAddressBook,
          onCheckChanged = callbacks::onUseAddressBookChanged
        )
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences__pref_keep_muted_chats_archived),
          label = stringResource(R.string.preferences__muted_chats_that_are_archived_will_remain_archived),
          enabled = state.isRegisteredAndUpToDate(),
          checked = state.keepMutedChatsArchived,
          onCheckChanged = callbacks::onKeepMutedChatsArchivedChanged
        )
      }

      item {
        Dividers.Default()
      }

      item {
        Texts.SectionHeader(stringResource(R.string.ChatsSettingsFragment__chat_folders))
      }

      if (state.folderCount == 1) {
        item {
          Rows.TextRow(
            text = stringResource(R.string.ChatsSettingsFragment__add_chat_folder),
            enabled = state.isRegisteredAndUpToDate(),
            onClick = callbacks::onAddAChatFolderClick
          )
        }
      } else {
        item {
          Rows.TextRow(
            text = stringResource(R.string.ChatsSettingsFragment__add_edit_chat_folder),
            label = pluralStringResource(R.plurals.ChatsSettingsFragment__d_folder, state.folderCount, state.folderCount),
            enabled = state.isRegisteredAndUpToDate(),
            onClick = callbacks::onAddOrEditFoldersClick
          )
        }
      }

      item {
        Dividers.Default()
      }

      item {
        Texts.SectionHeader(stringResource(R.string.ChatsSettingsFragment__keyboard))
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences_advanced__use_system_emoji),
          enabled = state.isRegisteredAndUpToDate(),
          checked = state.useSystemEmoji,
          onCheckChanged = callbacks::onUseSystemEmojiChanged
        )
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.ChatsSettingsFragment__send_with_enter),
          enabled = state.isRegisteredAndUpToDate(),
          checked = state.enterKeySends,
          onCheckChanged = callbacks::onEnterKeySendsChanged
        )
      }

      // JW: added ------------------------------------------------------------
      item {
        Dividers.Default()
      }

      item {
        Texts.SectionHeader(stringResource(R.string.preferences_chats__backups))
      }

      if (Build.VERSION.SDK_INT < 30) {
        item {
          Rows.ToggleRow(
            text = stringResource(R.string.preferences_chats__chat_backups_removable),
            label = stringResource(R.string.preferences_chats__backup_chats_to_removable_storage),
            checked = state.chatBackupsLocation,
            onCheckChanged = callbacks::onChatBackupLocationChanged
          )
        }
      } else {
          item {
            Rows.TextRow(
              text = stringResource(R.string.preferences_chats__chat_backups_location_tap_to_change),
              label = state.chatBackupsLocationApi30,
              onClick = callbacks::onChatBackupLocationChangedApi30
            )
          }
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences_chats__chat_backups_zipfile),
          label = stringResource(R.string.preferences_chats__backup_chats_to_encrypted_zipfile),
          checked = state.chatBackupZipfile,
          onCheckChanged = callbacks::ontChatBackupZipfileChanged
        )
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences_chats__chat_backups_zipfile_plain),
          label = stringResource(R.string.preferences_chats__backup_chats_to_encrypted_zipfile_plain),
          checked = state.chatBackupZipfilePlain,
          onCheckChanged = callbacks::onChatBackupZipfilePlainChanged
        )
      }

      item {
        Dividers.Default()
      }
      
      item {
        Texts.SectionHeader(stringResource(R.string.preferences_chats__control_message_deletion))
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences_chats__chat_keep_view_once_messages),
          label = stringResource(R.string.preferences_chats__keep_view_once_messages_summary),
          checked = state.keepViewOnceMessages,
          onCheckChanged = callbacks::onKeepViewOnceMessagesChanged
        )
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences_chats__chat_ignore_remote_delete),
          label = stringResource(R.string.preferences_chats__chat_ignore_remote_delete_summary),
          checked = state.ignoreRemoteDelete,
          onCheckChanged = callbacks::onIgnoreRemoteDeleteChanged
        )
      }

      item {
        Rows.ToggleRow(
          text = stringResource(R.string.preferences_chats__delete_media_only),
          label = stringResource(R.string.preferences_chats__delete_media_only_summary),
          checked = state.deleteMediaOnly,
          onCheckChanged = callbacks::onDeleteMediaOnlyChanged
        )
      }

      item {
        Dividers.Default()
      }
      
      item {
        Texts.SectionHeader(stringResource(R.string.preferences_chats__group_control))
      }

      item {
        Rows.RadioListRow(
          text = stringResource(R.string.preferences_chats__who_can_add_you_to_groups),
          labels = stringArrayResource(R.array.pref_group_add_entries),
          values = stringArrayResource(R.array.pref_group_add_values),
          selectedValue = state.whoCanAddYouToGroups,
          onSelected = callbacks::onWhoCanAddYouToGroupsClicked
        )
      }

      item {
        Dividers.Default()
      }
      
      item {
        Texts.SectionHeader(stringResource(R.string.preferences_chats__google_map_type))
      }

      item {
        Rows.RadioListRow(
          text = stringResource(R.string.preferences__map_type),
          
          labels = stringArrayResource(R.array.pref_map_type_entries),
          values = stringArrayResource(R.array.pref_map_type_values),
          selectedValue = state.googleMapType,
          onSelected = callbacks::onSetGoogleMapTypeClicked
        )
      }
     // ----------------------------------------------------------------------
    }
  }
}

@DayNightPreviews
@Composable
private fun ChatsSettingsScreenPreview() {
  Previews.Preview {
    ChatsSettingsScreen(
      state = ChatsSettingsState(
        generateLinkPreviews = true,
        useAddressBook = true,
        keepMutedChatsArchived = true,
        useSystemEmoji = false,
        enterKeySends = false,
        localBackupsEnabled = true,
        folderCount = 1,
        userUnregistered = false,
        clientDeprecated = false
        // JW: added
        ,
        chatBackupsLocation = false,
        chatBackupsLocationApi30 = "Disabled",
        chatBackupZipfile = false,
        chatBackupZipfilePlain = false,
        keepViewOnceMessages = false,
        ignoreRemoteDelete = false,
        deleteMediaOnly = false,
        googleMapType = "normal",
        whoCanAddYouToGroups = "nonblocked"
      ),
      callbacks = ChatsSettingsCallbacks.Empty
    )
  }
}
