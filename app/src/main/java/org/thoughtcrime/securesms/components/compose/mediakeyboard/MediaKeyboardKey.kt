/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.compose.mediakeyboard

/**
 * Identifies a keyboard a [MediaKeyboardScaffold] can put up. Callers declare their own, so a screen
 * only knows about the keyboards it offers.
 *
 * @param name Unique identifier for the keyboard.
 */
@JvmInline
value class MediaKeyboardKey(val name: String)
