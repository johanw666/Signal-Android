/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.totpapplist

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
import androidx.compose.material3.CircularProgressIndicator
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
import org.signal.appsettings.totpapplist.TotpAppListState.Dialog
import org.signal.appsettings.totpapplist.TotpAppListState.LoadState
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
object TotpAppListTestTags {
  const val SCROLLER = "scroller"
  const val LEARN_MORE = "learn-more"
  const val BUTTON_ADD = "button-add"
  const val ROW_APP = "row-app"
  const val BUTTON_APP_MENU = "button-app-menu"
  const val MENU_ITEM_RENAME = "menu-item-rename"
  const val MENU_ITEM_REMOVE = "menu-item-remove"
  const val EMPTY_MESSAGE = "empty-message"
  const val LOAD_FAILED_MESSAGE = "load-failed-message"
  const val DIALOG_CONFIRM_REMOVE = "dialog-confirm-remove"
  const val DIALOG_MAX_APPS_REACHED = "dialog-max-apps-reached"
  const val LOADING = "loading"
}

/**
 * Lists the authenticator apps configured on the account and lets the user add, rename, or remove one.
 */
@Composable
fun TotpAppListScreen(
  state: TotpAppListState,
  onEvent: (TotpAppListEvent) -> Unit
) {
  Scaffolds.Settings(
    title = stringResource(R.string.TotpAppListScreen__authenticator_app),
    onNavigationClick = { onEvent(TotpAppListEvent.NavigateBackClicked) },
    navigationIcon = SignalIcons.ArrowStart.imageVector
  ) { contentPadding ->
    LazyColumn(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(contentPadding)
        .testTag(TotpAppListTestTags.SCROLLER)
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
            .testTag(TotpAppListTestTags.LEARN_MORE)
        )
      }

      item {
        Buttons.MediumTonal(
          onClick = { onEvent(TotpAppListEvent.AddTotpAppClicked) },
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 20.dp)
            .padding(horizontal = 40.dp)
            .testTag(TotpAppListTestTags.BUTTON_ADD)
        ) {
          Text(text = stringResource(R.string.TotpAppListScreen__add_authenticator_app))
        }
      }

      item {
        Dividers.Default()
      }

      item {
        Texts.SectionHeader(
          text = stringResource(R.string.TotpAppListScreen__authenticator_apps),
          modifier = Modifier.fillMaxWidth()
        )
      }

      when (state.loadState) {
        LoadState.LOADING -> item {
          CircularProgressIndicator(
            modifier = Modifier
              .padding(top = 40.dp)
              .size(24.dp)
              .testTag(TotpAppListTestTags.LOADING)
          )
        }

        LoadState.NETWORK_FAILURE -> item {
          SectionMessage(
            text = stringResource(R.string.TotpAppListScreen__couldnt_load_authenticator_apps),
            modifier = Modifier.testTag(TotpAppListTestTags.LOAD_FAILED_MESSAGE)
          )
        }

        LoadState.LOADED -> if (state.apps.isEmpty()) {
          item {
            SectionMessage(
              text = stringResource(R.string.TotpAppListScreen__no_authenticator_apps),
              modifier = Modifier.testTag(TotpAppListTestTags.EMPTY_MESSAGE)
            )
          }
        } else {
          items(state.apps, key = { it.id }) { app ->
            TotpAppRow(
              app = app,
              onEvent = onEvent
            )
          }
        }
      }
    }
  }

  when (val dialog = state.dialog) {
    Dialog.None -> Unit
    is Dialog.ConfirmRemove -> ConfirmRemoveDialog(appId = dialog.appId, onEvent = onEvent)
    Dialog.MaxAppsReached -> MaxAppsReachedDialog(maxApps = state.maxApps, onEvent = onEvent)
  }
}

/** Whatever the list section has to say when it has no rows to show. */
@Composable
private fun SectionMessage(
  text: String,
  modifier: Modifier = Modifier
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = modifier
      .padding(top = 40.dp)
      .padding(horizontal = 34.dp)
  )
}

@Composable
private fun TotpAppRow(
  app: TotpApp,
  onEvent: (TotpAppListEvent) -> Unit
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
        label = stringResource(R.string.TotpAppListScreen__added_s, addedTime)
      )

      TotpAppMenuButton(
        app = app,
        onEvent = onEvent
      )
    },
    modifier = Modifier.testTag(TotpAppListTestTags.ROW_APP)
  )
}

