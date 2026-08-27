/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.addusername

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.signal.core.ui.CoreUiDependenciesRule
import org.signal.core.ui.compose.theme.SignalTheme
import org.signal.libsignal.usernames.Username
import org.signal.registration.test.TestTags

/**
 * Tests for [AddUsernameScreen] that validate event emissions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AddUsernameScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @get:Rule
  val coreUiDependenciesRule = CoreUiDependenciesRule(ApplicationProvider.getApplicationContext())

  private val reservedState = AddUsernameState(
    username = "maya",
    discriminator = "45",
    showDiscriminator = true,
    reservation = Username("maya.45")
  )

  @Test
  fun `the discriminator field is hidden until the service assigns one`() {
    composeTestRule.setContent {
      SignalTheme {
        AddUsernameScreen(state = AddUsernameState(), onEvent = {})
      }
    }

    composeTestRule.onNodeWithTag(TestTags.ADD_USERNAME_DISCRIMINATOR_FIELD).assertDoesNotExist()
  }

  @Test
  fun `the discriminator field is shown once one has been assigned`() {
    composeTestRule.setContent {
      SignalTheme {
        AddUsernameScreen(state = reservedState, onEvent = {})
      }
    }

    composeTestRule.onNodeWithTag(TestTags.ADD_USERNAME_DISCRIMINATOR_FIELD).assertIsDisplayed()
  }

  @Test
  fun `when typing in the discriminator field, DiscriminatorChanged is emitted`() {
    var emittedEvent: AddUsernameScreenEvents? = null

    composeTestRule.setContent {
      SignalTheme {
        AddUsernameScreen(state = reservedState, onEvent = { emittedEvent = it })
      }
    }

    composeTestRule.onNodeWithTag(TestTags.ADD_USERNAME_DISCRIMINATOR_FIELD).performTextReplacement("77")

    assertThat(emittedEvent).isNotNull()
    assertThat(emittedEvent).isEqualTo(AddUsernameScreenEvents.DiscriminatorChanged("77"))
  }

  @Test
  fun `when typing in the username field, UsernameChanged is emitted`() {
    var emittedEvent: AddUsernameScreenEvents? = null

    composeTestRule.setContent {
      SignalTheme {
        AddUsernameScreen(state = AddUsernameState(), onEvent = { emittedEvent = it })
      }
    }

    composeTestRule.onNodeWithTag(TestTags.ADD_USERNAME_FIELD).performTextInput("maya")

    assertThat(emittedEvent).isEqualTo(AddUsernameScreenEvents.UsernameChanged("maya"))
  }

  @Test
  fun `skip emits SkipClicked`() {
    var emittedEvent: AddUsernameScreenEvents? = null

    composeTestRule.setContent {
      SignalTheme {
        AddUsernameScreen(state = AddUsernameState(), onEvent = { emittedEvent = it })
      }
    }

    composeTestRule.onNodeWithTag(TestTags.ADD_USERNAME_SKIP_BUTTON).performClick()

    assertThat(emittedEvent).isEqualTo(AddUsernameScreenEvents.SkipClicked)
  }
}
