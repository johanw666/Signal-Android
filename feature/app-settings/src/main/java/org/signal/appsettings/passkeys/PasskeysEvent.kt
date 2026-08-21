/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

/**
 * Reminder that these events are logged, so don't include anything sensitive in the toString.
 */
sealed interface PasskeysEvent {

  /** The user tapped the navigation (back) icon. */
  data object NavigateBackClicked : PasskeysEvent

  /** The user tapped the button that starts creating a passkey. */
  data object SetUpPasskeyClicked : PasskeysEvent

  /** The user tapped the learn more link. */
  data object LearnMoreClicked : PasskeysEvent

  /** The user tapped the rename option in a passkey's overflow menu. */
  data class RenamePasskeyClicked(val passkeyId: Long) : PasskeysEvent

  /** The user tapped the remove option in a passkey's overflow menu. */
  data class RemovePasskeyClicked(val passkeyId: Long) : PasskeysEvent
}
