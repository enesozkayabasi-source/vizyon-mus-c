package com.example.ui.navigation

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.MusicTrack
import com.example.model.PlaylistCoverStyle
import com.example.model.UiState
import com.example.model.sanitizeFirestoreId
import com.example.notifications.VizyonNotificationHelper
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.AnimatedWaveformBackdrop
import com.example.ui.components.DirectChatThreadSheet
import com.example.ui.components.FollowConnectionsModalSheet
import com.example.ui.components.InAppNotificationBubble
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NotificationInboxSheet
import com.example.ui.components.NowPlayingModal
import com.example.ui.components.ShareTrackDialog
import com.example.ui.components.StoryStudioSheet
import com.example.ui.components.VaultUnlockDialog
import com.example.ui.components.rememberSharedYouTubeWebView
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.ArtistProfileScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SocialFeedScreen
import com.example.ui.viewmodel.HomeCatalogData
import com.example.ui.viewmodel.VizyonMainViewModel
import kotlinx.serialization.Serializable

@Serializable object DiscoverRoute
@Serializable object SearchRoute
@Serializable object SocialFeedRoute
@Serializable object LibraryRoute
@Serializable object StudioRoute

private data class TopNavItem(
  val route: Any,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
)

private fun tabOrderFor(dest: NavDestination?): Int =
  when {
    dest == null -> 0
    dest.hasRoute<DiscoverRoute>() -> 0
    dest.hasRoute<SearchRoute>() -> 1
    dest.hasRoute<SocialFeedRoute>() -> 2
    dest.hasRoute<LibraryRoute>() -> 3
    dest.hasRoute<StudioRoute>() -> 4
    else -> 0
  }

