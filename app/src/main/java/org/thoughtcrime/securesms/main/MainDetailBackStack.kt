/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.main

import org.signal.core.ui.compose.split.ListDetailBackStack
import org.signal.core.ui.compose.split.detailLocation
import org.signal.core.ui.compose.split.listLocation
import org.thoughtcrime.securesms.calls.log.CallLogRow
import org.thoughtcrime.securesms.recipients.RecipientId

/**
 * The list currently being displayed.
 */
val ListDetailBackStack.listLocation: MainListRoute
  get() = listLocation<MainListRoute>()

/**
 * The detail content displayed above the current list, or null when the list is showing on its own.
 */
val ListDetailBackStack.detailLocation: MainDetailRoute?
  get() = detailLocation<MainDetailRoute>()

/**
 * The recipient whose content is displayed by the topmost entry that has one.
 */
val ListDetailBackStack.activeRecipientId: RecipientId?
  get() = asReversed().firstNotNullOfOrNull {
    when (it) {
      is MainDetailRoute.Conversation -> it.conversationArgs.recipientId
      is MainDetailRoute.Chats -> it.controllerKey
      else -> null
    }
  }

/**
 * The call whose content is displayed by the topmost entry that has one.
 */
val ListDetailBackStack.activeCallId: CallLogRow.Id?
  get() = asReversed().firstNotNullOfOrNull {
    when (it) {
      is MainDetailRoute.Calls -> it.controllerKey
      is MainDetailRoute.CallLinkDetails -> it.controllerKey
      else -> null
    }
  }
