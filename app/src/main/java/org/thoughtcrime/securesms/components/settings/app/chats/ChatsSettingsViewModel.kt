package org.thoughtcrime.securesms.components.settings.app.chats

import android.net.Uri
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.signal.appsettings.backups.BackupCreationProgress
import org.signal.appsettings.chats.ChatExportState
import org.signal.appsettings.chats.ChatsSettingsEvents
import org.signal.appsettings.chats.ChatsSettingsState
import org.signal.core.ui.compose.EventDrivenViewModel
import org.signal.core.util.concurrent.SignalDispatchers
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.backup.toBackupCreationProgress
import org.thoughtcrime.securesms.dependencies.AppDependencies // JW: added
import org.thoughtcrime.securesms.util.UriUtils // JW: added

class ChatsSettingsViewModel : EventDrivenViewModel<ChatsSettingsEvents>(TAG) {

  companion object {
    private val TAG = Log.tag(ChatsSettingsViewModel::class)
  }

  private val _state = MutableStateFlow(
    ChatsSettingsState(
      generateLinkPreviews = ChatsSettingsRepository.isLinkPreviewsEnabled(),
      useAddressBook = ChatsSettingsRepository.isPreferSystemContactPhotos(),
      keepMutedChatsArchived = ChatsSettingsRepository.isKeepMutedChatsArchived(),
      useSystemEmoji = ChatsSettingsRepository.isPreferSystemEmoji(),
      enterKeySends = ChatsSettingsRepository.isEnterKeySends(),
      localBackupsEnabled = ChatsSettingsRepository.isLocalBackupsEnabled(),
      folderCount = 0,
      userUnregistered = ChatsSettingsRepository.isUserUnregistered(),
      clientDeprecated = ChatsSettingsRepository.isClientDeprecated(),
      isPlaintextExportEnabled = ChatsSettingsRepository.isPlaintextExportEnabled(),
      plaintextExportProgress = ChatsSettingsRepository.observePlaintextExportProgress().value.toBackupCreationProgress(),
      chatExportState = ChatExportState.None,
      shouldAutoplayStickersAndGifs = ChatsSettingsRepository.isAutoplayStickersAndGifsEnabled()
      // JW
      ,
      legacyLocalBackupEnabled = ChatsSettingsRepository.isLegacyLocalBackupEnabled(),
      chatBackupsLocation = ChatsSettingsRepository.isChatBackupLocation(),
      chatBackupsLocationApi30 = ChatsSettingsRepository.isChatBackupLocationApi30(),
      chatBackupZipfile = ChatsSettingsRepository.isChatBackupZipfile(),
      chatBackupZipfilePlain = ChatsSettingsRepository.isChatBackupZipfilePlain(),
      keepViewOnceMessages = ChatsSettingsRepository.isKeepViewOnceMessages(),
      ignoreRemoteDelete = ChatsSettingsRepository.isIgnoreRemoteDelete(),
      ignoreAdminDelete = ChatsSettingsRepository.isIgnoreAdminDelete(),
      deleteMediaOnly = ChatsSettingsRepository.isDeleteMediaOnly(),
      googleMapType = ChatsSettingsRepository.isGoogleMapType(),
      whoCanAddYouToGroups = ChatsSettingsRepository.isWhoCanAddYouToGroups(),
      generateLinkPreviewImages = ChatsSettingsRepository.isGenerateLinkPreviewImages()
    )
  )

  val state: StateFlow<ChatsSettingsState> = _state.asStateFlow()

  init {
    ChatsSettingsRepository.observePlaintextExportProgress()
      .onEach { onEvent(ChatsSettingsEvents.PlaintextExportProgressChanged(it.toBackupCreationProgress())) }
      .launchIn(viewModelScope)
  }

