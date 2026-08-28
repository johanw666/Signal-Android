/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.contactshare

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.IntentCompat
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.PassphraseRequiredActivity
import org.thoughtcrime.securesms.recipients.RecipientId

/**
 * Hosts [ContactShareEditFragment].
 */
class ContactShareEditActivityV2 : PassphraseRequiredActivity() {

  companion object {
    private val TAG = Log.tag(ContactShareEditActivityV2::class)

    const val KEY_CONTACTS = "contacts"
    private const val KEY_CONTACT_URIS = "contact_uris"
    private const val KEY_RECIPIENT_ID = "recipient_id"

    /** @param recipientId the conversation being sent to, not the contact being shared. */
    @JvmStatic
    fun getIntent(context: Context, contactUris: List<Uri>, recipientId: RecipientId): Intent {
      return Intent(context, ContactShareEditActivityV2::class.java).apply {
        putParcelableArrayListExtra(KEY_CONTACT_URIS, ArrayList(contactUris))
        putExtra(KEY_RECIPIENT_ID, recipientId)
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    super.onCreate(savedInstanceState, ready)

    val uris: List<Uri> = IntentCompat.getParcelableArrayListExtra(intent, KEY_CONTACT_URIS, Uri::class.java) ?: emptyList()
    val recipientId = IntentCompat.getParcelableExtra(intent, KEY_RECIPIENT_ID, RecipientId::class.java)

    if (uris.isEmpty() || recipientId == null) {
      Log.w(TAG, "No contact uris supplied.")
      finish()
      return
    }

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
        .replace(android.R.id.content, ContactShareEditFragment.create(uris, recipientId))
        .commit()
    }
  }
}
