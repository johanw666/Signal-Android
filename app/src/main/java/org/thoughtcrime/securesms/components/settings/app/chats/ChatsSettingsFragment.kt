package org.thoughtcrime.securesms.components.settings.app.chats

import android.app.Activity // JW: added
import android.content.Intent // JW: added
import android.os.Build // JW: added
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager // JW: added
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.backup.BackupDialog // JW: added
import org.thoughtcrime.securesms.components.settings.DSLConfiguration
import org.thoughtcrime.securesms.components.settings.DSLSettingsAdapter
import org.thoughtcrime.securesms.components.settings.DSLSettingsFragment
import org.thoughtcrime.securesms.components.settings.DSLSettingsText
import org.thoughtcrime.securesms.components.settings.configure
import org.thoughtcrime.securesms.keyvalue.SignalStore // JW: added
import org.thoughtcrime.securesms.util.navigation.safeNavigate
import org.thoughtcrime.securesms.util.UriUtils // JW: added


class ChatsSettingsFragment : DSLSettingsFragment(R.string.preferences_chats__chats) {

  private lateinit var viewModel: ChatsSettingsViewModel

  private val mapLabels by lazy { resources.getStringArray(R.array.pref_map_type_entries) } // JW: added
  private val mapValues by lazy { resources.getStringArray(R.array.pref_map_type_values) }  // JW: added
  val CHOOSE_BACKUPS_LOCATION_REQUEST_CODE = 1201 // JW: added

