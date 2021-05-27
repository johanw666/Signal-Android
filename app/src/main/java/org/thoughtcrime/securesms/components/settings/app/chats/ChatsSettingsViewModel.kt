package org.thoughtcrime.securesms.components.settings.app.chats

import android.content.Context // JW: added
import android.content.SharedPreferences // JW: added
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.thoughtcrime.securesms.components.settings.app.privacy.advanced.AdvancedPrivacySettingsState // JW: added
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies
import org.thoughtcrime.securesms.jobs.MultiDeviceContactUpdateJob
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.storage.StorageSyncHelper
import org.thoughtcrime.securesms.util.ConversationUtil
import org.thoughtcrime.securesms.util.TextSecurePreferences // JW: added
import org.thoughtcrime.securesms.util.ThrottledDebouncer
import org.thoughtcrime.securesms.util.livedata.Store

// JW: changed, added sharedPreferences
class ChatsSettingsViewModel(private val sharedPreferences: SharedPreferences, private val repository: ChatsSettingsRepository) : ViewModel() {

  private val refreshDebouncer = ThrottledDebouncer(500L)
  private val context: Context = ApplicationDependencies.getApplication() // JW: added

  private val store: Store<ChatsSettingsState> = Store(
    ChatsSettingsState(
      generateLinkPreviews = SignalStore.settings().isLinkPreviewsEnabled,
      useAddressBook = SignalStore.settings().isPreferSystemContactPhotos,
      useSystemEmoji = SignalStore.settings().isPreferSystemEmoji,
      enterKeySends = SignalStore.settings().isEnterKeySends,
      chatBackupsEnabled = SignalStore.settings().isBackupEnabled
      // JW: added
      ,
      chatBackupsLocation = TextSecurePreferences.isBackupLocationRemovable(ApplicationDependencies.getApplication()),
      chatBackupZipfile = TextSecurePreferences.isRawBackupInZipfile(ApplicationDependencies.getApplication()),
      chatBackupZipfilePlain = TextSecurePreferences.isPlainBackupInZipfile(ApplicationDependencies.getApplication()),
      keepViewOnceMessages = TextSecurePreferences.isKeepViewOnceMessages(ApplicationDependencies.getApplication()),
      ignoreRemoteDelete = TextSecurePreferences.isIgnoreRemoteDelete(ApplicationDependencies.getApplication()),
      deleteMediaOnly = TextSecurePreferences.isDeleteMediaOnly(ApplicationDependencies.getApplication()),
      googleMapType = TextSecurePreferences.getGoogleMapType(ApplicationDependencies.getApplication())
    )
  )

  val state: LiveData<ChatsSettingsState> = store.stateLiveData

  fun setGenerateLinkPreviewsEnabled(enabled: Boolean) {
    store.update { it.copy(generateLinkPreviews = enabled) }
    SignalStore.settings().isLinkPreviewsEnabled = enabled
    repository.syncLinkPreviewsState()
  }

  fun setUseAddressBook(enabled: Boolean) {
    store.update { it.copy(useAddressBook = enabled) }
    SignalStore.settings().isPreferSystemContactPhotos = enabled
    refreshDebouncer.publish { ConversationUtil.refreshRecipientShortcuts() }
    ApplicationDependencies.getJobManager().add(MultiDeviceContactUpdateJob(true))
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  fun setUseSystemEmoji(enabled: Boolean) {
    store.update { it.copy(useSystemEmoji = enabled) }
    SignalStore.settings().isPreferSystemEmoji = enabled
  }

  fun setEnterKeySends(enabled: Boolean) {
    store.update { it.copy(enterKeySends = enabled) }
    SignalStore.settings().isEnterKeySends = enabled
  }

  // JW: changed, added sharedPreferences
  class Factory(private val sharedPreferences: SharedPreferences, private val repository: ChatsSettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return requireNotNull(modelClass.cast(ChatsSettingsViewModel(sharedPreferences, repository)))
    }
  }

  // JW: added
  fun setChatBackupLocation(enabled: Boolean) {
    TextSecurePreferences.setBackupLocationRemovable(context, enabled)
    TextSecurePreferences.setBackupLocationChanged(context, true) // Used in BackupUtil.getAllBackupsNewestFirst()
    refresh()
  }

  // JW: added
  fun setChatBackupZipfile(enabled: Boolean) {
    TextSecurePreferences.setRawBackupZipfile(context, enabled)
    refresh()
  }

  // JW: added
  fun setChatBackupZipfilePlain(enabled: Boolean) {
    TextSecurePreferences.setPlainBackupZipfile(context, enabled)
    refresh()
  }

  // JW: added
  fun keepViewOnceMessages(enabled: Boolean) {
    TextSecurePreferences.setKeepViewOnceMessages(context, enabled)
    refresh()
  }

  // JW: added
  fun ignoreRemoteDelete(enabled: Boolean) {
    TextSecurePreferences.setIgnoreRemoteDelete(context, enabled)
    refresh()
  }

  // JW: added
  fun deleteMediaOnly(enabled: Boolean) {
    TextSecurePreferences.setDeleteMediaOnly(context, enabled)
    refresh()
  }

  // JW: added
  fun setGoogleMapType(mapType: String) {
    TextSecurePreferences.setGoogleMapType(context, mapType)
    refresh()
  }

  // JW: added. This is required to update the UI for settings that are not in the
  // Signal store but in the shared preferences.
  fun refresh() {
    store.update { getState().copy() }
  }

  // JW: added
  private fun getState() = ChatsSettingsState(
    generateLinkPreviews = SignalStore.settings().isLinkPreviewsEnabled,
    useAddressBook = SignalStore.settings().isPreferSystemContactPhotos,
    useSystemEmoji = SignalStore.settings().isPreferSystemEmoji,
    enterKeySends = SignalStore.settings().isEnterKeySends,
    chatBackupsEnabled = SignalStore.settings().isBackupEnabled,
    chatBackupsLocation = TextSecurePreferences.isBackupLocationRemovable(context),
    chatBackupZipfile = TextSecurePreferences.isRawBackupInZipfile(context),
    chatBackupZipfilePlain = TextSecurePreferences.isPlainBackupInZipfile(context),
    keepViewOnceMessages = TextSecurePreferences.isKeepViewOnceMessages(context),
    ignoreRemoteDelete = TextSecurePreferences.isIgnoreRemoteDelete(context),
    deleteMediaOnly = TextSecurePreferences.isDeleteMediaOnly(context),
    googleMapType = TextSecurePreferences.getGoogleMapType(context)
  )


}