@Composable
private fun TotpAppMenuButton(
  app: TotpApp,
  onEvent: (TotpAppListEvent) -> Unit
) {
  val menuController = remember { DropdownMenus.MenuController() }

  Box {
    IconButton(
      onClick = menuController::show,
      modifier = Modifier.testTag(TotpAppListTestTags.BUTTON_APP_MENU)
    ) {
      Icon(
        imageVector = SignalIcons.MoreVertical.imageVector,
        contentDescription = stringResource(R.string.TotpAppListScreen__open_authenticator_app_options),
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

    DropdownMenus.Menu(controller = menuController) { controller ->
      DropdownMenus.Item(
        leadingIconResId = CoreUiR.drawable.symbol_edit_24,
        text = { Text(text = stringResource(R.string.TotpAppListScreen__rename)) },
        onClick = {
          onEvent(TotpAppListEvent.RenameAppClicked(app.id))
          controller.hide()
        },
        modifier = Modifier.testTag(TotpAppListTestTags.MENU_ITEM_RENAME)
      )

      DropdownMenus.Item(
        leadingIconResId = CoreUiR.drawable.symbol_x_circle_24,
        text = { Text(text = stringResource(R.string.TotpAppListScreen__remove)) },
        onClick = {
          onEvent(TotpAppListEvent.RemoveAppClicked(app.id))
          controller.hide()
        },
        modifier = Modifier.testTag(TotpAppListTestTags.MENU_ITEM_REMOVE)
      )
    }
  }
}

@Composable
private fun DescriptionWithLearnMore(
  onEvent: (TotpAppListEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Text(
    text = buildAnnotatedString {
      append(stringResource(R.string.TotpAppListScreen__set_up_an_authenticator_app))
      append(' ')

      withLink(
        LinkAnnotation.Clickable(
          tag = "learn-more",
          styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
          linkInteractionListener = { onEvent(TotpAppListEvent.LearnMoreClicked) }
        )
      ) {
        append(stringResource(R.string.TotpAppListScreen__learn_more))
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
  appId: Long,
  onEvent: (TotpAppListEvent) -> Unit
) {
  Dialogs.SimpleAlertDialog(
    title = stringResource(R.string.TotpAppListScreen__remove_authenticator_app),
    body = stringResource(R.string.TotpAppListScreen__you_wont_be_able_to_use_this_app),
    confirm = stringResource(R.string.TotpAppListScreen__remove),
    onConfirm = { onEvent(TotpAppListEvent.RemoveAppConfirmed(appId)) },
    onDismiss = { onEvent(TotpAppListEvent.DialogDismissed) },
    dismiss = stringResource(android.R.string.cancel),
    onDismissRequest = { onEvent(TotpAppListEvent.DialogDismissed) },
    modifier = Modifier.testTag(TotpAppListTestTags.DIALOG_CONFIRM_REMOVE)
  )
}

@Composable
private fun MaxAppsReachedDialog(
  maxApps: Int,
  onEvent: (TotpAppListEvent) -> Unit
) {
  Dialogs.SimpleAlertDialog(
    title = stringResource(R.string.TotpAppListScreen__cant_add_authenticator_app),
    body = stringResource(R.string.TotpAppListScreen__you_cant_add_more_than_d, maxApps),
    confirm = stringResource(android.R.string.ok),
    onConfirm = {},
    onDismiss = { onEvent(TotpAppListEvent.DialogDismissed) },
    dismiss = stringResource(R.string.TotpAppListScreen__learn_more),
    onDeny = { onEvent(TotpAppListEvent.LearnMoreClicked) },
    onDismissRequest = { onEvent(TotpAppListEvent.DialogDismissed) },
    modifier = Modifier.testTag(TotpAppListTestTags.DIALOG_MAX_APPS_REACHED)
  )
}

@DayNightPreviews
@Composable
private fun TotpAppListScreenPreview() {
  Previews.Preview {
    TotpAppListScreen(
      state = TotpAppListState(),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun TotpAppListScreenEmptyPreview() {
  Previews.Preview {
    TotpAppListScreen(
      state = TotpAppListState(loadState = LoadState.LOADED),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun TotpAppListScreenLoadFailedPreview() {
  Previews.Preview {
    TotpAppListScreen(
      state = TotpAppListState(loadState = LoadState.NETWORK_FAILURE),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun TotpAppListPreview() {
  Previews.Preview {
    TotpAppListScreen(
      state = TotpAppListState(apps = PREVIEW_APPS, loadState = LoadState.LOADED),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun ConfirmRemoveDialogPreview() {
  Previews.Preview {
    ConfirmRemoveDialog(appId = 1, onEvent = {})
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
  TotpApp(id = 1, name = "Bitwarden Authenticator", createdAt = System.currentTimeMillis()),
  TotpApp(id = 2, name = "Twilio Authy", createdAt = System.currentTimeMillis())
)
