/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.thoughtcrime.securesms.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.reactivex.rxjava3.core.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.signal.core.ui.compose.split.ListDetailEvents
import org.signal.core.ui.compose.split.ListDetailNavigator
import org.signal.core.ui.compose.split.PaneAnchor
import org.signal.core.ui.compose.split.exitDetail
import org.signal.core.util.logging.Log
import org.thoughtcrime.securesms.calls.log.CallLogRow
import org.thoughtcrime.securesms.components.settings.app.notifications.profiles.NotificationProfilesRepository
import org.thoughtcrime.securesms.components.snackbars.SnackbarStateConsumerRegistry
import org.thoughtcrime.securesms.dependencies.AppDependencies
import org.thoughtcrime.securesms.keyvalue.SignalStore
import org.thoughtcrime.securesms.megaphone.Megaphone
import org.thoughtcrime.securesms.megaphone.Megaphones
import org.thoughtcrime.securesms.notifications.profiles.NotificationProfile
import org.thoughtcrime.securesms.recipients.Recipient
import org.thoughtcrime.securesms.recipients.RecipientId
import org.thoughtcrime.securesms.stories.Stories
import java.util.Optional
import kotlin.time.Duration.Companion.milliseconds

class MainNavigationViewModel(
  savedStateHandle: SavedStateHandle,
  initialListLocation: MainListRoute = MainListRoute.Chats
) : ViewModel(), MainNavigationRouter {

  companion object {
    private val TAG = Log.tag(MainNavigationViewModel::class)
    private const val NAV_PREFETCH_TIMEOUT_MS = 250L
    private const val CHATS_BACK_STACK_KEY = "chats_back_stack_v2"
    private const val CALLS_BACK_STACK_KEY = "calls_back_stack_v2"
    private const val STORIES_BACK_STACK_KEY = "stories_back_stack_v2"
  }

  class Factory(
    private val initialListLocation: MainListRoute = MainListRoute.Chats
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
      val savedStateHandle = extras.createSavedStateHandle()
      @Suppress("UNCHECKED_CAST")
      return MainNavigationViewModel(savedStateHandle, initialListLocation) as T
    }
  }

  private val megaphoneRepository = AppDependencies.megaphoneRepository

  private var captureChatListSnapshot: (suspend () -> Unit)? = null

  /**
   * The stacks behind the main window, one per tab. The archive gets no stack of its own: it is a list
   * pushed onto the chats stack, so that opening it keeps whatever chat was open beside it.
   */
  val navigator = ListDetailNavigator<MainListRoute, MainDetailRoute>(
    savedStateHandle = savedStateHandle,
    scope = viewModelScope,
    stackKeys = mapOf(
      MainListRoute.Chats to CHATS_BACK_STACK_KEY,
      MainListRoute.Calls to CALLS_BACK_STACK_KEY,
      MainListRoute.Stories to STORIES_BACK_STACK_KEY
    ),
    initialRoot = initialListLocation.tab
  )

  /** The currently selected tab. */
  val currentTab: StateFlow<MainListRoute> = navigator.currentRoot

  /** How a split-pane window currently divides the list and detail panes. */
  val paneAnchor: StateFlow<PaneAnchor> = navigator.paneAnchor

  /** Whether one pane currently occupies the whole window. Only meaningful in split-pane layouts. */
  val isFullScreenPane: StateFlow<Boolean> = navigator.isFullScreenPane

  val observableActiveRecipientId: Observable<Optional<out RecipientId>> =
    navigator.snapshotsOf(MainListRoute.Chats) { activeRecipientId }
      .combine(isFullScreenPane) { id, expanded -> if (expanded) Optional.ofNullable(null) else Optional.ofNullable(id) }
      .asObservable()

  val observableActiveCallId: Observable<Optional<out CallLogRow.Id>> =
    navigator.snapshotsOf(MainListRoute.Calls) { activeCallId }
      .combine(isFullScreenPane) { id, expanded -> if (expanded) Optional.ofNullable(null) else Optional.ofNullable(id) }
      .asObservable()

  private val internalMegaphone = MutableStateFlow(Megaphone.NONE)
  val megaphone: StateFlow<Megaphone> = internalMegaphone

  private val internalNavigationEvents = MutableSharedFlow<NavigationEvent>()
  val navigationEvents: Flow<NavigationEvent> = internalNavigationEvents

  private val notificationProfilesRepository: NotificationProfilesRepository = NotificationProfilesRepository()

  private val internalMainNavigationBarState = MutableStateFlow(MainNavigationBarState(currentListLocation = initialListLocation))
  val mainNavigationBarState: StateFlow<MainNavigationBarState> = combine(internalMainNavigationBarState, navigator.displayedList) { state, listLocation ->
    state.copy(currentListLocation = listLocation)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, MainNavigationBarState(currentListLocation = initialListLocation))

  /**
   * The detail content displayed above the current list, or null when the list is showing on its own.
   */
  val detailLocation: StateFlow<MainDetailRoute?> = navigator.detail

  /**
   * Whether the current tab is displaying detail content.
   */
  val hasDetailContent: StateFlow<Boolean> = navigator.hasDetail

  /**
   * This is Rx because these are still accessed from Java.
   */
  private val internalTabClickEvents: MutableSharedFlow<MainListRoute> = MutableSharedFlow()
  val tabClickEventsObservable: Observable<MainListRoute> = internalTabClickEvents.asObservable()

  val snackbarRegistry = SnackbarStateConsumerRegistry()

  init {
    performStoreUpdate(MainNavigationRepository.getNumberOfUnreadMessages()) { unreadChats, state ->
      state.copy(chatsCount = unreadChats.toInt())
    }

    performStoreUpdate(MainNavigationRepository.getNumberOfUnseenCalls()) { unseenCalls, state ->
      state.copy(callsCount = unseenCalls.toInt())
    }

    performStoreUpdate(MainNavigationRepository.getNumberOfUnseenStories()) { unseenStories, state ->
      state.copy(storiesCount = unseenStories.toInt())
    }

    performStoreUpdate(MainNavigationRepository.getHasFailedOutgoingStories()) { hasFailedStories, state ->
      state.copy(storyFailure = hasFailedStories)
    }
  }

  /**
   * The user dragged the pane divider to [anchor].
   */
  fun onPaneAnchorSelected(anchor: PaneAnchor) {
    navigator.processEvent(ListDetailEvents.AnchorSelected(anchor))
  }

  /** Set from the MainActivity composition, and cleared when it is disposed. */
  fun setChatListSnapshotCaptureProvider(capture: (suspend () -> Unit)?) {
    captureChatListSnapshot = capture
  }

  override fun goTo(location: MainDetailRoute) = setDetailLocation(location)

  private suspend fun MainDetailRoute.Conversation.withPreloadedWallpaper(): MainDetailRoute.Conversation {
    val args = conversationArgs
    val liveRecipient = Recipient.live(args.recipientId)
    val recipientSnapshot = liveRecipient.get()
    val wallpaper = recipientSnapshot.wallpaper

    val updatedArgs = if (recipientSnapshot.isResolving || (wallpaper?.isPhoto == true && !wallpaper.isPrefetched)) {
      withTimeoutOrNull(NAV_PREFETCH_TIMEOUT_MS.milliseconds) {
        withContext(Dispatchers.Default) {
          val freshWallpaper = liveRecipient.resolve().wallpaper
          if (freshWallpaper?.prefetch(AppDependencies.application, NAV_PREFETCH_TIMEOUT_MS) == false) {
            Log.w(TAG, "[goToConversation] Failed to prefetch wallpaper.")
          }
          args.copy(hasWallpaper = freshWallpaper != null)
        }
      } ?: run {
        Log.w(TAG, "[goToConversation] Timed out resolving recipient/wallpaper. Navigating without prefetch.")
        args
      }
    } else {
      args.copy(hasWallpaper = wallpaper != null)
    }

    return copy(conversationArgs = updatedArgs)
  }

  private fun setDetailLocation(location: MainDetailRoute) {
    when (location) {
      is MainDetailRoute.Chats -> pushChatsDetailLocation(location)
      is MainDetailRoute.Conversation -> goToConversation(location)
      is MainDetailRoute.Calls, is MainDetailRoute.CallLinkDetails -> pushCallsDetailLocation(location)
      is MainDetailRoute.Stories -> pushStoriesDetailLocation(location)
    }
  }

  /**
   * Drops the detail content above the current list, leaving that list displayed on its own.
   */
  override fun exitDetailLocation() {
    navigator.processEvent(ListDetailEvents.ExitDetail)
  }

  private fun goToConversation(location: MainDetailRoute.Conversation) {
    val captureSnapshot = captureChatListSnapshot

    if (captureSnapshot == null) {
      // share intent or process restore - push synchronously, since there's no chat-list snapshot to capture and no need to preload a wallpaper
      pushChatsDetailLocation(location)
    } else {
      viewModelScope.launch {
        captureSnapshot()
        pushChatsDetailLocation(location.withPreloadedWallpaper())
      }
    }
  }

  private fun pushChatsDetailLocation(location: MainDetailRoute) {
    val chatsBackStack = navigator[MainListRoute.Chats]
    if (location is MainDetailRoute.Chats && chatsBackStack.activeRecipientId != location.controllerKey) {
      chatsBackStack.exitDetail()
    }

    navigator.processEvent(ListDetailEvents.Push(location, MainListRoute.Chats))
  }

  private fun pushCallsDetailLocation(location: MainDetailRoute) {
    val callsBackStack = navigator[MainListRoute.Calls]
    if (location is MainDetailRoute.Calls && callsBackStack.activeCallId != location.controllerKey) {
      callsBackStack.exitDetail()
    }

    navigator.processEvent(ListDetailEvents.Push(location, MainListRoute.Calls))
  }

  private fun pushStoriesDetailLocation(location: MainDetailRoute) {
    navigator.processEvent(ListDetailEvents.Push(location, MainListRoute.Stories))
  }

  /** Pops the stack belonging to whichever tab the user is currently on. */
  fun popCurrentDetailLocation() {
    navigator.processEvent(ListDetailEvents.Back)
  }

  /** Switching tabs only changes which stack is displayed; each tab comes back to whatever it had open. */
  override fun goTo(location: MainListRoute) {
    navigator.processEvent(
      ListDetailEvents.GoToList(
        listRoute = location,
        root = location.tab,
        push = location == MainListRoute.Archive
      )
    )
  }

  fun goToCameraFirstStoryCapture() {
    viewModelScope.launch {
      internalNavigationEvents.emit(NavigationEvent.STORY_CAMERA_FIRST)
    }
  }

  fun getNextMegaphone() {
    megaphoneRepository.getNextMegaphone { next ->
      internalMegaphone.update { next ?: Megaphone.NONE }
    }
  }

  fun onMegaphoneSnoozed(event: Megaphones.Event) {
    megaphoneRepository.markInteractedWith(event)
    internalMegaphone.update { Megaphone.NONE }
  }

  fun onMegaphoneCompleted(event: Megaphones.Event) {
    internalMegaphone.update { Megaphone.NONE }
    megaphoneRepository.markFinished(event)
  }

  fun onMegaphoneVisible(visible: Megaphone) {
    megaphoneRepository.markVisible(visible.event)
  }

  fun refreshNavigationBarState() {
    internalMainNavigationBarState.update {
      it.copy(
        compact = SignalStore.settings.useCompactNavigationBar,
        destinations = MainNavigationBarState.ALL_DESTINATIONS.filter { destination ->
          destination != MainListRoute.Stories || Stories.isFeatureEnabled()
        }
      )
    }
  }

  fun getNotificationProfiles(): Flow<List<NotificationProfile>> {
    return notificationProfilesRepository.getProfiles().asFlow()
  }

  fun onChatsSelected() {
    onTabSelected(MainListRoute.Chats)
  }

  fun onArchiveSelected() {
    onTabSelected(MainListRoute.Archive)
  }

  fun onCallsSelected() {
    onTabSelected(MainListRoute.Calls)
  }

  fun onStoriesSelected() {
    onTabSelected(MainListRoute.Stories)
  }

  private fun onTabSelected(destination: MainListRoute) {
    viewModelScope.launch {
      val displayed = navigator.displayedList.value
      if (displayed == destination) {
        navigator.processEvent(ListDetailEvents.RevealList)
        internalTabClickEvents.emit(destination)
      } else {
        goTo(destination)
      }
    }
  }

  private fun <T : Any> performStoreUpdate(flow: Flow<T>, fn: (T, MainNavigationBarState) -> MainNavigationBarState) {
    viewModelScope.launch {
      flow.collectLatest { item ->
        internalMainNavigationBarState.update { state -> fn(item, state) }
      }
    }
  }

  enum class NavigationEvent {
    STORY_CAMERA_FIRST
  }
}
