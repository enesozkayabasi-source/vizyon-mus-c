package com.example

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.webkit.WebView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.VizyonFirestoreRepository
import com.example.data.local.AppearancePreferences
import com.example.data.local.LocalNotificationEntity
import com.example.data.local.VizyonDatabase
import com.example.data.remote.HybridMusicRepository
import com.example.model.AccentPalette
import com.example.model.AppAppearanceConfig
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.MusicTrack
import com.example.model.NotificationType
import com.example.model.PlaylistCoverStyle
import com.example.model.PublicProfile
import com.example.model.ReportReason
import com.example.model.StoryCardTheme
import com.example.model.TrackLyrics
import com.example.model.TrackSource
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.notifications.NotificationDispatchGuard
import com.example.notifications.VizyonFirebaseMessagingService
import com.example.notifications.VizyonNotificationHelper
import com.example.player.PlaybackUiState
import com.example.player.RepeatMode
import com.example.player.YouTubeForegroundPlayerManager
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingModal
import com.example.ui.screens.AppearanceSettingsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.VizyonMusicTheme
import com.example.ui.viewmodel.NotificationDeepLinkTarget
import com.example.ui.viewmodel.VizyonMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ComprehensiveFlowVerificationRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var app: Application
  private lateinit var prefs: AppearancePreferences
  private lateinit var roomDb: VizyonDatabase
  private lateinit var playerManager: YouTubeForegroundPlayerManager
  private val activeViewModels = mutableListOf<VizyonMainViewModel>()

  @Before
  fun setUp() {
    app = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(app).isEmpty()) {
      FirebaseApp.initializeApp(app)
    }
    Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    prefs = AppearancePreferences(app)
    roomDb = VizyonDatabase.getInstance(app)
    playerManager = YouTubeForegroundPlayerManager()

    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancelAll()
    VizyonNotificationHelper.clearAllHistory(app)
    NotificationDispatchGuard.clearForUser(app, "guest")
    NotificationDispatchGuard.setLastActiveUserId(app, null)
    runBlocking {
      roomDb.localLibraryDao().clearAllUserData()
      prefs.resetAllForAccountDeletion()
    }
  }

  @After
  fun tearDown() {
    activeViewModels.clear()
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancelAll()
    VizyonNotificationHelper.clearAllHistory(app)
    runBlocking {
      roomDb.localLibraryDao().clearAllUserData()
      prefs.resetAllForAccountDeletion()
    }
  }

  private fun createGuestViewModel(): VizyonMainViewModel {
    val dbId = app.getString(R.string.firestore_database_id)
    val firestoreRepo = VizyonFirestoreRepository(FirebaseFirestore.getInstance(dbId), Firebase.auth)
    val vm =
      VizyonMainViewModel(
        currentUserId = null,
        firestoreRepo = firestoreRepo,
        localLibraryDao = roomDb.localLibraryDao(),
        hybridMusicRepo = HybridMusicRepository(),
        playerManager = playerManager,
        appearancePrefs = prefs,
        appContext = app
      )
    activeViewModels.add(vm)
    return vm
  }

  private fun pumpMainUntil(timeoutMs: Long = 5000L, predicate: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    val mainLooper = Shadows.shadowOf(Looper.getMainLooper())
    while (System.currentTimeMillis() < deadline) {
      mainLooper.idle()
      if (predicate()) return
      Thread.sleep(20L)
    }
    mainLooper.idle()
    assertTrue("Timed out after ${timeoutMs}ms waiting for condition", predicate())
  }

  @Test
  fun fcmService_suppressesSelfBlockedAndDisabledPrefs_andRoutesAllChannels() {
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = Shadows.shadowOf(nm)
    val recipientId = "u_me_fcm"
    NotificationDispatchGuard.clearForUser(app, recipientId)
    NotificationDispatchGuard.setLastActiveUserId(app, recipientId)

    val service = Robolectric.setupService(VizyonFirebaseMessagingService::class.java)

    // 1) Disabled notificationsEnabled suppresses FCM delivery
    runBlocking { prefs.updateNotificationsEnabled(false) }
    service.onMessageReceived(
      RemoteMessage.Builder("fcm@gcm.googleapis.com")
        .addData("notificationId", "n_sup_off_1")
        .addData("recipientId", recipientId)
        .addData("senderId", "u_other")
        .addData("senderName", "Ali")
        .addData("type", NotificationType.MESSAGE.name)
        .addData("message", "Bildirim kapalıyken gelmemeli")
        .build()
    )
    assertEquals(0, shadowNm.allNotifications.size)

    // 2) Disabled backgroundNotifServiceEnabled suppresses FCM delivery
    runBlocking {
      prefs.updateNotificationsEnabled(true)
      prefs.updateBackgroundNotifServiceEnabled(false)
    }
    service.onMessageReceived(
      RemoteMessage.Builder("fcm@gcm.googleapis.com")
        .addData("notificationId", "n_sup_off_2")
        .addData("recipientId", recipientId)
        .addData("senderId", "u_other")
        .addData("senderName", "Ali")
        .addData("type", NotificationType.MESSAGE.name)
        .addData("message", "Push anahtarı kapalıyken gelmemeli")
        .build()
    )
    assertEquals(0, shadowNm.allNotifications.size)

    // Re-enable both flags
    runBlocking {
      prefs.updateNotificationsEnabled(true)
      prefs.updateBackgroundNotifServiceEnabled(true)
    }

    // 3) Self-sent notification must be suppressed
    service.onMessageReceived(
      RemoteMessage.Builder("fcm@gcm.googleapis.com")
        .addData("notificationId", "n_sup_self")
        .addData("recipientId", recipientId)
        .addData("senderId", recipientId)
        .addData("senderName", "Kendim")
        .addData("type", NotificationType.MESSAGE.name)
        .addData("message", "Kendi mesajım")
        .build()
    )
    assertEquals(0, shadowNm.allNotifications.size)

    // 4) Blocked sender notification must be suppressed
    NotificationDispatchGuard.setBlockedUserIds(app, recipientId, setOf("u_blocked_sender"))
    service.onMessageReceived(
      RemoteMessage.Builder("fcm@gcm.googleapis.com")
        .addData("notificationId", "n_sup_blocked")
        .addData("recipientId", recipientId)
        .addData("senderId", "u_blocked_sender")
        .addData("senderName", "Engelli Kişi")
        .addData("type", NotificationType.MESSAGE.name)
        .addData("message", "Engelli kişiden bildirim")
        .build()
    )
    assertEquals(0, shadowNm.allNotifications.size)

    // 5) Distinct notification types route to their dedicated channels
    val typeChannelPairs =
      listOf(
        NotificationType.FOLLOW to VizyonNotificationHelper.CHANNEL_FOLLOWS,
        NotificationType.LIKE to VizyonNotificationHelper.CHANNEL_REACTIONS,
        NotificationType.STORY to VizyonNotificationHelper.CHANNEL_STORIES,
        NotificationType.VOICE_MESSAGE to VizyonNotificationHelper.CHANNEL_MESSAGES
      )
    typeChannelPairs.forEachIndexed { idx, (type, expectedChannel) ->
      service.onMessageReceived(
        RemoteMessage.Builder("fcm@gcm.googleapis.com")
          .addData("notificationId", "n_route_${idx + 1}")
          .addData("recipientId", recipientId)
          .addData("senderId", "u_friend_${idx + 1}")
          .addData("senderName", "Arkadaş ${idx + 1}")
          .addData("type", type.name)
          .addData("message", "Tip testi ${type.name}")
          .build()
      )
      assertNotNull(nm.getNotificationChannel(expectedChannel))
    }
    val postedChannels = shadowNm.allNotifications.map { it.channelId }.toSet()
    assertTrue(VizyonNotificationHelper.CHANNEL_FOLLOWS in postedChannels)
    assertTrue(VizyonNotificationHelper.CHANNEL_REACTIONS in postedChannels)
    assertTrue(VizyonNotificationHelper.CHANNEL_STORIES in postedChannels)
    assertTrue(VizyonNotificationHelper.CHANNEL_MESSAGES in postedChannels)

    // 6) onNewToken caches token cleanly in NotificationDispatchGuard
    service.onNewToken("fcm_token_verified_xyz")
    assertEquals("fcm_token_verified_xyz", NotificationDispatchGuard.getCachedFcmToken(app))

    // 7) Closed-app fast path must NOT block on unreachable HTTPS avatar URL and preserves MessagingStyle
    val startMs = System.currentTimeMillis()
    service.onMessageReceived(
      RemoteMessage.Builder("fcm@gcm.googleapis.com")
        .addData("notificationId", "n_fast_avatar")
        .addData("recipientId", recipientId)
        .addData("senderId", "u_fast_sender")
        .addData("senderName", "Arda")
        .addData("senderAvatarUrl", "https://10.255.255.1/unreachable_avatar.png")
        .addData("type", NotificationType.MESSAGE.name)
        .addData("channelId", VizyonNotificationHelper.CHANNEL_MESSAGES)
        .addData("message", "Kapalıyken anında teslim edildi")
        .build()
    )
    val elapsedMs = System.currentTimeMillis() - startMs
    assertTrue("Closed-app FCM fast path took too long: ${elapsedMs}ms", elapsedMs < 900L)
    val ardaNotif =
      shadowNm.allNotifications.lastOrNull {
        it.extras?.getCharSequence("android.title")?.toString() == "Arda" ||
          androidx.core.app.NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) != null
      }
    assertNotNull(ardaNotif)

    // 8) Guest VM startup must NOT wipe previously signed-in lastActiveUserId
    NotificationDispatchGuard.setLastActiveUserId(app, recipientId)
    val bgVm = createGuestViewModel()
    assertEquals(recipientId, NotificationDispatchGuard.getLastActiveUserId(app))

    // 9) Background processIncomingNotifications delivers immediately without network stall
    NotificationDispatchGuard.markBaselineSeen(app, "guest", listOf("n_seed_bg"))
    bgVm.pauseForBackground()
    val bgNotif =
      com.example.model.UserNotification(
        id = "n_bg_instant_1",
        senderId = "u_bg_sender",
        senderName = "Mert",
        senderAvatarUrl = "https://10.255.255.1/unreachable_bg.png",
        type = NotificationType.MESSAGE.name,
        message = "Arka plan anlık bildirimi",
        read = false
      )
    val bgStartMs = System.currentTimeMillis()
    bgVm.processIncomingNotifications(listOf(bgNotif))
    val bgElapsedMs = System.currentTimeMillis() - bgStartMs
    assertTrue("Background notification delivery stalled: ${bgElapsedMs}ms", bgElapsedMs < 900L)
    assertTrue(NotificationDispatchGuard.isNotificationSeen(app, "guest", "n_bg_instant_1"))
    assertTrue(
      shadowNm.allNotifications.any {
        androidx.core.app.NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
          ?.messages?.any { m -> m.text?.toString()?.contains("Arka plan anlık bildirimi") == true } == true
      }
    )
  }

  @Test
  fun viewModel_onboardingAndVaultPasscodeLock_enforcesUnlockAndPasscodeUpdate() {
    val vm1 = createGuestViewModel()
    vm1.completeOnboarding(
      displayName = "Zeynep Aksoy",
      handle = "zeynepak",
      bio = "Indie & Anadolu Rock",
      favoriteGenres = listOf("Türkçe Pop", "Anadolu Rock"),
      ageVerified = true,
      preferredLanguage = AppLanguage.TURKISH,
      themeMode = AppThemeMode.NIGHT_AMOLED,
      shareActivity = true,
      accentPalette = AccentPalette.VIZYON_BLUE,
      cornerDensity = CornerDensity.BALANCED,
      defaultStoryTheme = StoryCardTheme.NEON_WAVE,
      chatPrivacy = ChatPrivacy.PUBLIC,
      passcode = "4321",
      rememberMe = true,
      lockOnLaunch = true,
      requireFollowApproval = false,
      backgroundNotifServiceEnabled = true
    )

    pumpMainUntil {
      val cfg = runBlocking { prefs.configFlow.first() }
      cfg.onboardingCompleted && cfg.hasVaultPasscode && cfg.lockOnLaunch
    }

    // Cold relaunch with lockOnLaunch=true + passcode must lock the vault initially
    val vm2 = createGuestViewModel()
    pumpMainUntil { vm2.isVaultLocked.value }
    assertTrue(vm2.isVaultLocked.value)

    // Attempting passcode update while locked must be rejected
    vm2.updateVaultPasscode("9999", rememberMe = true, lockOnLaunch = false)
    assertTrue(vm2.snackbarMessage.value?.contains("kilitliyken", ignoreCase = true) == true)

    // Wrong passcode fails and sets error
    vm2.unlockVault("0000")
    pumpMainUntil { vm2.vaultUnlockError.value != null }
    assertTrue(vm2.isVaultLocked.value)

    // Correct passcode unlocks cleanly
    vm2.unlockVault("4321")
    pumpMainUntil { !vm2.isVaultLocked.value }
    assertNull(vm2.vaultUnlockError.value)

    // Too-short passcode (<4 chars) rejected after unlock
    vm2.updateVaultPasscode("12", rememberMe = true, lockOnLaunch = false)
    pumpMainUntil { vm2.snackbarMessage.value?.contains("en az 4", ignoreCase = true) == true }

    // Valid new passcode updates cleanly
    vm2.updateVaultPasscode("9876", rememberMe = true, lockOnLaunch = false)
    pumpMainUntil {
      runBlocking { prefs.verifyLocalPasscode("9876") }
    }
    assertTrue(runBlocking { prefs.verifyLocalPasscode("9876") })
  }

  @Test
  fun viewModel_favoritesPlaylistsAndDeletion_keepRoomConsistentWithNoOrphans() {
    val vm = createGuestViewModel()
    val collectorJob =
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
        launch { vm.favoritesState.collect {} }
        launch { vm.playlistsState.collect {} }
        launch { vm.selectedPlaylistTracksState.collect {} }
      }
    try {
      val track =
        MusicTrack(
          trackId = "tr_lib_1",
          title = "Yalnızlık Senfonisi",
          artistId = "art_sertab",
          artistName = "Sertab Erener",
          albumName = "Sertab Gibi",
          durationMs = 240_000L,
          source = TrackSource.ITUNES
        )

      // Toggle favorite ON -> persisted in Room
      vm.toggleFavorite(track)
      pumpMainUntil {
        val list = (vm.favoritesState.value as? UiState.Success)?.data.orEmpty()
        list.any { it.trackId == "tr_lib_1" }
      }

      // Toggle favorite OFF -> removed from Room
      vm.toggleFavorite(track)
      pumpMainUntil {
        val st = vm.favoritesState.value
        st is UiState.Success && st.data.isEmpty()
      }

      // Create playlist
      vm.createPlaylist(
        title = "Gece Sürüşü",
        description = "Test listesi",
        accentHex = "#2979FF",
        coverStyle = PlaylistCoverStyle.CHROME,
        sharedPublic = true
      )
      pumpMainUntil {
        (vm.playlistsState.value as? UiState.Success)?.data?.isNotEmpty() == true
      }
      val playlistId = (vm.playlistsState.value as UiState.Success).data.first().id
      vm.selectPlaylist(playlistId)

      // Add track to playlist -> count becomes 1 and track appears
      vm.addTrackToPlaylist(playlistId, track)
      pumpMainUntil {
        val tracks = (vm.selectedPlaylistTracksState.value as? UiState.Success)?.data.orEmpty()
        val pl = (vm.playlistsState.value as? UiState.Success)?.data?.firstOrNull { it.id == playlistId }
        tracks.any { it.trackId == "tr_lib_1" } && pl?.trackCount == 1L
      }

      // Remove track from playlist -> count returns to 0
      vm.removeTrackFromPlaylist(playlistId, track.trackId)
      pumpMainUntil {
        val tracks = (vm.selectedPlaylistTracksState.value as? UiState.Success)?.data.orEmpty()
        val pl = (vm.playlistsState.value as? UiState.Success)?.data?.firstOrNull { it.id == playlistId }
        tracks.isEmpty() && pl?.trackCount == 0L
      }

      // Re-add track, then delete playlist -> MUST clean up both playlist AND playlist tracks
      vm.addTrackToPlaylist(playlistId, track)
      pumpMainUntil {
        runBlocking { roomDb.localLibraryDao().countPlaylistTracks(playlistId) == 1L }
      }
      vm.deletePlaylist(playlistId)
      pumpMainUntil {
        val playlists = (vm.playlistsState.value as? UiState.Success)?.data.orEmpty()
        playlists.none { it.id == playlistId } && vm.selectedPlaylistId.value == null
      }
      val orphanCount = runBlocking { roomDb.localLibraryDao().countPlaylistTracks(playlistId) }
      assertEquals(0L, orphanCount)
    } finally {
      collectorJob.cancel()
    }
  }

  @Test
  fun viewModel_socialFeedStoriesCommentsRepostsAndDelete_workEndToEnd() {
    val vm = createGuestViewModel()
    val collectorJob =
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
        launch { vm.activityFeedState.collect {} }
        launch { vm.activityCommentsState.collect {} }
        launch { vm.storiesState.collect {} }
      }
    try {
      val track =
        MusicTrack(
          trackId = "tr_soc_1",
          title = "Kış Güneşi",
          artistId = "art_tarkan",
          artistName = "Tarkan"
        )

      // Quick post -> Like toggle -> Comment -> Repost -> Delete
      vm.publishQuickSocialPost("Akşam modu harika", track)
      pumpMainUntil {
        (vm.activityFeedState.value as? UiState.Success)?.data?.any { it.note == "Akşam modu harika" } == true
      }
      val post = (vm.activityFeedState.value as UiState.Success).data.first { it.note == "Akşam modu harika" }

      vm.toggleLikeActivity(post)
      pumpMainUntil {
        (vm.activityFeedState.value as? UiState.Success)?.data?.firstOrNull { it.id == post.id }?.likesCount == 1L
      }
      vm.toggleLikeActivity(post)
      pumpMainUntil {
        (vm.activityFeedState.value as? UiState.Success)?.data?.firstOrNull { it.id == post.id }?.likesCount == 0L
      }

      vm.openActivityComments(post.id)
      vm.submitActivityComment(post, "Müthiş seçim!")
      pumpMainUntil {
        val comments = (vm.activityCommentsState.value as? UiState.Success)?.data.orEmpty()
        val updatedPost = (vm.activityFeedState.value as? UiState.Success)?.data?.firstOrNull { it.id == post.id }
        comments.any { it.text == "Müthiş seçim!" } && updatedPost?.commentsCount == 1L
      }

      vm.repostActivity(post, "Kesinlikle dinlenmeli")
      pumpMainUntil {
        val feed = (vm.activityFeedState.value as? UiState.Success)?.data.orEmpty()
        val updatedPost = feed.firstOrNull { it.id == post.id }
        feed.any { it.note == "Kesinlikle dinlenmeli" } && updatedPost?.repostsCount == 1L
      }

      vm.deleteActivityPost(post)
      pumpMainUntil {
        val feed = (vm.activityFeedState.value as? UiState.Success)?.data.orEmpty()
        feed.none { it.id == post.id }
      }

      // Stories publish & delete
      vm.publishStory(track, "24 saatlik hikaye", StoryCardTheme.AMOLED_GOLD)
      pumpMainUntil {
        (vm.storiesState.value as? UiState.Success)?.data?.any { it.note == "24 saatlik hikaye" } == true
      }
      val story = (vm.storiesState.value as UiState.Success).data.first { it.note == "24 saatlik hikaye" }
      vm.deleteStory(story.id)
      pumpMainUntil {
        (vm.storiesState.value as? UiState.Success)?.data?.none { it.id == story.id } == true
      }
    } finally {
      collectorJob.cancel()
    }
  }

  @Test
  fun viewModel_directChatBlockingReportingAndNotifCleanup_workEndToEnd() {
    val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNm = Shadows.shadowOf(nm)
    val vm = createGuestViewModel()
    val collectorJob =
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
        launch { vm.blockedUsersState.collect {} }
        launch { vm.followingState.collect {} }
        launch { vm.directChatsState.collect {} }
        launch { vm.activeChatMessagesState.collect {} }
        launch { vm.notificationsState.collect {} }
      }
    try {
      val peer =
        PublicProfile(
          id = "u_peer_dm",
          userId = "u_peer_dm",
          displayName = "Kerem Demir",
          handle = "@keremd"
        )
      val sharedTrack =
        MusicTrack(
          trackId = "tr_dm_1",
          title = "Dudu",
          artistId = "art_tarkan",
          artistName = "Tarkan"
        )

      // Follow peer, open chat, send text + track + voice message
      vm.toggleFollowListener(peer)
      pumpMainUntil {
        (vm.followingState.value as? UiState.Success)?.data?.any { it.targetUserId == "u_peer_dm" } == true
      }

      vm.openDirectChat(peer)
      assertNotNull(vm.activeChatPeer.value)

      vm.sendDirectMessage("Selam Kerem!")
      vm.sendTrackToPeer(peer, sharedTrack, "Bunu dinle")
      vm.sendVoiceDirectMessage("UklGRg==", 3200L, "")

      pumpMainUntil {
        val msgs = (vm.activeChatMessagesState.value as? UiState.Success)?.data.orEmpty()
        val threads = (vm.directChatsState.value as? UiState.Success)?.data.orEmpty()
        msgs.size == 3 && threads.size == 1
      }

      // Single message deletion removes only that message
      val firstMsg = (vm.activeChatMessagesState.value as UiState.Success).data.first()
      vm.deleteDirectMessage("u_peer_dm", firstMsg.id)
      pumpMainUntil {
        (vm.activeChatMessagesState.value as? UiState.Success)?.data?.size == 2
      }

      // Post system notification from this peer so we can verify blockUser cleans tray too
      val peerNotif =
        com.example.model.UserNotification(
          id = "n_peer_blk",
          senderId = "u_peer_dm",
          senderName = "Kerem Demir",
          type = NotificationType.MESSAGE.name,
          message = "Engel öncesi mesaj"
        )
      VizyonNotificationHelper.postSocialNotificationSync(app, peerNotif)
      assertTrue(shadowNm.allNotifications.isNotEmpty())

      // Report user confirmation snackbar
      vm.reportUser("u_peer_dm", "USER", "u_peer_dm", ReportReason.SPAM, "Spam test")
      pumpMainUntil { vm.snackbarMessage.value?.contains("Şikayetin", ignoreCase = true) == true }

      // Block user -> closes active chat, removes following, hides guest DM thread, updates Guard, cancels tray notif
      vm.blockUser("u_peer_dm", "Kerem Demir", "@keremd")
      pumpMainUntil {
        val blocked = (vm.blockedUsersState.value as? UiState.Success)?.data.orEmpty()
        val following = (vm.followingState.value as? UiState.Success)?.data.orEmpty()
        val threads = (vm.directChatsState.value as? UiState.Success)?.data.orEmpty()
        blocked.any { it.blockedUserId == "u_peer_dm" } &&
          following.none { it.targetUserId == "u_peer_dm" } &&
          threads.none { "u_peer_dm" in it.participantIds } &&
          vm.activeChatPeer.value == null
      }
      assertTrue(NotificationDispatchGuard.isSenderBlocked(app, "guest", "u_peer_dm"))
      assertEquals(0, shadowNm.allNotifications.size)

      // Re-opening chat while blocked must be refused
      vm.openDirectChat(peer)
      assertNull(vm.activeChatPeer.value)

      // Unblocking restores access & chat history remains clean or deletable
      vm.unblockUser("u_peer_dm")
      pumpMainUntil {
        val blocked = (vm.blockedUsersState.value as? UiState.Success)?.data.orEmpty()
        blocked.none { it.blockedUserId == "u_peer_dm" }
      }
      vm.deleteDirectChat("u_peer_dm")
      pumpMainUntil {
        (vm.directChatsState.value as? UiState.Success)?.data?.isEmpty() == true
      }
    } finally {
      collectorJob.cancel()
    }
  }

  @Test
  fun viewModel_notificationDeepLinksReadClearAndDeleteAccount_workEndToEnd() {
    val vm = createGuestViewModel()
    val collectorJob =
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate).launch {
        launch { vm.notificationsState.collect {} }
        launch { vm.unreadNotificationCount.collect {} }
      }
    try {
      // Seed 2 unread notifications in Room
      runBlocking {
        roomDb.localLibraryDao().insertNotification(
          LocalNotificationEntity(
            id = "n_dl_1",
            senderName = "Selin",
            senderHandle = "@selin",
            senderAvatarUrl = "",
            type = NotificationType.MESSAGE.name,
            trackId = "",
            trackTitle = "",
            message = "Mesaj 1",
            targetType = "CHAT",
            targetId = "u_selin",
            read = false
          )
        )
        roomDb.localLibraryDao().insertNotification(
          LocalNotificationEntity(
            id = "n_dl_2",
            senderName = "Emre",
            senderHandle = "@emre",
            senderAvatarUrl = "",
            type = NotificationType.LIKE.name,
            trackId = "tr_1",
            trackTitle = "Dudu",
            message = "Beğeni 2",
            targetType = "POST",
            targetId = "post_99",
            read = false
          )
        )
      }
      pumpMainUntil { vm.unreadNotificationCount.value == 2 }

      // Intent deep links for CHAT, POST, PROFILE, STORY, REQUESTS
      val scenarios =
        listOf(
          Triple("CHAT", "u_chat_1", NotificationDeepLinkTarget.OpenChat("u_chat_1")),
          Triple("POST", "act_post_1", NotificationDeepLinkTarget.OpenPostComments("act_post_1")),
          Triple("PROFILE", "u_prof_1", NotificationDeepLinkTarget.OpenListenerProfile("u_prof_1")),
          Triple("STORY", "story_77", NotificationDeepLinkTarget.OpenStory("story_77", "tr_s")),
          Triple("REQUESTS", "", NotificationDeepLinkTarget.OpenRequests)
        )
      scenarios.forEach { (targetType, targetId, expected) ->
        val intent =
          Intent().apply {
            putExtra(VizyonNotificationHelper.EXTRA_NOTIF_ID, "n_dl_1")
            putExtra(VizyonNotificationHelper.EXTRA_NOTIF_TARGET_TYPE, targetType)
            putExtra(VizyonNotificationHelper.EXTRA_NOTIF_TARGET_ID, targetId)
            putExtra(VizyonNotificationHelper.EXTRA_NOTIF_TRACK_ID, "tr_s")
            putExtra(VizyonNotificationHelper.EXTRA_NOTIF_TYPE, NotificationType.MESSAGE.name)
          }
        vm.handleNotificationIntent(intent)
        assertEquals(expected, vm.pendingDeepLink.value)
        vm.consumePendingDeepLink()
        assertNull(vm.pendingDeepLink.value)
      }

      // First intent marked n_dl_1 as read -> unread count is now 1
      pumpMainUntil { vm.unreadNotificationCount.value == 1 }

      // markAllNotificationsRead -> unread count becomes 0
      vm.markAllNotificationsRead()
      pumpMainUntil { vm.unreadNotificationCount.value == 0 }

      // deleteNotification removes single item
      vm.deleteNotification("n_dl_1")
      pumpMainUntil {
        (vm.notificationsState.value as? UiState.Success)?.data?.none { it.id == "n_dl_1" } == true
      }

      // clearAllNotifications clears remaining items
      vm.clearAllNotifications()
      pumpMainUntil {
        (vm.notificationsState.value as? UiState.Success)?.data?.isEmpty() == true
      }

      // deleteAccount resets local preferences, Room tables, and playback state
      var deletedCallbackFired = false
      vm.toggleFavorite(MusicTrack(trackId = "tr_wipe", title = "Wipe", artistId = "a", artistName = "A"))
      runBlocking {
        prefs.completeOnboarding(
          displayName = "Silinecek",
          handle = "@sil",
          bio = "",
          favoriteGenres = listOf("Pop"),
          ageVerified = true,
          preferredLanguage = AppLanguage.TURKISH,
          themeMode = AppThemeMode.NIGHT_AMOLED,
          shareActivity = true
        )
      }
      vm.deleteAccount(bypassFreshnessPreCheck = true, onCompleted = { deletedCallbackFired = true })
      pumpMainUntil { deletedCallbackFired }
      val afterCfg = runBlocking { prefs.configFlow.first() }
      assertFalse(afterCfg.onboardingCompleted)
      assertNull(vm.playbackState.value.currentTrack)
    } finally {
      collectorJob.cancel()
    }
  }

  @Test
  fun playerManager_andViewModelPlayback_coverQueueShuffleRepeatSeekAndAutoFallback() {
    val vm = createGuestViewModel()
    val t1 = MusicTrack(trackId = "q1", title = "Birinci", artistId = "a1", artistName = "S1", youtubeVideoId = "vid_1", durationMs = 180_000L)
    val t2 = MusicTrack(trackId = "q2", title = "İkinci", artistId = "a2", artistName = "S2", youtubeVideoId = "vid_2", durationMs = 200_000L)
    val t3 = MusicTrack(trackId = "q3", title = "Üçüncü", artistId = "a3", artistName = "S3", youtubeVideoId = "vid_3", durationMs = 210_000L)
    val queue = listOf(t1, t2)

    playerManager.prepareLoading(t1, queue)
    playerManager.onVideoResolved(t1, queue)
    playerManager.onIFrameReady("vid_1")
    playerManager.onIFrameStateChanged(1) // playing
    assertTrue(vm.playbackState.value.isPlaying)
    assertEquals("q1", vm.playbackState.value.currentTrack?.trackId)

    // addToQueue appends unique track only
    vm.addToQueue(t3)
    vm.addToQueue(t3)
    assertEquals(3, vm.playbackState.value.queue.size)

    // Seek > 4s -> skipPrevious rewinds to 0 instead of jumping tracks
    vm.seekTo(12_000L)
    assertEquals(12_000L, vm.playbackState.value.positionMs)
    vm.skipPrevious()
    assertEquals(0L, vm.playbackState.value.positionMs)
    assertEquals("q1", vm.playbackState.value.currentTrack?.trackId)

    // skipPrevious at <= 4s wraps to last track (q3)
    vm.skipPrevious()
    assertEquals("q3", vm.playbackState.value.currentTrack?.trackId)

    // Shuffle & Repeat cycling
    vm.toggleShuffle()
    assertTrue(vm.playbackState.value.shuffleEnabled)
    assertEquals(RepeatMode.NONE, vm.playbackState.value.repeatMode)
    vm.cycleRepeat()
    assertEquals(RepeatMode.ALL, vm.playbackState.value.repeatMode)
    vm.cycleRepeat()
    assertEquals(RepeatMode.ONE, vm.playbackState.value.repeatMode)
    vm.cycleRepeat()
    assertEquals(RepeatMode.NONE, vm.playbackState.value.repeatMode)

    // RepeatMode.NONE ending on last track stops playback cleanly
    playerManager.prepareLoading(t3, listOf(t1, t2, t3))
    playerManager.onIFrameReady("vid_3")
    playerManager.onIFrameStateChanged(1)
    playerManager.onIFrameStateChanged(0) // ended
    assertFalse(vm.playbackState.value.isPlaying)

    // Auto-fallback retry: up to 2 attempts on embed error 150, then 3rd surfaces error message
    playerManager.prepareLoading(t1, listOf(t1))
    playerManager.onVideoResolved(t1, listOf(t1))
    playerManager.onIFrameReady("vid_1")

    vm.onYouTubeIFrameError(150) // attempt 1 -> triggers retryCurrentTrackVideo (isResolvingVideo = true)
    assertTrue(vm.playbackState.value.isResolvingVideo)
    assertNull(vm.playbackState.value.playbackMessage)

    vm.onYouTubeIFrameError(150) // attempt 2 -> triggers second fallback
    assertTrue(vm.playbackState.value.isResolvingVideo)
    assertNull(vm.playbackState.value.playbackMessage)

    vm.onYouTubeIFrameError(150) // 3rd consecutive error -> surfaces user-facing error!
    assertFalse(vm.playbackState.value.isResolvingVideo)
    assertTrue(vm.playbackState.value.playbackMessage?.contains("150") == true)
  }

  @Test
  fun miniPlayerUi_verifyControlsAndSwipeDismiss() {
    val dummyWebView = WebView(app)
    val track1 = MusicTrack(trackId = "ui_1", title = "Mavi", artistId = "a1", artistName = "Gripin", youtubeVideoId = "vid_ui_1", durationMs = 200_000L)
    val track2 = MusicTrack(trackId = "ui_2", title = "Aşk", artistId = "a2", artistName = "Sertab Erener", youtubeVideoId = "vid_ui_2", durationMs = 210_000L)

    var playPauseClicked = false
    var nextClicked = false
    var dismissSwiped = false

    val miniState =
      PlaybackUiState(
        currentTrack = track1,
        queue = listOf(track1, track2),
        currentIndex = 0,
        isPlaying = true,
        isPlayerReady = true,
        positionMs = 45_000L,
        durationMs = 200_000L,
        activeVideoId = "vid_ui_1"
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        MiniPlayerBar(
          playbackState = miniState,
          sharedWebView = dummyWebView,
          onIFrameLoadFailure = {},
          onRetryVideo = {},
          onExpandClick = {},
          onPlayPauseClick = { playPauseClicked = true },
          onNextClick = { nextClicked = true },
          onDismissSwipe = { dismissSwiped = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_player_live_video_slot").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mini_player_play_pause").performClick()
    composeTestRule.onNodeWithTag("mini_player_next_btn").performClick()
    assertTrue(playPauseClicked)
    assertTrue(nextClicked)

    composeTestRule.onNodeWithTag("mini_player_bar").performTouchInput { swipeRight() }
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    assertTrue(dismissSwiped)
  }

  @Test
  fun nowPlayingUi_verifyErrorRetryLyricsAndNoChangeVideoButton() {
    val dummyWebView = WebView(app)
    val track1 = MusicTrack(trackId = "ui_1", title = "Mavi", artistId = "a1", artistName = "Gripin", youtubeVideoId = "vid_ui_1", durationMs = 200_000L)
    val track2 = MusicTrack(trackId = "ui_2", title = "Aşk", artistId = "a2", artistName = "Sertab Erener", youtubeVideoId = "vid_ui_2", durationMs = 210_000L)
    var retryClicked = false

    val errorPlaybackState =
      PlaybackUiState(
        currentTrack = track1,
        queue = listOf(track1, track2),
        currentIndex = 0,
        isPlaying = false,
        isResolvingVideo = false,
        isPlayerReady = false,
        playbackMessage = "Bu video sahibi tarafından gömülü oynatmaya kısıtlı (Hata Kodu: 150).",
        activeVideoId = "vid_ui_1"
      )
    val lyricsState =
      UiState.Success(
        TrackLyrics(
          trackId = "ui_1",
          trackName = "Mavi",
          artistName = "Gripin",
          syncedLines = emptyList(),
          plainLines = listOf("Bir mavi rüya gibi")
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        NowPlayingModal(
          playbackState = errorPlaybackState,
          lyricsState = lyricsState,
          sharedWebView = dummyWebView,
          isFavorite = false,
          onDismiss = {},
          onIFrameLoadFailure = {},
          onRetryVideo = { retryClicked = true },
          onPlayPauseClick = {},
          onSeekTo = {},
          onNextClick = {},
          onPreviousClick = {},
          onToggleShuffle = {},
          onCycleRepeat = {},
          onToggleFavorite = {},
          onOpenPlaylistPicker = {},
          onShareTrack = {},
          onRefreshLyrics = {},
          onArtistClick = {},
          onSelectTrackFromQueue = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("now_playing_modal").assertIsDisplayed()
    composeTestRule.onNodeWithTag("youtube_player_error_box").assertIsDisplayed()
    composeTestRule.onNodeWithTag("retry_youtube_video_btn").assertIsDisplayed().performClick()
    assertTrue(retryClicked)

    // Verify "Videoyu Değiştir" button was permanently removed
    assertEquals(0, composeTestRule.onAllNodesWithText("Videoyu Değiştir", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("switch_matched_video_btn").fetchSemanticsNodes().size)

    // Verify Lyrics sheet opens cleanly
    composeTestRule.onNodeWithTag("lyrics_button").performClick()
    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Bir mavi rüya gibi", substring = true).assertIsDisplayed()
  }

  @Test
  fun onboardingUi_confirmsZeroTestModeRemnants() {
    composeTestRule.setContent {
      VizyonMusicTheme {
        OnboardingScreen(
          initialDisplayName = "Deniz",
          isSignedInWithGoogle = false,
          initialThemeMode = AppThemeMode.NIGHT_AMOLED,
          initialLanguage = AppLanguage.TURKISH,
          onThemePreviewChange = {},
          onCompleteOnboarding = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> }
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("onboarding_quick_test_mode_btn").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Test Modu", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun profilePrivacyUi_confirmsZeroTestModeRemnantsAndKeepsExistingTools() {
    composeTestRule.setContent {
      VizyonMusicTheme {
        AppearanceSettingsScreen(
          isSignedIn = true,
          firebaseUid = "uid_clean_1",
          firebaseEmail = "user@example.com",
          userPhotoUrl = "",
          unreadNotificationCount = 0,
          appearance = AppAppearanceConfig(),
          userProfileState = UiState.Success(UserProfile(displayName = "Deniz", handle = "@deniz")),
          bridgeStatus = null,
          isDeletingAccount = false,
          onThemeModeChange = {},
          onAccentChange = {},
          onCornerDensityChange = {},
          onLanguageChange = {},
          onShareActivityToggle = {},
          onNotificationsToggle = {},
          onPickProfilePhoto = {},
          onOpenNotifications = {},
          onSaveProfile = { _, _, _, _ -> },
          onDeleteAccount = { _, _, _ -> },
          onNotifyMessage = {},
          onSignedOut = {}
        )
      }
    }
    composeTestRule.waitForIdle()
    assertEquals(0, composeTestRule.onAllNodesWithTag("test_mode_floating_pill").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("test_mode_settings_card").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("test_mode_lab_sheet").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Tam Test Modu", substring = true).fetchSemanticsNodes().size)

    // Navigate into Hakkında & Gizlilik -> verify Test Bildirimi and battery settings helper exist while Test Mode is absent
    composeTestRule.onNodeWithTag("open_about_privacy_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("open_app_battery_settings_btn"))
    composeTestRule.onNodeWithTag("send_local_test_notif_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_app_battery_settings_btn").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("test_mode_settings_card").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Tam Test Modu", substring = true).fetchSemanticsNodes().size)
  }
}
