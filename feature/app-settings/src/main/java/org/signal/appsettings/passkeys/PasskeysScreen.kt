/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.signal.appsettings.passkeys

import android.text.format.DateUtils
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.signal.core.ui.compose.Buttons
import org.signal.core.ui.compose.DayNightPreviews
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
object PasskeysTestTags {
  const val SCROLLER = "scroller"
  const val LEARN_MORE = "learn-more"
  const val BUTTON_SET_UP = "button-set-up"
  const val ROW_PASSKEY = "row-passkey"
  const val BUTTON_PASSKEY_MENU = "button-passkey-menu"
  const val MENU_ITEM_RENAME = "menu-item-rename"
  const val MENU_ITEM_REMOVE = "menu-item-remove"
}

/**
 * Explains what passkeys are and lets the user start creating one. Once passkeys exist, they're listed here with
 * management options instead.
 */
@Composable
fun PasskeysScreen(
  state: PasskeysState,
  onEvent: (PasskeysEvent) -> Unit
) {
  Scaffolds.Settings(
    title = stringResource(R.string.PasskeysScreen__passkeys),
    onNavigationClick = { onEvent(PasskeysEvent.NavigateBackClicked) },
    navigationIcon = SignalIcons.ArrowStart.imageVector
  ) { contentPadding ->
    if (state.passkeys.isEmpty()) {
      NoPasskeysContent(
        onEvent = onEvent,
        modifier = Modifier.padding(contentPadding)
      )
    } else {
      PasskeyListContent(
        passkeys = state.passkeys,
        onEvent = onEvent,
        modifier = Modifier.padding(contentPadding)
      )
    }
  }
}

/**
 * Shown before any passkeys exist: an explanation of what passkeys are with a button to create the first one.
 */
@Composable
private fun NoPasskeysContent(
  onEvent: (PasskeysEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxSize()) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .testTag(PasskeysTestTags.SCROLLER)
    ) {
      Image(
        painter = painterResource(R.drawable.image_passkeys_phone),
        contentDescription = null,
        modifier = Modifier.padding(top = 32.dp)
      )

      DescriptionWithLearnMore(
        text = stringResource(R.string.PasskeysScreen__with_passkeys_you_can_easily_add),
        onEvent = onEvent,
        modifier = Modifier
          .padding(top = 24.dp)
          .padding(horizontal = 28.dp)
          .testTag(PasskeysTestTags.LEARN_MORE)
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 40.dp)
          .padding(horizontal = 64.dp)
      ) {
        BulletRow(
          icon = SignalIcons.CheckCircle,
          text = stringResource(R.string.PasskeysScreen__give_your_passkey_a_friendly_name)
        )

        BulletRow(
          icon = SignalIcons.Lock,
          text = stringResource(R.string.PasskeysScreen__use_your_devices_biometrics)
        )

        BulletRow(
          icon = SignalIcons.Trash,
          text = stringResource(R.string.PasskeysScreen__add_or_remove_passkeys_at_anytime)
        )
      }
    }

    SetUpPasskeyButton(
      text = stringResource(R.string.PasskeysScreen__set_up_a_passkey),
      onEvent = onEvent,
      modifier = Modifier.padding(vertical = 16.dp)
    )
  }
}

/**
 * Shown once passkeys exist: a shorter explanation with a button to add another, followed by the list of passkeys.
 */
