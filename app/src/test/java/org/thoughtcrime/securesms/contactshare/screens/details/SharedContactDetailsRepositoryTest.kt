/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.contactshare.screens.details

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.thoughtcrime.securesms.contactshare.Contact
import org.thoughtcrime.securesms.database.SignalDatabase
import org.thoughtcrime.securesms.keyvalue.AccountValues
import org.thoughtcrime.securesms.testutil.MockSignalStoreRule
import java.util.Locale
import java.util.Optional

/**
 * Covers the mapping of a received card into screen state. The recipient lookup is stubbed out, so
 * these are about what the card itself produces.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SharedContactDetailsRepositoryTest {

  @get:Rule
  val signalStore = MockSignalStoreRule(relaxed = setOf(AccountValues::class))

  private val repository = SharedContactDetailsRepository(
    context = ApplicationProvider.getApplicationContext(),
    locale = Locale.US
  )

  @Before
  fun setUp() {
    // A relaxed AccountValues hands back "" rather than null, which the number formatter cannot parse.
    every { signalStore.account.e164 } returns "+15105550000"

    mockkObject(SignalDatabase)
    every { SignalDatabase.recipients } returns mockk {
      every { getByE164(any()) } returns Optional.empty()
    }
  }

  @After
  fun tearDown() {
    unmockkObject(SignalDatabase)
  }

  @Test
  fun `a company alongside a name is offered under the name`() = runTest {
    val state = repository.loadState(contact(name = name("Paige", "Hall"), organization = "Signal Messenger"))

    assertThat(state.displayName).isEqualTo("Paige Hall")
    assertThat(state.organization).isEqualTo("Signal Messenger")
  }

  @Test
  fun `a company that is the only name is not repeated under itself`() = runTest {
    val state = repository.loadState(contact(organization = "Pacific Plumbing"))

    assertThat(state.displayName).isEqualTo("Pacific Plumbing")
    assertThat(state.organization).isNull()
  }

  @Test
  fun `a company only card has no initials worth drawing`() = runTest {
    assertThat(repository.loadState(contact(organization = "Pacific Plumbing")).hasPersonalName).isFalse()
    assertThat(repository.loadState(contact(name = name("Paige", "Hall"))).hasPersonalName).isTrue()
  }

  @Test
  fun `every shared detail becomes a row, in a stable order`() = runTest {
    val state = repository.loadState(
      contact(
        name = name("Paige", "Hall"),
        phones = listOf("+15105550101"),
        emails = listOf("paigehall@example.com"),
        addresses = listOf("123 Beach Drive")
      )
    )

    assertThat(state.details.map { it.id }).containsExactly("phone:0", "email:0", "address:0")
    assertThat(state.details.map { it.kind }).containsExactly(
      SharedContactDetailsState.DetailKind.PHONE,
      SharedContactDetailsState.DetailKind.EMAIL,
      SharedContactDetailsState.DetailKind.ADDRESS
    )
  }

  @Test
  fun `a card with no matched recipient is not on Signal`() = runTest {
    val state = repository.loadState(contact(name = name("Paige", "Hall"), phones = listOf("+15105550101")))

    assertThat(state.isOnSignal).isFalse()
    assertThat(state.showCallButtons).isFalse()
  }

  @Test
  fun `an address only card can still be saved but not invited`() = runTest {
    val state = repository.loadState(contact(organization = "Pacific Plumbing", addresses = listOf("123 Beach Drive")))

    assertThat(state.actions).containsExactly(SharedContactDetailsState.ContactAction.ADD_TO_PHONE_CONTACTS)
  }

  private fun name(given: String? = null, family: String? = null) = Contact.Name(given, family, null, null, null, null)

  private fun contact(
    name: Contact.Name = Contact.Name(null, null, null, null, null, null),
    organization: String? = null,
    phones: List<String> = emptyList(),
    emails: List<String> = emptyList(),
    addresses: List<String> = emptyList()
  ): Contact {
    return Contact(
      name,
      organization,
      phones.map { Contact.Phone(it, Contact.Phone.Type.MOBILE, null) },
      emails.map { Contact.Email(it, Contact.Email.Type.HOME, null) },
      addresses.map { Contact.PostalAddress(Contact.PostalAddress.Type.HOME, null, it, null, null, null, null, null, null) },
      null
    )
  }
}