@Composable
fun VizyonMainShell(
  currentUserId: String?,
  firebaseUserEmail: String? = null,
  viewModel: VizyonMainViewModel,
  onSignedOut: () -> Unit
) {
  val context = LocalContext.current
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDest = backStackEntry?.destination

  val appearance by viewModel.appearanceState.collectAsStateWithLifecycle()
  val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
  val searchState by viewModel.searchState.collectAsStateWithLifecycle()
  val searchSuggestions by viewModel.searchSuggestionsState.collectAsStateWithLifecycle()
  val artistDetailState by viewModel.artistDetailState.collectAsStateWithLifecycle()
  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val favoritesState by viewModel.favoritesState.collectAsStateWithLifecycle()
  val playlistsState by viewModel.playlistsState.collectAsStateWithLifecycle()
  val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
  val selectedPlaylistTracksState by viewModel.selectedPlaylistTracksState.collectAsStateWithLifecycle()
  val publicListenersState by viewModel.publicListenersState.collectAsStateWithLifecycle()
  val followingState by viewModel.followingState.collectAsStateWithLifecycle()
  val followersState by viewModel.followersState.collectAsStateWithLifecycle()
  val inspectedFollowUserId by viewModel.inspectedFollowUserId.collectAsStateWithLifecycle()
  val inspectedFollowingState by viewModel.inspectedFollowingState.collectAsStateWithLifecycle()
  val inspectedFollowersState by viewModel.inspectedFollowersState.collectAsStateWithLifecycle()
  val directChatsState by viewModel.directChatsState.collectAsStateWithLifecycle()
  val activeChatPeer by viewModel.activeChatPeer.collectAsStateWithLifecycle()
  val activeChatMessagesState by viewModel.activeChatMessagesState.collectAsStateWithLifecycle()
  val followedArtistsState by viewModel.followedArtistsState.collectAsStateWithLifecycle()
  val activityFeedState by viewModel.activityFeedState.collectAsStateWithLifecycle()
  val activityCommentsState by viewModel.activityCommentsState.collectAsStateWithLifecycle()
  val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
  val selfRecentTracks by viewModel.selfRecentTracksState.collectAsStateWithLifecycle()
  val isAutoRefreshingCatalog by viewModel.isAutoRefreshingCatalog.collectAsStateWithLifecycle()
  val pendingDeepLink by viewModel.pendingDeepLink.collectAsStateWithLifecycle()
  val likedActivityIds by viewModel.likedActivityIdsInSession.collectAsStateWithLifecycle()
  val artistFanPostsState by viewModel.artistFanPostsState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
  val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
  val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
  val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
  val openNowPlayingNonce by viewModel.openNowPlayingNonce.collectAsStateWithLifecycle()
  val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
  val isDeletingAccount by viewModel.isDeletingAccount.collectAsStateWithLifecycle()
  val isVaultLocked by viewModel.isVaultLocked.collectAsStateWithLifecycle()
  val vaultUnlockError by viewModel.vaultUnlockError.collectAsStateWithLifecycle()
  val activeInAppBubble by viewModel.activeInAppBubble.collectAsStateWithLifecycle()

  var showNowPlaying by remember { mutableStateOf(false) }
  var showNotificationInbox by remember { mutableStateOf(false) }
  var playlistPickerTrack by remember { mutableStateOf<MusicTrack?>(null) }
  var shareDialogTrack by remember { mutableStateOf<MusicTrack?>(null) }
  var storyStudioTrackOverlay by remember { mutableStateOf<MusicTrack?>(null) }

  LaunchedEffect(Unit) {
    VizyonNotificationHelper.ensureChannel(context)
  }

  LaunchedEffect(openNowPlayingNonce) {
    if (openNowPlayingNonce > 0 && playbackState.currentTrack != null) {
      showNowPlaying = true
    }
  }

  LaunchedEffect(pendingDeepLink) {
    if (pendingDeepLink != null) {
      navController.navigate(SocialFeedRoute) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(snackbarMessage) {
    snackbarMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearSnackbar()
    }
  }

  LaunchedEffect(playbackState.playbackMessage) {
    val msg = playbackState.playbackMessage
    if (msg != null && !showNowPlaying) {
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissPlaybackMessage()
    }
  }

  val favoriteIds =
    ((favoritesState as? UiState.Success)?.data ?: emptyList())
      .map { sanitizeFirestoreId(it.trackId.ifBlank { it.id }) }
      .toSet()

  val playlists = (playlistsState as? UiState.Success)?.data.orEmpty()

  val navItems = remember {
    listOf(
      TopNavItem(DiscoverRoute, "Keşfet", Icons.Rounded.Explore, Icons.Outlined.Explore),
      TopNavItem(SearchRoute, "Ara", Icons.Rounded.Search, Icons.Outlined.Search),
      TopNavItem(SocialFeedRoute, "Akış", Icons.Rounded.Groups, Icons.Outlined.Groups),
      TopNavItem(LibraryRoute, "Kitaplık", Icons.Rounded.LibraryMusic, Icons.Outlined.LibraryMusic),
      TopNavItem(StudioRoute, "Profil", Icons.Rounded.Person, Icons.Outlined.Person)
    )
  }

  val profile = (userProfileState as? UiState.Success)?.data
  val userName = profile?.displayName ?: "Dinleyici"
  val effectiveUserPhoto =
    appearance.cloudAvatarDataUrl.ifBlank { profile?.photoUrl.orEmpty() }.ifBlank { appearance.localAvatarPath }
  val followingCount = (followingState as? UiState.Success)?.data?.size ?: 0
  val followersCount = (followersState as? UiState.Success)?.data?.size ?: 0
  val myFollowingIds =
    ((followingState as? UiState.Success)?.data ?: emptyList())
      .map { sanitizeFirestoreId(it.targetUserId.ifBlank { it.id }) }
      .toSet()
  val myFollowerIds =
    ((followersState as? UiState.Success)?.data ?: emptyList())
      .map { sanitizeFirestoreId(it.targetUserId.ifBlank { it.id }) }
      .toSet()

  // Allocate one shared foreground WebView whenever a track is active so minimizing into the
  // compact MiniPlayer video viewport continues playback with zero interruption or overlap.
  val sharedWebView: WebView? =
    if (playbackState.currentTrack != null) {
      rememberSharedYouTubeWebView(
        initialVideoId = playbackState.activeVideoId,
        onAttachWebView = { viewModel.attachYouTubeWebView(it) },
        onIFrameReady = { viewModel.onYouTubeIFrameReady(it) },
        onIFrameStateChanged = { viewModel.onYouTubeIFrameStateChanged(it) },
        onIFrameProgress = { cur, dur -> viewModel.onYouTubeIFrameProgress(cur, dur) },
        onIFrameError = { viewModel.onYouTubeIFrameError(it) },
        onIFrameLoadFailure = { viewModel.onYouTubeIFrameLoadFailure(it) }
      )
    } else null

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useRail = maxWidth >= 700.dp

    AnimatedWaveformBackdrop(
      currentStep = if (playbackState.isPlaying) 3 else 1,
      selectedGenreCount = appearance.favoriteGenres.size
    )

    Scaffold(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onBackground,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        Column {
          if (sharedWebView != null && playbackState.currentTrack != null && !showNowPlaying) {
            MiniPlayerBar(
              playbackState = playbackState,
              sharedWebView = sharedWebView,
              onIFrameLoadFailure = { viewModel.onYouTubeIFrameLoadFailure(it) },
              onRetryVideo = { viewModel.retryCurrentTrackVideo() },
              onExpandClick = {
                val track = playbackState.currentTrack
                if (track != null) {
                  if (track.youtubeVideoId.isBlank() && playbackState.activeVideoId.isBlank()) {
                    viewModel.playTrack(track, playbackState.queue)
                  }
                  showNowPlaying = true
                }
              },
              onPlayPauseClick = { viewModel.togglePlayPause() },
              onNextClick = { viewModel.skipNext() },
              onDismissSwipe = { viewModel.stopAndDismissPlayer() }
            )
          }

          if (!useRail) {
            val dockShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            Surface(
              color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
              shape = dockShape,
              tonalElevation = 0.dp,
              shadowElevation = 0.dp,
              modifier =
                Modifier.clip(dockShape)
                  .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = dockShape
                  )
            ) {
              NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
              ) {
                navItems.forEach { item ->
                  val selected = currentDest?.hasRoute(item.route::class) == true
                  val showSocialBadge = item.route == SocialFeedRoute && unreadNotificationCount > 0
                  NavigationBarItem(
                    selected = selected,
                    onClick = {
                      navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                      unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                      unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    icon = {
                      if (showSocialBadge) {
                        BadgedBox(badge = { Badge { Text(unreadNotificationCount.coerceAtMost(9).toString()) } }) {
                          Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                          )
                        }
                      } else {
                        Icon(
                          imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                          contentDescription = item.label
                        )
                      }
                    },
                    modifier = Modifier.testTag("nav_tab_${item.label}"),
                    label = {
                      Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  )
                }
              }
            }
          }
        }
      }
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useRail) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            modifier = Modifier.fillMaxHeight()
          ) {
            navItems.forEach { item ->
              val selected = currentDest?.hasRoute(item.route::class) == true
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(item.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                colors = NavigationRailItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.primary,
                  selectedTextColor = MaterialTheme.colorScheme.primary,
                  indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                  unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                icon = {
                  Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label
                  )
                },
                label = {
                  Text(
                    text = item.label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                  )
                }
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          val smoothEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
          val navSlideSpec = tween<IntOffset>(durationMillis = 150, easing = smoothEase)
          val navEnterFade = tween<Float>(durationMillis = 150, easing = smoothEase)
          val navExitFade = tween<Float>(durationMillis = 100, easing = smoothEase)
          NavHost(
            navController = navController,
            startDestination = DiscoverRoute,
            enterTransition = {
              val dir = if (tabOrderFor(targetState.destination) >= tabOrderFor(initialState.destination)) 1 else -1
              slideInHorizontally(animationSpec = navSlideSpec) { fullWidth -> dir * (fullWidth / 32) } +
                fadeIn(animationSpec = navEnterFade)
            },
            exitTransition = {
              fadeOut(animationSpec = navExitFade)
            },
            popEnterTransition = {
              val dir = if (tabOrderFor(targetState.destination) >= tabOrderFor(initialState.destination)) 1 else -1
              slideInHorizontally(animationSpec = navSlideSpec) { fullWidth -> dir * (fullWidth / 32) } +
                fadeIn(animationSpec = navEnterFade)
            },
            popExitTransition = {
              fadeOut(animationSpec = navExitFade)
            }
          ) {
            composable<DiscoverRoute> {
              DiscoverScreen(
                catalogState = catalogState,
                userName = userName,
                userPhotoUrl = effectiveUserPhoto,
                unreadNotificationCount = unreadNotificationCount,
                preferredGenres = appearance.favoriteGenres,
                currentTrackId = playbackState.currentTrack?.trackId,
                isPlaying = playbackState.isPlaying,
                favoriteIds = favoriteIds,
                lastCatalogRefreshMs = appearance.lastCatalogRefreshMs,
                isAutoRefreshing = isAutoRefreshingCatalog,
                onRefresh = { viewModel.refreshCatalog() },
                onOpenNotifications = { showNotificationInbox = true },
                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                onArtistClick = { artist ->
                  viewModel.openArtistProfile(artist.artistId, artist.name, artist.imageUrl)
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddToQueue = { viewModel.addToQueue(it) },
                onOpenPlaylistPicker = { playlistPickerTrack = it },
                onShareTrack = { shareDialogTrack = it },
                onOpenStudio = {
                  navController.navigate(StudioRoute) { launchSingleTop = true }
                }
              )
            }
            composable<SearchRoute> {
              SearchScreen(
                searchState = searchState,
                suggestions = searchSuggestions,
                currentTrackId = playbackState.currentTrack?.trackId,
                isPlaying = playbackState.isPlaying,
                favoriteIds = favoriteIds,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSearchSubmit = { viewModel.search(it) },
                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                onArtistClick = { artist ->
                  viewModel.openArtistProfile(artist.artistId, artist.name, artist.imageUrl)
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddToQueue = { viewModel.addToQueue(it) },
                onOpenPlaylistPicker = { playlistPickerTrack = it },
                onShareTrack = { shareDialogTrack = it }
              )
            }

            composable<SocialFeedRoute> {
              val favTracks =
                ((favoritesState as? UiState.Success)?.data ?: emptyList())
                  .map { it.toMusicTrack() }
              val catalogTracks =
                (catalogState as? UiState.Success<HomeCatalogData>)?.data?.tracks.orEmpty()
              val candidateTracks =
                remember(favTracks, catalogTracks) {
                  (favTracks + catalogTracks).distinctBy { it.trackId }.take(8)
                }
              SocialFeedScreen(
                currentUserId = currentUserId,
                userName = userName,
                userHandle = profile?.handle ?: "@vizyon",
                userBio = profile?.bio ?: "",
                userPhotoUrl = effectiveUserPhoto,
                currentTrack = playbackState.currentTrack,
                candidateTracks = candidateTracks,
                selfRecentTracks = selfRecentTracks,
                defaultStoryTheme = appearance.defaultStoryTheme,
                storiesState = storiesState,
                activityFeedState = activityFeedState,
                activityCommentsState = activityCommentsState,
                publicListenersState = publicListenersState,
                followingState = followingState,
                followersCount = followersCount,
                directChatsState = directChatsState,
                likedActivityIds = likedActivityIds,
                unreadNotificationCount = unreadNotificationCount,
                pendingDeepLink = pendingDeepLink,
                onConsumeDeepLink = { viewModel.consumePendingDeepLink() },
                onOpenNotifications = { showNotificationInbox = true },
                onPublishQuickPost = { note, track -> viewModel.publishQuickSocialPost(note, track) },
                onPublishStory = { tr, note, theme -> viewModel.publishStory(tr, note, theme) },
                onDeleteStory = { viewModel.deleteStory(it) },
                onPlayActivityTrack = { viewModel.playTrack(it) },
                onLikeActivity = { viewModel.likeActivity(it) },
                onOpenComments = { viewModel.openActivityComments(it.id) },
                onCloseComments = { viewModel.closeActivityComments() },
                onSubmitComment = { activity, text -> viewModel.submitActivityComment(activity, text) },
                onRepostActivity = { activity, quote -> viewModel.repostActivity(activity, quote) },
                onDeleteActivityPost = { viewModel.deleteActivityPost(it) },
                onToggleFollowListener = { viewModel.toggleFollowListener(it) },
                onOpenDirectChat = { viewModel.openDirectChat(it) },
                onOpenDirectChatById = { viewModel.openDirectChatById(it) },
                onDeleteDirectChat = { viewModel.deleteDirectChat(it) },
                onInspectFollowLists = { viewModel.inspectFollowLists(it) },
                onArtistClick = { artist ->
                  viewModel.openArtistProfile(artist.artistId, artist.name, artist.imageUrl)
                },
                onOpenStudioForCloudSync = {
                  navController.navigate(StudioRoute) { launchSingleTop = true }
                },
                onNotifyMessage = { viewModel.notifyUser(it) }
              )
            }
            composable<LibraryRoute> {
              LibraryScreen(
                favoritesState = favoritesState,
                playlistsState = playlistsState,
                selectedPlaylistId = selectedPlaylistId,
                selectedPlaylistTracksState = selectedPlaylistTracksState,
                followedArtistsState = followedArtistsState,
                currentTrackId = playbackState.currentTrack?.trackId,
                isPlaying = playbackState.isPlaying,
                favoriteIds = favoriteIds,
                onSelectPlaylist = { viewModel.selectPlaylist(it) },
                onCreatePlaylist = { title, desc, hex, style, pub ->
                  viewModel.createPlaylist(title, desc, hex, style, pub)
                },
                onDeletePlaylist = { viewModel.deletePlaylist(it) },
                onRemoveTrackFromPlaylist = { plId, trId ->
                  viewModel.removeTrackFromPlaylist(plId, trId)
                },
                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                onArtistClick = { artist ->
                  viewModel.openArtistProfile(artist.artistId, artist.name, artist.imageUrl)
                },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAddToQueue = { viewModel.addToQueue(it) },
                onOpenPlaylistPicker = { playlistPickerTrack = it },
                onShareTrack = { shareDialogTrack = it }
              )
            }
            composable<StudioRoute> {
              val bridge = (catalogState as? UiState.Success<HomeCatalogData>)?.data?.bridgeStatus
              AppearanceSettingsScreen(
                isSignedIn = !currentUserId.isNullOrBlank(),
                firebaseUid = currentUserId,
                firebaseEmail = firebaseUserEmail,
                userPhotoUrl = effectiveUserPhoto,
                unreadNotificationCount = unreadNotificationCount,
                appearance = appearance,
                userProfileState = userProfileState,
                recentTracks = selfRecentTracks,
                bridgeStatus = bridge,
                isDeletingAccount = isDeletingAccount,
                followersCount = followersCount,
                followingCount = followingCount,
                favoritesCount = favoriteIds.size,
                onThemeModeChange = { viewModel.updateThemeMode(it) },
                onAccentChange = { viewModel.updateAccentPalette(it) },
                onCornerDensityChange = { viewModel.updateCornerDensity(it) },
                onLanguageChange = { viewModel.updatePreferredLanguage(it) },
                onShareActivityToggle = { viewModel.updateShareListening(it) },
                onNotificationsToggle = { viewModel.updateNotificationsEnabled(it) },
                onPickProfilePhoto = { uri -> viewModel.updateProfilePhotoFromUri(context, uri) },
                onOpenNotifications = { showNotificationInbox = true },
                onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
                onShareTrackAsStory = { tr -> storyStudioTrackOverlay = tr },
                onSaveProfile = { name, handle, bio, share ->
                  viewModel.updateUserProfile(name, handle, bio, share)
                },
                onSaveVaultPasscode = { passcode, rememberMe, lockOnLaunch ->
                  viewModel.updateVaultPasscode(passcode, rememberMe, lockOnLaunch)
                },
                onUpdateVaultSettings = { rememberMe, lockOnLaunch ->
                  viewModel.updateVaultSettings(rememberMe, lockOnLaunch)
                },
                onChatPrivacyChange = { viewModel.updateChatPrivacy(it) },
                onDefaultStoryThemeChange = { viewModel.updateDefaultStoryTheme(it) },
                onUpdateFavoriteGenres = { viewModel.updateFavoriteGenres(it) },
                onOpenFollowLists = { viewModel.inspectFollowLists(currentUserId ?: "local") },
                onSyncCloudBackupNow = { viewModel.syncCloudBackupNow() },
                onDeleteAccount = { bypassPreCheck, onReauth, onCompleted ->
                  viewModel.deleteAccount(
                    bypassFreshnessPreCheck = bypassPreCheck,
                    onReauthRequired = onReauth,
                    onCompleted = onCompleted
                  )
                },
                onNotifyMessage = { viewModel.notifyUser(it) },
                onSignedOut = onSignedOut
              )
            }
          }
        }
      }
    }

    // Artist Profile Overlay with crisp 160ms glide+fade
    val overlayEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
    AnimatedVisibility(
      visible = artistDetailState != null,
      enter =
        slideInHorizontally(tween(160, easing = overlayEase)) { it / 16 } +
          fadeIn(tween(160, easing = overlayEase)),
      exit =
        fadeOut(tween(110, easing = overlayEase))
    ) {
      artistDetailState?.let { detailState ->
        val followedArtists = (followedArtistsState as? UiState.Success)?.data.orEmpty()
        val currentArtistId = (detailState as? UiState.Success)?.data?.artist?.artistId.orEmpty()
        val isFollowing = followedArtists.any { sanitizeFirestoreId(it.artistId) == currentArtistId }

        ArtistProfileScreen(
          artistDetailState = detailState,
          fanPostsState = artistFanPostsState,
          isFollowingArtist = isFollowing,
          currentTrackId = playbackState.currentTrack?.trackId,
          isPlaying = playbackState.isPlaying,
          favoriteIds = favoriteIds,
          onBack = { viewModel.closeArtistProfile() },
          onToggleFollow = { viewModel.toggleFollowArtist(it) },
          onPlayTrack = { track, queue -> viewModel.playTrack(track, queue) },
          onToggleFavorite = { viewModel.toggleFavorite(it) },
          onAddToQueue = { viewModel.addToQueue(it) },
          onOpenPlaylistPicker = { playlistPickerTrack = it },
          onShareTrack = { shareDialogTrack = it },
          onPostFanMessage = { artId, msg, tag -> viewModel.postFanMessage(artId, msg, tag) },
          onCheerFanPost = { artId, post -> viewModel.cheerFanPost(artId, post) }
        )
      }
    }

    // Fullscreen Now Playing Modal sharing the active WebView cleanly with MiniPlayerBar
    AnimatedVisibility(
      visible = showNowPlaying && sharedWebView != null && playbackState.currentTrack != null,
      enter =
        slideInVertically(tween(170, easing = overlayEase)) { it / 18 } +
          fadeIn(tween(170, easing = overlayEase)),
      exit =
        slideOutVertically(tween(130, easing = overlayEase)) { it / 20 } +
          fadeOut(tween(130, easing = overlayEase))
    ) {
      val activeTrack = playbackState.currentTrack
      if (sharedWebView != null && activeTrack != null) {
        val safeId = sanitizeFirestoreId(activeTrack.trackId)
        NowPlayingModal(
          playbackState = playbackState,
          lyricsState = lyricsState,
          sharedWebView = sharedWebView,
          isFavorite = safeId in favoriteIds,
          onDismiss = { showNowPlaying = false },
          onIFrameLoadFailure = { viewModel.onYouTubeIFrameLoadFailure(it) },
          onRetryVideo = { viewModel.retryCurrentTrackVideo() },
          onPlayPauseClick = { viewModel.togglePlayPause() },
          onSeekTo = { viewModel.seekTo(it) },
          onNextClick = { viewModel.skipNext() },
          onPreviousClick = { viewModel.skipPrevious() },
          onToggleShuffle = { viewModel.toggleShuffle() },
          onCycleRepeat = { viewModel.cycleRepeat() },
          onToggleFavorite = { viewModel.toggleFavorite(it) },
          onOpenPlaylistPicker = { playlistPickerTrack = it },
          onShareTrack = { shareDialogTrack = it },
          onRefreshLyrics = { viewModel.loadLyricsForTrack(activeTrack, forceRefresh = true) },
          onArtistClick = { tr ->
            showNowPlaying = false
            viewModel.openArtistProfile(tr.artistId, tr.artistName, tr.artworkUrl)
          },
          onSelectTrackFromQueue = { tr -> viewModel.playTrack(tr, playbackState.queue) }
        )
      }
    }

    if (showNotificationInbox) {
      NotificationInboxSheet(
        notificationsState = notificationsState,
        onDismiss = { showNotificationInbox = false },
        onNotificationClick = { notif ->
          showNotificationInbox = false
          viewModel.handleNotificationTap(notif)
        },
        onMarkAllRead = { viewModel.markAllNotificationsRead() },
        onDeleteNotification = { notif -> viewModel.deleteNotification(notif.id) },
        onClearAllNotifications = { viewModel.clearAllNotifications() },
        onPermissionResultMessage = { viewModel.notifyUser(it) }
      )
    }

    playlistPickerTrack?.let { tr ->
      AddToPlaylistDialog(
        track = tr,
        playlists = playlists,
        onDismiss = { playlistPickerTrack = null },
        onSelectPlaylist = { plId -> viewModel.addTrackToPlaylist(plId, tr) },
        onCreateNewQuick = { name ->
          viewModel.createPlaylist(
            title = name,
            description = "",
            accentHex = appearance.accentPalette.hexString,
            coverStyle = PlaylistCoverStyle.CHROME,
            sharedPublic = true
          )
        }
      )
    }

    shareDialogTrack?.let { tr ->
      ShareTrackDialog(
        track = tr,
        onDismiss = { shareDialogTrack = null },
        onShareToFeed = { note -> viewModel.shareTrackToActivityFeed(tr, note) }
      )
    }

    storyStudioTrackOverlay?.let { tr ->
      val catalogTracks =
        (catalogState as? UiState.Success<HomeCatalogData>)?.data?.tracks.orEmpty()
      StoryStudioSheet(
        userName = userName,
        userHandle = profile?.handle ?: "@vizyon",
        userPhotoUrl = effectiveUserPhoto,
        initialTrack = tr,
        candidateTracks = (listOf(tr) + catalogTracks).distinctBy { it.trackId }.take(8),
        initialTheme = appearance.defaultStoryTheme,
        onDismiss = { storyStudioTrackOverlay = null },
        onPublishStory = { track, note, theme -> viewModel.publishStory(track, note, theme) },
        onNotifyMessage = { viewModel.notifyUser(it) }
      )
    }

    activeChatPeer?.let { peer ->
      val peerSafeId = sanitizeFirestoreId(peer.userId.ifBlank { peer.id })
      val favTracks =
        ((favoritesState as? UiState.Success)?.data ?: emptyList()).map { it.toMusicTrack() }
      val catalogTracks =
        (catalogState as? UiState.Success<HomeCatalogData>)?.data?.tracks.orEmpty()
      val dmCandidateTracks =
        remember(favTracks, catalogTracks) {
          (favTracks + catalogTracks).distinctBy { it.trackId }.take(8)
        }
      DirectChatThreadSheet(
        peer = peer,
        currentUserId = currentUserId,
        isFollowerOrFollowing = peerSafeId in myFollowingIds || peerSafeId in myFollowerIds,
        messagesState = activeChatMessagesState,
        currentTrack = playbackState.currentTrack,
        candidateTracks = dmCandidateTracks,
        onDismiss = { viewModel.closeDirectChat() },
        onSendMessage = { txt, tr -> viewModel.sendDirectMessage(txt, tr) },
        onPlayTrack = { tr ->
          viewModel.closeDirectChat()
          viewModel.playTrack(tr)
        },
        onDeleteChatThread = { viewModel.deleteDirectChat(peerSafeId) },
        onDeleteMessage = { msg -> viewModel.deleteDirectMessage(peerSafeId, msg.id) }
      )
    }

    inspectedFollowUserId?.let { targetUid ->
      val listeners = (publicListenersState as? UiState.Success)?.data.orEmpty()
      val targetLabel =
        if (targetUid == currentUserId || targetUid == "local") {
          "$userName • Bağlantılar"
        } else {
          listeners.firstOrNull { sanitizeFirestoreId(it.userId.ifBlank { it.id }) == targetUid }?.displayName
            ?.let { "$it • Bağlantılar" } ?: "Takip Bağlantıları"
        }
      FollowConnectionsModalSheet(
        title = targetLabel,
        followersState = inspectedFollowersState,
        followingState = inspectedFollowingState,
        currentUserId = currentUserId,
        myFollowingIds = myFollowingIds,
        onDismiss = { viewModel.closeInspectFollowLists() },
        onOpenProfileById = { uidToOpen ->
          viewModel.closeInspectFollowLists()
          if (uidToOpen == currentUserId || uidToOpen == "local") {
            navController.navigate(StudioRoute) { launchSingleTop = true }
          } else {
            viewModel.openListenerProfileById(uidToOpen)
          }
        },
        onOpenChatById = { uidToChat ->
          viewModel.closeInspectFollowLists()
          viewModel.openDirectChatById(uidToChat)
        }
      )
    }

    if (isVaultLocked) {
      VaultUnlockDialog(
        userName = userName,
        errorMessage = vaultUnlockError,
        onUnlockSubmit = { viewModel.unlockVault(it) }
      )
    }

    InAppNotificationBubble(
      notification = activeInAppBubble,
      onOpen = { notif ->
        viewModel.dismissInAppBubble()
        viewModel.handleNotificationTap(notif)
      },
      onDismiss = { viewModel.dismissInAppBubble() },
      modifier = Modifier.align(Alignment.TopCenter)
    )
  }
}
