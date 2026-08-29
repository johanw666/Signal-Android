/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.totpentry

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.signal.core.ui.compose.AllDevicePreviews
import org.signal.core.ui.compose.Previews
import org.signal.registration.R
import org.signal.registration.screens.OnePaneRegistrationScaffold
import org.signal.registration.screens.RegistrationScaffold
import org.signal.registration.screens.TwoPaneRegistrationScaffold
import org.signal.registration.screens.attachDebugLogHelper
import org.signal.registration.test.TestTags

/**
 * Two-factor authentication code entry screen. Displays a 6-digit code input in XXX-XXX format for a code from the
 * user's authenticator app.
 */
@Composable
fun TotpEntryScreen(
  state: TotpEntryState,
  onEvent: (TotpEntryScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val focusRequesters = remember { List(TotpEntryState.CODE_LENGTH) { FocusRequester() } }

  LaunchedEffect(state.focusedDigitIndex) {
    focusRequesters[state.focusedDigitIndex].requestFocus()
  }

  Surface(modifier = modifier.testTag(TestTags.TOTP_ENTRY_SCREEN)) {
    when (val layoutParams = RegistrationScaffold.rememberLayoutParams()) {
      is RegistrationScaffold.Params.OnePane -> OnePaneLayout(
        params = layoutParams,
        focusRequesters = focusRequesters,
        state = state,
        onEvent = onEvent
      )

      is RegistrationScaffold.Params.TwoPane -> TwoPaneLayout(
        params = layoutParams,
        focusRequesters = focusRequesters,
        state = state,
        onEvent = onEvent
      )
    }
  }
}

@Composable
private fun OnePaneLayout(
  params: RegistrationScaffold.Params.OnePane,
  focusRequesters: List<FocusRequester>,
  state: TotpEntryState,
  onEvent: (TotpEntryScreenEvents) -> Unit
) {
  val scrollState = rememberScrollState()

  OnePaneRegistrationScaffold(
    modifier = Modifier.fillMaxSize(),
    params = params,
    content = { paddingValues ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(paddingValues)
      ) {
        Illustration()

        Spacer(modifier = Modifier.height(32.dp))

        Description()

        Spacer(modifier = Modifier.height(32.dp))

        CodeField(
          focusRequesters = focusRequesters,
          state = state,
          onEvent = onEvent
        )
      }
    },
    footer = {
      RegistrationScaffold.FooterSurface(
        isElevated = scrollState.canScrollForward
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(params.footerPadding),
          horizontalArrangement = Arrangement.Center
        ) {
          CancelButton(onEvent)
        }
      }
    }
  )
}

@Composable
private fun TwoPaneLayout(
  params: RegistrationScaffold.Params.TwoPane,
  focusRequesters: List<FocusRequester>,
  state: TotpEntryState,
  onEvent: (TotpEntryScreenEvents) -> Unit
) {
  val firstPaneScrollState = rememberScrollState()
  val secondPaneScrollState = rememberScrollState()

  TwoPaneRegistrationScaffold(
    modifier = Modifier.fillMaxSize(),
    params = params,
    firstPane = { paddingValues ->
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(firstPaneScrollState)
          .padding(paddingValues)
      ) {
        Illustration()

        Spacer(modifier = Modifier.height(32.dp))

        Description(twoPane = true)
      }
    },
    secondPane = { paddingValues ->
      Column(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(secondPaneScrollState)
          .padding(paddingValues)
      ) {
        CodeField(
          focusRequesters = focusRequesters,
          state = state,
          onEvent = onEvent
        )
      }
    },
    footer = {
      RegistrationScaffold.FooterSurface(
        isElevated = firstPaneScrollState.canScrollForward || secondPaneScrollState.canScrollForward
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(params.footerPadding),
          horizontalArrangement = Arrangement.End
        ) {
          CancelButton(onEvent)
        }
      }
    }
  )
}

@Composable
private fun Illustration() {
  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(R.drawable.image_totp_phone),
      contentDescription = null
    )
  }
}