  override suspend fun processEvent(event: ChatsSettingsEvents) {
    when (event) {
      ChatsSettingsEvents.Refresh -> refresh()
      is ChatsSettingsEvents.PlaintextExportProgressChanged -> applyPlaintextExportProgress(event.progress)
      is ChatsSettingsEvents.GenerateLinkPreviewsChanged -> setGenerateLinkPreviewsEnabled(event.enabled)
      is ChatsSettingsEvents.UseAddressBookChanged -> setUseAddressBook(event.enabled)
      is ChatsSettingsEvents.KeepMutedChatsArchivedChanged -> setKeepMutedChatsArchived(event.enabled)
      is ChatsSettingsEvents.UseSystemEmojiChanged -> setUseSystemEmoji(event.enabled)
      is ChatsSettingsEvents.EnterKeySendsChanged -> setEnterKeySends(event.enabled)
      ChatsSettingsEvents.ExportChatHistoryAuthenticated -> _state.update { it.copy(chatExportState = ChatExportState.ConfirmExport) }
      ChatsSettingsEvents.CancelInFlightExportClicked -> cancelChatExport()
      is ChatsSettingsEvents.ExportConfirmed -> _state.update { it.copy(chatExportState = ChatExportState.ChooseAFolder, includeMediaInExport = event.withMedia) }
      is ChatsSettingsEvents.ExportFolderSelected -> startChatExportToFolder(event.uri)
      ChatsSettingsEvents.StartExportCanceled,
      ChatsSettingsEvents.ExportCompletionConfirmed -> _state.update { it.copy(chatExportState = ChatExportState.None, includeMediaInExport = false) }
      is ChatsSettingsEvents.AutoplayStickersAndGifsChanged -> setAutoplayStickersAndGifsEnabled(event.enabled)
      ChatsSettingsEvents.ChatFoldersClicked -> error("Handled in the fragment.")
      // JW: added
      is ChatsSettingsEvents.SetChatBackupLocationChanged -> setChatBackupLocation(event.enabled)
      is ChatsSettingsEvents.ChatBackupClicked -> error("Handled in the fragment.")
      is ChatsSettingsEvents.ChatBackupsLocationApi30Clicked -> error("Handled in the fragment.")
      is ChatsSettingsEvents.SetChatBackupZipfile -> setChatBackupZipfile(event.enabled)
      is ChatsSettingsEvents.SetChatBackupZipfilePlain -> setChatBackupZipfilePlain(event.enabled)
      is ChatsSettingsEvents.SetKeepViewOnceMessages -> setKeepViewOnceMessages(event.enabled)
      is ChatsSettingsEvents.SetIgnoreRemoteDelete -> setIgnoreRemoteDelete(event.enabled)
      is ChatsSettingsEvents.SetIgnoreAdminDelete -> setIgnoreAdminDelete(event.enabled)
      is ChatsSettingsEvents.SetDeleteMediaOnly -> setDeleteMediaOnly(event.enabled)
      is ChatsSettingsEvents.SetGoogleMapType -> setGoogleMapType(event.maptype)
      is ChatsSettingsEvents.SetWhoCanAddYouToGroups -> setWhoCanAddYouToGroups(event.adder)
      is ChatsSettingsEvents.SetGenerateLinkPreviewImages -> setGenerateLinkPreviewImages(event.enabled)
      // JW end
    }
  }

  private fun applyPlaintextExportProgress(progress: BackupCreationProgress) {
    _state.update {
      it.copy(
        plaintextExportProgress = progress,
        chatExportState = when (progress) {
          is BackupCreationProgress.Succeeded if it.plaintextExportProgress !is BackupCreationProgress.Succeeded -> ChatExportState.Success
          is BackupCreationProgress.Canceled -> ChatExportState.None
          else -> it.chatExportState
        }
      )
    }
  }

  private fun startChatExportToFolder(uri: Uri) {
    _state.update { it.copy(chatExportState = ChatExportState.None) }
    ChatsSettingsRepository.startPlaintextExport(uri, _state.value.includeMediaInExport)
  }

  private fun cancelChatExport() {
    _state.update { it.copy(chatExportState = ChatExportState.Canceling) }
    ChatsSettingsRepository.cancelPlaintextExport()
  }

  private fun setGenerateLinkPreviewsEnabled(enabled: Boolean) {
    _state.update { it.copy(generateLinkPreviews = enabled) }
    ChatsSettingsRepository.setLinkPreviewsEnabled(enabled)
  }

