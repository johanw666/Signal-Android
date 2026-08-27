package org.thoughtcrime.securesms.database

import android.app.Application
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.testutil.RecipientTestRule

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class CallTableTest_unreadForReminder {

  @get:Rule
  val recipients = RecipientTestRule()

  private val calls: CallTable
    get() = SignalDatabase.calls

  private var nextCallId = 1L

  @Test
  fun `counts unread missed calls from a single caller`() {
    val caller = recipients.createRecipient("Alice Android")
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(caller))

    insertMissedCall(caller, time = 1000)
    insertMissedCall(caller, time = 1001)

    val (count, authors) = calls.getUnreadCallsForReminderNotification(threadId, 1001)

    assertThat(count).isEqualTo(2)
    assertThat(authors).isEqualTo(listOf(caller))
  }

  @Test
  fun `a missed-notification-profile call also counts as missed`() {
    val caller = recipients.createRecipient("Bob Caller")
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(caller))

    insertCall(caller, time = 1000, event = CallTable.Event.MISSED_NOTIFICATION_PROFILE)

    val (count, authors) = calls.getUnreadCallsForReminderNotification(threadId, 1001)

    assertThat(count).isEqualTo(1)
    assertThat(authors).isEqualTo(listOf(caller))
  }

  @Test
  fun `non-missed call events are excluded`() {
    val caller = recipients.createRecipient("Carol Answered")
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(caller))

    insertCall(caller, time = 1000, event = CallTable.Event.ACCEPTED)
    insertMissedCall(caller, time = 1001)

    val (count, authors) = calls.getUnreadCallsForReminderNotification(threadId, 1002)

    assertThat(count).isEqualTo(1)
    assertThat(authors).isEqualTo(listOf(caller))
  }

  @Test
  fun `already-read missed calls are excluded`() {
    val caller = recipients.createRecipient("Dave Rung")
    val threadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(caller))

    insertMissedCall(caller, time = 1000)
    calls.markAllCallEventsRead(timestamp = 1000)
    insertMissedCall(caller, time = 1001)

    val (count, authors) = calls.getUnreadCallsForReminderNotification(threadId, 1002)

    assertThat(count).isEqualTo(1)
    assertThat(authors).isEqualTo(listOf(caller))
  }

  @Test
  fun `only the requested threads are counted`() {
    val included = recipients.createRecipient("Erin Included")
    val excluded = recipients.createRecipient("Frank Excluded")
    val includedThreadId = SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(included))
    SignalDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(excluded))

    insertMissedCall(included, time = 1000)
    insertMissedCall(excluded, time = 1000)

    val (count, authors) = calls.getUnreadCallsForReminderNotification(includedThreadId, 1001)

    assertThat(count).isEqualTo(1)
    assertThat(authors).isEqualTo(listOf(included))
  }

  private fun insertMissedCall(caller: RecipientId, time: Long): Long {
    return insertCall(caller, time, CallTable.Event.MISSED)
  }

  private fun insertCall(caller: RecipientId, time: Long, event: CallTable.Event): Long {
    val callId = nextCallId++
    calls.insertOneToOneCall(
      callId = callId,
      timestamp = time,
      peer = caller,
      type = CallTable.Type.AUDIO_CALL,
      direction = CallTable.Direction.INCOMING,
      event = event
    )
    return callId
  }
}
