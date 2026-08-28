/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.contactshare.screens.details

import android.content.Context
import kotlinx.coroutines.withContext
import org.signal.core.util.concurrent.SignalDispatchers
import org.signal.core.util.nullIfBlank
import org.thoughtcrime.securesms.contactshare.ADDRESS_PREFIX
import org.thoughtcrime.securesms.contactshare.Contact
import org.thoughtcrime.securesms.contactshare.ContactUtil
import org.thoughtcrime.securesms.contactshare.EMAIL_PREFIX
import org.thoughtcrime.securesms.contactshare.PHONE_PREFIX
import org.thoughtcrime.securesms.contactshare.displayLines
import org.thoughtcrime.securesms.contactshare.labelText
import org.thoughtcrime.securesms.contactshare.resolveSignalRecipient
import org.thoughtcrime.securesms.dependencies.AppDependencies
import java.util.Locale

/** Maps a received card into details screen state. */
class SharedContactDetailsRepository(
  private val context: Context = AppDependencies.application,
  private val locale: Locale = Locale.getDefault()
) {

  suspend fun loadState(contact: Contact): SharedContactDetailsState = withContext(SignalDispatchers.IO) {
    toState(contact)
  }

  private fun toState(contact: Contact): SharedContactDetailsState {
    val signalRecipient = contact.resolveSignalRecipient()
    val isOnSignal = signalRecipient != null
    val displayName = ContactUtil.getDisplayName(contact)

    return SharedContactDetailsState(
      displayName = displayName,
      organization = contact.organization.nullIfBlank()?.takeIf { it != displayName },
      photoUri = contact.avatar?.attachment?.uri?.toString(),
      hasPersonalName = !contact.name.isEmpty || contact.organization.isNullOrBlank(),
      signalRecipientId = signalRecipient,
      actions = SharedContactDetailsViewModel.contactActionsFor(
        isOnSignal = isOnSignal,
        hasInviteTarget = contact.phoneNumbers.isNotEmpty() || contact.emails.isNotEmpty(),
        hasAnythingToSave = displayName.isNotBlank() ||
          contact.phoneNumbers.isNotEmpty() ||
          contact.emails.isNotEmpty() ||
          contact.postalAddresses.isNotEmpty()
      ),
      details = contact.buildDetailRows()
    )
  }

  private fun Contact.buildDetailRows(): List<SharedContactDetailsState.DetailRow> {
    val rows = mutableListOf<SharedContactDetailsState.DetailRow>()

    this.phoneNumbers.forEachIndexed { index, phone ->
      rows += SharedContactDetailsState.DetailRow(
        id = "$PHONE_PREFIX:$index",
        lines = listOf(ContactUtil.getPrettyPhoneNumber(phone, locale)),
        label = phone.labelText(context),
        kind = SharedContactDetailsState.DetailKind.PHONE
      )
    }

    this.emails.forEachIndexed { index, email ->
      rows += SharedContactDetailsState.DetailRow(
        id = "$EMAIL_PREFIX:$index",
        lines = listOf(email.email),
        label = email.labelText(context),
        kind = SharedContactDetailsState.DetailKind.EMAIL
      )
    }

    this.postalAddresses.forEachIndexed { index, address ->
      rows += SharedContactDetailsState.DetailRow(
        id = "$ADDRESS_PREFIX:$index",
        lines = address.displayLines(),
        label = address.labelText(context),
        kind = SharedContactDetailsState.DetailKind.ADDRESS
      )
    }

    return rows
  }
}
