/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversation.v2

import org.thoughtcrime.securesms.components.compose.mediakeyboard.MediaKeyboardKey

/** The keyboards the conversation offers in place of the system keyboard. */
object ChatKeyboards {
  val Media = MediaKeyboardKey("conversation.media")
  val Attachment = MediaKeyboardKey("conversation.attachment")
}
