/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorname

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
object AuthenticatorNameTestTags {
  const val NAME_INPUT = "name-input"
  const val BUTTON_NEXT = "button-next"
}

/**
 * Collects the name the user wants to identify an authenticator app by, either right after pairing one or when
 * renaming one that already exists.
 */
@Composable
fun AuthenticatorNameScreen(
  state: AuthenticatorNameState,
  onEvent: (AuthenticatorNameEvent) -> Unit
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Scaffolds.Settings(
    title = stringResource(R.string.AuthenticatorNameScreen__choose_a_name),
    onNavigationClick = { onEvent(AuthenticatorNameEvent.NavigateBackClicked) },
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
        text = if (state.renaming) {
          stringResource(R.string.AuthenticatorNameScreen__choose_a_unique_name)
        } else {
          stringResource(R.string.AuthenticatorNameScreen__choose_a_unique_name_to_help_you_identify_it)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 16.dp)
      )

      TextField(
        value = state.name,
        onValueChange = { onEvent(AuthenticatorNameEvent.NameChanged(it)) },
        label = { Text(text = stringResource(R.string.AuthenticatorNameScreen__name)) },
        singleLine = true,
        enabled = !state.submitting,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (state.canSubmit) onEvent(AuthenticatorNameEvent.NextClicked) }),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp)
          .focusRequester(focusRequester)
          .testTag(AuthenticatorNameTestTags.NAME_INPUT)
      )

      Spacer(modifier = Modifier.weight(1f))

      Buttons.LargeTonal(
        onClick = { onEvent(AuthenticatorNameEvent.NextClicked) },
        enabled = state.canSubmit,
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier
          .padding(horizontal = 24.dp, vertical = 24.dp)
          .testTag(AuthenticatorNameTestTags.BUTTON_NEXT)
      ) {
        Text(text = stringResource(R.string.AuthenticatorNameScreen__next))
      }
    }
  }
}

@DayNightPreviews
@Composable
private fun AuthenticatorNameScreenPreview() {
  Previews.Preview {
    AuthenticatorNameScreen(
      state = AuthenticatorNameState(name = "Bitwarden Authenticator"),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun AuthenticatorNameScreenRenamePreview() {
  Previews.Preview {
    AuthenticatorNameScreen(
      state = AuthenticatorNameState(name = "Twilio Authy", renaming = true),
      onEvent = {}
    )
  }
}
