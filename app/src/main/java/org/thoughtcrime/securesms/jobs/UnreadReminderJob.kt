package org.thoughtcrime.securesms.jobs

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.signal.core.util.PendingIntentFlags
import org.signal.core.util.ServiceUtil
import org.signal.core.util.Stopwatch
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.MainActivity
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.components.settings.app.notifications.ReminderType
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.jobmanager.Job
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.notifications.NotificationChannels
import org.thoughtcrime.securesms.notifications.NotificationIds
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.util.RemoteConfig

/**
 * Job that periodically runs and sends an unread reminder for muted chats.
 */
class UnreadReminderJob(parameters: Parameters) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(UnreadReminderJob::class.java)
    const val KEY = "UnreadReminderJob"

    @JvmStatic
    fun enqueue() {
      if (!RemoteConfig.internalUser || !NotificationChannels.getInstance().areNotificationsEnabled()) {
        return
      }

      AppDependencies.jobManager.add(
        UnreadReminderJob(
          parameters = Parameters.Builder()
            .setGlobalPriority(Parameters.PRIORITY_LOWER)
            .setMaxInstancesForFactory(1)
            .build()
        )
      )
    }

    @VisibleForTesting
    fun create(): UnreadReminderJob {
      return UnreadReminderJob(
        Parameters.Builder()
          .setGlobalPriority(Parameters.PRIORITY_LOWER)
          .setMaxInstancesForFactory(1)
          .build()
      )
    }
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    val stopwatch = Stopwatch("unread-reminder")
    if (!SignalStore.account.isRegistered) {
      Log.w(TAG, "Not registered. Skipping.")
      return Result.success()
    }

    val hideAuthors = SignalStore.settings.messageNotificationsPrivacy.isDisplayNothing

    // Get all muted threads that have opted for reminders. If notification privacy is on, ignore mentions and replies.
    val messageThreadIds = SignalDatabase.threads.getMutedThreadIds(ReminderType.MESSAGES)
    val callThreadIds = SignalDatabase.threads.getMutedThreadIds(ReminderType.CALLS)
    val mentionThreadIds = if (hideAuthors) emptyList() else SignalDatabase.threads.getMutedThreadIds(ReminderType.MENTIONS)
    val replyThreadIds = if (hideAuthors) emptyList() else SignalDatabase.threads.getMutedThreadIds(ReminderType.REPLIES)
    stopwatch.split("fetch-threads")

    // Get the unread counts/authors
    val (messages, unreadAuthorIds) = getUnreadForReminder(messageThreadIds, ReminderType.MESSAGES)
    stopwatch.split("fetch-messages")
    val (calls, callsAuthorIds) = getUnreadForReminder(callThreadIds, ReminderType.CALLS)
    stopwatch.split("fetch-calls")
    val (mentions, mentionsAuthorIds) = getUnreadForReminder(mentionThreadIds, ReminderType.MENTIONS)
    stopwatch.split("fetch-mentions")
    val (replies, repliesAuthorIds) = getUnreadForReminder(replyThreadIds, ReminderType.REPLIES)
    stopwatch.split("fetch-replies")

    val summary = buildSummary(
      context = context,
      messages = messages,
      calls = calls,
      mentions = mentions,
      replies = replies,
      messageAndCallAuthors = (unreadAuthorIds + callsAuthorIds).distinct().map { Recipient.resolved(it).getShortDisplayName(context) },
      mentionAuthors = mentionsAuthorIds.map { Recipient.resolved(it).getShortDisplayName(context) },
      replyAuthors = repliesAuthorIds.map { Recipient.resolved(it).getShortDisplayName(context) },
      hideAuthors = hideAuthors
    )

    if (summary.isEmpty()) {
      ServiceUtil.getNotificationManager(context).cancel(NotificationIds.UNREAD_REMINDER)
    } else if (NotificationChannels.getInstance().areNotificationsEnabled()) {
      val builder = NotificationCompat.Builder(context, NotificationChannels.getInstance().ADDITIONAL_MESSAGE_NOTIFICATIONS)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentText(summary)
        .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
        .setContentIntent(PendingIntent.getActivity(context, 0, MainActivity.clearTop(context), PendingIntentFlags.mutable()))
        .setAutoCancel(true)

      ContextCompat.getSystemService(context, NotificationManager::class.java)!!.notify(NotificationIds.UNREAD_REMINDER, builder.build())
    }

    stopwatch.stop(TAG)
    return Result.success()
  }

  /**
   * Builds the full notification summary including types of messages as well as who it is from, see [buildAuthorSummary]
   */
  @VisibleForTesting
  internal fun buildSummary(
    context: Context,
    messages: Int = 0,
    calls: Int = 0,
    mentions: Int = 0,
    replies: Int = 0,
    messageAndCallAuthors: List<String> = emptyList(),
    mentionAuthors: List<String> = emptyList(),
    replyAuthors: List<String> = emptyList(),
    hideAuthors: Boolean = false
  ): String {
    val showUnread = messages > 0
    val showCalls = calls > 0
    val showAuthors = (showUnread || showCalls) && !hideAuthors
    val showMentions = mentions > 0 && !hideAuthors
    val showReplies = replies > 0 && !hideAuthors

    val messagesString = if (showUnread) context.resources.getQuantityString(R.plurals.UnreadReminderJob__messages, messages, messages) else ""
    val callsString = if (showCalls) context.resources.getQuantityString(R.plurals.UnreadReminderJob__calls, calls, calls) else ""
    val authorsString = if (showAuthors) buildAuthorSummary(context, ReminderType.MESSAGES, messages + calls, messageAndCallAuthors) else ""
    val mentionsString = if (showMentions) buildAuthorSummary(context, ReminderType.MENTIONS, mentions, mentionAuthors) else ""
    val repliesString = if (showReplies) buildAuthorSummary(context, ReminderType.REPLIES, replies, replyAuthors) else ""

    return if (showUnread && showCalls && showMentions && showReplies) {
      context.getString(R.string.UnreadReminderJob__calls_and_unread_full_summary, callsString, messagesString, mentionsString, repliesString)
    } else if (showUnread && showCalls && showMentions) {
      context.getString(R.string.UnreadReminderJob__calls_and_unread_summary, callsString, messagesString, mentionsString)
    } else if (showUnread && showCalls && showReplies) {
      context.getString(R.string.UnreadReminderJob__calls_and_unread_summary, callsString, messagesString, repliesString)
    } else if (showUnread && showCalls && hideAuthors) {
      context.getString(R.string.UnreadReminderJob__calls_and_unread, callsString, messagesString)
    } else if (showUnread && showCalls) {
      context.getString(R.string.UnreadReminderJob__calls_and_unread_author, callsString, messagesString, authorsString)
    } else if (showUnread && showMentions && showReplies) {
      context.getString(R.string.UnreadReminderJob__unread_both_summary, messagesString, mentionsString, repliesString)
    } else if (showUnread && showMentions) {
      context.getString(R.string.UnreadReminderJob__unread_one_summary, messagesString, mentionsString)
    } else if (showUnread && showReplies) {
      context.getString(R.string.UnreadReminderJob__unread_one_summary, messagesString, repliesString)
    } else if (hideAuthors && showCalls) {
      context.getString(R.string.UnreadReminderJob__calls_or_unread, callsString)
    } else if (hideAuthors && showUnread) {
      context.getString(R.string.UnreadReminderJob__calls_or_unread, messagesString)
    } else if (showCalls) {
      context.getString(R.string.UnreadReminderJob__calls_or_unread_author, callsString, authorsString)
    } else if (showUnread) {
      context.getString(R.string.UnreadReminderJob__calls_or_unread_author, messagesString, authorsString)
    } else {
      ""
    }
  }

  /**
   * Formats the authors and number of messages based off of reminder type and number of authors
   * eg '5 mentions from Alice and others' or 'a reply from Bob'
   */
  @VisibleForTesting
  internal fun buildAuthorSummary(context: Context, reminderType: ReminderType, count: Int, authors: List<String>): String {
    val (oneRes, twoRes, manyRes) = when (reminderType) {
      ReminderType.MESSAGES,
      ReminderType.CALLS -> Triple(R.string.UnreadReminderJob__authors_one, R.string.UnreadReminderJob__authors_two, R.string.UnreadReminderJob__authors_many)
      ReminderType.MENTIONS -> Triple(R.string.UnreadReminderJob__mentions_one, R.string.UnreadReminderJob__mentions_two, R.string.UnreadReminderJob__mentions_many)
      ReminderType.REPLIES -> Triple(R.string.UnreadReminderJob__replies_one, R.string.UnreadReminderJob__replies_two, R.string.UnreadReminderJob__replies_many)
    }
    val includesCount = reminderType == ReminderType.MENTIONS || reminderType == ReminderType.REPLIES

    return when (authors.size) {
      1 -> context.getString(oneRes, authors[0])
      2 -> if (includesCount) {
        context.getString(twoRes, count, authors[0], authors[1])
      } else {
        context.getString(twoRes, authors[0], authors[1])
      }
      else -> if (includesCount) {
        context.getString(manyRes, count, authors[0])
      } else {
        context.getString(manyRes, authors[0])
      }
    }
  }

  private fun getUnreadForReminder(threadIds: List<Long>, reminderType: ReminderType): Pair<Int, List<RecipientId>> {
    return if (threadIds.isEmpty()) {
      0 to emptyList()
    } else if (reminderType == ReminderType.CALLS) {
      SignalDatabase.calls.getUnreadCallsForReminderNotification(threadIds)
    } else {
      SignalDatabase.messages.getUnreadContentForReminderNotification(threadIds, reminderType)
    }
  }

  override fun onFailure() {
    Log.w(TAG, "Failed to create unread reminder notification")
  }

  class Factory : Job.Factory<UnreadReminderJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): UnreadReminderJob {
      return UnreadReminderJob(parameters)
    }
  }
}
