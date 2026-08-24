
package org.thoughtcrime.securesms.database

import android.app.Application
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.every
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thoughtcrime.securesms.components.settings.app.notifications.ReminderType
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.testutil.RecipientTestRule

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class ThreadTableTest_mutedThreadIds {

  @get:Rule
  val recipients = RecipientTestRule()

  private lateinit var contactId: RecipientId
  private var threadId: Long = 0

  @Before
  fun setUp() {
    contactId = recipients.createRecipient("Alice Android")
    threadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(contactId))
    SignalDatabase.recipients.setMuted(contactId, Long.MAX_VALUE)
  }

  private fun globalDefaults(unreadReminder: Boolean = true, calls: Boolean = false, mentions: Boolean = true, replies: Boolean = true) {
    every { recipients.signalStore.settings.unreadReminderEnabled } returns unreadReminder
    every { recipients.signalStore.settings.allowCallsWhileMuted } returns calls
    every { recipients.signalStore.settings.allowMentionsWhileMuted } returns mentions
    every { recipients.signalStore.settings.allowRepliesWhileMuted } returns replies
  }

  private fun isMutedFor(reminderType: ReminderType): Boolean {
    return threadId in SignalDatabase.threads.getMutedThreadIds(reminderType)
  }

  @Test
  fun `allow-by-default plus system-default recipient setting includes the thread`() {
    globalDefaults(unreadReminder = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.SYSTEM_DEFAULT)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isTrue()
  }

  @Test
  fun `allow-by-default plus an explicit opt-out excludes the thread`() {
    globalDefaults(unreadReminder = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.DO_NOT_NOTIFY)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isFalse()
  }

  @Test
  fun `allow-by-default plus an explicit always-allow includes the thread`() {
    globalDefaults(unreadReminder = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isTrue()
  }

  @Test
  fun `always-allow-required plus system-default recipient setting excludes the thread`() {
    globalDefaults(unreadReminder = false)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.SYSTEM_DEFAULT)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isFalse()
  }

  @Test
  fun `always-allow-required plus an explicit opt-out excludes the thread`() {
    globalDefaults(unreadReminder = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.DO_NOT_NOTIFY)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isFalse()
  }

  @Test
  fun `always-allow-required plus an explicit always-allow includes the thread`() {
    globalDefaults(unreadReminder = false)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isTrue()
  }

  @Test
  fun `an unmuted thread is excluded even when always-allowed`() {
    globalDefaults(unreadReminder = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    SignalDatabase.recipients.setMuted(contactId, 0L)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isFalse()
  }

  @Test
  fun `an archived thread is excluded even when muted and always-allowed`() {
    globalDefaults(unreadReminder = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    SignalDatabase.threads.setArchived(setOf(threadId), true)
    assertThat(isMutedFor(ReminderType.MESSAGES)).isFalse()
  }

  @Test
  fun `calls, allow-by-default false, require an explicit always-allow for calls specifically`() {
    globalDefaults(unreadReminder = true, calls = false)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)

    SignalDatabase.recipients.setCallNotificationSetting(contactId, RecipientTable.NotificationSetting.SYSTEM_DEFAULT)
    assertThat(isMutedFor(ReminderType.CALLS)).isFalse()

    SignalDatabase.recipients.setCallNotificationSetting(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    assertThat(isMutedFor(ReminderType.CALLS)).isTrue()
  }

  @Test
  fun `calls, allow-by-default true, an explicit opt-out for calls specifically still excludes it`() {
    globalDefaults(unreadReminder = true, calls = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)

    SignalDatabase.recipients.setCallNotificationSetting(contactId, RecipientTable.NotificationSetting.DO_NOT_NOTIFY)
    assertThat(isMutedFor(ReminderType.CALLS)).isFalse()

    SignalDatabase.recipients.setCallNotificationSetting(contactId, RecipientTable.NotificationSetting.SYSTEM_DEFAULT)
    assertThat(isMutedFor(ReminderType.CALLS)).isTrue()
  }

  @Test
  fun `mentions are excluded for a 1-to-1 thread no matter the notification settings`() {
    globalDefaults(unreadReminder = true, mentions = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    SignalDatabase.recipients.setMentionSetting(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    assertThat(isMutedFor(ReminderType.MENTIONS)).isFalse()
  }

  @Test
  fun `mentions in a muted GV2 group thread follow the same allow-by-default vs always-allow rule`() {
    val group = recipients.createGroup()
    val groupThreadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(group.recipientId))
    SignalDatabase.recipients.setMuted(group.recipientId, Long.MAX_VALUE)
    SignalDatabase.recipients.setUnreadReminder(group.recipientId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    globalDefaults(unreadReminder = true, mentions = false)

    SignalDatabase.recipients.setMentionSetting(group.recipientId, RecipientTable.NotificationSetting.SYSTEM_DEFAULT)
    assertThat(groupThreadId in SignalDatabase.threads.getMutedThreadIds(ReminderType.MENTIONS)).isFalse()

    SignalDatabase.recipients.setMentionSetting(group.recipientId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    assertThat(groupThreadId in SignalDatabase.threads.getMutedThreadIds(ReminderType.MENTIONS)).isTrue()
  }

  @Test
  fun `replies are excluded for a 1-to-1 thread no matter the notification settings`() {
    globalDefaults(unreadReminder = true, replies = true)
    SignalDatabase.recipients.setUnreadReminder(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    SignalDatabase.recipients.setReplyNotificationSetting(contactId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    assertThat(isMutedFor(ReminderType.REPLIES)).isFalse()
  }

  @Test
  fun `replies in a muted GV2 group thread follow the same allow-by-default vs always-allow rule`() {
    val group = recipients.createGroup()
    val groupThreadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(group.recipientId))
    SignalDatabase.recipients.setMuted(group.recipientId, Long.MAX_VALUE)
    SignalDatabase.recipients.setUnreadReminder(group.recipientId, RecipientTable.NotificationSetting.ALWAYS_NOTIFY)
    globalDefaults(unreadReminder = true, replies = true)

    SignalDatabase.recipients.setReplyNotificationSetting(group.recipientId, RecipientTable.NotificationSetting.DO_NOT_NOTIFY)
    assertThat(groupThreadId in SignalDatabase.threads.getMutedThreadIds(ReminderType.REPLIES)).isFalse()

    SignalDatabase.recipients.setReplyNotificationSetting(group.recipientId, RecipientTable.NotificationSetting.SYSTEM_DEFAULT)
    assertThat(groupThreadId in SignalDatabase.threads.getMutedThreadIds(ReminderType.REPLIES)).isTrue()
  }
}
