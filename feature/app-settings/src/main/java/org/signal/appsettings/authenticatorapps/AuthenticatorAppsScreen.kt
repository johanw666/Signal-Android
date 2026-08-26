/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.authenticatorapps

import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import org.signal.appsettings.R
import org.signal.appsettings.authenticatorapps.AuthenticatorAppsState.Dialog
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.DayNightPreviews
import org.signal.core.ui.compose.Dialogs
import org.signal.core.ui.compose.Dividers
import org.signal.core.ui.compose.DropdownMenus
import org.signal.core.ui.compose.Previews
import org.signal.core.ui.compose.Rows
import org.signal.core.ui.compose.Rows.TextAndLabel
import org.signal.core.ui.compose.Scaffolds
import org.signal.core.ui.compose.SignalIcons
import org.signal.core.ui.compose.Texts
import org.signal.core.ui.R as CoreUiR

@VisibleForTesting
object AuthenticatorAppsTestTags {
  const val SCROLLER = "scroller"
  const val LEARN_MORE = "learn-more"
  const val BUTTON_ADD = "button-add"
  const val ROW_APP = "row-app"
  const val BUTTON_APP_MENU = "button-app-menu"
  const val MENU_ITEM_RENAME = "menu-item-rename"
  const val MENU_ITEM_REMOVE = "menu-item-remove"
  const val EMPTY_MESSAGE = "empty-message"
  const val DIALOG_CONFIRM_REMOVE = "dialog-confirm-remove"
  const val DIALOG_MAX_APPS_REACHED = "dialog-max-apps-reached"
}

/**
 * Lists the authenticator apps configured on the account and lets the user add, rename, or remove one.
 */
@Composable
fun AuthenticatorAppsScreen(
  state: AuthenticatorAppsState,
  onEvent: (AuthenticatorAppsEvent) -> Unit
) {
  Scaffolds.Settings(
    title = stringResource(R.string.AuthenticatorAppsScreen__authenticator_app),
    onNavigationClick = { onEvent(AuthenticatorAppsEvent.NavigateBackClicked) },
    navigationIcon = SignalIcons.ArrowStart.imageVector
  ) { contentPadding ->
    LazyColumn(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(contentPadding)
        .testTag(AuthenticatorAppsTestTags.SCROLLER)
    ) {
      item {
        Image(
          painter = painterResource(R.drawable.image_authenticator_open_app),
          contentDescription = null,
          modifier = Modifier
            .padding(top = 32.dp)
            .size(width = 55.dp, height = 105.dp)
        )
      }

      item {
        DescriptionWithLearnMore(
          onEvent = onEvent,
          modifier = Modifier
            .padding(top = 24.dp)
            .padding(horizontal = 34.dp)
            .testTag(AuthenticatorAppsTestTags.LEARN_MORE)
        )
      }

      item {
        Buttons.MediumTonal(
          onClick = { onEvent(AuthenticatorAppsEvent.AddAuthenticatorAppClicked) },
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 20.dp)
            .padding(horizontal = 40.dp)
            .testTag(AuthenticatorAppsTestTags.BUTTON_ADD)
        ) {
          Text(text = stringResource(R.string.AuthenticatorAppsScreen__add_authenticator_app))
        }
      }

      item {
        Dividers.Default()
      }

      item {
        Texts.SectionHeader(
          text = stringResource(R.string.AuthenticatorAppsScreen__authenticator_apps),
          modifier = Modifier.fillMaxWidth()
        )
      }

      if (state.apps.isEmpty()) {
        item {
          Text(
            text = stringResource(R.string.AuthenticatorAppsScreen__no_authenticator_apps),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(top = 40.dp)
              .testTag(AuthenticatorAppsTestTags.EMPTY_MESSAGE)
          )
        }
      } else {
        items(state.apps, key = { it.id }) { app ->
          AuthenticatorAppRow(
            app = app,
            onEvent = onEvent
          )
        }
      }
    }
  }

  when (val dialog = state.dialog) {
    Dialog.None -> Unit
    is Dialog.ConfirmRemove -> ConfirmRemoveDialog(onEvent)
    Dialog.MaxAppsReached -> MaxAppsReachedDialog(maxApps = state.maxApps, onEvent = onEvent)
  }
}

