/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpcodeentry

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.signal.appsettings.R
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.DayNightPreviews
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons

@VisibleForTesting
object TotpCodeEntryTestTags {
  const val CODE_INPUT = "code-input"
  const val BUTTON_DONE = "button-done"
  const val ERROR = "error"
}

/**
 * Collects the one-time code the user's authenticator app generated, which is the last step of setting one up.
 */
@Composable
fun TotpCodeEntryScreen(
  state: TotpCodeEntryState,
  onEvent: (TotpCodeEntryEvent) -> Unit
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Scaffolds.Settings(
    title = stringResource(R.string.TotpCodeEntryScreen__enter_your_code),
    onNavigationClick = { onEvent(TotpCodeEntryEvent.NavigateBackClicked) },
    navigationIcon = SignalIcons.ArrowStart.imageVector
  ) { contentPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .imePadding(),
      horizontalAlignment = Alignment.End
    ) {
      Text(
        text = stringResource(R.string.TotpCodeEntryScreen__enter_the_6_digit_code),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp)
      )

      val errorMessage = state.error.message()

      TextField(
        value = state.code,
        onValueChange = { onEvent(TotpCodeEntryEvent.CodeChanged(it)) },
        label = { Text(text = stringResource(R.string.TotpCodeEntryScreen__code)) },
        singleLine = true,
        enabled = !state.submitting,
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message ->
          { Text(text = message, modifier = Modifier.testTag(TotpCodeEntryTestTags.ERROR)) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onEvent(TotpCodeEntryEvent.DoneClicked) }),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .focusRequester(focusRequester)
          .testTag(TotpCodeEntryTestTags.CODE_INPUT)
      )

      Spacer(modifier = Modifier.weight(1f))

      Buttons.LargeTonal(
        onClick = { onEvent(TotpCodeEntryEvent.DoneClicked) },
        enabled = state.canSubmit,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
          .padding(horizontal = 24.dp, vertical = 24.dp)
          .testTag(TotpCodeEntryTestTags.BUTTON_DONE)
      ) {
        Text(text = stringResource(R.string.TotpCodeEntryScreen__done))
      }
    }
  }
}

/**
 * The message shown under the code field, or null when there's nothing wrong.
 */
@Composable
private fun TotpCodeEntryState.Error.message(): String? = when (this) {
  TotpCodeEntryState.Error.None -> null
  TotpCodeEntryState.Error.IncorrectCode -> stringResource(R.string.TotpCodeEntryScreen__incorrect_code)
  TotpCodeEntryState.Error.NetworkFailure -> stringResource(R.string.TotpCodeEntryScreen__couldnt_reach_signal)
}

@DayNightPreviews
@Composable
private fun TotpCodeEntryScreenPreview() {
  Previews.Preview {
    TotpCodeEntryScreen(
      state = TotpCodeEntryState(code = "123456"),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun TotpCodeEntryScreenErrorPreview() {
  Previews.Preview {
    TotpCodeEntryScreen(
      state = TotpCodeEntryState(code = "123456", error = TotpCodeEntryState.Error.IncorrectCode),
      onEvent = {}
    )
  }
}
