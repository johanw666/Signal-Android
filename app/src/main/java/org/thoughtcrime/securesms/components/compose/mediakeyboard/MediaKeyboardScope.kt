/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.compose.mediakeyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Declares which keyboards a [MediaKeyboardScaffold] offers. */
interface MediaKeyboardScope {
  /**
   * Offers the keyboard identified by [key].
   *
   * @param key Identifies the keyboard.
   * @param enabled False to keep the key known but refuse requests for it.
   * @param containerColor Fills the sheet, navigation bar included, so it should match whatever
   *   [content] paints its edges with. Unspecified falls back to the scaffold's surface.
   * @param content The keyboard itself.
   */
  fun keyboard(
    key: MediaKeyboardKey,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
  )
}

internal class MediaKeyboardRegistry : MediaKeyboardScope {
  private val entries = LinkedHashMap<MediaKeyboardKey, Entry>()

  override fun keyboard(
    key: MediaKeyboardKey,
    enabled: Boolean,
    containerColor: Color,
    content: @Composable () -> Unit
  ) {
    entries[key] = Entry(enabled, containerColor, content)
  }

  fun isEnabled(key: MediaKeyboardKey?): Boolean = key != null && entries[key]?.enabled == true

  fun contentFor(key: MediaKeyboardKey?): (@Composable () -> Unit)? {
    return key?.let { entries[it] }?.takeIf { it.enabled }?.content
  }

  fun containerColorFor(key: MediaKeyboardKey?): Color {
    return key?.let { entries[it] }?.takeIf { it.enabled }?.containerColor ?: Color.Unspecified
  }

  private class Entry(val enabled: Boolean, val containerColor: Color, val content: @Composable () -> Unit)
}