  private fun setAutoplayStickersAndGifsEnabled(enabled: Boolean) {
    _state.update { it.copy(shouldAutoplayStickersAndGifs = enabled) }
    ChatsSettingsRepository.setAutoplayStickersAndGifsEnabled(enabled)
  }

  private fun setUseAddressBook(enabled: Boolean) {
    _state.update { it.copy(useAddressBook = enabled) }
    ChatsSettingsRepository.setPreferSystemContactPhotos(enabled)
  }

  private fun setKeepMutedChatsArchived(enabled: Boolean) {
    _state.update { it.copy(keepMutedChatsArchived = enabled) }
    ChatsSettingsRepository.setKeepMutedChatsArchived(enabled)
  }

  private fun setUseSystemEmoji(enabled: Boolean) {
    _state.update { it.copy(useSystemEmoji = enabled) }
    ChatsSettingsRepository.setPreferSystemEmoji(enabled)
  }

  private fun setEnterKeySends(enabled: Boolean) {
    _state.update { it.copy(enterKeySends = enabled) }
    ChatsSettingsRepository.setEnterKeySends(enabled)
  }

  private suspend fun refresh() {
    val (count, backupsEnabled) = withContext(SignalDispatchers.Default) {
      ChatsSettingsRepository.getFolderCount() to ChatsSettingsRepository.isLocalBackupsEnabled()
    }

    _state.update { it.copy(folderCount = count, localBackupsEnabled = backupsEnabled) }
  }

  // JW: added
  private fun setChatBackupLocationApi30(directory: String) {
    _state.update { it.copy(chatBackupsLocationApi30 = directory) }
    ChatsSettingsRepository.setChatBackupsLocationApi30(directory)
  }

  private fun setChatBackupLocation(enabled: Boolean) {
    _state.update { it.copy(chatBackupsLocation = enabled) }
    ChatsSettingsRepository.setChatBackupLocation(enabled)
  }

  private fun setChatBackupZipfile(enabled: Boolean) {
    _state.update { it.copy(chatBackupZipfile = enabled) }
    ChatsSettingsRepository.setChatBackupZipfile(enabled)
  }

  private fun setChatBackupZipfilePlain(enabled: Boolean) {
    _state.update { it.copy(chatBackupZipfilePlain = enabled) }
    ChatsSettingsRepository.setChatBackupZipfilePlain(enabled)
  }

  private fun setKeepViewOnceMessages(enabled: Boolean) {
    _state.update { it.copy(keepViewOnceMessages = enabled) }
    ChatsSettingsRepository.setKeepViewOnceMessages(enabled)
  }

  private fun setIgnoreRemoteDelete(enabled: Boolean) {
    _state.update { it.copy(ignoreRemoteDelete = enabled) }
    ChatsSettingsRepository.setIgnoreRemoteDelete(enabled)
  }

  private fun setIgnoreAdminDelete(enabled: Boolean) {
    _state.update { it.copy(ignoreAdminDelete = enabled) }
    ChatsSettingsRepository.setIgnoreAdminDelete(enabled)
  }

  private fun setDeleteMediaOnly(enabled: Boolean) {
    _state.update { it.copy(deleteMediaOnly = enabled) }
    ChatsSettingsRepository.setDeleteMediaOnly(enabled)
  }

  private fun setGoogleMapType(maptype: String) {
    _state.update { it.copy(googleMapType = maptype) }
    ChatsSettingsRepository.setGoogleMapType(maptype)
  }

  private fun setWhoCanAddYouToGroups(adder: String) {
    _state.update { it.copy(whoCanAddYouToGroups = adder) }
    ChatsSettingsRepository.setWhoCanAddYouToGroups(adder)
  }

  private fun setGenerateLinkPreviewImages(enabled: Boolean) {
    _state.update { it.copy(generateLinkPreviewImages = enabled) }
    ChatsSettingsRepository.setGenerateLinkPreviewImages(enabled)
  }
  // JW: end
}