@Composable
private fun Description(twoPane: Boolean = false) {
  Text(
    text = stringResource(R.string.TotpEntryScreen__two_factor_authentication),
    style = if (twoPane) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
    modifier = Modifier
      .fillMaxWidth()
      .attachDebugLogHelper()
  )

  Text(
    text = stringResource(R.string.TotpEntryScreen__to_continue_enter_the_code),
    style = if (twoPane) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(top = 16.dp)
  )
}

@Composable
private fun CodeField(
  focusRequesters: List<FocusRequester>,
  state: TotpEntryState,
  onEvent: (TotpEntryScreenEvents) -> Unit
) {
  val digits = state.digits

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(TestTags.TOTP_ENTRY_INPUT),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0..2) {
      DigitField(
        value = digits[i],
        onValueChange = { newValue -> onEvent(TotpEntryScreenEvents.DigitChanged(i, newValue)) },
        focusRequester = focusRequesters[i],
        testTag = when (i) {
          0 -> TestTags.TOTP_ENTRY_DIGIT_0
          1 -> TestTags.TOTP_ENTRY_DIGIT_1
          else -> TestTags.TOTP_ENTRY_DIGIT_2
        },
        modifier = Modifier.weight(1f, fill = false)
      )
      if (i < 2) {
        Spacer(modifier = Modifier.width(4.dp))
      }
    }

    Text(
      text = "-",
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(horizontal = 8.dp),
      color = MaterialTheme.colorScheme.onSurface
    )

    for (i in 3..5) {
      if (i > 3) {
        Spacer(modifier = Modifier.width(4.dp))
      }
      DigitField(
        value = digits[i],
        onValueChange = { newValue -> onEvent(TotpEntryScreenEvents.DigitChanged(i, newValue)) },
        focusRequester = focusRequesters[i],
        testTag = when (i) {
          3 -> TestTags.TOTP_ENTRY_DIGIT_3
          4 -> TestTags.TOTP_ENTRY_DIGIT_4
          else -> TestTags.TOTP_ENTRY_DIGIT_5
        },
        modifier = Modifier.weight(1f, fill = false)
      )
    }
  }
}

@Composable
private fun DigitField(
  value: String,
  onValueChange: (String) -> Unit,
  focusRequester: FocusRequester,
  testTag: String,
  modifier: Modifier = Modifier
) {
  TextField(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier
      .width(48.dp)
      .focusRequester(focusRequester)
      .testTag(testTag)
      .onKeyEvent { keyEvent ->
        if ((keyEvent.key == Key.Backspace || keyEvent.key == Key.Delete) && value.isEmpty()) {
          onValueChange("")
          true
        } else {
          false
        }
      },
    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
    singleLine = true,
    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    colors = TextFieldDefaults.colors(
      focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      focusedIndicatorColor = MaterialTheme.colorScheme.primary,
      unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
    )
  )
}

@Composable
private fun CancelButton(onEvent: (TotpEntryScreenEvents) -> Unit) {
  TextButton(
    onClick = { onEvent(TotpEntryScreenEvents.CancelClicked) },
    modifier = Modifier.testTag(TestTags.TOTP_ENTRY_CANCEL_BUTTON)
  ) {
    Text(
      text = stringResource(R.string.TotpEntryScreen__cancel),
      color = MaterialTheme.colorScheme.primary,
      style = MaterialTheme.typography.labelLarge
    )
  }
}

@AllDevicePreviews
@Composable
private fun TotpEntryScreenPreview() {
  Previews.Preview {
    TotpEntryScreen(
      state = TotpEntryState(),
      onEvent = {}
    )
  }
}

@AllDevicePreviews
@Composable
private fun TotpEntryScreenPartiallyFilledPreview() {
  Previews.Preview {
    TotpEntryScreen(
      state = TotpEntryState(
        digits = listOf("4", "1", "8", "3", "7", ""),
        focusedDigitIndex = 5
      ),
      onEvent = {}
    )
  }
}
