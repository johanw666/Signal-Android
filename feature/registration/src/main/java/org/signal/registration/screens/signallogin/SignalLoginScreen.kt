/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.registration.screens.signallogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.signal.core.ui.compose.AllDevicePreviews
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.Dialogs
import org.signal.core.ui.compose.Previews
import org.signal.registration.R
import org.signal.registration.fonts.MonoTypeface
import org.signal.registration.screens.OnePaneRegistrationScaffold
import org.signal.registration.screens.RegistrationScaffold
import org.signal.registration.screens.TwoPaneRegistrationScaffold
import org.signal.registration.screens.attachDebugLogHelper
import org.signal.registration.screens.shared.BackTopAppBar
import org.signal.registration.test.TestTags

/**
 * Logs an existing Signal Login in by asking for its account key.
 */
@Composable
fun SignalLoginScreen(
  state: SignalLoginState,
  onEvent: (SignalLoginScreenEvents) -> Unit,
  modifier: Modifier = Modifier
) {
  val simpleError: Pair<String, SignalLoginScreenEvents>? = when {
    state.dialogs.networkError -> stringResource(R.string.VerificationCodeScreen__network_error) to SignalLoginScreenEvents.NetworkErrorDialogDismissed
    state.dialogs.unknownError -> stringResource(R.string.VerificationCodeScreen__an_unexpected_error_occurred) to SignalLoginScreenEvents.UnknownErrorDialogDismissed
    else -> null
  }

  simpleError?.let { (message, dismissedEvent) ->
    Dialogs.SimpleMessageDialog(
      message = message,
      dismiss = stringResource(android.R.string.ok),
      onDismiss = { onEvent(dismissedEvent) }
    )
  }

  Surface(
    modifier = modifier
      .fillMaxSize()
      .testTag(TestTags.SIGNAL_LOGIN_SCREEN)
  ) {
    when (val params = RegistrationScaffold.rememberLayoutParams()) {
      is RegistrationScaffold.Params.OnePane -> OnePaneLayout(params, state, onEvent)
      is RegistrationScaffold.Params.TwoPane -> TwoPaneLayout(params, state, onEvent)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnePaneLayout(
  params: RegistrationScaffold.Params.OnePane,
  state: SignalLoginState,
  onEvent: (SignalLoginScreenEvents) -> Unit
) {
  val scrollState = rememberScrollState()
  val topBarScrollBehavior = RegistrationScaffold.rememberTopBarScrollBehavior()

  OnePaneRegistrationScaffold(
    params = params,
    topBar = { BackTopAppBar(scrollBehavior = topBarScrollBehavior, onBackClick = { onEvent(SignalLoginScreenEvents.BackClicked) }) },
    content = { paddingValues ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .fillMaxSize()
          .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
          .verticalScroll(scrollState)
          .padding(paddingValues)
      ) {
        Header()

        Spacer(modifier = Modifier.height(32.dp))

        AccountKeyTextField(state = state, onEvent = onEvent)
      }
    },
    footer = { Footer(params, state, scrollState.canScrollForward, onEvent) }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoPaneLayout(
  params: RegistrationScaffold.Params.TwoPane,
  state: SignalLoginState,
  onEvent: (SignalLoginScreenEvents) -> Unit
) {
  val firstPaneScrollState = rememberScrollState()
  val secondPaneScrollState = rememberScrollState()
  val topBarScrollBehavior = RegistrationScaffold.rememberTopBarScrollBehavior()

  TwoPaneRegistrationScaffold(
    params = params,
    topBar = { BackTopAppBar(scrollBehavior = topBarScrollBehavior, onBackClick = { onEvent(SignalLoginScreenEvents.BackClicked) }) },
    firstPane = { paddingValues ->
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
          .verticalScroll(firstPaneScrollState)
          .padding(paddingValues)
      ) {
        Header(twoPane = true)
      }
    },
    secondPane = { paddingValues ->
      Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .nestedScroll(topBarScrollBehavior.nestedScrollConnection)
          .verticalScroll(secondPaneScrollState)
          .padding(paddingValues)
      ) {
        AccountKeyTextField(state = state, onEvent = onEvent)
      }
    },
    footer = { Footer(params, state, firstPaneScrollState.canScrollForward || secondPaneScrollState.canScrollForward, onEvent) }
  )
}

@Composable
private fun Header(twoPane: Boolean = false) {
  Image(
    painter = painterResource(R.drawable.image_signal_login_ring),
    contentDescription = null,
    modifier = Modifier.size(64.dp)
  )

  Spacer(modifier = Modifier.height(20.dp))

  Text(
    text = stringResource(R.string.SignalLoginScreen__signal_login),
    style = if (twoPane) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
    textAlign = TextAlign.Center,
    modifier = Modifier
      .fillMaxWidth()
      .attachDebugLogHelper()
  )

  Spacer(modifier = Modifier.height(12.dp))

  Text(
    text = stringResource(R.string.SignalLoginScreen__enter_your_32_character_account_key),
    style = if (twoPane) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal) else MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth()
  )
}

@Composable
private fun AccountKeyTextField(
  state: SignalLoginState,
  onEvent: (SignalLoginScreenEvents) -> Unit
) {
  val focusRequester = remember { FocusRequester() }
  var requestFocus by remember { mutableStateOf(true) }
  val keyboardController = LocalSoftwareKeyboardController.current

  TextField(
    value = state.accountKey,
    onValueChange = { onEvent(SignalLoginScreenEvents.AccountKeyChanged(it)) },
    label = { Text(stringResource(R.string.SignalLoginScreen__account_key)) },
    enabled = !state.isSubmitting,
    singleLine = true,
    textStyle = MaterialTheme.typography.bodyLarge.copy(
      fontFamily = MonoTypeface.fontFamily(),
      fontSize = 18.sp,
      letterSpacing = 1.44.sp
    ),
    colors = TextFieldDefaults.colors(
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
      errorContainerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      capitalization = KeyboardCapitalization.None,
      imeAction = ImeAction.Next,
      autoCorrectEnabled = false
    ),
    keyboardActions = KeyboardActions(
      onNext = {
        if (state.isNextEnabled) {
          keyboardController?.hide()
          onEvent(SignalLoginScreenEvents.NextClicked)
        }
      }
    ),
    supportingText = {
      when (val error = state.accountKeyError) {
        is AccountKeyError.TooLong -> Text(stringResource(R.string.SignalLoginScreen__too_long, error.count, SignalLoginState.ACCOUNT_KEY_LENGTH))
        is AccountKeyError.Invalid -> Text(stringResource(R.string.SignalLoginScreen__invalid_account_key))
        is AccountKeyError.Incorrect -> Text(stringResource(R.string.SignalLoginScreen__incorrect_account_key))
        null -> {}
      }
    },
    isError = state.accountKeyError != null,
    visualTransformation = AccountKeyVisualTransformation,
    modifier = Modifier
      .fillMaxWidth()
      .testTag(TestTags.SIGNAL_LOGIN_ACCOUNT_KEY_FIELD)
      .focusRequester(focusRequester)
      .onGloballyPositioned {
        if (requestFocus) {
          focusRequester.requestFocus()
          requestFocus = false
        }
      }
  )
}

@Composable
private fun Footer(
  params: RegistrationScaffold.Params,
  state: SignalLoginState,
  isElevated: Boolean,
  onEvent: (SignalLoginScreenEvents) -> Unit
) {
  RegistrationScaffold.FooterSurface(isElevated = isElevated) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(params.footerPadding)
    ) {
      Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier.weight(1f)
      ) {
        NeedHelpButton(onEvent)
      }

      Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier.weight(1f)
      ) {
        NextButton(state, onEvent)
      }
    }
  }
}

