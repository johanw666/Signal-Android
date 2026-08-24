package org.thoughtcrime.securesms.mediapreview

import android.content.Context
import android.content.Intent
import android.text.SpannableString
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import org.signal.core.models.database.AttachmentId
import org.signal.core.util.Stopwatch
import org.signal.core.util.logging.Log
import org.signal.core.util.requireLong
import org.thoughtcrime.securesms.attachments.DatabaseAttachment
import org.thoughtcrime.securesms.conversation.ConversationIntents
import org.thoughtcrime.securesms.database.AttachmentTable
import org.thoughtcrime.securesms.database.MediaTable
import org.thoughtcrime.securesms.database.MediaTable.Sorting
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.database.SignalDatabase.Companion.media
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.database.model.MmsMessageRecord
import org.thoughtcrime.securesms.database.withAttachments
import org.thoughtcrime.securesms.jobs.MultiDeviceDeleteSyncJob
import org.thoughtcrime.securesms.longmessage.resolveBody
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.sms.MessageSender
import org.thoughtcrime.securesms.util.AttachmentUtil

/**
 * Repository for accessing the attachments in the encrypted database.
 */
class MediaPreviewRepository {
  companion object {
    private val TAG: String = Log.tag(MediaPreviewRepository::class.java)
  }

  /**
   * Accessor for database attachments.
   * @param startingAttachmentId the initial position to select from
   * @param threadId the thread to select from
   * @param sorting the ordering of the results
   * @param limit the maximum quantity of the results
   */
  fun getAttachments(context: Context, startingAttachmentId: AttachmentId, threadId: Long, sorting: Sorting, limit: Int = 500): Flowable<Result> {
    return Single.fromCallable {
      val stopwatch = Stopwatch("Attachment Window")

      media.getGalleryMediaForThread(threadId, sorting).use { cursor ->
        val mediaRecords = mutableListOf<MediaTable.MediaRecord>()
        var startingRow = -1
        while (cursor.moveToNext()) {
          if (startingAttachmentId.id == cursor.requireLong(AttachmentTable.ID)) {
            startingRow = cursor.position
            break
          }
        }
        stopwatch.split("find starting row")

        var itemPosition = -1
        if (startingRow >= 0) {
          val frontLimit: Int = limit / 2
          val windowStart = if (startingRow >= frontLimit) startingRow - frontLimit else 0

          cursor.moveToPosition(windowStart)

          for (i in 0..limit) {
            val element = MediaTable.MediaRecord.from(cursor)
            if (element.attachment?.isDisplayable() == true) {
              mediaRecords.add(element)

              if (startingAttachmentId.id == cursor.requireLong(AttachmentTable.ID)) {
                itemPosition = mediaRecords.lastIndex
              }
            }

            if (!cursor.moveToNext()) {
              break
            }
          }

          if (itemPosition == -1) {
            Log.w(TAG, "Unable to find target image for $startingAttachmentId")
          }
        }
        stopwatch.split("build window of ${mediaRecords.size}")

        stopwatch.stop(TAG)
        Result(if (mediaRecords.isNotEmpty()) itemPosition.coerceIn(mediaRecords.indices) else itemPosition, mediaRecords)
      }
    }.subscribeOn(Schedulers.io()).toFlowable()
  }

  /**
   * Primary-key read of a single attachment, so the tapped media can be rendered without waiting on
   * [getAttachments] to sort and materialize the whole attachment window. Empty when the id does not
   * name a displayable attachment, which is the normal case for previews of draft media.
   */
  fun getInitialAttachment(attachmentId: AttachmentId): Maybe<DatabaseAttachment> {
    return Maybe.fromCallable<DatabaseAttachment> {
      SignalDatabase.attachments.getAttachment(attachmentId)?.takeIf { it.isDisplayable() }
    }.subscribeOn(Schedulers.io())
  }

  /** Matches the filter [getAttachments] applies when building its window, so both agree on what can be paged to. */
  private fun DatabaseAttachment.isDisplayable(): Boolean {
    return transferState == AttachmentTable.TRANSFER_PROGRESS_DONE ||
      transferState == AttachmentTable.TRANSFER_PROGRESS_STARTED ||
      thumbnailUri != null
  }

  fun resolveMessageBodies(context: Context, messageIds: Set<Long>): Single<Map<Long, SpannableString>> {
    return Single.fromCallable {
      SignalDatabase.messages.getMessages(messageIds).toList().withAttachments()
        .filterIsInstance<MmsMessageRecord>()
        .associate { it.id to it.resolveBody(context).getDisplayBody(context) }
    }.subscribeOn(Schedulers.io())
  }

  fun localDelete(attachment: DatabaseAttachment): Completable {
    return Completable.fromRunnable {
      val deletedMessageRecord = AttachmentUtil.deleteAttachment(attachment)
      if (deletedMessageRecord != null) {
        MultiDeviceDeleteSyncJob.enqueueMessageDeletes(setOf(deletedMessageRecord))
      }
    }.subscribeOn(Schedulers.io())
  }

  fun remoteDelete(attachment: DatabaseAttachment): Completable {
    return Completable.fromRunnable {
      MessageSender.sendRemoteDelete(attachment.mmsId)
    }.subscribeOn(Schedulers.io())
  }

  fun getMessagePositionIntent(context: Context, messageId: Long): Single<Intent> {
    return Single.fromCallable {
      val stopwatch = Stopwatch("Message Position Intent")
      val messageRecord: MessageRecord = SignalDatabase.messages.getMessageRecord(messageId)
      stopwatch.split("get message record")

      val threadId: Long = messageRecord.threadId
      val messagePosition: Int = SignalDatabase.messages.getMessagePositionInConversation(threadId, messageRecord.dateReceived)
      stopwatch.split("get message position")

      val recipientId: RecipientId = SignalDatabase.threads.getRecipientForThreadId(threadId)?.id ?: throw IllegalStateException("Could not find recipient for thread ID $threadId")
      stopwatch.split("get recipient ID")

      stopwatch.stop(TAG)
      ConversationIntents.createBuilderSync(context, recipientId, threadId)
        .withStartingPosition(messagePosition)
        .build()
    }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
  }

  data class Result(val initialPosition: Int, val records: List<MediaTable.MediaRecord>)
}
