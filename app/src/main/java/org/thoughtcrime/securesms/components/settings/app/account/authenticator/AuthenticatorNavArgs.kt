/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.components.settings.app.account.authenticator

import android.os.Bundle
import org.signal.appsettings.authenticatorcodeentry.AuthenticatorCodeEntryState.Purpose
import org.signal.core.util.logging.Log

/**
 * The nav arguments the authenticator app screens pass between each other, and the parsing that turns them back into
 * something typed. Keep the values in sync with the argument defaults declared for these destinations in
 * app_settings_with_change_number.xml.
 */
object AuthenticatorNavArgs {

  private val TAG = Log.tag(AuthenticatorNavArgs::class)

  /** Which of [PURPOSE_ADD]/[PURPOSE_REMOVE] a code is being collected for. */
  const val ARG_PURPOSE = "purpose"
  const val PURPOSE_ADD = "ADD"
  const val PURPOSE_REMOVE = "REMOVE"

  /** The app being removed or renamed, or [NO_APP_ID] when the screen is acting on a newly paired app. */
  const val ARG_APP_ID = "app_id"
  const val NO_APP_ID = -1L

  /** The app id in [arguments], or null when the screen is acting on a newly paired app. */
  fun appId(arguments: Bundle?): Long? = arguments?.getLong(ARG_APP_ID, NO_APP_ID)?.takeIf { it != NO_APP_ID }

  /** The purpose in [arguments], falling back to [Purpose.Add] rather than removing an app we can't identify. */
  fun purpose(arguments: Bundle?): Purpose {
    if (arguments?.getString(ARG_PURPOSE) != PURPOSE_REMOVE) {
      return Purpose.Add
    }

    val appId = appId(arguments)
    if (appId == null) {
      Log.w(TAG, "Asked to remove an authenticator app without an id. Collecting a code to add one instead.")
      return Purpose.Add
    }

    return Purpose.Remove(appId)
  }
}
