/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.compose.mediakeyboard

/**
 * What a [MediaKeyboardScaffold] reports back. Requests flow the other way, through
 * [MediaKeyboardController].
 */
sealed interface MediaKeyboardEvents {
  /** @param key The keyboard now showing. */
  data class KeyboardShown(val key: MediaKeyboardKey) : MediaKeyboardEvents

  /** None of ours is showing any more. */
  data object KeyboardHidden : MediaKeyboardEvents

  /** One of ours was dismissed by a back gesture rather than by a request. */
  data object DismissedByBack : MediaKeyboardEvents

  /** @param visible Whether the system keyboard is up, on the target state rather than the animated one. */
  data class SystemKeyboardVisibilityChanged(val visible: Boolean) : MediaKeyboardEvents

  /** The system keyboard finished animating, in either direction. */
  data object SystemKeyboardAnimationEnded : MediaKeyboardEvents

  /**
   * A trustworthy system keyboard height was observed. The scaffold does not persist it.
   *
   * @param heightPx The measured height.
   */
  data class SystemKeyboardHeightMeasured(val heightPx: Int) : MediaKeyboardEvents
}
