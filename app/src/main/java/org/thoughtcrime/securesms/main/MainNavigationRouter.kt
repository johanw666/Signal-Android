/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.main

/**
 * Handles navigation for sub-screens within the chats detail pane.
 */
interface MainNavigationChatDetailRouter {
  fun exitDetailLocation()
  fun goToChatDetail(location: MainDetailRoute.Chats)
}

/**
 * Handles navigation for sub-screens within the calls detail pane.
 */
interface MainNavigationCallDetailRouter {
  fun exitDetailLocation()
  fun goToCallDetail(location: MainDetailRoute.Calls)
}

/**
 * Handles navigation to all [MainListRoute]s and [MainDetailRoute]s, including the top-level roots.
 */
interface MainNavigationRouter : MainNavigationChatDetailRouter, MainNavigationCallDetailRouter {
  fun goTo(location: MainListRoute)
  fun goTo(location: MainDetailRoute)

  override fun goToChatDetail(location: MainDetailRoute.Chats) = goTo(location)
  override fun goToCallDetail(location: MainDetailRoute.Calls) = goTo(location)
}