@Composable
private fun PasskeyListContent(
  passkeys: List<Passkey>,
  onEvent: (PasskeysEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyColumn(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
      .fillMaxSize()
      .testTag(PasskeysTestTags.SCROLLER)
  ) {
    item {
      Image(
        painter = painterResource(R.drawable.image_passkeys_phone),
        contentDescription = null,
        modifier = Modifier.padding(top = 32.dp)
      )
    }

    item {
      DescriptionWithLearnMore(
        text = stringResource(R.string.PasskeysScreen__set_up_a_passkey_with),
        onEvent = onEvent,
        modifier = Modifier
          .padding(top = 24.dp)
          .padding(horizontal = 34.dp)
          .testTag(PasskeysTestTags.LEARN_MORE)
      )
    }

    item {
      SetUpPasskeyButton(
        text = stringResource(R.string.PasskeysScreen__add_a_new_passkey),
        onEvent = onEvent,
        modifier = Modifier.padding(top = 24.dp, bottom = 20.dp)
      )
    }

    item {
      Dividers.Default()
    }

    item {
      Texts.SectionHeader(
        text = stringResource(R.string.PasskeysScreen__passkeys),
        modifier = Modifier.fillMaxWidth()
      )
    }

    items(passkeys, key = { it.id }) { passkey ->
      PasskeyRow(
        passkey = passkey,
        onEvent = onEvent
      )
    }
  }
}

@Composable
private fun PasskeyRow(
  passkey: Passkey,
  onEvent: (PasskeysEvent) -> Unit
) {
  val context = LocalContext.current
  val addedTime = remember(passkey.createdAt) {
    DateUtils.getRelativeDateTimeString(context, passkey.createdAt, DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0).toString()
  }

  Rows.TextRow(
    icon = {
      Icon(
        painter = SignalIcons.Key.painter,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      TextAndLabel(
        text = passkey.name,
        label = stringResource(R.string.PasskeysScreen__added_s, addedTime)
      )

      PasskeyMenuButton(
        passkey = passkey,
        onEvent = onEvent
      )
    },
    modifier = Modifier.testTag(PasskeysTestTags.ROW_PASSKEY)
  )
}

@Composable
private fun PasskeyMenuButton(
  passkey: Passkey,
  onEvent: (PasskeysEvent) -> Unit
) {
  val menuController = remember { DropdownMenus.MenuController() }

  Box {
    IconButton(
      onClick = menuController::show,
      modifier = Modifier.testTag(PasskeysTestTags.BUTTON_PASSKEY_MENU)
    ) {
      Icon(
        imageVector = SignalIcons.MoreVertical.imageVector,
        contentDescription = stringResource(R.string.PasskeysScreen__open_passkey_options),
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

    DropdownMenus.Menu(controller = menuController) { controller ->
      DropdownMenus.Item(
        leadingIconResId = CoreUiR.drawable.symbol_edit_24,
        text = { Text(text = stringResource(R.string.PasskeysScreen__rename)) },
        onClick = {
          onEvent(PasskeysEvent.RenamePasskeyClicked(passkey.id))
          controller.hide()
        },
        modifier = Modifier.testTag(PasskeysTestTags.MENU_ITEM_RENAME)
      )

      DropdownMenus.Item(
        leadingIconResId = CoreUiR.drawable.symbol_x_circle_24,
        text = { Text(text = stringResource(R.string.PasskeysScreen__remove)) },
        onClick = {
          onEvent(PasskeysEvent.RemovePasskeyClicked(passkey.id))
          controller.hide()
        },
        modifier = Modifier.testTag(PasskeysTestTags.MENU_ITEM_REMOVE)
      )
    }
  }
}

@Composable
private fun DescriptionWithLearnMore(
  text: String,
  onEvent: (PasskeysEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Text(
    text = buildAnnotatedString {
      append(text)
      append(' ')

      withLink(
        LinkAnnotation.Clickable(
          tag = "learn-more",
          styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
          linkInteractionListener = { onEvent(PasskeysEvent.LearnMoreClicked) }
        )
      ) {
        append(stringResource(R.string.PasskeysScreen__learn_more))
      }
    },
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
    modifier = modifier
  )
}

@Composable
private fun SetUpPasskeyButton(
  text: String,
  onEvent: (PasskeysEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Buttons.MediumTonal(
    onClick = { onEvent(PasskeysEvent.SetUpPasskeyClicked) },
    colors = ButtonDefaults.filledTonalButtonColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ),
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 40.dp)
      .testTag(PasskeysTestTags.BUTTON_SET_UP)
  ) {
    Text(text = text)
  }
}

@Composable
private fun BulletRow(
  icon: SignalIcons,
  text: String
) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Icon(
      painter = icon.painter,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(24.dp)
    )

    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@DayNightPreviews
@Composable
private fun PasskeysScreenPreview() {
  Previews.Preview {
    PasskeysScreen(
      state = PasskeysState(),
      onEvent = {}
    )
  }
}

@DayNightPreviews
@Composable
private fun PasskeysScreenWithPasskeysPreview() {
  Previews.Preview {
    PasskeysScreen(
      state = PasskeysState(
        passkeys = listOf(
          Passkey(id = 1, name = "My Security Key", createdAt = System.currentTimeMillis()),
          Passkey(id = 2, name = "My Pixel Phone", createdAt = System.currentTimeMillis())
        )
      ),
      onEvent = {}
    )
  }
}