@Composable
private fun NeedHelpButton(onEvent: (SignalLoginScreenEvents) -> Unit) {
  TextButton(
    shape = RoundedCornerShape(0.dp),
    onClick = { onEvent(SignalLoginScreenEvents.NeedHelpClicked) },
    modifier = Modifier.testTag(TestTags.SIGNAL_LOGIN_NEED_HELP_BUTTON)
  ) {
    Text(text = stringResource(R.string.SignalLoginScreen__need_help))
  }
}

@Composable
private fun NextButton(state: SignalLoginState, onEvent: (SignalLoginScreenEvents) -> Unit) {
  Buttons.LargeTonal(
    enabled = state.isNextEnabled,
    onClick = { onEvent(SignalLoginScreenEvents.NextClicked) },
    modifier = Modifier.testTag(TestTags.SIGNAL_LOGIN_NEXT_BUTTON)
  ) {
    if (state.isSubmitting) {
      CircularProgressIndicator(
        strokeWidth = 3.dp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
      )
    } else {
      Text(text = stringResource(R.string.SignalLoginScreen__next))
    }
  }
}

/**
 * Renders an account key the way its ACI is normally written: uppercased and split into 8-4-4-4-12 groups by dashes.
 * The dashes are display-only, so what the view model sees is always the unformatted key.
 */
internal object AccountKeyVisualTransformation : VisualTransformation {

  /** Offsets in the raw key that a dash is inserted in front of. */
  private val DASH_OFFSETS = intArrayOf(8, 12, 16, 20)

  override fun filter(text: AnnotatedString): TransformedText {
    val transformed = buildString {
      for ((index, character) in text.text.withIndex()) {
        if (index in DASH_OFFSETS) {
          append('-')
        }
        append(character.uppercaseChar())
      }
    }

    return TransformedText(
      text = AnnotatedString(transformed),
      offsetMapping = AccountKeyOffsetMapping(text.length)
    )
  }

  /**
   * A dash is only present if the key is long enough to have a character after it, so [inputLength] decides which of
   * [DASH_OFFSETS] actually made it into the transformed text.
   */
  private class AccountKeyOffsetMapping(private val inputLength: Int) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int = offset + DASH_OFFSETS.count { it <= offset && it < inputLength }

    override fun transformedToOriginal(offset: Int): Int = offset - DASH_OFFSETS.withIndex().count { (index, dashOffset) -> dashOffset < inputLength && dashOffset + index < offset }
  }
}

@AllDevicePreviews
@Composable
private fun SignalLoginScreenPreview() {
  Previews.Preview {
    SignalLoginScreen(
      state = SignalLoginState(),
      onEvent = {}
    )
  }
}

@AllDevicePreviews
@Composable
private fun SignalLoginScreenFilledPreview() {
  Previews.Preview {
    SignalLoginScreen(
      state = SignalLoginState(accountKey = "a6b284822e3283d07f2391360a4c2b91"),
      onEvent = {}
    )
  }
}

@AllDevicePreviews
@Composable
private fun SignalLoginScreenSubmittingPreview() {
  Previews.Preview {
    SignalLoginScreen(
      state = SignalLoginState(
        accountKey = "a6b284822e3283d07f2391360a4c2b91",
        isSubmitting = true
      ),
      onEvent = {}
    )
  }
}

@AllDevicePreviews
@Composable
private fun SignalLoginScreenErrorPreview() {
  Previews.Preview {
    SignalLoginScreen(
      state = SignalLoginState(
        accountKey = "a6b284822e3283d07f2391360a4c2b91",
        accountKeyError = AccountKeyError.Incorrect
      ),
      onEvent = {}
    )
  }
}
