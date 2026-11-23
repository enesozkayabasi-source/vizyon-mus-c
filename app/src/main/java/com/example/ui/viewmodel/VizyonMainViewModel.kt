package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.StudioSoundEngine
import com.example.data.firebase.VizyonFirestoreRepository
import com.example.data.local.AppearancePreferences
import com.example.data.local.LocalActivityCommentEntity
import com.example.data.local.LocalActivityEntity
import com.example.data.local.LocalBlockedUserEntity
import com.example.data.local.LocalDirectMessageEntity
import com.example.data.local.LocalFanPostEntity
import com.example.data.local.LocalFavoriteEntity
import com.example.data.local.LocalFollowedArtistEntity
import com.example.data.local.LocalFollowingEntity
import com.example.data.local.LocalLibraryDao
import com.example.data.local.LocalPlaylistEntity
import com.example.data.local.LocalPlaylistTrackEntity
import com.example.data.local.LocalStoryEntity
import com.example.data.remote.HybridMusicRepository
import com.example.data.remote.VideoResolutionResult
import com.example.model.AccentPalette
import com.example.model.ActivityActionType
import com.example.model.ActivityComment
import com.example.model.ActivityFeedItem
import com.example.model.ApiBridgeStatus
import com.example.model.AppAppearanceConfig
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ArtistFanPost
import com.example.model.ArtistSummary
import com.example.model.BlockedUser
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.DISCOVER_AUTO_REFRESH_INTERVAL_MS
import com.example.model.DirectChatMessage
import com.example.model.DirectChatThread
import com.example.model.DirectMessageType
import com.example.model.FanPostTag
import com.example.model.FollowConnection
import com.example.model.FollowRequest
import com.example.model.FollowedArtist
import com.example.model.MessageRequest
import com.example.model.MusicStoryItem
import com.example.model.MusicTrack
import com.example.model.NotificationType
import com.example.model.Playlist
import com.example.model.PlaylistCoverStyle
import com.example.model.PublicProfile
import com.example.model.RecentTrackSummary
import com.example.model.ReportReason
import com.example.model.SavedTrack
import com.example.model.SearchAutocompleteSuggestion
import com.example.model.StoryCardTheme
import com.example.model.TrackLyrics
import com.example.model.TrackSource
import com.example.model.UiState
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.example.model.buildDirectChatId
import com.example.model.sanitizeFirestoreId
import com.example.notifications.BackgroundNotificationJobService
import com.example.notifications.NotificationDispatchGuard
import com.example.notifications.VizyonNotificationHelper
import com.example.notifications.VizyonRealtimeNotificationService
import com.example.player.PlaybackUiState
import com.example.player.YouTubeForegroundPlayerManager
import com.example.ui.auth.isRecentLoginFresh
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface NotificationDeepLinkTarget {
  data object OpenFeed : NotificationDeepLinkTarget
  data object OpenRequests : NotificationDeepLinkTarget
  data class OpenPostComments(val activityId: String) : NotificationDeepLinkTarget
  data class OpenListenerProfile(val userId: String) : NotificationDeepLinkTarget
  data class OpenStory(val storyId: String, val trackId: String = "") : NotificationDeepLinkTarget
  data class OpenChat(val userId: String) : NotificationDeepLinkTarget
}

data class HomeCatalogData(
  val tracks: List<MusicTrack> = emptyList(),
  val artists: List<ArtistSummary> = emptyList(),
  val bridgeStatus: ApiBridgeStatus? = null
)

data class SearchResultData(
  val query: String = "",
  val tracks: List<MusicTrack> = emptyList(),
  val artists: List<ArtistSummary> = emptyList()
)

data class ArtistDetailData(
  val artist: ArtistSummary,
  val topTracks: List<MusicTrack>
)

