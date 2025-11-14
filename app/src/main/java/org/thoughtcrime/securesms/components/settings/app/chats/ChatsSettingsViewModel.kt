package org.thoughtcrime.securesms.components.settings.app.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.thoughtcrime.securesms.components.settings.app.chats.folders.ChatFoldersRepository
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.util.BackupUtil
import org.thoughtcrime.securesms.util.ConversationUtil
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.thoughtcrime.securesms.util.ThrottledDebouncer
import org.thoughtcrime.securesms.util.UriUtils // JW: added

class ChatsSettingsViewModel @JvmOverloads constructor(
  private val repository: ChatsSettingsRepository = ChatsSettingsRepository()
) : ViewModel() {

  private val refreshDebouncer = ThrottledDebouncer(500L)

  private val store = MutableStateFlow(
    ChatsSettingsState(
      generateLinkPreviews = SignalStore.settings.isLinkPreviewsEnabled,
      useAddressBook = SignalStore.settings.isPreferSystemContactPhotos,
      keepMutedChatsArchived = SignalStore.settings.shouldKeepMutedChatsArchived(),
      useSystemEmoji = SignalStore.settings.isPreferSystemEmoji,
      enterKeySends = SignalStore.settings.isEnterKeySends,
      localBackupsEnabled = SignalStore.settings.isBackupEnabled && BackupUtil.canUserAccessBackupDirectory(AppDependencies.application),
      folderCount = 0,
      userUnregistered = TextSecurePreferences.isUnauthorizedReceived(AppDependencies.application) || !SignalStore.account.isRegistered,
      clientDeprecated = SignalStore.misc.isClientDeprecated
      // JW: added
      ,
      chatBackupsLocation = TextSecurePreferences.isBackupLocationRemovable(AppDependencies.application),
      chatBackupsLocationApi30 = UriUtils.getFullPathFromTreeUri(AppDependencies.application, SignalStore.settings.signalBackupDirectory),
      chatBackupZipfile = TextSecurePreferences.isRawBackupInZipfile(AppDependencies.application),
      chatBackupZipfilePlain = TextSecurePreferences.isPlainBackupInZipfile(AppDependencies.application),
      keepViewOnceMessages = TextSecurePreferences.isKeepViewOnceMessages(AppDependencies.application),
      ignoreRemoteDelete = TextSecurePreferences.isIgnoreRemoteDelete(AppDependencies.application),
      deleteMediaOnly = TextSecurePreferences.isDeleteMediaOnly(AppDependencies.application),
      googleMapType = TextSecurePreferences.getGoogleMapType(AppDependencies.application),
      whoCanAddYouToGroups = TextSecurePreferences.whoCanAddYouToGroups(AppDependencies.application)
    )
  )

  val state: StateFlow<ChatsSettingsState> = store

  fun setGenerateLinkPreviewsEnabled(enabled: Boolean) {
    store.update { it.copy(generateLinkPreviews = enabled) }
    SignalStore.settings.isLinkPreviewsEnabled = enabled
    repository.syncLinkPreviewsState()
  }

  fun setUseAddressBook(enabled: Boolean) {
    store.update { it.copy(useAddressBook = enabled) }
    refreshDebouncer.publish { ConversationUtil.refreshRecipientShortcuts() }
    SignalStore.settings.isPreferSystemContactPhotos = enabled
    repository.syncPreferSystemContactPhotos()
  }

  fun setKeepMutedChatsArchived(enabled: Boolean) {
    store.update { it.copy(keepMutedChatsArchived = enabled) }
    SignalStore.settings.setKeepMutedChatsArchived(enabled)
    repository.syncKeepMutedChatsArchivedState()
  }

  fun setUseSystemEmoji(enabled: Boolean) {
    store.update { it.copy(useSystemEmoji = enabled) }
    SignalStore.settings.isPreferSystemEmoji = enabled
  }

  fun setEnterKeySends(enabled: Boolean) {
    store.update { it.copy(enterKeySends = enabled) }
    SignalStore.settings.isEnterKeySends = enabled
  }

  fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      val count = ChatFoldersRepository.getFolderCount()
      val backupsEnabled = SignalStore.settings.isBackupEnabled && BackupUtil.canUserAccessBackupDirectory(AppDependencies.application)

      if (store.value.localBackupsEnabled != backupsEnabled) {
        store.update {
          it.copy(
            folderCount = count,
            localBackupsEnabled = backupsEnabled
          )
        }
      } else {
        store.update {
          it.copy(
            folderCount = count
          )
        }
      }
    }
    // JW: added. This is required to update the UI for settings that are not in the Signal store but in the shared preferences.
    store.update { getState().copy() }
  }

  // JW: added
  fun setChatBackupLocation(enabled: Boolean) {
    TextSecurePreferences.setBackupLocationRemovable(AppDependencies.application, enabled)
    TextSecurePreferences.setBackupLocationChanged(AppDependencies.application, true) // Used in BackupUtil.getAllBackupsNewestFirst()
    refresh()
  }

  // JW: added
  fun setChatBackupLocationApi30(value: String) {
    refresh()
  }

  // JW: added
  fun setChatBackupZipfile(enabled: Boolean) {
    TextSecurePreferences.setRawBackupZipfile(AppDependencies.application, enabled)
    refresh()
  }

  // JW: added
  fun setChatBackupZipfilePlain(enabled: Boolean) {
    TextSecurePreferences.setPlainBackupZipfile(AppDependencies.application, enabled)
    refresh()
  }

  // JW: added
  fun keepViewOnceMessages(enabled: Boolean) {
    TextSecurePreferences.setKeepViewOnceMessages(AppDependencies.application, enabled)
    refresh()
  }

  // JW: added
  fun setIgnoreRemoteDelete(enabled: Boolean) {
    TextSecurePreferences.setIgnoreRemoteDelete(AppDependencies.application, enabled)
    refresh()
  }

  // JW: added
  fun setDeleteMediaOnly(enabled: Boolean) {
    TextSecurePreferences.setDeleteMediaOnly(AppDependencies.application, enabled)
    refresh()
  }

  // JW: added
  fun setGoogleMapType(mapType: String) {
    TextSecurePreferences.setGoogleMapType(AppDependencies.application, mapType)
    refresh()
  }

  // JW: added
  fun setWhoCanAddYouToGroups(adder: String) {
    TextSecurePreferences.setWhoCanAddYouToGroups(AppDependencies.application, adder)
    refresh()
  }
  
  // JW: added
  private fun getState() = ChatsSettingsState(
    generateLinkPreviews = SignalStore.settings.isLinkPreviewsEnabled,
    useAddressBook = SignalStore.settings.isPreferSystemContactPhotos,
    keepMutedChatsArchived = SignalStore.settings.shouldKeepMutedChatsArchived(),
    useSystemEmoji = SignalStore.settings.isPreferSystemEmoji,
    enterKeySends = SignalStore.settings.isEnterKeySends,
    localBackupsEnabled = SignalStore.settings.isBackupEnabled,
    folderCount = ChatFoldersRepository.getFolderCount(),
    userUnregistered = TextSecurePreferences.isUnauthorizedReceived(AppDependencies.application) || !SignalStore.account.isRegistered,
    clientDeprecated = SignalStore.misc.isClientDeprecated,
    chatBackupsLocationApi30 = UriUtils.getFullPathFromTreeUri(AppDependencies.application, SignalStore.settings.signalBackupDirectory),
    chatBackupsLocation = TextSecurePreferences.isBackupLocationRemovable(AppDependencies.application),
    chatBackupZipfile = TextSecurePreferences.isRawBackupInZipfile(AppDependencies.application),
    chatBackupZipfilePlain = TextSecurePreferences.isPlainBackupInZipfile(AppDependencies.application),
    keepViewOnceMessages = TextSecurePreferences.isKeepViewOnceMessages(AppDependencies.application),
    ignoreRemoteDelete = TextSecurePreferences.isIgnoreRemoteDelete(AppDependencies.application),
    deleteMediaOnly = TextSecurePreferences.isDeleteMediaOnly(AppDependencies.application),
    googleMapType = TextSecurePreferences.getGoogleMapType(AppDependencies.application),
    whoCanAddYouToGroups = TextSecurePreferences.whoCanAddYouToGroups(AppDependencies.application)
  )
}
