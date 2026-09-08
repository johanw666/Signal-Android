/*
 * Copyright 2026 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.conversation.v2

import android.content.Context
import android.widget.EditText
import org.thoughtcrime.securesms.components.compose.mediakeyboard.MediaKeyboardController
import org.thoughtcrime.securesms.components.compose.mediakeyboard.MediaKeyboardKey
import org.thoughtcrime.securesms.util.ViewUtil

/**
 * Adapts [MediaKeyboardController] to the conversation's view code, which asks for keyboards from
 * click listeners rather than from composition.
 *
 * @param context Used to hide the system keyboard.
 * @param controller The controller to drive.
 */
class ChatInputController(
  private val context: Context,
  private val controller: MediaKeyboardController
) {

  private var wasKeyboardVisibleBeforeToggle: Boolean = false

  private val listeners: MutableSet<Listener> = mutableSetOf()
  private val keyboardStateListeners: MutableSet<KeyboardStateListener> = mutableSetOf()

  val isInputShowing: Boolean
    get() = controller.isShowing

  val isKeyboardShowing: Boolean
    get() = controller.isSystemKeyboardVisible

  fun addInputListener(listener: Listener) {
    listeners.add(listener)
  }

  fun removeInputListener(listener: Listener) {
    listeners.remove(listener)
  }

  fun addKeyboardStateListener(listener: KeyboardStateListener) {
    keyboardStateListeners.add(listener)
  }

  fun removeKeyboardStateListener(listener: KeyboardStateListener) {
    keyboardStateListeners.remove(listener)
  }

  /** Drops everything still listening, for a host whose view is going away. */
  fun clearListeners() {
    listeners.clear()
    keyboardStateListeners.clear()
  }

  fun onKeyboardVisibilityChanged(visible: Boolean) {
    keyboardStateListeners.toList().forEach {
      if (visible) it.onKeyboardShown() else it.onKeyboardHidden()
    }
  }

  fun onKeyboardAnimationEnded() {
    keyboardStateListeners.toList().forEach { it.onKeyboardAnimationEnded() }
  }

  fun onInputShown(key: MediaKeyboardKey) {
    listeners.toList().forEach { it.onInputShown(key) }
  }

  fun onInputHidden() {
    listeners.toList().forEach { it.onInputHidden() }
  }

  fun showSoftkey(editText: EditText) {
    controller.hideForSystemKeyboard()
    ViewUtil.focusAndShowKeyboard(editText)
  }

  fun hideAll(imeTarget: EditText) {
    wasKeyboardVisibleBeforeToggle = false
    controller.hide()
    ViewUtil.hideKeyboard(context, imeTarget)
  }

  fun hideInput() {
    wasKeyboardVisibleBeforeToggle = false
    controller.hide()
  }

  fun hideKeyboard(imeTarget: EditText) {
    if (isKeyboardShowing) {
      ViewUtil.hideKeyboard(context, imeTarget)
    }
  }

  fun runAfterAllHidden(imeTarget: EditText, onHidden: () -> Unit) {
    if (isInputShowing || isKeyboardShowing) {
      val listener = object : Listener, KeyboardStateListener {
        override fun onInputHidden() {
          onHidden()
          removeInputListener(this)
          removeKeyboardStateListener(this)
        }

        override fun onKeyboardHidden() {
          onHidden()
          removeInputListener(this)
          removeKeyboardStateListener(this)
        }

        override fun onInputShown(key: MediaKeyboardKey) = Unit
        override fun onKeyboardShown() = Unit
      }

      addInputListener(listener)
      addKeyboardStateListener(listener)
      hideAll(imeTarget)
    } else {
      onHidden()
    }
  }

  /**
   * Like [runAfterAllHidden], but suspends until the keyboards have finished animating out rather
   * than returning as soon as the hide has been asked for. For callers that measure themselves
   * against the content area, which stays shrunk for the length of that animation.
   */
  suspend fun hideAllAndAwaitSettled(imeTarget: EditText) {
    if (controller.isSettled) {
      return
    }

    hideAll(imeTarget)
    controller.awaitSettled()
  }

  /**
   * @param key The keyboard to bring up, or take away if already showing.
   * @param imeTarget The field the system keyboard belongs to.
   * @param showSoftKeyOnHide Whether the system keyboard replaces [key] when it is taken away.
   */
  fun toggleInput(key: MediaKeyboardKey, imeTarget: EditText, showSoftKeyOnHide: Boolean = wasKeyboardVisibleBeforeToggle) {
    if (controller.current == key) {
      if (showSoftKeyOnHide) {
        showSoftkey(imeTarget)
      } else {
        hideInput()
      }
    } else {
      wasKeyboardVisibleBeforeToggle = isKeyboardShowing
      controller.show(key)
      ViewUtil.hideKeyboard(context, imeTarget)
    }
  }

  interface Listener {
    fun onInputShown(key: MediaKeyboardKey)
    fun onInputHidden()
  }

  interface KeyboardStateListener {
    fun onKeyboardShown()
    fun onKeyboardHidden()
    fun onKeyboardAnimationEnded() = Unit
  }
}
