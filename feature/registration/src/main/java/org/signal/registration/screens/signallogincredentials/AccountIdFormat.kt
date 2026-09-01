/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogincredentials

import org.signal.core.models.ServiceId.ACI

/**
 * The 8-4-4-4-12 layout an account ID shares with the [ACI] it is written from.
 * Exists to support formatting ACI's as-you-type.
 */
internal object AccountIdFormat {

  /** Offsets in a raw account ID that a dash is inserted in front of. */
  private val DASH_OFFSETS = intArrayOf(8, 12, 16, 20)

  /** Rewrites a raw account ID with the dashes a UUID is normally written with. */
  fun dashed(accountId: String): String {
    return buildString {
      for ((index, character) in accountId.withIndex()) {
        if (index in DASH_OFFSETS) {
          append('-')
        }
        append(character)
      }
    }
  }

  /**
   * How many dashes [dashed] inserts before [offset] in an ID of [length] characters. A dash is only present if the ID
   * is long enough to have a character after it, so [length] decides which offsets actually contribute.
   */
  fun dashesBeforeRawOffset(offset: Int, length: Int): Int {
    return DASH_OFFSETS.count { it <= offset && it < length }
  }

  /** How many dashes precede [offset] in the output of [dashed] for an ID of [length] characters. */
  fun dashesBeforeDashedOffset(offset: Int, length: Int): Int {
    return DASH_OFFSETS.withIndex().count { (index, dashOffset) -> dashOffset < length && dashOffset + index < offset }
  }

  /** Parses a complete raw account ID as the [ACI] it stands for. Null if it isn't a valid ACI. */
  fun toAciOrNull(accountId: String): ACI? {
    if (accountId.length != SignalLoginCredentialEntryState.ACCOUNT_ID_LENGTH) {
      return null
    }

    return ACI.parseOrNull(dashed(accountId))
  }
}