@Composable
private fun AuthenticatorAppRow(
  app: AuthenticatorApp,
  onEvent: (AuthenticatorAppsEvent) -> Unit
) {
  val context = LocalContext.current
  val addedTime = remember(app.createdAt) {
    DateUtils.getRelativeDateTimeString(context, app.createdAt, DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString()
  }

  Rows.TextRow(
    icon = {
      Icon(
        painter = SignalIcons.DevicePhone.painter,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      TextAndLabel(
        text = app.name,
        label = stringResource(R.string.AuthenticatorAppsScreen__added_s, addedTime)
      )

      AuthenticatorAppMenuButton(
        app = app,
        onEvent = onEvent
      )
    },
    modifier = Modifier.testTag(AuthenticatorAppsTestTags.ROW_APP)
  )
}

@Composable
private fun AuthenticatorAppMenuButton(
  app: AuthenticatorApp,
  onEvent: (AuthenticatorAppsEvent) -> Unit
) {
  val menuController = remember { DropdownMenus.MenuController() }

  Box {
    IconButton(
      onClick = menuController::show,
      modifier = Modifier.testTag(AuthenticatorAppsTestTags.BUTTON_APP_MENU)
    ) {
      Icon(
        imageVector = SignalIcons.MoreVertical.imageVector,
        contentDescription = stringResource(R.string.AuthenticatorAppsScreen__open_authenticator_app_options),
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

    DropdownMenus.Menu(controller = menuController) { controller ->
      DropdownMenus.Item(
        leadingIconResId = CoreUiR.drawable.symbol_edit_24,
        text = { Text(text = stringResource(R.string.AuthenticatorAppsScreen__rename)) },
        onClick = {
          onEvent(AuthenticatorAppsEvent.RenameAppClicked(app.id))
          controller.hide()
        },
        modifier = Modifier.testTag(AuthenticatorAppsTestTags.MENU_ITEM_RENAME)
      )

      DropdownMenus.Item(
        leadingIconResId = CoreUiR.drawable.symbol_x_circle_24,
        text = { Text(text = stringResource(R.string.AuthenticatorAppsScreen__remove)) },
        onClick = {
          onEvent(AuthenticatorAppsEvent.RemoveAppClicked(app.id))
          controller.hide()
        },
        modifier = Modifier.testTag(AuthenticatorAppsTestTags.MENU_ITEM_REMOVE)
      )
    }
  }
}

@Composable
private fun DescriptionWithLearnMore(
  onEvent: (AuthenticatorAppsEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Text(
    text = buildAnnotatedString {
      append(stringResource(R.string.AuthenticatorAppsScreen__set_up_an_authenticator_app))
      append(' ')

      withLink(
        LinkAnnotation.Clickable(
          tag = "learn-more",
          styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
          linkInteractionListener = { onEvent(AuthenticatorAppsEvent.LearnMoreClicked) }
        )
      ) {
        append(stringResource(R.string.AuthenticatorAppsScreen__learn_more))
      }
    },
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = modifier
  )
}

@Composable
private fun ConfirmRemoveDialog(
  onEvent: (AuthenticatorAppsEvent) -> Unit
) {
  Dialogs.SimpleAlertDialog(
    title = stringResource(R.string.AuthenticatorAppsScreen__remove_authenticator_app),
    body = stringResource(R.string.AuthenticatorAppsScreen__to_remove_this_authentication_method),
    confirm = stringResource(R.string.AuthenticatorAppsScreen__remove),
    onConfirm = { onEvent(AuthenticatorAppsEvent.RemoveAppConfirmed) },
    onDismiss = { onEvent(AuthenticatorAppsEvent.DialogDismissed) },
    dismiss = stringResource(android.R.string.cancel),
    onDismissRequest = { onEvent(AuthenticatorAppsEvent.DialogDismissed) },
    modifier = Modifier.testTag(AuthenticatorAppsTestTags.DIALOG_CONFIRM_REMOVE)
  )
}

@Composable
private fun MaxAppsReachedDialog(
  maxApps: Int,
  onEvent: (AuthenticatorAppsEvent) -> Unit
) {
  Dialogs.SimpleAlertDialog(
    title = stringResource(R.string.AuthenticatorAppsScreen__cant_add_authenticator_app),
    body = stringResource(R.string.AuthenticatorAppsScreen__you_cant_add_more_than_d, maxApps),
    confirm = stringResource(android.R.string.ok),
    onConfirm = {},
    onDismiss = { onEvent(AuthenticatorAppsEvent.DialogDismissed) },
    dismiss = stringResource(R.string.AuthenticatorAppsScreen__learn_more),
    onDeny = { onEvent(AuthenticatorAppsEvent.LearnMoreClicked) },
    onDismissRequest = { onEvent(AuthenticatorAppsEvent.DialogDismissed) },
    modifier = Modifier.testTag(AuthenticatorAppsTestTags.DIALOG_MAX_APPS_REACHED)
  )
}

@DayNightPreviews
@Composable
private fun AuthenticatorAppsScreenPreview() {
  Previews.Preview {
    AuthenticatorAppsScreen(
      state = AuthenticatorAppsState(),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun AuthenticatorAppsScreenWithAppsPreview() {
  Previews.Preview {
    AuthenticatorAppsScreen(
      state = AuthenticatorAppsState(apps = PREVIEW_APPS),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun ConfirmRemoveDialogPreview() {
  Previews.Preview {
    ConfirmRemoveDialog(onEvent = {})
  }
}

@DayNightPreviews
@Composable
private fun MaxAppsReachedDialogPreview() {
  Previews.Preview {
    MaxAppsReachedDialog(maxApps = 2, onEvent = {})
  }
}

private val PREVIEW_APPS = listOf(
  AuthenticatorApp(id = 1, name = "Bitwarden Authenticator", createdAt = System.currentTimeMillis()),
  AuthenticatorApp(id = 2, name = "Twilio Authy", createdAt = System.currentTimeMillis())
)
