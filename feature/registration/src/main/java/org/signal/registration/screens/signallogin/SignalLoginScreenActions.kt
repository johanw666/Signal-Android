/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

sealed interface SignalLoginScreenActions {
  /** Open the article explaining where to find your account key. */
  data object OpenNeedHelpArticle : SignalLoginScreenActions
}