  override fun onResume() {
    super.onResume()
    viewModel.refresh()
  }

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    val repository = ChatsSettingsRepository()
    val preferences = PreferenceManager.getDefaultSharedPreferences(context) // JW: added
    val factory = ChatsSettingsViewModel.Factory(preferences, repository) // JW: added preferences
    viewModel = ViewModelProvider(this, factory)[ChatsSettingsViewModel::class.java]

    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  // JW: added
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (data != null && data.data != null) {
      if (resultCode == Activity.RESULT_OK) {
        if (requestCode == CHOOSE_BACKUPS_LOCATION_REQUEST_CODE) {
          val backupUri = data.data
          SignalStore.settings().setSignalBackupDirectory(backupUri!!)
          viewModel.setChatBackupLocationApi30(UriUtils.getFullPathFromTreeUri(context, backupUri))
        }
      }
    }
  }

  private fun getConfiguration(state: ChatsSettingsState): DSLConfiguration {
    return configure {

      clickPref(
        title = DSLSettingsText.from(R.string.preferences__sms_mms),
        onClick = {
          Navigation.findNavController(requireView()).safeNavigate(R.id.action_chatsSettingsFragment_to_smsSettingsFragment)
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__generate_link_previews),
        summary = DSLSettingsText.from(R.string.preferences__retrieve_link_previews_from_websites_for_messages),
        isChecked = state.generateLinkPreviews,
        onClick = {
          viewModel.setGenerateLinkPreviewsEnabled(!state.generateLinkPreviews)
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.preferences__pref_use_address_book_photos),
        summary = DSLSettingsText.from(R.string.preferences__display_contact_photos_from_your_address_book_if_available),
        isChecked = state.useAddressBook,
        onClick = {
          viewModel.setUseAddressBook(!state.useAddressBook)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.ChatsSettingsFragment__keyboard)

      switchPref(
        title = DSLSettingsText.from(R.string.preferences_advanced__use_system_emoji),
        isChecked = state.useSystemEmoji,
        onClick = {
          viewModel.setUseSystemEmoji(!state.useSystemEmoji)
        }
      )

      switchPref(
        title = DSLSettingsText.from(R.string.ChatsSettingsFragment__enter_key_sends),
        isChecked = state.enterKeySends,
        onClick = {
          viewModel.setEnterKeySends(!state.enterKeySends)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.preferences_chats__backups)

      clickPref(
        title = DSLSettingsText.from(R.string.preferences_chats__chat_backups),
        summary = DSLSettingsText.from(if (state.chatBackupsEnabled) R.string.arrays__enabled else R.string.arrays__disabled),
        onClick = {
          Navigation.findNavController(requireView()).safeNavigate(R.id.action_chatsSettingsFragment_to_backupsPreferenceFragment)
        }
      )

      // JW: added
      if (Build.VERSION.SDK_INT < 30) {
        switchPref(
          title = DSLSettingsText.from(R.string.preferences_chats__chat_backups_removable),
          summary = DSLSettingsText.from(R.string.preferences_chats__backup_chats_to_removable_storage),
          isChecked = state.chatBackupsLocation,
          onClick = {
            viewModel.setChatBackupLocation(!state.chatBackupsLocation)
          }
        )
      } else {
        val backupUri = SignalStore.settings().signalBackupDirectory
        val summaryText = UriUtils.getFullPathFromTreeUri(context, backupUri)

        clickPref(
          title = DSLSettingsText.from(R.string.preferences_chats__chat_backups_removable),
          summary = DSLSettingsText.from(summaryText),
          onClick = {
            BackupDialog.showChooseBackupLocationDialog(this@ChatsSettingsFragment, CHOOSE_BACKUPS_LOCATION_REQUEST_CODE)
            viewModel.setChatBackupLocationApi30(UriUtils.getFullPathFromTreeUri(context, backupUri))
          }
        )
      }

      // JW: added
      switchPref(
        title = DSLSettingsText.from(R.string.preferences_chats__chat_backups_zipfile),
        summary = DSLSettingsText.from(R.string.preferences_chats__backup_chats_to_encrypted_zipfile),
        isChecked = state.chatBackupZipfile,
        onClick = {
          viewModel.setChatBackupZipfile(!state.chatBackupZipfile)
        }
      )

      // JW: added
      switchPref(
        title = DSLSettingsText.from(R.string.preferences_chats__chat_backups_zipfile_plain),
        summary = DSLSettingsText.from(R.string.preferences_chats__backup_chats_to_encrypted_zipfile_plain),
        isChecked = state.chatBackupZipfilePlain,
        onClick = {
          viewModel.setChatBackupZipfilePlain(!state.chatBackupZipfilePlain)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.preferences_chats__control_message_deletion)

      // JW: added
      switchPref(
        title = DSLSettingsText.from(R.string.preferences_chats__chat_keep_view_once_messages),
        summary = DSLSettingsText.from(R.string.preferences_chats__keep_view_once_messages_summary),
        isChecked = state.keepViewOnceMessages,
        onClick = {
          viewModel.keepViewOnceMessages(!state.keepViewOnceMessages)
        }
      )

      // JW: added
      switchPref(
        title = DSLSettingsText.from(R.string.preferences_chats__chat_ignore_remote_delete),
        summary = DSLSettingsText.from(R.string.preferences_chats__chat_ignore_remote_delete_summary),
        isChecked = state.ignoreRemoteDelete,
        onClick = {
          viewModel.ignoreRemoteDelete(!state.ignoreRemoteDelete)
        }
      )

      // JW: added
      switchPref(
        title = DSLSettingsText.from(R.string.preferences_chats__delete_media_only),
        summary = DSLSettingsText.from(R.string.preferences_chats__delete_media_only_summary),
        isChecked = state.deleteMediaOnly,
        onClick = {
          viewModel.deleteMediaOnly(!state.deleteMediaOnly)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.preferences_chats__group_control)

      // JW: added
      switchPref(
        title = DSLSettingsText.from(R.string.preferences_chats__can_blocked_contacts_add_you_to_groups),
        summary = DSLSettingsText.from(R.string.preferences_chats__can_blocked_contacts_add_you_to_groups_summary),
        isChecked = state.blockedContactsCantAddYouToGroups,
        onClick = {
          viewModel.setBlockedCanAddYouToGroups(!state.blockedContactsCantAddYouToGroups)
        }
      )

      dividerPref()

      sectionHeaderPref(R.string.preferences_chats__google_map_type)

      // JW: added
      radioListPref(
        title = DSLSettingsText.from(R.string.preferences__map_type),
        listItems = mapLabels,
        selected = mapValues.indexOf(state.googleMapType),
        onSelected = {
          viewModel.setGoogleMapType(mapValues[it])
        }
      )
    }
  }
}
