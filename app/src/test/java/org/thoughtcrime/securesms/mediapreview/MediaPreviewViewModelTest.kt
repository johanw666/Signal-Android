/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.mediapreview

import android.app.Application
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.signal.core.models.database.AttachmentId
import org.thoughtcrime.securesms.database.FakeMessageRecords
import org.thoughtcrime.securesms.database.MediaTable

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class)
class MediaPreviewViewModelTest {

  @Test
  fun `resolvePosition uses the query position on a first load, when nothing is on screen yet`() {
    val position = MediaPreviewViewModel.resolvePosition(
      oldState = MediaPreviewState(),
      records = recordsFor(1, 2, 3),
      queryPosition = 1
    )

    assertThat(position).isEqualTo(1)
  }

  @Test
  fun `resolvePosition follows the visible attachment when newly sent media shifts the window`() {
    val position = MediaPreviewViewModel.resolvePosition(
      oldState = stateFor(visiblePosition = 1, attachmentIds = longArrayOf(1, 2, 3)),
      records = recordsFor(4, 1, 2, 3),
      queryPosition = 1
    )

    assertThat(position).isEqualTo(2)
  }

  @Test
  fun `resolvePosition ignores a query position anchored to a page the user has already swiped past`() {
    val position = MediaPreviewViewModel.resolvePosition(
      oldState = stateFor(visiblePosition = 2, attachmentIds = longArrayOf(1, 2, 3)),
      records = recordsFor(1, 2, 3),
      queryPosition = 1
    )

    assertThat(position).isEqualTo(2)
  }

  @Test
  fun `resolvePosition falls back to the query position when the visible attachment left the window`() {
    val position = MediaPreviewViewModel.resolvePosition(
      oldState = stateFor(visiblePosition = 1, attachmentIds = longArrayOf(1, 2, 3)),
      records = recordsFor(1, 3),
      queryPosition = 0
    )

    assertThat(position).isEqualTo(0)
  }

  @Test
  fun `resolvePosition falls back to the query position when the visible position is out of bounds`() {
    val position = MediaPreviewViewModel.resolvePosition(
      oldState = stateFor(visiblePosition = 5, attachmentIds = longArrayOf(1, 2)),
      records = recordsFor(1, 2),
      queryPosition = 1
    )

    assertThat(position).isEqualTo(1)
  }

  @Test
  fun `resolvePosition does not match a record without an attachment to a visible record without one`() {
    val position = MediaPreviewViewModel.resolvePosition(
      oldState = MediaPreviewState(mediaRecords = listOf(recordFor(null)), position = 0),
      records = listOf(recordFor(null), recordFor(2)),
      queryPosition = 1
    )

    assertThat(position).isEqualTo(1)
  }

  private fun stateFor(visiblePosition: Int, attachmentIds: LongArray): MediaPreviewState {
    return MediaPreviewState(mediaRecords = recordsFor(*attachmentIds), position = visiblePosition)
  }

  private fun recordsFor(vararg attachmentIds: Long): List<MediaTable.MediaRecord> {
    return attachmentIds.map { recordFor(it) }
  }

  private fun recordFor(attachmentId: Long?): MediaTable.MediaRecord {
    val record: MediaTable.MediaRecord = mockk()
    every { record.attachment } returns attachmentId?.let { FakeMessageRecords.buildDatabaseAttachment(attachmentId = AttachmentId(it)) }
    return record
  }
}