@OptIn(ExperimentalCoroutinesApi::class)
class VizyonMainViewModel(
  private val currentUserId: String?,
  private val firestoreRepo: VizyonFirestoreRepository,
  private val localLibraryDao: LocalLibraryDao,
  private val hybridMusicRepo: HybridMusicRepository,
  private val playerManager: YouTubeForegroundPlayerManager,
  private val appearancePrefs: AppearancePreferences,
  private val appContext: Context? = null
) : ViewModel() {

  private fun <T> Flow<T>.asSafeUiState(label: String): StateFlow<UiState<T>> =
    map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        emit(UiState.Error(rootError.message ?: "$label yüklenemedi"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = UiState.Loading
      )

  val appearanceState: StateFlow<AppAppearanceConfig> =
    appearancePrefs.configFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = AppAppearanceConfig()
    )

  val playbackState: StateFlow<PlaybackUiState> = playerManager.state

  private val _catalogState = MutableStateFlow<UiState<HomeCatalogData>>(UiState.Loading)
  val catalogState: StateFlow<UiState<HomeCatalogData>> = _catalogState.asStateFlow()

  private val _searchState = MutableStateFlow<UiState<SearchResultData>>(
    UiState.Success(SearchResultData())
  )
  val searchState: StateFlow<UiState<SearchResultData>> = _searchState.asStateFlow()

  private val _searchSuggestionsState =
    MutableStateFlow<List<SearchAutocompleteSuggestion>>(emptyList())
  val searchSuggestionsState: StateFlow<List<SearchAutocompleteSuggestion>> =
    _searchSuggestionsState.asStateFlow()

  private val _selectedArtistId = MutableStateFlow<String?>(null)
  private val _artistDetailState = MutableStateFlow<UiState<ArtistDetailData>?>(null)
  val artistDetailState: StateFlow<UiState<ArtistDetailData>?> = _artistDetailState.asStateFlow()

  private val _selectedPlaylistId = MutableStateFlow<String?>(null)
  val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

  private val _snackbarMessage = MutableStateFlow<String?>(null)
  val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

  private val _lyricsState = MutableStateFlow<UiState<TrackLyrics?>>(UiState.Success(null))
  val lyricsState: StateFlow<UiState<TrackLyrics?>> = _lyricsState.asStateFlow()

  private val _openNowPlayingNonce = MutableStateFlow(0)
  val openNowPlayingNonce: StateFlow<Int> = _openNowPlayingNonce.asStateFlow()

  private val _isDeletingAccount = MutableStateFlow(false)
  val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

  private val _isAutoRefreshingCatalog = MutableStateFlow(false)
  val isAutoRefreshingCatalog: StateFlow<Boolean> = _isAutoRefreshingCatalog.asStateFlow()

  private val _pendingDeepLink = MutableStateFlow<NotificationDeepLinkTarget?>(null)
  val pendingDeepLink: StateFlow<NotificationDeepLinkTarget?> = _pendingDeepLink.asStateFlow()

  private val _selectedCommentActivityId = MutableStateFlow<String?>(null)
  private val _likedActivityIdsInSession = MutableStateFlow<Set<String>>(emptySet())
  val likedActivityIdsInSession: StateFlow<Set<String>> = _likedActivityIdsInSession.asStateFlow()

  private val _activeChatPeer = MutableStateFlow<PublicProfile?>(null)
  val activeChatPeer: StateFlow<PublicProfile?> = _activeChatPeer.asStateFlow()

  private val _inspectedFollowUserId = MutableStateFlow<String?>(null)
  val inspectedFollowUserId: StateFlow<String?> = _inspectedFollowUserId.asStateFlow()

  private val _isVaultLocked = MutableStateFlow(false)
  val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()

  private val _vaultUnlockError = MutableStateFlow<String?>(null)
  val vaultUnlockError: StateFlow<String?> = _vaultUnlockError.asStateFlow()

  private val _isAppInForeground = MutableStateFlow(true)
  val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

  private val _activeInAppBubble = MutableStateFlow<UserNotification?>(null)
  val activeInAppBubble: StateFlow<UserNotification?> = _activeInAppBubble.asStateFlow()

  private var lastLyricsTrackId: String? = null
  private var resolveVideoJob: Job? = null
  private var autocompleteJob: Job? = null
  private var catalogSeedCounter: Long = 0L
  private var inMemoryBaselineReady: Boolean = false
  private val inMemorySeenNotificationIds = mutableSetOf<String>()
  private var openChatBaselinePeerId: String? = null
  private val seenOpenChatMessageIds = mutableSetOf<String>()

  val selfRecentTracksState: StateFlow<List<RecentTrackSummary>> =
    appearancePrefs.recentTracksFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeUserProfile(currentUserId).asSafeUiState("userProfile")
    } else {
      appearancePrefs.localProfileFlow.asSafeUiState("localProfile")
    }

  val favoritesState: StateFlow<UiState<List<SavedTrack>>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeFavorites(currentUserId).asSafeUiState("favorites")
    } else {
      localLibraryDao.observeFavorites()
        .map { list -> list.map { it.toSavedTrack() } }
        .asSafeUiState("localFavorites")
    }

  val playlistsState: StateFlow<UiState<List<Playlist>>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observePlaylists(currentUserId).asSafeUiState("playlists")
    } else {
      localLibraryDao.observePlaylists()
        .map { list -> list.map { it.toPlaylist() } }
        .asSafeUiState("localPlaylists")
    }

  val selectedPlaylistTracksState: StateFlow<UiState<List<SavedTrack>>> =
    _selectedPlaylistId
      .flatMapLatest { playlistId ->
        if (playlistId.isNullOrBlank()) {
          flowOf(emptyList())
        } else if (!currentUserId.isNullOrBlank()) {
          firestoreRepo.observePlaylistTracks(currentUserId, playlistId)
        } else {
          localLibraryDao.observePlaylistTracks(playlistId).map { list -> list.map { it.toSavedTrack() } }
        }
      }
      .asSafeUiState("playlistTracks")

  val blockedUsersState: StateFlow<UiState<List<BlockedUser>>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeBlockedUsers(currentUserId).asSafeUiState("blockedUsers")
    } else {
      localLibraryDao.observeBlockedUsers()
        .map { list -> list.map { it.toBlockedUser() } }
        .asSafeUiState("localBlockedUsers")
    }

  private val blockedIdSetFlow: Flow<Set<String>> =
    blockedUsersState.map { st ->
      (st as? UiState.Success)?.data
        ?.mapNotNull { blocked ->
          val raw = blocked.blockedUserId.ifBlank { blocked.id }
          raw.takeIf { it.isNotBlank() }?.let(::sanitizeFirestoreId)
        }
        ?.toSet()
        .orEmpty()
    }

  val publicListenersState: StateFlow<UiState<List<PublicProfile>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observePublicListeners(currentUserId),
        blockedIdSetFlow
      ) { list, blockedIds ->
        list.filterNot { profile ->
          val raw = profile.userId.ifBlank { profile.id }
          raw.isNotBlank() && sanitizeFirestoreId(raw) in blockedIds
        }
      }.asSafeUiState("publicListeners")
    } else {
      flowOf(emptyList<PublicProfile>()).asSafeUiState("localPublicListeners")
    }

  val followingState: StateFlow<UiState<List<FollowConnection>>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeFollowing(currentUserId).asSafeUiState("following")
    } else {
      localLibraryDao.observeFollowing()
        .map { list -> list.map { it.toFollowConnection() } }
        .asSafeUiState("localFollowing")
    }

  val followersState: StateFlow<UiState<List<FollowConnection>>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeFollowers(currentUserId).asSafeUiState("followers")
    } else {
      flowOf(emptyList<FollowConnection>()).asSafeUiState("localFollowers")
    }

  val incomingFollowRequestsState: StateFlow<UiState<List<FollowRequest>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observeIncomingFollowRequests(currentUserId),
        blockedIdSetFlow
      ) { list, blockedIds ->
        list.filter { req ->
          val rid = req.requesterId.ifBlank { req.id }
          rid.isNotBlank() && sanitizeFirestoreId(rid) !in blockedIds
        }
      }.asSafeUiState("incomingFollowRequests")
    } else {
      flowOf(emptyList<FollowRequest>()).asSafeUiState("localFollowRequests")
    }

  val incomingMessageRequestsState: StateFlow<UiState<List<MessageRequest>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observeIncomingMessageRequests(currentUserId),
        blockedIdSetFlow
      ) { list, blockedIds ->
        list.filter { req ->
          val rid = req.requesterId.ifBlank { req.id }
          rid.isNotBlank() && sanitizeFirestoreId(rid) !in blockedIds
        }
      }.asSafeUiState("incomingMessageRequests")
    } else {
      flowOf(emptyList<MessageRequest>()).asSafeUiState("localMessageRequests")
    }

  val outgoingRequestKeysState: StateFlow<Set<String>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeOutgoingRequestKeys(currentUserId)
        .catch { emit(emptySet()) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = emptySet()
        )
    } else {
      MutableStateFlow(emptySet())
    }

  val pendingRequestsCount: StateFlow<Int> =
    combine(incomingFollowRequestsState, incomingMessageRequestsState) { followSt, msgSt ->
      val fCount = (followSt as? UiState.Success)?.data?.size ?: 0
      val mCount = (msgSt as? UiState.Success)?.data?.size ?: 0
      fCount + mCount
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = 0
    )

  val inspectedFollowingState: StateFlow<UiState<List<FollowConnection>>> =
    _inspectedFollowUserId
      .flatMapLatest { targetId ->
        if (targetId.isNullOrBlank()) {
          flowOf(emptyList())
        } else if (!currentUserId.isNullOrBlank()) {
          firestoreRepo.observeFollowing(targetId)
        } else {
          localLibraryDao.observeFollowing().map { list -> list.map { it.toFollowConnection() } }
        }
      }
      .asSafeUiState("inspectedFollowing")

  val inspectedFollowersState: StateFlow<UiState<List<FollowConnection>>> =
    _inspectedFollowUserId
      .flatMapLatest { targetId ->
        if (targetId.isNullOrBlank()) {
          flowOf(emptyList())
        } else if (!currentUserId.isNullOrBlank()) {
          firestoreRepo.observeFollowers(targetId)
        } else {
          flowOf(emptyList())
        }
      }
      .asSafeUiState("inspectedFollowers")

  val directChatsState: StateFlow<UiState<List<DirectChatThread>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observeDirectChats(currentUserId),
        blockedIdSetFlow
      ) { threads, blockedIds ->
        threads.filterNot { thread ->
          val peer = thread.peerIdFor(currentUserId)
          peer.isNotBlank() && sanitizeFirestoreId(peer) in blockedIds
        }
      }.asSafeUiState("directChats")
    } else {
      combine(
        localLibraryDao.observeAllDirectMessages(),
        blockedIdSetFlow
      ) { msgs, blockedIds ->
        msgs.filterNot { it.peerId.isNotBlank() && sanitizeFirestoreId(it.peerId) in blockedIds }
          .groupBy { it.chatId }.mapNotNull { (chatId, list) ->
            val latest = list.maxByOrNull { it.createdAtMs } ?: return@mapNotNull null
            DirectChatThread(
              id = chatId,
              chatId = chatId,
              participantIds = listOf("local", latest.peerId),
              participantNames = mapOf("local" to "Sen", latest.peerId to latest.peerName),
              participantHandles = mapOf("local" to "@vizyon", latest.peerId to latest.peerHandle),
              participantAvatars = mapOf("local" to "", latest.peerId to latest.peerAvatarUrl),
              lastMessageText = latest.text,
              lastSenderId = latest.senderId,
              lastTrackTitle = latest.trackTitle,
              lastMessageAtMillis = latest.createdAtMs
            )
          }.sortedByDescending { it.lastMessageAtMillis }
      }.asSafeUiState("localDirectChats")
    }

  val activeChatMessagesState: StateFlow<UiState<List<DirectChatMessage>>> =
    _activeChatPeer
      .flatMapLatest { peer ->
        val rawPeerId = peer?.userId?.ifBlank { peer.id }.orEmpty()
        if (peer == null || rawPeerId.isBlank()) {
          flowOf(emptyList())
        } else {
          val peerId = sanitizeFirestoreId(rawPeerId)
          if (!currentUserId.isNullOrBlank()) {
            val chatId = buildDirectChatId(currentUserId, peerId)
            firestoreRepo.observeChatMessages(chatId, currentUserId)
          } else {
            val chatId = buildDirectChatId("local", peerId)
            localLibraryDao.observeDirectMessagesForChat(chatId)
              .map { list -> list.map { it.toDirectChatMessage() } }
          }
        }
      }
      .asSafeUiState("activeChatMessages")

  val followedArtistsState: StateFlow<UiState<List<FollowedArtist>>> =
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.observeFollowedArtists(currentUserId).asSafeUiState("followedArtists")
    } else {
      localLibraryDao.observeFollowedArtists()
        .map { list -> list.map { it.toFollowedArtist() } }
        .asSafeUiState("localFollowedArtists")
    }

  // Strictly user-published shares & reposts; automatic LISTENING pings are NEVER shown in Akış.
  val activityFeedState: StateFlow<UiState<List<ActivityFeedItem>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observePublicActivityFeed(currentUserId),
        blockedIdSetFlow
      ) { list, blockedIds ->
        list.filterNot { item ->
          item.actionType == ActivityActionType.LISTENING.name ||
            (item.userId.isNotBlank() && sanitizeFirestoreId(item.userId) in blockedIds)
        }
      }.asSafeUiState("activityFeed")
    } else {
      localLibraryDao.observeActivityFeed()
        .map { list ->
          list.filterNot { it.actionType == ActivityActionType.LISTENING.name }
            .map { it.toActivityFeedItem() }
        }
        .asSafeUiState("localActivityFeed")
    }

  val storiesState: StateFlow<UiState<List<MusicStoryItem>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observePublicStories(currentUserId),
        blockedIdSetFlow
      ) { list, blockedIds ->
        val now = System.currentTimeMillis()
        list.filter { story ->
          (story.expiresAtMillis == 0L || story.expiresAtMillis > now) &&
            (story.userId.isBlank() || sanitizeFirestoreId(story.userId) !in blockedIds)
        }
      }.asSafeUiState("stories")
    } else {
      localLibraryDao.observeStories()
        .map { list ->
          val now = System.currentTimeMillis()
          list.map { it.toMusicStoryItem() }.filter { it.expiresAtMillis == 0L || it.expiresAtMillis > now }
        }
        .asSafeUiState("localStories")
    }

  val artistFanPostsState: StateFlow<UiState<List<ArtistFanPost>>> =
    _selectedArtistId
      .flatMapLatest { artistId ->
        if (artistId.isNullOrBlank()) {
          flowOf(emptyList())
        } else if (!currentUserId.isNullOrBlank()) {
          firestoreRepo.observeArtistFanPosts(currentUserId, artistId)
        } else {
          localLibraryDao.observeFanPosts(artistId).map { list -> list.map { it.toArtistFanPost() } }
        }
      }
      .asSafeUiState("artistFanPosts")

  val activityCommentsState: StateFlow<UiState<List<ActivityComment>>> =
    _selectedCommentActivityId
      .flatMapLatest { activityId ->
        if (activityId.isNullOrBlank()) {
          flowOf(emptyList())
        } else if (!currentUserId.isNullOrBlank()) {
          firestoreRepo.observeActivityComments(currentUserId, activityId)
        } else {
          localLibraryDao.observeActivityComments(activityId).map { list -> list.map { it.toActivityComment() } }
        }
      }
      .asSafeUiState("activityComments")

  val notificationsState: StateFlow<UiState<List<UserNotification>>> =
    if (!currentUserId.isNullOrBlank()) {
      combine(
        firestoreRepo.observeNotifications(currentUserId),
        blockedIdSetFlow
      ) { list, blockedIds ->
        list.filterNot { notif ->
          notif.senderId.isNotBlank() && sanitizeFirestoreId(notif.senderId) in blockedIds
        }
      }.asSafeUiState("notifications")
    } else {
      localLibraryDao.observeNotifications()
        .map { list -> list.map { it.toUserNotification() } }
        .asSafeUiState("localNotifications")
    }

  val unreadNotificationCount: StateFlow<Int> =
    notificationsState
      .map { st ->
        (st as? UiState.Success)?.data?.count { !it.read } ?: 0
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
      )

  init {
    viewModelScope.launch {
      val cfg = runCatching { appearancePrefs.configFlow.first() }.getOrNull()
      if (cfg != null && cfg.onboardingCompleted && cfg.lockOnLaunch && cfg.hasVaultPasscode) {
        _isVaultLocked.value = true
      }
      syncBackgroundNotifService(cfg)
    }
    if (!currentUserId.isNullOrBlank()) {
      bootstrapCloudProfile(currentUserId)
    }
    refreshCatalog()
    viewModelScope.launch {
      appearancePrefs.configFlow
        .map { it.favoriteGenres }
        .distinctUntilChanged()
        .collect { genres ->
          if (genres.isNotEmpty()) {
            refreshCatalog(genres)
          }
        }
    }
    viewModelScope.launch {
      while (isActive) {
        delay(60_000L)
        checkAndAutoRefreshDiscover()
      }
    }
    playerManager.setOnTrackStartedListener { track ->
      updateProfileListeningStatusOnly(track)
      if (lastLyricsTrackId != track.trackId) {
        loadLyricsForTrack(track)
      }
    }
    playerManager.setOnRequestTrackPlayListener { track, queue ->
      playTrack(track, queue)
    }
    startRealtimeNotificationDispatcher()
    startActiveChatReceiveChimeWatcher()
  }

  private fun startRealtimeNotificationDispatcher() {
    val flow =
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.observeNotifications(currentUserId)
      } else {
        localLibraryDao.observeNotifications().map { list -> list.map { it.toUserNotification() } }
      }
    viewModelScope.launch {
      flow
        .retryWhen { cause, attempt ->
          if (cause is CancellationException) {
            false
          } else {
            Log.w(TAG, "Notification observer interrupted (attempt=$attempt), reconnecting...", cause)
            val backoffMs = (2_000L * (attempt + 1L)).coerceAtMost(20_000L)
            delay(backoffMs)
            true
          }
        }
        .catch { error -> Log.w(TAG, "Notification observer terminated", error) }
        .collect { list ->
          processIncomingNotifications(list)
        }
    }
  }

  private fun startActiveChatReceiveChimeWatcher() {
    viewModelScope.launch {
      combine(_activeChatPeer, activeChatMessagesState, _isAppInForeground) { peer, msgState, fg ->
        Triple(peer, msgState, fg)
      }.collect { (peer, msgState, fg) ->
        if (peer == null) {
          openChatBaselinePeerId = null
          seenOpenChatMessageIds.clear()
          return@collect
        }
        if (msgState !is UiState.Success) return@collect
        val peerId = sanitizeFirestoreId(peer.userId.ifBlank { peer.id })
        val selfUid = currentUserId ?: "local"
        val messages = msgState.data
        if (openChatBaselinePeerId != peerId) {
          seenOpenChatMessageIds.clear()
          seenOpenChatMessageIds.addAll(messages.map { it.id })
          openChatBaselinePeerId = peerId
          return@collect
        }
        val newlyReceived =
          messages.filter { msg ->
            msg.id.isNotBlank() &&
              msg.id !in seenOpenChatMessageIds &&
              msg.senderId != selfUid &&
              msg.senderId.isNotBlank()
          }
        seenOpenChatMessageIds.addAll(messages.map { it.id })
        if (newlyReceived.isNotEmpty() && fg && appearanceState.value.notificationsEnabled) {
          StudioSoundEngine.playMessageReceiveChime(appContext)
        }
      }
    }
  }

  private fun syncBackgroundNotifService(cfgOverride: AppAppearanceConfig? = null) {
    val ctx = appContext ?: return
    val cfg = cfgOverride ?: appearanceState.value
    VizyonRealtimeNotificationService.syncServiceState(
      context = ctx,
      uid = currentUserId,
      notificationsEnabled = cfg.notificationsEnabled,
      backgroundNotifServiceEnabled = cfg.backgroundNotifServiceEnabled
    )
  }

  internal fun processIncomingNotifications(list: List<UserNotification>) {
    val ctx = appContext
    val uidKey = currentUserId ?: "guest"
    val blockedIds =
      (blockedUsersState.value as? UiState.Success)?.data
        ?.mapNotNull { b -> b.blockedUserId.ifBlank { b.id }.takeIf { it.isNotBlank() }?.let(::sanitizeFirestoreId) }
        ?.toSet()
        .orEmpty()
    val filteredList =
      list.filterNot { notif ->
        notif.senderId.isNotBlank() && sanitizeFirestoreId(notif.senderId) in blockedIds
      }
    if (ctx != null) {
      if (!NotificationDispatchGuard.isBaselineInitialized(ctx, uidKey)) {
        NotificationDispatchGuard.markBaselineSeen(ctx, uidKey, filteredList.map { it.id })
        return
      }
      val unread = filteredList.filter { !it.read }
      val unseenIds =
        NotificationDispatchGuard.filterAndMarkUnseen(ctx, uidKey, unread.map { it.id }).toSet()
      if (!appearanceState.value.notificationsEnabled) return
      unread
        .filter { it.id in unseenIds }
        .forEach { newNotif ->
          if (_isAppInForeground.value) {
            presentInAppBubbleIfAllowed(newNotif)
          } else {
            VizyonNotificationHelper.postSocialNotification(ctx, newNotif)
          }
        }
    } else {
      if (!inMemoryBaselineReady) {
        inMemorySeenNotificationIds.addAll(filteredList.map { it.id })
        inMemoryBaselineReady = true
        return
      }
      val newlyArrived = filteredList.filter { !it.read && it.id !in inMemorySeenNotificationIds }
      inMemorySeenNotificationIds.addAll(filteredList.map { it.id })
      if (!appearanceState.value.notificationsEnabled) return
      newlyArrived.forEach { newNotif ->
        if (_isAppInForeground.value) {
          presentInAppBubbleIfAllowed(newNotif)
        }
      }
    }
  }

  private fun bootstrapCloudProfile(uid: String) {
    viewModelScope.launch {
      val fbUser = Firebase.auth.currentUser
      val localProfile = runCatching { appearancePrefs.localProfileFlow.first() }.getOrNull()
      val config = runCatching { appearancePrefs.configFlow.first() }.getOrNull()
      val recentTracks = appearancePrefs.currentRecentTracks()

      val remoteProfile = firestoreRepo.fetchUserProfileOnce(uid).getOrNull()
      if (remoteProfile != null && remoteProfile.onboardingCompleted && !(config?.onboardingCompleted ?: false)) {
        // Reinstall / new-device cloud recovery!
        appearancePrefs.restoreFromCloudUserProfile(remoteProfile)
        if (remoteProfile.securityPasscodeHash.isNotBlank() && (remoteProfile.lockOnLaunch || !remoteProfile.rememberMe)) {
          _isVaultLocked.value = true
        }
        refreshCatalog(remoteProfile.favoriteGenres, AppLanguage.entries.firstOrNull { it.name == remoteProfile.preferredLanguage } ?: AppLanguage.TURKISH)
        notifyUser("Hoş geldin ${remoteProfile.displayName}! Bulut verilerin ve ayarların geri yüklendi.")
        return@launch
      }

      val preferredName =
        localProfile?.displayName?.takeIf { it.isNotBlank() && it != "Vizyon Dinleyicisi" }
          ?: fbUser?.displayName?.takeIf { it.isNotBlank() }
          ?: "Vizyon Dinleyicisi"
      val preferredAvatar =
        config?.cloudAvatarDataUrl?.takeIf { it.isNotBlank() }
          ?: fbUser?.photoUrl?.toString().orEmpty()
      val (passHash, passSalt) = appearancePrefs.currentVaultPasscodePair()

      firestoreRepo.ensureUserProfileExists(
        uid = uid,
        displayName = preferredName,
        photoUrl = preferredAvatar,
        handle = localProfile?.handle.orEmpty(),
        bio = localProfile?.bio.orEmpty(),
        favoriteGenres = config?.favoriteGenres.orEmpty(),
        shareActivity = localProfile?.shareActivity ?: true,
        chatPrivacy = config?.chatPrivacy ?: ChatPrivacy.PUBLIC,
        defaultStoryTheme = config?.defaultStoryTheme ?: StoryCardTheme.NEON_WAVE,
        securityPasscodeHash = passHash,
        securityPasscodeSalt = passSalt,
        rememberMe = config?.rememberMe ?: true,
        lockOnLaunch = config?.lockOnLaunch ?: false,
        preferredThemeMode = config?.themeMode?.name ?: AppThemeMode.NIGHT_AMOLED.name,
        preferredAccentPalette = config?.accentPalette?.name ?: AccentPalette.VIZYON_BLUE.name,
        preferredCornerDensity = config?.cornerDensity?.name ?: CornerDensity.BALANCED.name,
        preferredLanguage = config?.preferredLanguage?.name ?: AppLanguage.TURKISH.name,
        onboardingCompleted = config?.onboardingCompleted ?: false,
        recentTracks = recentTracks
      )

      if (remoteProfile != null && (config?.onboardingCompleted == true)) {
        // Sync latest local setup or photo to cloud if user finished onboarding before Google sign-in
        val backupMap = mutableMapOf<String, Any>()
        if (passHash.isNotBlank() && passSalt.isNotBlank() && remoteProfile.securityPasscodeHash.isBlank()) {
          backupMap["securityPasscodeHash"] = passHash
          backupMap["securityPasscodeSalt"] = passSalt
          backupMap["rememberMe"] = config.rememberMe
          backupMap["lockOnLaunch"] = config.lockOnLaunch
        }
        if (preferredAvatar.startsWith("data:image/") && remoteProfile.photoUrl != preferredAvatar) {
          backupMap["photoUrl"] = preferredAvatar
        }
        if (recentTracks.isNotEmpty() && remoteProfile.recentTracks.isEmpty()) {
          backupMap["recentTracks"] = recentTracks.take(5).map { it.toFirestoreMap() }
        }
        if (!remoteProfile.onboardingCompleted) {
          backupMap["onboardingCompleted"] = true
          backupMap["preferredThemeMode"] = config.themeMode.name
          backupMap["preferredAccentPalette"] = config.accentPalette.name
          backupMap["preferredCornerDensity"] = config.cornerDensity.name
          backupMap["preferredLanguage"] = config.preferredLanguage.name
          backupMap["chatPrivacy"] = config.chatPrivacy.name
          backupMap["defaultStoryTheme"] = config.defaultStoryTheme.name
        }
        if (backupMap.isNotEmpty()) {
          firestoreRepo.updateCloudBackupFields(uid, backupMap)
        }
      }
    }
  }

  fun completeOnboarding(
    displayName: String,
    handle: String,
    bio: String,
    favoriteGenres: List<String>,
    ageVerified: Boolean,
    preferredLanguage: AppLanguage,
    themeMode: AppThemeMode,
    shareActivity: Boolean,
    accentPalette: AccentPalette = AccentPalette.VIZYON_BLUE,
    cornerDensity: CornerDensity = CornerDensity.BALANCED,
    defaultStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
    chatPrivacy: ChatPrivacy = ChatPrivacy.PUBLIC,
    passcode: String = "",
    rememberMe: Boolean = true,
    lockOnLaunch: Boolean = false,
    requireFollowApproval: Boolean = false,
    backgroundNotifServiceEnabled: Boolean = true
  ) {
    viewModelScope.launch {
      appearancePrefs.completeOnboarding(
        displayName = displayName,
        handle = handle,
        bio = bio,
        favoriteGenres = favoriteGenres,
        ageVerified = ageVerified,
        preferredLanguage = preferredLanguage,
        themeMode = themeMode,
        shareActivity = shareActivity,
        accentPalette = accentPalette,
        cornerDensity = cornerDensity,
        defaultStoryTheme = defaultStoryTheme,
        chatPrivacy = chatPrivacy,
        passcode = passcode,
        rememberMe = rememberMe,
        lockOnLaunch = lockOnLaunch,
        requireFollowApproval = requireFollowApproval,
        backgroundNotifServiceEnabled = backgroundNotifServiceEnabled
      )
      if (!currentUserId.isNullOrBlank()) {
        val fbUser = Firebase.auth.currentUser
        val avatarUrl =
          appearancePrefs.currentCloudAvatarDataUrl().ifBlank { fbUser?.photoUrl?.toString().orEmpty() }
        val (passHash, passSalt) = appearancePrefs.currentVaultPasscodePair()
        val recents = appearancePrefs.currentRecentTracks()

        firestoreRepo.ensureUserProfileExists(
          uid = currentUserId,
          displayName = displayName,
          photoUrl = avatarUrl,
          handle = handle,
          bio = bio,
          favoriteGenres = favoriteGenres,
          shareActivity = shareActivity,
          requireFollowApproval = requireFollowApproval,
          chatPrivacy = chatPrivacy,
          defaultStoryTheme = defaultStoryTheme,
          securityPasscodeHash = passHash,
          securityPasscodeSalt = passSalt,
          rememberMe = rememberMe,
          lockOnLaunch = lockOnLaunch,
          preferredThemeMode = themeMode.name,
          preferredAccentPalette = accentPalette.name,
          preferredCornerDensity = cornerDensity.name,
          preferredLanguage = preferredLanguage.name,
          onboardingCompleted = true,
          recentTracks = recents
        )
        firestoreRepo.updateUserProfile(
          userId = currentUserId,
          displayName = displayName,
          handle = handle,
          bio = bio,
          shareActivity = shareActivity,
          requireFollowApproval = requireFollowApproval,
          favoriteGenres = favoriteGenres,
          photoUrl = avatarUrl.takeIf { it.isNotBlank() },
          chatPrivacy = chatPrivacy,
          defaultStoryTheme = defaultStoryTheme,
          securityPasscodeHash = passHash.takeIf { it.isNotBlank() },
          securityPasscodeSalt = passSalt.takeIf { it.isNotBlank() },
          rememberMe = rememberMe,
          lockOnLaunch = lockOnLaunch,
          preferredThemeMode = themeMode.name,
          preferredAccentPalette = accentPalette.name,
          preferredCornerDensity = cornerDensity.name,
          preferredLanguage = preferredLanguage.name,
          onboardingCompleted = true,
          recentTracks = recents
        )
      }
      val latestCfg = runCatching { appearancePrefs.configFlow.first() }.getOrNull()
      syncBackgroundNotifService(latestCfg)
      refreshCatalog(favoriteGenres, preferredLanguage)
      notifyUser("Stüdyo kurulumu tamamlandı, hoş geldin!")
    }
  }

  fun unlockVault(passcode: String) {
    viewModelScope.launch {
      val localOk = appearancePrefs.verifyLocalPasscode(passcode)
      val cloudProfile = (userProfileState.value as? UiState.Success)?.data
      val cloudOk =
        if (cloudProfile != null && cloudProfile.securityPasscodeHash.isNotBlank()) {
          com.example.security.PasscodeSecurityHelper.verifyPasscode(
            passcode,
            cloudProfile.securityPasscodeHash,
            cloudProfile.securityPasscodeSalt
          )
        } else true

      if (localOk && cloudOk) {
        _isVaultLocked.value = false
        _vaultUnlockError.value = null
      } else {
        _vaultUnlockError.value = "Güvenlik şifresi hatalı. Lütfen tekrar dene."
      }
    }
  }

  fun updateVaultPasscode(
    newPasscode: String,
    rememberMe: Boolean,
    lockOnLaunch: Boolean
  ) {
    if (_isVaultLocked.value) {
      notifyUser("Kasa kilitliyken güvenlik şifresi değiştirilemez.")
      return
    }
    viewModelScope.launch {
      val ok = appearancePrefs.updateVaultPasscode(newPasscode, rememberMe, lockOnLaunch)
      if (!ok) {
        notifyUser("Güvenlik şifresi en az 4 karakter olmalıdır.")
        return@launch
      }
      if (!currentUserId.isNullOrBlank()) {
        val (hash, salt) = appearancePrefs.currentVaultPasscodePair()
        firestoreRepo.updateCloudBackupFields(
          currentUserId,
          mapOf(
            "securityPasscodeHash" to hash,
            "securityPasscodeSalt" to salt,
            "rememberMe" to rememberMe,
            "lockOnLaunch" to lockOnLaunch
          )
        )
      }
      notifyUser("Stüdyo güvenlik şifren güncellendi ve buluta yedeklendi.")
    }
  }

  fun updateVaultSettings(rememberMe: Boolean, lockOnLaunch: Boolean) {
    if (_isVaultLocked.value) {
      notifyUser("Önce güvenlik kilidini açmalısın.")
      return
    }
    viewModelScope.launch {
      appearancePrefs.updateVaultSettings(rememberMe, lockOnLaunch)
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.updateCloudBackupFields(
          currentUserId,
          mapOf("rememberMe" to rememberMe, "lockOnLaunch" to lockOnLaunch)
        )
      }
    }
  }

  fun syncCloudBackupNow() {
    val uid = currentUserId
    if (uid.isNullOrBlank()) {
      notifyUser("Bulut yedekleme için Google hesabınla oturum açmalısın.")
      return
    }
    viewModelScope.launch {
      val cfg = appearanceState.value
      val profile = (userProfileState.value as? UiState.Success)?.data ?: appearancePrefs.localProfileFlow.first()
      val (passHash, passSalt) = appearancePrefs.currentVaultPasscodePair()
      val recents = appearancePrefs.currentRecentTracks()
      val avatar = cfg.cloudAvatarDataUrl.ifBlank { profile.photoUrl }

      firestoreRepo.updateUserProfile(
        userId = uid,
        displayName = profile.displayName,
        handle = profile.handle,
        bio = profile.bio,
        shareActivity = cfg.shareListeningActivity,
        requireFollowApproval = cfg.requireFollowApproval,
        favoriteGenres = cfg.favoriteGenres,
        photoUrl = avatar.takeIf { it.isNotBlank() },
        chatPrivacy = cfg.chatPrivacy,
        defaultStoryTheme = cfg.defaultStoryTheme,
        securityPasscodeHash = passHash.takeIf { it.isNotBlank() },
        securityPasscodeSalt = passSalt.takeIf { it.isNotBlank() },
        rememberMe = cfg.rememberMe,
        lockOnLaunch = cfg.lockOnLaunch,
        preferredThemeMode = cfg.themeMode.name,
        preferredAccentPalette = cfg.accentPalette.name,
        preferredCornerDensity = cfg.cornerDensity.name,
        preferredLanguage = cfg.preferredLanguage.name,
        onboardingCompleted = true,
        recentTracks = recents
      ).onSuccess {
        notifyUser("Tüm profil, fotoğraf, tema ve son şarkı verilerin buluta yedeklendi!")
      }.onFailure {
        notifyUser(it.localizedMessage ?: "Bulut yedeklemesi başarısız")
      }
    }
  }

  fun deleteAccount(
    bypassFreshnessPreCheck: Boolean = false,
    onReauthRequired: () -> Unit = {},
    onCompleted: () -> Unit = {}
  ) {
    if (_isVaultLocked.value) {
      notifyUser("Önce güvenlik kilidini doğrulamalısın.")
      return
    }
    if (_isDeletingAccount.value) return
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank() && !bypassFreshnessPreCheck) {
        val fbUser = Firebase.auth.currentUser
        if (fbUser != null && !isRecentLoginFresh(fbUser)) {
          notifyUser("Güvenlik duvarı: Hesabı silmeden önce Google hesabınla yeniden doğrulama yapmalısın.")
          onReauthRequired()
          return@launch
        }
      }
      _isDeletingAccount.value = true
      try {
        appContext?.let { VizyonRealtimeNotificationService.stop(it) }
        playerManager.resetAllForAccountDeletion()
        if (!currentUserId.isNullOrBlank()) {
          val res = firestoreRepo.deleteAllUserDataAndAccount(currentUserId)
          val err = res.exceptionOrNull()
          if (err is FirebaseAuthRecentLoginRequiredException) {
            _isDeletingAccount.value = false
            notifyUser("Güvenlik doğrulaması için lütfen Google hesabınla yeniden oturum aç.")
            onReauthRequired()
            return@launch
          }
          if (res.isFailure) {
            _isDeletingAccount.value = false
            notifyUser(err?.localizedMessage ?: "Hesap silinemedi")
            return@launch
          }
        }
        localLibraryDao.clearAllUserData()
        appearancePrefs.resetAllForAccountDeletion()
        notifyUser("Hesabın ve tüm verilerin kalıcı olarak silindi.")
        onCompleted()
      } finally {
        _isDeletingAccount.value = false
      }
    }
  }

  fun refreshCatalog(
    preferredGenres: List<String> = appearanceState.value.favoriteGenres,
    preferredLanguage: AppLanguage = appearanceState.value.preferredLanguage,
    isAutoRefresh: Boolean = false
  ) {
    viewModelScope.launch {
      if (isAutoRefresh) {
        _isAutoRefreshingCatalog.value = true
      } else if (_catalogState.value !is UiState.Success) {
        _catalogState.value = UiState.Loading
      }
      try {
        catalogSeedCounter += 1L
        val launchSeed = System.currentTimeMillis() + catalogSeedCounter * 7919L
        val (tracks, artists, status) =
          hybridMusicRepo.loadHomeCatalog(preferredGenres, preferredLanguage, showcaseSeed = launchSeed)
        _catalogState.value = UiState.Success(HomeCatalogData(tracks, artists, status))
        appearancePrefs.updateLastCatalogRefreshMs(System.currentTimeMillis())
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        if (!isAutoRefresh) {
          _catalogState.value = UiState.Error(e.localizedMessage ?: "Katalog yüklenemedi")
        }
      } finally {
        _isAutoRefreshingCatalog.value = false
      }
    }
  }

  fun checkAndAutoRefreshDiscover() {
    val lastRefresh = appearanceState.value.lastCatalogRefreshMs
    if (lastRefresh > 0L && System.currentTimeMillis() - lastRefresh >= DISCOVER_AUTO_REFRESH_INTERVAL_MS) {
      refreshCatalog(isAutoRefresh = true)
    }
  }

  fun onSearchQueryChanged(rawQuery: String) {
    autocompleteJob?.cancel()
    val clean = rawQuery.trim()
    if (clean.length < 2) {
      _searchSuggestionsState.value = emptyList()
      return
    }
    val instant = hybridMusicRepo.instantSearchSuggestions(clean)
    _searchSuggestionsState.value = instant
    autocompleteJob =
      viewModelScope.launch {
        delay(280L)
        val country = appearanceState.value.preferredLanguage.itunesCountry
        val live = hybridMusicRepo.fetchLiveSearchSuggestions(clean, country)
        val merged =
          (instant + live)
            .distinctBy { it.text.lowercase(java.util.Locale.ROOT) }
            .take(8)
        _searchSuggestionsState.value = merged
      }
  }

  fun search(query: String) {
    autocompleteJob?.cancel()
    _searchSuggestionsState.value = emptyList()
    viewModelScope.launch {
      val clean = query.trim()
      if (clean.isEmpty()) {
        _searchState.value = UiState.Success(SearchResultData())
        return@launch
      }
      _searchState.value = UiState.Loading
      try {
        val country = appearanceState.value.preferredLanguage.itunesCountry
        val (tracks, artists) = hybridMusicRepo.searchUnified(clean, country)
        _searchState.value = UiState.Success(SearchResultData(clean, tracks, artists))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _searchState.value = UiState.Error(e.localizedMessage ?: "Arama başarısız")
      }
    }
  }

  fun playTrack(track: MusicTrack, queue: List<MusicTrack> = listOf(track)) {
    resolveVideoJob?.cancel()
    playerManager.prepareLoading(track, queue)
    _openNowPlayingNonce.value += 1
    viewModelScope.launch {
      val updatedRecent = appearancePrefs.recordRecentTrack(track)
      if (!currentUserId.isNullOrBlank()) {
        val cfg = appearanceState.value
        val profile = (userProfileState.value as? UiState.Success)?.data
        val shareAllowed = cfg.shareListeningActivity && (profile?.shareActivity ?: true)
        if (shareAllowed) {
          firestoreRepo.updateLiveListeningStatus(currentUserId, track, updatedRecent)
        }
      }
    }
    resolveVideoJob =
      viewModelScope.launch {
        when (val result = hybridMusicRepo.resolveYouTubeVideoForTrack(track)) {
          is VideoResolutionResult.Success -> {
            playerManager.onVideoResolved(result.track, queue)
          }
          is VideoResolutionResult.MissingApiKey -> {
            playerManager.onVideoResolutionFailed(
              "YouTube Data API v3 anahtarı tanımlı değil. Lütfen YOUTUBE_API_KEY ekleyin."
            )
          }
          is VideoResolutionResult.NotFound -> {
            playerManager.onVideoResolutionFailed(result.reason)
          }
        }
      }
  }

  fun retryCurrentTrackVideo() {
    val current = playbackState.value.currentTrack ?: return
    val failedVideoId = playbackState.value.activeVideoId.takeIf { it.isNotBlank() }
    val excluded = if (failedVideoId != null && playbackState.value.playbackMessage != null) setOf(failedVideoId) else emptySet()
    resolveVideoJob?.cancel()
    playerManager.prepareLoading(current.copy(youtubeVideoId = ""), playbackState.value.queue)
    resolveVideoJob =
      viewModelScope.launch {
        when (
          val result =
            hybridMusicRepo.resolveYouTubeVideoForTrack(
              track = current,
              forceRefresh = true,
              excludedVideoIds = excluded
            )
        ) {
          is VideoResolutionResult.Success ->
            playerManager.onVideoResolved(result.track, playbackState.value.queue)
          is VideoResolutionResult.MissingApiKey ->
            playerManager.onVideoResolutionFailed("YOUTUBE_API_KEY tanımlı değil.")
          is VideoResolutionResult.NotFound ->
            playerManager.onVideoResolutionFailed(result.reason)
        }
      }
  }

  fun attachYouTubeWebView(webView: WebView?) = playerManager.attachWebView(webView)
  fun onYouTubeIFrameReady(videoId: String) = playerManager.onIFrameReady(videoId)
  fun onYouTubeIFrameStateChanged(stateCode: Int) = playerManager.onIFrameStateChanged(stateCode)
  fun onYouTubeIFrameProgress(currentSec: Double, durationSec: Double) =
    playerManager.onIFrameProgress(currentSec, durationSec)
  fun onYouTubeIFrameError(code: Int) = playerManager.onIFrameError(code)
  fun onYouTubeIFrameLoadFailure(reason: String) = playerManager.onIFrameLoadFailure(reason)

  fun pauseForBackground() {
    _isAppInForeground.value = false
    playerManager.pauseForBackground()
    appContext?.let { BackgroundNotificationJobService.schedule(it) }
  }

  fun onHostResumed() {
    _isAppInForeground.value = true
    playerManager.onHostResumed()
    checkAndAutoRefreshDiscover()
    appContext?.let { ctx ->
      StudioSoundEngine.warmup(ctx)
      BackgroundNotificationJobService.schedule(ctx)
      syncBackgroundNotifService()
    }
    if (!currentUserId.isNullOrBlank()) {
      viewModelScope.launch {
        val latest = firestoreRepo.fetchLatestNotificationsOnce(currentUserId)
        if (latest.isNotEmpty()) {
          processIncomingNotifications(latest)
        }
      }
    }
  }

  fun presentInAppBubbleIfAllowed(notification: UserNotification) {
    if (!_isAppInForeground.value || _isVaultLocked.value) return
    val type = NotificationType.fromRaw(notification.type)
    val openPeerId =
      _activeChatPeer.value?.let {
        val raw = it.userId.ifBlank { it.id }
        if (raw.isBlank()) null else sanitizeFirestoreId(raw)
      }
    val senderSafeId = if (notification.senderId.isNotBlank()) sanitizeFirestoreId(notification.senderId) else ""
    if ((type == NotificationType.MESSAGE || type == NotificationType.VOICE_MESSAGE) &&
      openPeerId != null &&
      openPeerId == senderSafeId
    ) {
      // User is already inside the direct chat sheet with this sender; skip top pill bubble
      return
    }
    _activeInAppBubble.value = notification
  }

  fun dismissInAppBubble() {
    _activeInAppBubble.value = null
  }
  fun pauseWhenMinimized() = playerManager.pauseWhenMinimized()
  fun togglePlayPause() = playerManager.togglePlayPause()
  fun seekTo(ms: Long) = playerManager.seekTo(ms)
  fun skipNext() = playerManager.skipNext()
  fun skipPrevious() = playerManager.skipPrevious()
  fun toggleShuffle() = playerManager.toggleShuffle()
  fun cycleRepeat() = playerManager.cycleRepeatMode()

  fun addToQueue(track: MusicTrack) {
    playerManager.addToQueue(track)
    notifyUser("${track.title} sıraya eklendi")
  }

  fun dismissPlaybackMessage() = playerManager.dismissMessage()

  fun stopAndDismissPlayer() {
    resolveVideoJob?.cancel()
    playerManager.stopAndClear()
  }

  fun toggleFavorite(track: MusicTrack) {
    val safeId = sanitizeFirestoreId(track.trackId)
    val currentFavs = (favoritesState.value as? UiState.Success)?.data.orEmpty()
    val isFav = currentFavs.any { it.trackId == safeId || it.id == safeId }
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .toggleFavorite(currentUserId, track, isFav)
          .onSuccess { notifyUser(if (isFav) "Favorilerden çıkarıldı" else "Favorilere eklendi") }
          .onFailure { notifyUser(it.localizedMessage ?: "İşlem başarısız") }
      } else {
        if (isFav) {
          localLibraryDao.deleteFavorite(safeId)
          notifyUser("Favorilerden çıkarıldı")
        } else {
          localLibraryDao.upsertFavorite(
            LocalFavoriteEntity(
              trackId = safeId,
              title = track.title,
              artistId = sanitizeFirestoreId(track.artistId),
              artistName = track.artistName,
              albumName = track.albumName,
              artworkUrl = track.artworkUrl,
              youtubeVideoId = track.youtubeVideoId,
              durationMs = track.durationMs,
              source = track.source.name
            )
          )
          notifyUser("Favorilere eklendi")
        }
      }
    }
  }

  fun loadLyricsForTrack(track: MusicTrack? = playbackState.value.currentTrack, forceRefresh: Boolean = false) {
    val target = track ?: return
    if (!forceRefresh && lastLyricsTrackId == target.trackId && _lyricsState.value is UiState.Success) {
      return
    }
    lastLyricsTrackId = target.trackId
    viewModelScope.launch {
      _lyricsState.value = UiState.Loading
      try {
        val lyrics = hybridMusicRepo.fetchLyrics(target)
        if (lastLyricsTrackId == target.trackId) {
          _lyricsState.value = UiState.Success(lyrics)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        if (lastLyricsTrackId == target.trackId) {
          _lyricsState.value = UiState.Error(e.localizedMessage ?: "Şarkı sözleri alınamadı")
        }
      }
    }
  }

  fun createPlaylist(
    title: String,
    description: String,
    accentHex: String,
    coverStyle: PlaylistCoverStyle,
    sharedPublic: Boolean
  ) {
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .createPlaylist(currentUserId, title, description, accentHex, coverStyle, sharedPublic)
          .onSuccess { notifyUser("Çalma listesi oluşturuldu") }
          .onFailure { notifyUser(it.localizedMessage ?: "Çalma listesi oluşturulamadı") }
      } else {
        val plId = sanitizeFirestoreId("pl_${UUID.randomUUID().toString().take(12)}")
        localLibraryDao.upsertPlaylist(
          LocalPlaylistEntity(
            id = plId,
            title = title.trim().ifBlank { "Yeni Çalma Listem" },
            description = description.trim(),
            accentColorHex = accentHex,
            coverStyle = coverStyle.name,
            sharedPublic = sharedPublic,
            trackCount = 0L
          )
        )
        notifyUser("Çalma listesi oluşturuldu")
      }
    }
  }

  fun selectPlaylist(playlistId: String?) {
    _selectedPlaylistId.value = playlistId
  }

  fun addTrackToPlaylist(playlistId: String, track: MusicTrack) {
    val safeTrackId = sanitizeFirestoreId(track.trackId)
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .addTrackToPlaylist(currentUserId, playlistId, track)
          .onSuccess { notifyUser("Parça çalma listesine eklendi") }
          .onFailure { notifyUser(it.localizedMessage ?: "Eklenemedi") }
      } else {
        localLibraryDao.upsertPlaylistTrack(
          LocalPlaylistTrackEntity(
            playlistId = playlistId,
            trackId = safeTrackId,
            title = track.title,
            artistId = sanitizeFirestoreId(track.artistId),
            artistName = track.artistName,
            albumName = track.albumName,
            artworkUrl = track.artworkUrl,
            youtubeVideoId = track.youtubeVideoId,
            durationMs = track.durationMs,
            source = track.source.name
          )
        )
        val count = localLibraryDao.countPlaylistTracks(playlistId)
        localLibraryDao.updatePlaylistCount(playlistId, count)
        notifyUser("Parça çalma listesine eklendi")
      }
    }
  }

  fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
    val safeTrackId = sanitizeFirestoreId(trackId)
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .removeTrackFromPlaylist(currentUserId, playlistId, safeTrackId)
          .onSuccess { notifyUser("Parça listeden çıkarıldı") }
          .onFailure { notifyUser(it.localizedMessage ?: "Çıkarılamadı") }
      } else {
        localLibraryDao.deletePlaylistTrack(playlistId, safeTrackId)
        val count = localLibraryDao.countPlaylistTracks(playlistId)
        localLibraryDao.updatePlaylistCount(playlistId, count)
        notifyUser("Parça listeden çıkarıldı")
      }
    }
  }

  fun deletePlaylist(playlistId: String) {
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .deletePlaylist(currentUserId, playlistId)
          .onSuccess {
            if (_selectedPlaylistId.value == playlistId) _selectedPlaylistId.value = null
            notifyUser("Çalma listesi silindi")
          }
          .onFailure { notifyUser(it.localizedMessage ?: "Silinemedi") }
      } else {
        localLibraryDao.deletePlaylist(playlistId)
        if (_selectedPlaylistId.value == playlistId) _selectedPlaylistId.value = null
        notifyUser("Çalma listesi silindi")
      }
    }
  }

  fun openArtistProfile(artistId: String, fallbackName: String, fallbackImage: String) {
    val safeId = sanitizeFirestoreId(artistId)
    _selectedArtistId.value = safeId
    _artistDetailState.value = UiState.Loading
    viewModelScope.launch {
      try {
        val (detail, tracks) = hybridMusicRepo.fetchArtistDetail(safeId, fallbackName, fallbackImage)
        _artistDetailState.value = UiState.Success(ArtistDetailData(detail, tracks))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _artistDetailState.value = UiState.Error(e.localizedMessage ?: "Sanatçı profili yüklenemedi")
      }
    }
  }

  fun closeArtistProfile() {
    _selectedArtistId.value = null
    _artistDetailState.value = null
  }

  fun toggleFollowArtist(artist: ArtistSummary) {
    val safeId = sanitizeFirestoreId(artist.artistId)
    val followed = (followedArtistsState.value as? UiState.Success)?.data.orEmpty()
    val isFollowing = followed.any { it.artistId == safeId || it.id == safeId }
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .toggleFollowArtist(currentUserId, artist, isFollowing)
          .onSuccess {
            notifyUser(if (isFollowing) "${artist.name} takipten çıkarıldı" else "${artist.name} takip ediliyor")
          }
          .onFailure { notifyUser(it.localizedMessage ?: "İşlem başarısız") }
      } else {
        if (isFollowing) {
          localLibraryDao.deleteFollowedArtist(safeId)
          notifyUser("${artist.name} takipten çıkarıldı")
        } else {
          localLibraryDao.upsertFollowedArtist(
            LocalFollowedArtistEntity(
              artistId = safeId,
              name = artist.name,
              imageUrl = artist.imageUrl,
              genresJoined = artist.genres.joinToString("|"),
              source = artist.source.name
            )
          )
          notifyUser("${artist.name} takip ediliyor")
        }
      }
    }
  }

  fun postFanMessage(artistId: String, message: String, tag: FanPostTag) {
    val profile = (userProfileState.value as? UiState.Success)?.data
    val authorName = profile?.displayName ?: "Vizyon Dinleyicisi"
    val authorHandle = profile?.handle ?: "@vizyon"
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .postArtistFanMessage(currentUserId, artistId, authorName, authorHandle, message, tag)
          .onSuccess { notifyUser("Sanatçı duvarına gönderildi") }
          .onFailure { notifyUser(it.localizedMessage ?: "Gönderilemedi") }
      } else {
        localLibraryDao.insertFanPost(
          LocalFanPostEntity(
            id = sanitizeFirestoreId("post_${UUID.randomUUID().toString().take(12)}"),
            artistId = sanitizeFirestoreId(artistId),
            authorName = authorName,
            authorHandle = authorHandle,
            message = message.trim().take(280),
            tag = tag.name,
            cheersCount = 0L
          )
        )
        notifyUser("Sanatçı duvarına gönderildi")
      }
    }
  }

  fun cheerFanPost(artistId: String, post: ArtistFanPost) {
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.cheerArtistFanPost(artistId, post.id, post.cheersCount)
      } else {
        localLibraryDao.incrementFanPostCheer(post.id)
      }
    }
  }

  private fun currentBlockedIdSet(): Set<String> =
    (blockedUsersState.value as? UiState.Success)?.data
      ?.mapNotNull { b -> b.blockedUserId.ifBlank { b.id }.takeIf { it.isNotBlank() }?.let(::sanitizeFirestoreId) }
      ?.toSet()
      .orEmpty()

  fun isUserBlocked(userId: String): Boolean {
    if (userId.isBlank()) return false
    return sanitizeFirestoreId(userId) in currentBlockedIdSet()
  }

  fun hasPendingFollowRequestTo(targetUserId: String): Boolean {
    if (targetUserId.isBlank()) return false
    val key = "follow_${sanitizeFirestoreId(targetUserId)}::PENDING"
    return key in outgoingRequestKeysState.value
  }

  fun hasPendingMessageRequestTo(targetUserId: String): Boolean {
    if (targetUserId.isBlank()) return false
    val key = "message_${sanitizeFirestoreId(targetUserId)}::PENDING"
    return key in outgoingRequestKeysState.value
  }

  fun toggleFollowListener(target: PublicProfile) {
    val rawTargetId = target.userId.ifBlank { target.id }
    if (rawTargetId.isBlank()) {
      notifyUser("Geçersiz kullanıcı profili.")
      return
    }
    val targetId = sanitizeFirestoreId(rawTargetId)
    if (targetId == currentUserId || targetId == "local") {
      notifyUser("Kendini takip edemezsin.")
      return
    }
    if (targetId in currentBlockedIdSet()) {
      notifyUser("Engellediğin bir kullanıcıyı takip edemezsin.")
      return
    }
    val following = (followingState.value as? UiState.Success)?.data.orEmpty()
    val isFollowing = following.any { it.targetUserId == targetId || it.id == targetId }
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()

    if (currentUserId.isNullOrBlank()) {
      viewModelScope.launch {
        if (isFollowing) {
          localLibraryDao.deleteFollowing(targetId)
          notifyUser("${target.displayName} takipten çıkarıldı")
        } else {
          localLibraryDao.upsertFollowing(
            LocalFollowingEntity(
              targetUserId = targetId,
              displayName = target.displayName.ifBlank { "Dinleyici" },
              handle = target.handle.ifBlank { "@vizyon" },
              photoUrl = target.photoUrl
            )
          )
          notifyUser("${target.displayName} takip edildi")
        }
      }
      return
    }

    if (!isFollowing && target.requireFollowApproval) {
      if (hasPendingFollowRequestTo(targetId)) {
        notifyUser("Bu kullanıcıya zaten takip isteği gönderdin.")
        return
      }
      viewModelScope.launch {
        firestoreRepo.sendFollowRequest(
          requesterId = currentUserId,
          requesterName = myName,
          requesterHandle = myHandle,
          requesterAvatarUrl = myAvatar,
          recipientId = targetId
        ).onSuccess {
          notifyUser("${target.displayName} kullanıcısına takip isteği gönderildi.")
          firestoreRepo.sendNotification(
            recipientUserId = targetId,
            senderId = currentUserId,
            senderName = myName,
            senderHandle = myHandle,
            senderAvatarUrl = myAvatar,
            type = NotificationType.FOLLOW_REQUEST,
            message = "Sana takip isteği gönderdi.",
            targetType = "REQUESTS",
            targetId = currentUserId
          )
        }.onFailure {
          notifyUser(it.localizedMessage ?: "Takip isteği gönderilemedi.")
        }
      }
      return
    }

    viewModelScope.launch {
      firestoreRepo
        .toggleFollowListener(
          userId = currentUserId,
          followerName = myName,
          followerHandle = myHandle,
          followerAvatarUrl = myAvatar,
          target = target,
          currentlyFollowing = isFollowing
        )
        .onSuccess {
          notifyUser(if (isFollowing) "${target.displayName} takipten çıkarıldı" else "${target.displayName} takip edildi")
          if (!isFollowing) {
            firestoreRepo.sendNotification(
              recipientUserId = targetId,
              senderId = currentUserId,
              senderName = myName,
              senderHandle = myHandle,
              senderAvatarUrl = myAvatar,
              type = NotificationType.FOLLOW,
              message = "Seni takip etmeye başladı!",
              targetType = "PROFILE",
              targetId = currentUserId
            )
          }
        }
        .onFailure { notifyUser(it.localizedMessage ?: "İşlem başarısız") }
    }
  }

  fun blockUser(
    targetUserId: String,
    targetName: String,
    targetHandle: String,
    targetAvatarUrl: String = ""
  ) {
    if (targetUserId.isBlank()) return
    val safeTarget = sanitizeFirestoreId(targetUserId)
    if (safeTarget == currentUserId || safeTarget == "local") {
      notifyUser("Kendini engelleyemezsin.")
      return
    }
    val openPeerId = _activeChatPeer.value?.let {
      val raw = it.userId.ifBlank { it.id }
      if (raw.isBlank()) null else sanitizeFirestoreId(raw)
    }
    if (openPeerId == safeTarget) {
      _activeChatPeer.value = null
    }
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.blockUser(
          userId = currentUserId,
          targetUserId = safeTarget,
          targetName = targetName,
          targetHandle = targetHandle,
          targetAvatarUrl = targetAvatarUrl
        ).onSuccess {
          val following = (followingState.value as? UiState.Success)?.data.orEmpty()
          if (following.any { it.targetUserId == safeTarget || it.id == safeTarget }) {
            val stub = PublicProfile(id = safeTarget, userId = safeTarget, displayName = targetName, handle = targetHandle)
            firestoreRepo.toggleFollowListener(
              userId = currentUserId,
              followerName = "",
              followerHandle = "",
              followerAvatarUrl = "",
              target = stub,
              currentlyFollowing = true
            )
          }
          notifyUser("${targetName.ifBlank { "Kullanıcı" }} engellendi.")
        }.onFailure {
          notifyUser(it.localizedMessage ?: "Engelleme işlemi başarısız")
        }
      } else {
        localLibraryDao.upsertBlockedUser(
          LocalBlockedUserEntity(
            blockedUserId = safeTarget,
            displayName = targetName.ifBlank { "Dinleyici" },
            handle = targetHandle.ifBlank { "@vizyon" },
            photoUrl = targetAvatarUrl
          )
        )
        localLibraryDao.deleteFollowing(safeTarget)
        notifyUser("${targetName.ifBlank { "Kullanıcı" }} engellendi.")
      }
    }
  }

  fun unblockUser(blockedUserId: String) {
    if (blockedUserId.isBlank()) return
    val safeTarget = sanitizeFirestoreId(blockedUserId)
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.unblockUser(currentUserId, safeTarget)
          .onSuccess { notifyUser("Kullanıcı engeli kaldırıldı.") }
          .onFailure { notifyUser(it.localizedMessage ?: "Engel kaldırılamadı") }
      } else {
        localLibraryDao.deleteBlockedUser(safeTarget)
        notifyUser("Kullanıcı engeli kaldırıldı.")
      }
    }
  }

  fun reportUser(
    targetUserId: String,
    targetType: String = "USER",
    targetId: String = targetUserId,
    reason: ReportReason,
    details: String = ""
  ) {
    if (targetUserId.isBlank()) return
    val safeTargetUser = sanitizeFirestoreId(targetUserId)
    if (safeTargetUser == currentUserId) {
      notifyUser("Kendi hesabını şikayet edemezsin.")
      return
    }
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.submitUserReport(
          reporterId = currentUserId,
          targetUserId = safeTargetUser,
          targetType = targetType,
          targetId = targetId,
          reason = reason,
          details = details
        ).onSuccess {
          notifyUser("Şikayetin incelemeye alındı. Teşekkür ederiz.")
        }.onFailure {
          notifyUser(it.localizedMessage ?: "Şikayet gönderilemedi")
        }
      } else {
        notifyUser("Şikayetin kaydedildi (${reason.label}).")
      }
    }
  }

  fun sendMessageRequest(
    peer: PublicProfile,
    previewText: String,
    messageType: DirectMessageType = DirectMessageType.TEXT,
    audioBase64: String = "",
    audioDurationMs: Long = 0L
  ) {
    val rawPeerId = peer.userId.ifBlank { peer.id }
    if (rawPeerId.isBlank()) return
    val safePeerId = sanitizeFirestoreId(rawPeerId)
    if (safePeerId == currentUserId || safePeerId == "local") {
      notifyUser("Kendine mesaj isteği gönderemezsin.")
      return
    }
    if (safePeerId in currentBlockedIdSet()) {
      notifyUser("Engellenen kullanıcıya mesaj isteği gönderilemez.")
      return
    }
    if (currentUserId.isNullOrBlank()) {
      notifyUser("Mesaj isteği göndermek için Google hesabınla oturum açmalısın.")
      return
    }
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    StudioSoundEngine.playMessageSendChime(appContext)
    viewModelScope.launch {
      firestoreRepo.sendMessageRequest(
        requesterId = currentUserId,
        requesterName = myName,
        requesterHandle = myHandle,
        requesterAvatarUrl = myAvatar,
        recipientId = safePeerId,
        previewText = previewText,
        messageType = messageType,
        audioBase64 = audioBase64,
        audioDurationMs = audioDurationMs
      ).onSuccess {
        notifyUser("${peer.displayName} kullanıcısına mesaj isteği gönderildi!")
        firestoreRepo.sendNotification(
          recipientUserId = safePeerId,
          senderId = currentUserId,
          senderName = myName,
          senderHandle = myHandle,
          senderAvatarUrl = myAvatar,
          type = NotificationType.MESSAGE_REQUEST,
          message =
            if (messageType == DirectMessageType.VOICE) "Sana sesli mesaj isteği gönderdi."
            else previewText.trim().ifBlank { "Sana mesaj isteği gönderdi." }.take(120),
          targetType = "REQUESTS",
          targetId = currentUserId
        )
      }.onFailure {
        notifyUser(it.localizedMessage ?: "Mesaj isteği gönderilemedi.")
      }
    }
  }

  fun respondToFollowRequest(request: FollowRequest, accept: Boolean) {
    val uid = currentUserId ?: return
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    viewModelScope.launch {
      firestoreRepo.respondToFollowRequest(
        recipientId = uid,
        request = request,
        accept = accept,
        recipientName = myName,
        recipientHandle = myHandle,
        recipientAvatarUrl = myAvatar
      ).onSuccess {
        notifyUser(
          if (accept) "${request.requesterName} takip isteği kabul edildi."
          else "${request.requesterName} takip isteği reddedildi."
        )
      }.onFailure {
        notifyUser(it.localizedMessage ?: "İstek yanıtlanamadı.")
      }
    }
  }

  fun respondToMessageRequest(
    request: MessageRequest,
    accept: Boolean,
    openChatOnAccept: Boolean = true
  ) {
    val uid = currentUserId ?: return
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    viewModelScope.launch {
      firestoreRepo.respondToMessageRequest(
        recipientId = uid,
        request = request,
        accept = accept,
        recipientName = myName,
        recipientHandle = myHandle,
        recipientAvatarUrl = myAvatar
      ).onSuccess { createdChatId ->
        if (accept) {
          notifyUser("${request.requesterName} mesaj isteği kabul edildi.")
          if (openChatOnAccept && !createdChatId.isNullOrBlank()) {
            val reqUid = sanitizeFirestoreId(request.requesterId.ifBlank { request.id })
            if (reqUid.isNotBlank()) {
              _activeChatPeer.value = PublicProfile(
                id = reqUid,
                userId = reqUid,
                displayName = request.requesterName.ifBlank { "Dinleyici" },
                handle = request.requesterHandle.ifBlank { "@vizyon" },
                photoUrl = request.requesterAvatarUrl
              )
            }
          }
        } else {
          notifyUser("Mesaj isteği reddedildi.")
        }
      }.onFailure {
        notifyUser(it.localizedMessage ?: "Mesaj isteği yanıtlanamadı.")
      }
    }
  }

  fun inspectFollowLists(userId: String) {
    if (userId.isBlank()) return
    _inspectedFollowUserId.value = sanitizeFirestoreId(userId)
  }

  fun closeInspectFollowLists() {
    _inspectedFollowUserId.value = null
  }

  fun openListenerProfileById(userId: String) {
    if (userId.isBlank()) return
    val cleanId = sanitizeFirestoreId(userId)
    if (cleanId in currentBlockedIdSet()) {
      notifyUser("Bu kullanıcı engellendiği için profili açılamıyor.")
      return
    }
    _pendingDeepLink.value = NotificationDeepLinkTarget.OpenListenerProfile(cleanId)
  }

  fun openDirectChat(peer: PublicProfile) {
    val rawPeerId = peer.userId.ifBlank { peer.id }
    if (rawPeerId.isBlank()) return
    val safePeerId = sanitizeFirestoreId(rawPeerId)
    if (safePeerId == currentUserId || safePeerId == "local") {
      notifyUser("Kendine mesaj gönderemezsin.")
      return
    }
    if (safePeerId in currentBlockedIdSet()) {
      notifyUser("Engellediğin kullanıcıyla sohbet açılamaz.")
      return
    }
    _activeChatPeer.value = peer
  }

  fun openDirectChatById(userId: String) {
    if (userId.isBlank()) return
    val safeId = sanitizeFirestoreId(userId)
    if (safeId == currentUserId || safeId == "local") return
    if (safeId in currentBlockedIdSet()) {
      notifyUser("Engellediğin kullanıcıyla sohbet açılamaz.")
      return
    }
    val listeners = (publicListenersState.value as? UiState.Success)?.data.orEmpty()
    val found =
      listeners.firstOrNull { sanitizeFirestoreId(it.userId.ifBlank { it.id }) == safeId }
        ?: (directChatsState.value as? UiState.Success)?.data
          ?.firstOrNull { safeId in it.participantIds }
          ?.let { thread ->
            PublicProfile(
              id = safeId,
              userId = safeId,
              displayName = thread.peerName(currentUserId ?: "local"),
              handle = thread.peerHandle(currentUserId ?: "local"),
              photoUrl = thread.peerAvatar(currentUserId ?: "local")
            )
          }
        ?: PublicProfile(id = safeId, userId = safeId, displayName = "Dinleyici", handle = "@vizyon")
    _activeChatPeer.value = found
  }

  fun closeDirectChat() {
    _activeChatPeer.value = null
  }

  private fun isConnectedWithPeer(peerId: String): Boolean {
    val followingIds = (followingState.value as? UiState.Success)?.data?.map { it.targetUserId }.orEmpty()
    val followerIds = (followersState.value as? UiState.Success)?.data?.map { it.targetUserId }.orEmpty()
    return peerId in followingIds || peerId in followerIds
  }

  fun sendDirectMessage(text: String, attachedTrack: MusicTrack? = null) {
    val peer = _activeChatPeer.value ?: return
    val rawPeerId = peer.userId.ifBlank { peer.id }
    if (rawPeerId.isBlank()) return
    val peerId = sanitizeFirestoreId(rawPeerId)
    if (peerId in currentBlockedIdSet()) {
      notifyUser("Engellenen kullanıcıya mesaj gönderilemez.")
      return
    }
    val cleanText =
      text.trim().ifBlank {
        attachedTrack?.let { "♪ ${it.title} — ${it.artistName}" } ?: ""
      }.take(600)
    if (cleanText.isBlank()) {
      notifyUser("Mesaj yaz veya bir şarkı ekle.")
      return
    }

    if (ChatPrivacy.fromRaw(peer.chatPrivacy) == ChatPrivacy.FOLLOWERS_ONLY && !isConnectedWithPeer(peerId)) {
      sendMessageRequest(peer = peer, previewText = cleanText, messageType = DirectMessageType.TEXT)
      return
    }

    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()

    StudioSoundEngine.playMessageSendChime(appContext)
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .sendDirectMessage(
            senderId = currentUserId,
            senderName = myName,
            senderHandle = myHandle,
            senderAvatarUrl = myAvatar,
            peer = peer,
            text = cleanText,
            attachedTrack = attachedTrack
          )
          .onSuccess {
            firestoreRepo.sendNotification(
              recipientUserId = peerId,
              senderId = currentUserId,
              senderName = myName,
              senderHandle = myHandle,
              senderAvatarUrl = myAvatar,
              type = NotificationType.MESSAGE,
              trackId = attachedTrack?.trackId.orEmpty(),
              trackTitle = attachedTrack?.title.orEmpty(),
              message = cleanText.take(120),
              targetType = "CHAT",
              targetId = currentUserId
            )
          }
          .onFailure { notifyUser(it.localizedMessage ?: "Mesaj gönderilemedi") }
      } else {
        val chatId = buildDirectChatId("local", peerId)
        localLibraryDao.insertDirectMessage(
          LocalDirectMessageEntity(
            id = sanitizeFirestoreId("msg_${UUID.randomUUID().toString().take(12)}"),
            chatId = chatId,
            peerId = peerId,
            peerName = peer.displayName.ifBlank { "Dinleyici" },
            peerHandle = peer.handle.ifBlank { "@vizyon" },
            peerAvatarUrl = peer.photoUrl,
            senderId = "local",
            senderName = myName,
            senderHandle = myHandle,
            senderAvatarUrl = myAvatar,
            text = cleanText,
            messageType = if (attachedTrack != null) DirectMessageType.TRACK.name else DirectMessageType.TEXT.name,
            trackId = attachedTrack?.trackId.orEmpty(),
            trackTitle = attachedTrack?.title.orEmpty(),
            artistName = attachedTrack?.artistName.orEmpty(),
            albumArtUrl = attachedTrack?.artworkUrl.orEmpty(),
            youtubeVideoId = attachedTrack?.youtubeVideoId.orEmpty(),
            source = attachedTrack?.source?.name.orEmpty()
          )
        )
      }
    }
  }

  fun sendVoiceDirectMessage(
    audioBase64: String,
    durationMs: Long,
    caption: String = ""
  ) {
    val peer = _activeChatPeer.value ?: return
    val rawPeerId = peer.userId.ifBlank { peer.id }
    if (rawPeerId.isBlank() || audioBase64.isBlank()) return
    val peerId = sanitizeFirestoreId(rawPeerId)
    if (peerId in currentBlockedIdSet()) {
      notifyUser("Engellenen kullanıcıya sesli mesaj gönderilemez.")
      return
    }
    if (ChatPrivacy.fromRaw(peer.chatPrivacy) == ChatPrivacy.FOLLOWERS_ONLY && !isConnectedWithPeer(peerId)) {
      sendMessageRequest(
        peer = peer,
        previewText = caption,
        messageType = DirectMessageType.VOICE,
        audioBase64 = audioBase64,
        audioDurationMs = durationMs
      )
      return
    }

    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    val secLabel = ((durationMs + 500L) / 1000L).coerceIn(1L, 30L).toString().padStart(2, '0')
    val cleanText = caption.trim().ifBlank { "🎤 Sesli mesaj (0:$secLabel)" }

    StudioSoundEngine.playMessageSendChime(appContext)
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .sendDirectMessage(
            senderId = currentUserId,
            senderName = myName,
            senderHandle = myHandle,
            senderAvatarUrl = myAvatar,
            peer = peer,
            text = cleanText,
            messageType = DirectMessageType.VOICE,
            audioBase64 = audioBase64,
            audioDurationMs = durationMs
          )
          .onSuccess {
            firestoreRepo.sendNotification(
              recipientUserId = peerId,
              senderId = currentUserId,
              senderName = myName,
              senderHandle = myHandle,
              senderAvatarUrl = myAvatar,
              type = NotificationType.VOICE_MESSAGE,
              message = cleanText.take(120),
              targetType = "CHAT",
              targetId = currentUserId
            )
          }
          .onFailure { notifyUser(it.localizedMessage ?: "Sesli mesaj gönderilemedi") }
      } else {
        val chatId = buildDirectChatId("local", peerId)
        localLibraryDao.insertDirectMessage(
          LocalDirectMessageEntity(
            id = sanitizeFirestoreId("msg_${UUID.randomUUID().toString().take(12)}"),
            chatId = chatId,
            peerId = peerId,
            peerName = peer.displayName.ifBlank { "Dinleyici" },
            peerHandle = peer.handle.ifBlank { "@vizyon" },
            peerAvatarUrl = peer.photoUrl,
            senderId = "local",
            senderName = myName,
            senderHandle = myHandle,
            senderAvatarUrl = myAvatar,
            text = cleanText,
            messageType = DirectMessageType.VOICE.name,
            audioBase64 = audioBase64,
            audioDurationMs = durationMs
          )
        )
      }
    }
  }

  fun deleteDirectChat(peerId: String) {
    if (peerId.isBlank()) return
    val safePeerId = sanitizeFirestoreId(peerId)
    if (safePeerId.isBlank()) return
    val openPeerId = _activeChatPeer.value?.let {
      val raw = it.userId.ifBlank { it.id }
      if (raw.isBlank()) null else sanitizeFirestoreId(raw)
    }
    if (openPeerId == safePeerId) {
      _activeChatPeer.value = null
    }
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        val chatId = buildDirectChatId(currentUserId, safePeerId)
        firestoreRepo
          .deleteDirectChatThread(currentUserId, chatId)
          .onSuccess { notifyUser("Sohbet kalıcı olarak silindi.") }
          .onFailure { notifyUser(it.localizedMessage ?: "Sohbet silinemedi") }
      } else {
        val chatId = buildDirectChatId("local", safePeerId)
        localLibraryDao.deleteDirectMessagesForChat(chatId)
        notifyUser("Sohbet kalıcı olarak silindi.")
      }
    }
  }

  fun deleteDirectMessage(peerId: String, messageId: String) {
    if (peerId.isBlank() || messageId.isBlank()) return
    val safePeerId = sanitizeFirestoreId(peerId)
    val safeMsgId = sanitizeFirestoreId(messageId)
    if (safePeerId.isBlank() || safeMsgId.isBlank()) return
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        val chatId = buildDirectChatId(currentUserId, safePeerId)
        firestoreRepo
          .deleteDirectMessage(currentUserId, chatId, safeMsgId)
          .onSuccess { notifyUser("Mesaj silindi.") }
          .onFailure { notifyUser(it.localizedMessage ?: "Mesaj silinemedi") }
      } else {
        localLibraryDao.deleteDirectMessageById(safeMsgId)
        notifyUser("Mesaj silindi.")
      }
    }
  }

  fun playPublicProfileLastTrack(profile: PublicProfile) {
    val track = profile.toLastListenedTrack()
    if (track == null) {
      notifyUser("${profile.displayName} henüz son dinlenen parça kaydetmemiş.")
      return
    }
    playTrack(track)
  }

  fun openActivityComments(activityId: String) {
    _selectedCommentActivityId.value = sanitizeFirestoreId(activityId)
  }

  fun closeActivityComments() {
    _selectedCommentActivityId.value = null
  }

  fun publishQuickSocialPost(note: String, selectedTrack: MusicTrack?) {
    val cleanNote = note.trim().take(240)
    if (cleanNote.isBlank() && selectedTrack == null) {
      notifyUser("Lütfen bir durum notu yaz veya parça seç.")
      return
    }
    val profileName =
      (userProfileState.value as? UiState.Success)?.data?.displayName?.ifBlank { "Vizyon Dinleyicisi" }
        ?: "Vizyon Dinleyicisi"
    val resolvedTrack =
      selectedTrack
        ?: MusicTrack(
          trackId = "status_note",
          title = "Durum Güncellemesi",
          artistId = "vizyon_studio",
          artistName = profileName,
          source = TrackSource.HYBRID
        )
    shareTrackToActivityFeed(resolvedTrack, cleanNote)
  }

  fun shareTrackToActivityFeed(track: MusicTrack, note: String) {
    val profile = (userProfileState.value as? UiState.Success)?.data
    val userName = profile?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val userHandle = profile?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val avatar = resolvedCurrentUserAvatar()
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .publishActivity(
            userId = currentUserId,
            userName = userName,
            userHandle = userHandle,
            userAvatarUrl = avatar,
            actionType = ActivityActionType.SHARED_TRACK,
            track = track,
            note = note
          )
          .onSuccess { notifyUser("Akışta paylaşıldı!") }
          .onFailure { notifyUser(it.localizedMessage ?: "Paylaşılamadı") }
      } else {
        localLibraryDao.insertActivity(
          LocalActivityEntity(
            id = sanitizeFirestoreId("act_${UUID.randomUUID().toString().take(12)}"),
            userName = userName,
            userHandle = userHandle,
            userAvatarUrl = avatar,
            actionType = ActivityActionType.SHARED_TRACK.name,
            trackId = sanitizeFirestoreId(track.trackId),
            trackTitle = track.title,
            artistId = sanitizeFirestoreId(track.artistId),
            artistName = track.artistName,
            albumArtUrl = track.artworkUrl,
            youtubeVideoId = track.youtubeVideoId,
            source = track.source.name,
            note = note.trim().take(240),
            likesCount = 0L
          )
        )
        notifyUser("Akışta paylaşıldı!")
      }
    }
  }

  fun likeActivity(item: ActivityFeedItem) {
    _likedActivityIdsInSession.value = _likedActivityIdsInSession.value + item.id
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.likeActivityItem(item.id, item.likesCount)
          .onSuccess {
            if (item.userId.isNotBlank() && item.userId != currentUserId) {
              firestoreRepo.sendNotification(
                recipientUserId = item.userId,
                senderId = currentUserId,
                senderName = myName,
                senderHandle = myHandle,
                senderAvatarUrl = myAvatar,
                type = NotificationType.LIKE,
                trackId = item.trackId,
                trackTitle = item.trackTitle,
                message = "Paylaştığın '${item.trackTitle}' parçasını beğendi.",
                targetType = "POST",
                targetId = item.id
              )
            }
          }
      } else {
        localLibraryDao.incrementActivityLike(item.id)
      }
    }
  }

  fun submitActivityComment(activity: ActivityFeedItem, rawText: String) {
    val cleanText = rawText.trim().take(280)
    if (cleanText.isEmpty()) return
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .addActivityComment(
            activityId = activity.id,
            userId = currentUserId,
            userName = myName,
            userHandle = myHandle,
            userAvatarUrl = myAvatar,
            text = cleanText
          )
          .onSuccess {
            if (activity.userId.isNotBlank() && activity.userId != currentUserId) {
              firestoreRepo.sendNotification(
                recipientUserId = activity.userId,
                senderId = currentUserId,
                senderName = myName,
                senderHandle = myHandle,
                senderAvatarUrl = myAvatar,
                type = NotificationType.COMMENT,
                trackId = activity.trackId,
                trackTitle = activity.trackTitle,
                message = "Paylaşımına yorum yaptı: ${cleanText.take(90)}",
                targetType = "POST",
                targetId = activity.id
              )
            }
          }
          .onFailure { notifyUser(it.localizedMessage ?: "Yorum gönderilemedi") }
      } else {
        val safeId = sanitizeFirestoreId(activity.id)
        localLibraryDao.insertActivityComment(
          LocalActivityCommentEntity(
            id = sanitizeFirestoreId("cmt_${UUID.randomUUID().toString().take(12)}"),
            activityId = safeId,
            userName = myName,
            userHandle = myHandle,
            userAvatarUrl = myAvatar,
            text = cleanText
          )
        )
        localLibraryDao.incrementActivityComment(safeId)
      }
    }
  }

  fun repostActivity(original: ActivityFeedItem, quoteNote: String) {
    val me = (userProfileState.value as? UiState.Success)?.data
    val myName = me?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val myHandle = me?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val myAvatar = resolvedCurrentUserAvatar()
    val cleanNote = quoteNote.trim().take(240)
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo
          .repostActivityItem(
            original = original,
            userId = currentUserId,
            userName = myName,
            userHandle = myHandle,
            userAvatarUrl = myAvatar,
            quoteNote = cleanNote
          )
          .onSuccess {
            notifyUser("Akışında yeniden paylaşıldı!")
            if (original.userId.isNotBlank() && original.userId != currentUserId) {
              firestoreRepo.sendNotification(
                recipientUserId = original.userId,
                senderId = currentUserId,
                senderName = myName,
                senderHandle = myHandle,
                senderAvatarUrl = myAvatar,
                type = NotificationType.REPOST,
                trackId = original.trackId,
                trackTitle = original.trackTitle,
                message = "'${original.trackTitle}' paylaşımını yeniden paylaştı.",
                targetType = "POST",
                targetId = original.id
              )
            }
          }
          .onFailure { notifyUser(it.localizedMessage ?: "Yeniden paylaşılamadı") }
      } else {
        localLibraryDao.insertActivity(
          LocalActivityEntity(
            id = sanitizeFirestoreId("act_${UUID.randomUUID().toString().take(12)}"),
            userName = myName,
            userHandle = myHandle,
            userAvatarUrl = myAvatar,
            actionType = ActivityActionType.REPOSTED_TRACK.name,
            trackId = sanitizeFirestoreId(original.trackId),
            trackTitle = original.trackTitle,
            artistId = sanitizeFirestoreId(original.artistId),
            artistName = original.artistName,
            albumArtUrl = original.albumArtUrl,
            youtubeVideoId = original.youtubeVideoId,
            source = original.source,
            note = cleanNote.ifBlank { original.note },
            likesCount = 0L,
            repostedFromName = original.userName.ifBlank { "Dinleyici" },
            repostedFromHandle = original.userHandle.ifBlank { "@vizyon" }
          )
        )
        localLibraryDao.incrementActivityRepost(sanitizeFirestoreId(original.id))
        notifyUser("Akışında yeniden paylaşıldı!")
      }
    }
  }

  fun deleteActivityPost(event: ActivityFeedItem) {
    val uid = currentUserId
    val isOwner = event.userId == "local" || (!uid.isNullOrBlank() && event.userId == uid)
    if (!isOwner) {
      notifyUser("Yalnızca kendi paylaşımlarını silebilirsin.")
      return
    }
    viewModelScope.launch {
      if (!uid.isNullOrBlank()) {
        firestoreRepo
          .deleteActivityItem(uid, event.id)
          .onSuccess { notifyUser("Paylaşım silindi.") }
          .onFailure { notifyUser(it.localizedMessage ?: "Paylaşım silinemedi") }
      } else {
        localLibraryDao.deleteCommentsForActivity(event.id)
        localLibraryDao.deleteActivity(event.id)
        notifyUser("Paylaşım silindi.")
      }
    }
  }

  /**
   * Updates only the listener's profile listening state and last 5 tracks; never injects cards into Akış.
   */
  private fun updateProfileListeningStatusOnly(track: MusicTrack) {
    val cfg = appearanceState.value
    val profile = (userProfileState.value as? UiState.Success)?.data
    val shareAllowed = cfg.shareListeningActivity && (profile?.shareActivity ?: true)
    if (!shareAllowed) return

    viewModelScope.launch {
      val updatedRecent = appearancePrefs.recordRecentTrack(track)
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.updateLiveListeningStatus(currentUserId, track, updatedRecent)
      }
    }
  }

  fun publishStory(track: MusicTrack, note: String, theme: StoryCardTheme) {
    val profile = (userProfileState.value as? UiState.Success)?.data
    val userName = profile?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
    val userHandle = profile?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
    val avatar = resolvedCurrentUserAvatar()
    val cleanNote = note.trim().take(140)
    val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L

    val uid = currentUserId
    viewModelScope.launch {
      if (!uid.isNullOrBlank()) {
        firestoreRepo
          .publishStory(
            userId = uid,
            userName = userName,
            userHandle = userHandle,
            userAvatarUrl = avatar,
            track = track,
            note = cleanNote,
            cardTheme = theme
          )
          .onSuccess { notifyUser("Hikayen paylaşıldı!") }
          .onFailure { notifyUser(it.localizedMessage ?: "Hikaye paylaşılamadı") }
      } else {
        localLibraryDao.insertStory(
          LocalStoryEntity(
            id = sanitizeFirestoreId("story_${UUID.randomUUID().toString().take(12)}"),
            userName = userName,
            userHandle = userHandle,
            userAvatarUrl = avatar,
            trackId = sanitizeFirestoreId(track.trackId),
            trackTitle = track.title,
            artistId = sanitizeFirestoreId(track.artistId),
            artistName = track.artistName,
            albumArtUrl = track.artworkUrl,
            youtubeVideoId = track.youtubeVideoId,
            source = track.source.name,
            note = cleanNote,
            cardTheme = theme.name,
            expiresAtMillis = expiresAt
          )
        )
        notifyUser("Hikayen paylaşıldı!")
      }
    }
  }

  fun deleteStory(storyId: String) {
    val uid = currentUserId
    viewModelScope.launch {
      if (!uid.isNullOrBlank()) {
        firestoreRepo.deleteStory(uid, storyId)
      } else {
        localLibraryDao.deleteStory(storyId)
      }
      notifyUser("Hikaye kaldırıldı")
    }
  }

  fun handleNotificationTap(notification: UserNotification) {
    if (notification.id.isNotBlank()) {
      markNotificationRead(notification.id)
    }
    val nType = NotificationType.fromRaw(notification.type)
    val target =
      when (notification.targetType.uppercase()) {
        "REQUESTS" -> NotificationDeepLinkTarget.OpenRequests
        "CHAT" ->
          if (notification.targetId.isNotBlank()) NotificationDeepLinkTarget.OpenChat(notification.targetId)
          else if (notification.senderId.isNotBlank()) NotificationDeepLinkTarget.OpenChat(notification.senderId)
          else NotificationDeepLinkTarget.OpenFeed
        "POST" ->
          if (notification.targetId.isNotBlank()) NotificationDeepLinkTarget.OpenPostComments(notification.targetId)
          else NotificationDeepLinkTarget.OpenFeed
        "PROFILE" ->
          if (notification.targetId.isNotBlank()) NotificationDeepLinkTarget.OpenListenerProfile(notification.targetId)
          else if (notification.senderId.isNotBlank()) NotificationDeepLinkTarget.OpenListenerProfile(notification.senderId)
          else NotificationDeepLinkTarget.OpenFeed
        "STORY" ->
          if (notification.targetId.isNotBlank()) NotificationDeepLinkTarget.OpenStory(notification.targetId, notification.trackId)
          else NotificationDeepLinkTarget.OpenFeed
        else ->
          when (nType) {
            NotificationType.FOLLOW_REQUEST, NotificationType.MESSAGE_REQUEST ->
              NotificationDeepLinkTarget.OpenRequests
            NotificationType.MESSAGE, NotificationType.VOICE_MESSAGE ->
              if (notification.senderId.isNotBlank()) NotificationDeepLinkTarget.OpenChat(notification.senderId)
              else NotificationDeepLinkTarget.OpenFeed
            NotificationType.FOLLOW ->
              if (notification.senderId.isNotBlank()) NotificationDeepLinkTarget.OpenListenerProfile(notification.senderId)
              else NotificationDeepLinkTarget.OpenFeed
            NotificationType.STORY ->
              if (notification.targetId.isNotBlank()) NotificationDeepLinkTarget.OpenStory(notification.targetId, notification.trackId)
              else NotificationDeepLinkTarget.OpenFeed
            NotificationType.COMMENT, NotificationType.REPOST, NotificationType.LIKE ->
              if (notification.targetId.isNotBlank()) NotificationDeepLinkTarget.OpenPostComments(notification.targetId)
              else NotificationDeepLinkTarget.OpenFeed
          }
      }
    _pendingDeepLink.value = target
  }

  fun handleNotificationIntent(intent: Intent?) {
    if (intent == null) return
    val notifId = intent.getStringExtra(VizyonNotificationHelper.EXTRA_NOTIF_ID).orEmpty()
    val targetType = intent.getStringExtra(VizyonNotificationHelper.EXTRA_NOTIF_TARGET_TYPE).orEmpty()
    val targetId = intent.getStringExtra(VizyonNotificationHelper.EXTRA_NOTIF_TARGET_ID).orEmpty()
    val trackId = intent.getStringExtra(VizyonNotificationHelper.EXTRA_NOTIF_TRACK_ID).orEmpty()
    val senderId = intent.getStringExtra(VizyonNotificationHelper.EXTRA_NOTIF_SENDER_ID).orEmpty()
    val typeRaw = intent.getStringExtra(VizyonNotificationHelper.EXTRA_NOTIF_TYPE).orEmpty()
    if (notifId.isBlank() && targetType.isBlank()) return

    if (notifId.isNotBlank()) {
      markNotificationRead(notifId)
    }
    val fakeNotif =
      UserNotification(
        id = notifId,
        senderId = senderId,
        type = typeRaw.ifBlank { NotificationType.LIKE.name },
        trackId = trackId,
        targetType = targetType,
        targetId = targetId
      )
    handleNotificationTap(fakeNotif)
    intent.removeExtra(VizyonNotificationHelper.EXTRA_NOTIF_ID)
    intent.removeExtra(VizyonNotificationHelper.EXTRA_NOTIF_TARGET_TYPE)
  }

  fun consumePendingDeepLink() {
    _pendingDeepLink.value = null
  }

  fun markNotificationRead(notificationId: String) {
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.markNotificationRead(currentUserId, notificationId)
      } else {
        localLibraryDao.markNotificationRead(notificationId)
      }
    }
  }

  fun markAllNotificationsRead() {
    val ids =
      (notificationsState.value as? UiState.Success)?.data
        ?.filter { !it.read }
        ?.map { it.id }
        .orEmpty()
    if (ids.isEmpty()) return
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.markAllNotificationsRead(currentUserId, ids)
      } else {
        localLibraryDao.markAllNotificationsRead()
      }
    }
  }

  fun deleteNotification(notificationId: String) {
    if (notificationId.isBlank()) return
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.deleteNotification(currentUserId, notificationId)
          .onSuccess {
            appContext?.let { VizyonNotificationHelper.cancelSystemNotification(it, notificationId) }
            notifyUser("Bildirim silindi")
          }
          .onFailure {
            notifyUser(it.localizedMessage ?: "Bildirim silinemedi")
          }
      } else {
        localLibraryDao.deleteNotificationById(notificationId)
        appContext?.let { VizyonNotificationHelper.cancelSystemNotification(it, notificationId) }
        notifyUser("Bildirim silindi")
      }
    }
  }

  fun clearAllNotifications() {
    val ids =
      (notificationsState.value as? UiState.Success)?.data
        ?.map { it.id }
        .orEmpty()
    viewModelScope.launch {
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.clearAllNotifications(currentUserId, ids)
          .onSuccess {
            appContext?.let { VizyonNotificationHelper.cancelAllSystemNotifications(it) }
            notifyUser("Tüm bildirimler temizlendi.")
          }
          .onFailure {
            notifyUser(it.localizedMessage ?: "Bildirimler temizlenemedi")
          }
      } else {
        localLibraryDao.deleteAllNotifications()
        appContext?.let { VizyonNotificationHelper.cancelAllSystemNotifications(it) }
        notifyUser("Tüm bildirimler temizlendi.")
      }
    }
  }

  fun updateProfilePhotoFromUri(context: Context, uri: Uri) {
    viewModelScope.launch {
      val savedOrCloud = appearancePrefs.savePickedAvatarUri(context, uri)
      if (savedOrCloud != null) {
        val cloudData = appearancePrefs.currentCloudAvatarDataUrl()
        if (!currentUserId.isNullOrBlank() && cloudData.isNotBlank()) {
          firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("photoUrl" to cloudData))
        }
        notifyUser("Profil fotoğrafın güncellendi ve bulutla eşitlendi!")
      } else {
        notifyUser("Fotoğraf seçilemedi, lütfen başka bir görsel dene.")
      }
    }
  }

  fun updateNotificationsEnabled(enabled: Boolean) =
    viewModelScope.launch {
      appearancePrefs.updateNotificationsEnabled(enabled)
      val cfg = appearanceState.value.copy(notificationsEnabled = enabled)
      syncBackgroundNotifService(cfg)
      notifyUser(if (enabled) "Sosyal bildirimler etkinleştirildi" else "Sosyal bildirimler sessize alındı")
    }

  fun updateBackgroundNotifServiceEnabled(enabled: Boolean) =
    viewModelScope.launch {
      appearancePrefs.updateBackgroundNotifServiceEnabled(enabled)
      val cfg = appearanceState.value.copy(backgroundNotifServiceEnabled = enabled)
      syncBackgroundNotifService(cfg)
      notifyUser(
        if (enabled) "Arka plan canlı bildirim servisi etkinleştirildi."
        else "Arka plan canlı bildirim servisi durduruldu."
      )
    }

  fun updateRequireFollowApproval(enabled: Boolean) =
    viewModelScope.launch {
      appearancePrefs.updateRequireFollowApproval(enabled)
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("requireFollowApproval" to enabled))
      }
      notifyUser(
        if (enabled) "Yeni takipçiler için onay isteği etkinleştirildi."
        else "Herkes doğrudan seni takip edebilir."
      )
    }

  fun updateUserProfile(displayName: String, handle: String, bio: String, shareActivity: Boolean) {
    if (currentUserId.isNullOrBlank()) {
      viewModelScope.launch {
        appearancePrefs.updateLocalProfile(displayName, handle, bio, shareActivity)
        notifyUser("Profil yerel olarak kaydedildi")
      }
      return
    }
    viewModelScope.launch {
      val cloudAvatar = appearancePrefs.currentCloudAvatarDataUrl()
      firestoreRepo
        .updateUserProfile(
          userId = currentUserId,
          displayName = displayName,
          handle = handle,
          bio = bio,
          shareActivity = shareActivity,
          photoUrl = cloudAvatar.takeIf { it.isNotBlank() }
        )
        .onSuccess {
          appearancePrefs.updateLocalProfile(displayName, handle, bio, shareActivity)
          notifyUser("Profil kaydedildi")
        }
        .onFailure { notifyUser(it.localizedMessage ?: "Profil güncellenemedi") }
    }
  }

  fun updateChatPrivacy(privacy: ChatPrivacy) {
    viewModelScope.launch {
      appearancePrefs.updateChatPrivacy(privacy)
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("chatPrivacy" to privacy.name))
      }
      notifyUser("Sohbet gizliliği: ${privacy.label}")
    }
  }

  fun updateDefaultStoryTheme(theme: StoryCardTheme) {
    viewModelScope.launch {
      appearancePrefs.updateDefaultStoryTheme(theme)
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("defaultStoryTheme" to theme.name))
      }
      notifyUser("Varsayılan hikaye kartı: ${theme.label}")
    }
  }

  fun updateFavoriteGenres(genres: List<String>) {
    viewModelScope.launch {
      val clean = genres.distinct().take(8)
      appearancePrefs.updateFavoriteGenres(clean)
      if (!currentUserId.isNullOrBlank()) {
        firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("favoriteGenres" to clean))
      }
      refreshCatalog(clean, appearanceState.value.preferredLanguage)
      notifyUser("Müzik türleri güncellendi!")
    }
  }

  fun updateThemeMode(mode: AppThemeMode) = viewModelScope.launch {
    appearancePrefs.updateThemeMode(mode)
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("preferredThemeMode" to mode.name))
    }
  }

  fun updateAccentPalette(palette: AccentPalette) = viewModelScope.launch {
    appearancePrefs.updateAccentPalette(palette)
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("preferredAccentPalette" to palette.name))
    }
  }

  fun updateCornerDensity(density: CornerDensity) = viewModelScope.launch {
    appearancePrefs.updateCornerDensity(density)
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("preferredCornerDensity" to density.name))
    }
  }

  fun updatePreferredLanguage(language: AppLanguage) = viewModelScope.launch {
    appearancePrefs.updatePreferredLanguage(language)
    if (!currentUserId.isNullOrBlank()) {
      firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("preferredLanguage" to language.name))
    }
    refreshCatalog(appearanceState.value.favoriteGenres, language)
  }

  fun updateCompactCards(compact: Boolean) = viewModelScope.launch { appearancePrefs.updateCompactCards(compact) }

  fun updateShareListening(enabled: Boolean) = viewModelScope.launch {
    appearancePrefs.updateShareListeningActivity(enabled)
    if (!currentUserId.isNullOrBlank()) {
      if (enabled) {
        firestoreRepo.updateCloudBackupFields(currentUserId, mapOf("shareActivity" to true))
      } else {
        firestoreRepo.clearPublicListeningStatus(currentUserId)
      }
    }
    notifyUser(
      if (enabled) "Son dinlediğin müzik profilinde gösterilecek."
      else "Dinlediğin son müzik gizlendi; profilin keşifte görünmeye devam ediyor."
    )
  }

  private fun resolvedCurrentUserAvatar(): String {
    val profilePhoto = (userProfileState.value as? UiState.Success)?.data?.photoUrl.orEmpty()
    val cloudAvatar = appearanceState.value.cloudAvatarDataUrl
    val localAvatar = appearanceState.value.localAvatarPath
    return when {
      cloudAvatar.isNotBlank() -> cloudAvatar
      profilePhoto.isNotBlank() -> profilePhoto
      else -> localAvatar
    }
  }

  fun notifyUser(msg: String) {
    _snackbarMessage.value = msg
  }

  fun clearSnackbar() {
    _snackbarMessage.value = null
  }

  override fun onCleared() {
    super.onCleared()
    playerManager.pauseForBackground()
  }

  private companion object {
    const val TAG = "VizyonMainVM"
  }
}
