/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.contactshare

import android.content.Context
import org.thoughtcrime.securesms.R
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.util.SignalE164Util

/** Detail ids are positional, so a selection can be resolved back against the contact it came from. */
internal const val PHONE_PREFIX = "phone"
internal const val EMAIL_PREFIX = "email"
internal const val ADDRESS_PREFIX = "address"

/** Not positional, since a contact carries at most one company. */
internal const val ORGANIZATION_ID = "organization"

internal const val PHOTO_ID_ADDRESS_BOOK = "address-book"
internal const val PHOTO_ID_SIGNAL_PROFILE = "signal-profile"

/** A lookup rather than an insert, so browsing contacts does not create recipient rows. */
internal fun Contact.resolveSignalRecipient(): RecipientId? {
  return this.phoneNumbers
    .asSequence()
    .mapNotNull { phone -> SignalE164Util.formatAsE164(phone.number) }
    .mapNotNull { e164 -> SignalDatabase.recipients.getByE164(e164).orElse(null) }
    .firstOrNull { Recipient.resolved(it).isRegistered }
}

internal fun Contact.Phone.labelText(context: Context): String {
  return when (this.type) {
    Contact.Phone.Type.HOME -> context.getString(R.string.ContactShareEditActivity_type_home)
    Contact.Phone.Type.MOBILE -> context.getString(R.string.ContactShareEditActivity_type_mobile)
    Contact.Phone.Type.WORK -> context.getString(R.string.ContactShareEditActivity_type_work)
    Contact.Phone.Type.CUSTOM -> this.label.orEmpty()
    else -> ""
  }
}

internal fun Contact.Email.labelText(context: Context): String {
  return when (this.type) {
    Contact.Email.Type.HOME -> context.getString(R.string.ContactShareEditActivity_type_home)
    Contact.Email.Type.MOBILE -> context.getString(R.string.ContactShareEditActivity_type_mobile)
    Contact.Email.Type.WORK -> context.getString(R.string.ContactShareEditActivity_type_work)
    Contact.Email.Type.CUSTOM -> this.label.orEmpty()
    else -> ""
  }
}

internal fun Contact.PostalAddress.labelText(context: Context): String {
  return when (this.type) {
    Contact.PostalAddress.Type.HOME -> context.getString(R.string.ContactShareEditActivity_type_home)
    Contact.PostalAddress.Type.WORK -> context.getString(R.string.ContactShareEditActivity_type_work)
    Contact.PostalAddress.Type.CUSTOM -> this.label ?: context.getString(R.string.ContactShareEditActivity_type_missing)
    else -> context.getString(R.string.ContactShareEditActivity_type_missing)
  }
}

internal fun Contact.PostalAddress.displayLines(): List<String> = this.toString().lines().filter { it.isNotBlank() }
