package org.thoughtcrime.securesms.components.settings.app.chats

import org.thoughtcrime.securesms.backup.LocalExportProgress
import org.thoughtcrime.securesms.keyvalue.protos.LocalBackupCreationProgress

data class ChatsSettingsState(
  val generateLinkPreviews: Boolean,
  val useAddressBook: Boolean,
  val keepMutedChatsArchived: Boolean,
  val useSystemEmoji: Boolean,
  val enterKeySends: Boolean,
  val localBackupsEnabled: Boolean,
  val folderCount: Int,
  val userUnregistered: Boolean,
  val clientDeprecated: Boolean,
  val isPlaintextExportEnabled: Boolean,
  val plaintextExportProgress: LocalBackupCreationProgress = LocalExportProgress.plaintextProgress.value,
  val chatExportState: ChatExportState = ChatExportState.None,
  val includeMediaInExport: Boolean = false
  // JW: added extra preferences
  ,
  val chatBackupsLocation: Boolean,
  val chatBackupsLocationApi30: String,
  val chatBackupZipfile: Boolean,
  val chatBackupZipfilePlain: Boolean,
  val keepViewOnceMessages: Boolean,
  val ignoreRemoteDelete: Boolean,
  val ignoreAdminDelete: Boolean,
  val deleteMediaOnly: Boolean,
  val googleMapType: String,
  val whoCanAddYouToGroups: String
) {
  fun isRegisteredAndUpToDate(): Boolean {
    return !userUnregistered && !clientDeprecated
  }
}
