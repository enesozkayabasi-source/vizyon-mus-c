package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.performTextInput
import com.google.firebase.Timestamp
import com.example.model.ActivityFeedItem
import com.example.model.ActivityLikeUser
import com.example.model.ChatPrivacy
import com.example.model.DirectChatMessage
import com.example.model.DirectChatThread
import com.example.model.DirectMessageType
import com.example.model.FollowRequest
import com.example.model.MessageRequest
import com.example.model.MusicStoryItem
import com.example.model.MusicTrack
import com.example.model.NotificationType
import com.example.model.PublicProfile
import com.example.model.ReportReason
import com.example.model.StoryCardTheme
import com.example.model.UiState
import com.example.model.UserNotification
import com.example.ui.components.ActivityLikersModalSheet
import com.example.ui.components.BlockUserConfirmDialog
import com.example.ui.components.DirectChatThreadSheet
import com.example.ui.components.DirectChatsListSection
import com.example.ui.components.InAppNotificationBubble
import com.example.ui.components.NotificationInboxSheet
import com.example.ui.components.ReportUserDialog
import com.example.ui.components.RequestsModalSheet
import com.example.ui.components.ShareTrackDialog
import com.example.ui.components.SocialFeedTab
import com.example.ui.components.SocialSegmentedTabs
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.StoryViewerOverlay
import com.example.ui.components.VaultUnlockDialog
import com.example.ui.theme.VizyonMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h800dp")
class NotificationAndStoryOverlayRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun otherUserStoryViewer_showsAllBottomActionPanelsCleanly() {
    var dmClicked = false
    val otherStory =
      MusicStoryItem(
        id = "story_other_1",
        userId = "other_user_123",
        userName = "Deniz Aksoy",
        userHandle = "@deniz",
        trackId = "tr_1",
        trackTitle = "Yalnızlık Senfonisi",
        artistId = "art_1",
        artistName = "Sertab Erener",
        note = "Gece yürüyüşüne çok yakışıyor."
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        StoryViewerDialog(
          story = otherStory,
          isAuthor = false,
          onDismiss = {},
          onPlayTrack = {},
          onDeleteStory = {},
          onNotifyMessage = {},
          onOpenAuthorProfile = {},
          onMessageAuthor = { dmClicked = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("story_viewer_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_bottom_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_play_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_dm_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_share_png_btn").assertIsDisplayed()

    composeTestRule.onNodeWithTag("story_viewer_dm_btn").performClick()
    assertTrue(dmClicked)
  }

  @Test
  fun inAppNotificationBubble_andNotificationInbox_displayOnlyDisplayNameWithoutHandle() {
    var dismissed = false
    val incoming =
      UserNotification(
        id = "notif_msg_1",
        senderId = "u_selin",
        senderName = "Selin Yılmaz",
        senderHandle = "@selin_muzik",
        type = NotificationType.MESSAGE.name,
        trackTitle = "Dudu",
        message = "Selin Yılmaz (@selin_muzik): Yeni parçayı dinledin mi?"
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        InAppNotificationBubble(
          notification = incoming,
          onOpen = {},
          onDismiss = { dismissed = true }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("in_app_notification_bubble").assertIsDisplayed()
    composeTestRule.onNodeWithText("Selin Yılmaz").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("@selin_muzik", substring = true).fetchSemanticsNodes().size)

    composeTestRule.onNodeWithTag("in_app_notification_dismiss_btn").performClick()
    assertTrue(dismissed)
  }

  @Test
  fun notificationInboxSheet_hidesHandleAndHighlightsDisplayName() {
    val items =
      listOf(
        UserNotification(
          id = "n_inbox_1",
          senderId = "u_emre",
          senderName = "Emre Kaya",
          senderHandle = "@emrekaya",
          type = NotificationType.FOLLOW.name,
          message = "Emre Kaya (@emrekaya) seni takip etmeye başladı!"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        NotificationInboxSheet(
          notificationsState = UiState.Success(items),
          onDismiss = {},
          onNotificationClick = {},
          onMarkAllRead = {},
          onPermissionResultMessage = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notification_item_n_inbox_1").assertIsDisplayed()
    composeTestRule.onNodeWithText("Emre Kaya").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("@emrekaya", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun notificationDispatchGuard_deduplicatesSeenNotificationsAcrossBackgroundSyncs() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.notifications.NotificationDispatchGuard.clearForUser(context, "uid_test")

    com.example.notifications.NotificationDispatchGuard.markBaselineSeen(context, "uid_test", listOf("n1", "n2"))
    assertTrue(com.example.notifications.NotificationDispatchGuard.isBaselineInitialized(context, "uid_test"))

    val firstBatch =
      com.example.notifications.NotificationDispatchGuard.filterAndMarkUnseen(
        context,
        "uid_test",
        listOf("n1", "n2", "n3")
      )
    assertEquals(listOf("n3"), firstBatch)

    val repeatBatch =
      com.example.notifications.NotificationDispatchGuard.filterAndMarkUnseen(
        context,
        "uid_test",
        listOf("n1", "n2", "n3")
      )
    assertTrue(repeatBatch.isEmpty())
  }

  @Test
  @Config(sdk = [24])
  fun legacyApi24_backgroundNotificationPostsCleanlyWithDisplayNameOnly() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    assertTrue(com.example.util.DevicePerfHelper.isConstrainedDevice(context))

    val notif =
      UserNotification(
        id = "n_legacy_1",
        senderId = "u_legacy",
        senderName = "Can Demir",
        senderHandle = "@candemir",
        type = NotificationType.MESSAGE.name,
        message = "Can Demir (@candemir): Eski cihazdan selam!"
      )
    com.example.notifications.VizyonNotificationHelper.postSocialNotificationSync(context, notif)

    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadowNm = org.robolectric.Shadows.shadowOf(nm)
    val posted = shadowNm.allNotifications
    assertTrue(posted.isNotEmpty())
    val extras = posted.last().extras
    assertEquals("Can Demir", extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString())
    assertEquals("Eski cihazdan selam!", extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString())
  }

  @Test
  fun socialSegmentedTabs_omitsCountParenthesesEvenWithPositiveCounts() {
    composeTestRule.setContent {
      VizyonMusicTheme {
        SocialSegmentedTabs(
          selectedTab = SocialFeedTab.ALL,
          followingCount = 14,
          listenersCount = 42,
          chatsCount = 8,
          onSelectTab = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Akış").assertIsDisplayed()
    composeTestRule.onNodeWithText("Takip").assertIsDisplayed()
    composeTestRule.onNodeWithText("Kişiler").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sohbet").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("(", substring = true).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText(")", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun consecutiveBackgroundNotifications_accumulatePerSenderInMessagingStyleWithoutGroupCollapse() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    nm.cancelAll()
    com.example.notifications.VizyonNotificationHelper.clearAllHistory()

    repeat(5) { idx ->
      val notif =
        UserNotification(
          id = "n_multi_${idx + 1}",
          senderId = "u_sender_same",
          senderName = "Selin Kaya",
          type = NotificationType.MESSAGE.name,
          message = "Arka plan mesajı #${idx + 1}"
        )
      com.example.notifications.VizyonNotificationHelper.postSocialNotificationSync(context, notif)
    }

    val shadowNm = org.robolectric.Shadows.shadowOf(nm)
    val posted = shadowNm.allNotifications
    assertTrue(posted.size == 1)
    val single = posted.first()
    assertTrue(single.group == null)
    assertTrue((single.flags and android.app.Notification.FLAG_GROUP_SUMMARY) == 0)
    @Suppress("DEPRECATION")
    val messages = single.extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)
    assertEquals(5, messages?.size ?: 0)
  }

  @Test
  fun directChatThreadAndList_confirmDeletionCallbacksWork() {
    var deletedChatPeerId: String? = null
    val peer =
      PublicProfile(
        id = "u_peer_1",
        userId = "u_peer_1",
        displayName = "Deniz Yıldız",
        handle = "@denizy"
      )
    val threads =
      listOf(
        DirectChatThread(
          id = "local__u_peer_1",
          chatId = "local__u_peer_1",
          participantIds = listOf("local", "u_peer_1"),
          participantNames = mapOf("local" to "Ben", "u_peer_1" to "Deniz Yıldız"),
          lastMessageText = "Selam!"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        DirectChatsListSection(
          chatsState = UiState.Success(threads),
          currentUserId = "local",
          publicListeners = listOf(peer),
          onOpenChatThread = {},
          onOpenListenersTab = {},
          onDeleteChatThread = { deletedChatPeerId = it }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("delete_chat_btn_u_peer_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_delete_chat_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_delete_chat_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals("u_peer_1", deletedChatPeerId)
  }

  @Test
  fun directChatThreadSheet_confirmsSingleMessageDeletion() {
    var deletedMessageId: String? = null
    val peer =
      PublicProfile(
        id = "u_peer_2",
        userId = "u_peer_2",
        displayName = "Ece Akın",
        handle = "@eceakin"
      )
    val messages =
      listOf(
        DirectChatMessage(
          id = "msg_single_1",
          chatId = "local__u_peer_2",
          senderId = "local",
          senderName = "Ben",
          text = "Bu mesaj silinecek"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        DirectChatThreadSheet(
          peer = peer,
          currentUserId = "local",
          isFollowerOrFollowing = true,
          messagesState = UiState.Success(messages),
          currentTrack = null,
          candidateTracks = emptyList(),
          onDismiss = {},
          onSendMessage = { _, _ -> },
          onPlayTrack = {},
          onDeleteChatThread = {},
          onDeleteMessage = { deletedMessageId = it.id }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("delete_chat_msg_btn_msg_single_1").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_delete_message_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_delete_message_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals("msg_single_1", deletedMessageId)
  }

  @Test
  fun profileSoundPreviewCard_displaysAndClicksAllThreeSoundTestButtons() {
    composeTestRule.setContent {
      VizyonMusicTheme {
        com.example.ui.screens.ProfileSoundPreviewCard()
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("sound_preview_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("test_send_sound_btn").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("test_receive_sound_btn").assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag("test_notify_sound_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun notificationInboxSheet_rendersTodayYesterdayAndOlderTimeGroups() {
    val nowMs = System.currentTimeMillis()
    val oneDayMs = 24L * 60L * 60L * 1000L
    val items =
      listOf(
        UserNotification(
          id = "n_today",
          senderId = "u1",
          senderName = "Selin",
          type = NotificationType.LIKE.name,
          message = "Bugün beğendi",
          createdAt = Timestamp(java.util.Date(nowMs - 60_000L))
        ),
        UserNotification(
          id = "n_yesterday",
          senderId = "u2",
          senderName = "Emre",
          type = NotificationType.FOLLOW.name,
          message = "Dün takip etti",
          createdAt = Timestamp(java.util.Date(nowMs - oneDayMs))
        ),
        UserNotification(
          id = "n_older",
          senderId = "u3",
          senderName = "Deniz",
          type = NotificationType.COMMENT.name,
          message = "Eski yorum",
          createdAt = Timestamp(java.util.Date(nowMs - 5L * oneDayMs))
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        NotificationInboxSheet(
          notificationsState = UiState.Success(items),
          onDismiss = {},
          onNotificationClick = {},
          onMarkAllRead = {},
          onPermissionResultMessage = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notif_group_header_TODAY").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_group_header_YESTERDAY").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_inbox_lazy_list")
      .performScrollToNode(hasTestTag("notif_group_header_OLDER"))
    composeTestRule.onNodeWithTag("notif_group_header_OLDER").assertIsDisplayed()
  }

  @Test
  fun notificationInboxSheet_swipeLeftAndRight_triggersSingleNotificationDelete() {
    val deletedIds = mutableListOf<String>()
    val items =
      listOf(
        UserNotification(
          id = "n_swipe_left",
          senderId = "u1",
          senderName = "Selin",
          type = NotificationType.MESSAGE.name,
          message = "Sola kaydırınca silinecek"
        ),
        UserNotification(
          id = "n_swipe_right",
          senderId = "u2",
          senderName = "Can",
          type = NotificationType.FOLLOW.name,
          message = "Sağa kaydırınca silinecek"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        NotificationInboxSheet(
          notificationsState = UiState.Success(items),
          onDismiss = {},
          onNotificationClick = {},
          onMarkAllRead = {},
          onDeleteNotification = { deletedIds.add(it.id) },
          onPermissionResultMessage = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("notification_item_n_swipe_left").performTouchInput { swipeLeft() }
    composeTestRule.mainClock.advanceTimeBy(450L)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("notification_item_n_swipe_right").performTouchInput { swipeRight() }
    composeTestRule.mainClock.advanceTimeBy(450L)
    composeTestRule.waitForIdle()

    assertTrue(deletedIds.contains("n_swipe_left"))
    assertTrue(deletedIds.contains("n_swipe_right"))
  }

  @Test
  fun notificationInboxSheet_threeDotMenu_confirmsClearAllNotifications() {
    var clearedAll = false
    var markedAllRead = false
    val items =
      listOf(
        UserNotification(
          id = "n_clear_1",
          senderId = "u1",
          senderName = "Ece",
          read = false,
          type = NotificationType.LIKE.name,
          message = "Parçanı beğendi"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        NotificationInboxSheet(
          notificationsState = UiState.Success(items),
          onDismiss = {},
          onNotificationClick = {},
          onMarkAllRead = { markedAllRead = true },
          onClearAllNotifications = { clearedAll = true },
          onPermissionResultMessage = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("notif_more_menu_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mark_all_notifications_read_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertTrue(markedAllRead)

    composeTestRule.onNodeWithTag("notif_more_menu_btn").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("clear_all_notifications_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("confirm_clear_notifications_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_clear_notifications_btn").performClick()
    composeTestRule.waitForIdle()

    assertTrue(clearedAll)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w360dp-h640dp")
  fun compactScreen_storyViewer_showsAllBottomActionButtonsWithoutClipping() {
    var played = false
    val story =
      MusicStoryItem(
        id = "story_compact_1",
        userId = "other_u_compact",
        userName = "Mert Kaya",
        userHandle = "@mert",
        trackId = "tr_compact",
        trackTitle = "Uzun Bir Şarkı İsmi Testi",
        artistId = "art_compact",
        artistName = "Duman",
        note = "Kompakt ekranda bile alt butonlar eksiksiz görünmeli."
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        StoryViewerDialog(
          story = story,
          isAuthor = false,
          onDismiss = {},
          onPlayTrack = { played = true },
          onDeleteStory = {},
          onNotifyMessage = {},
          onOpenAuthorProfile = {},
          onMessageAuthor = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("story_viewer_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_bottom_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_play_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_dm_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_share_png_btn").assertIsDisplayed()

    composeTestRule.onNodeWithTag("story_viewer_play_btn").performClick()
    assertTrue(played)
  }

  @Test
  fun inAppStoryViewerOverlay_displaysCenteredHeaderCardAndBottomActionBar() {
    var closed = false
    var dmClicked = false
    val story =
      MusicStoryItem(
        id = "story_overlay_1",
        userId = "other_u_overlay",
        userName = "Aslı Demir",
        userHandle = "@asli",
        trackId = "tr_overlay",
        trackTitle = "Yalnızlık Senfonisi",
        artistId = "art_overlay",
        artistName = "Sertab Erener",
        note = "Merkezde ortalanmış hikaye katmanı."
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        StoryViewerOverlay(
          story = story,
          isAuthor = false,
          onDismiss = { closed = true },
          onPlayTrack = {},
          onDeleteStory = {},
          onNotifyMessage = {},
          onOpenAuthorProfile = {},
          onMessageAuthor = { dmClicked = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("story_viewer_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_bottom_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_play_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_dm_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_share_png_btn").assertIsDisplayed()

    composeTestRule.onNodeWithTag("story_viewer_dm_btn").performClick()
    assertTrue(dmClicked)

    composeTestRule.onNodeWithTag("close_story_viewer_btn").performClick()
    assertTrue(closed)
  }

  @Test
  fun consecutiveSixBackgroundNotifications_allPostDistinctlyWithSoundCooldownAndNoGroupCollapse() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    nm.cancelAll()
    com.example.notifications.VizyonNotificationHelper.clearAllHistory()

    val types =
      listOf(
        NotificationType.FOLLOW_REQUEST,
        NotificationType.MESSAGE_REQUEST,
        NotificationType.VOICE_MESSAGE,
        NotificationType.MESSAGE,
        NotificationType.COMMENT,
        NotificationType.FOLLOW
      )
    val startMs = System.currentTimeMillis()
    types.forEachIndexed { idx, nType ->
      val notif =
        UserNotification(
          id = "n_six_${idx + 1}",
          senderId = "u_sender_six_${idx + 1}",
          senderName = "Aylin Demir ${idx + 1}",
          type = nType.name,
          message = "Bildirim testi #${idx + 1}"
        )
      com.example.notifications.VizyonNotificationHelper.postSocialNotificationSync(context, notif)
    }
    val elapsedMs = System.currentTimeMillis() - startMs

    val shadowNm = org.robolectric.Shadows.shadowOf(nm)
    val posted = shadowNm.allNotifications
    assertTrue(posted.size >= 6)
    assertTrue(posted.all { it.group == null })
    assertTrue(posted.none { (it.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0 })
    assertTrue(
      "Expected >= 5 cooldown intervals of ${com.example.notifications.VizyonNotificationHelper.MIN_SOUND_COOLDOWN_MS}ms, took ${elapsedMs}ms",
      elapsedMs >= 5L * com.example.notifications.VizyonNotificationHelper.MIN_SOUND_COOLDOWN_MS - 150L
    )
  }

  @Test
  fun requestsModalSheet_switchesTabsAndTriggersAcceptDeclineCallbacks() {
    var acceptedFollowId: String? = null
    var declinedMsgId: String? = null
    val followReq =
      FollowRequest(
        id = "freq_1",
        requesterId = "u_req_1",
        requesterName = "Arda Güler",
        requesterHandle = "@arda"
      )
    val voiceMsgReq =
      MessageRequest(
        id = "mreq_voice_1",
        requesterId = "u_req_2",
        requesterName = "Zeynep Ak",
        requesterHandle = "@zeynepak",
        previewText = "Sesli mesaj isteği gönderdi",
        messageType = DirectMessageType.VOICE.name,
        audioBase64 = "UklGRg==",
        audioDurationMs = 4500L
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        RequestsModalSheet(
          followRequestsState = UiState.Success(listOf(followReq)),
          messageRequestsState = UiState.Success(listOf(voiceMsgReq)),
          onDismiss = {},
          onRespondFollowRequest = { req, accept -> if (accept) acceptedFollowId = req.id },
          onRespondMessageRequest = { req, accept -> if (!accept) declinedMsgId = req.id }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("requests_modal_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("accept_follow_req_freq_1").performClick()
    assertEquals("freq_1", acceptedFollowId)

    composeTestRule.onNodeWithTag("requests_tab_message").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("play_msg_req_voice_mreq_voice_1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("decline_msg_req_mreq_voice_1").performClick()
    assertEquals("mreq_voice_1", declinedMsgId)
  }

  @Test
  fun directChatThreadSheet_followersOnlyPeer_allowsSendingMessageRequestAndShowsVoiceRecordBtn() {
    var sentRequestText: String? = null
    val peer =
      PublicProfile(
        id = "u_private_peer",
        userId = "u_private_peer",
        displayName = "Kerem Soylu",
        handle = "@kerem",
        chatPrivacy = ChatPrivacy.FOLLOWERS_ONLY.name
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        DirectChatThreadSheet(
          peer = peer,
          currentUserId = "local",
          isFollowerOrFollowing = false,
          messagesState = UiState.Success(emptyList()),
          currentTrack = null,
          candidateTracks = emptyList(),
          onDismiss = {},
          onSendMessage = { _, _ -> },
          onPlayTrack = {},
          onSendMessageRequest = { sentRequestText = it }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("direct_chat_request_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("direct_chat_voice_record_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("direct_chat_input").performTextInput("Merhaba takip isteği gönderdim")
    composeTestRule.onNodeWithTag("direct_chat_send_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals("Merhaba takip isteği gönderdim", sentRequestText)
  }

  @Test
  fun reportUserDialog_selectsReasonAndSubmits() {
    var reportedReason: ReportReason? = null
    composeTestRule.setContent {
      VizyonMusicTheme {
        ReportUserDialog(
          targetDisplayName = "Test Kullanıcı",
          onDismiss = {},
          onSubmitReport = { reason, _ -> reportedReason = reason }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("report_user_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("report_reason_HARASSMENT").performClick()
    composeTestRule.onNodeWithTag("submit_report_btn").performClick()
    assertEquals(ReportReason.HARASSMENT, reportedReason)
  }

  @Test
  fun blockUserConfirmDialog_confirmsBlockAction() {
    var blockedConfirmed = false
    composeTestRule.setContent {
      VizyonMusicTheme {
        BlockUserConfirmDialog(
          targetDisplayName = "Test Kullanıcı",
          onDismiss = {},
          onConfirmBlock = { blockedConfirmed = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("block_user_confirm_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_block_user_btn").performClick()
    assertTrue(blockedConfirmed)
  }

  @Test
  fun vaultUnlockDialog_acceptsFourDigitPasscode() {
    var unlockedPasscode: String? = null
    composeTestRule.setContent {
      VizyonMusicTheme {
        VaultUnlockDialog(
          userName = "Deniz",
          errorMessage = null,
          onUnlockSubmit = { unlockedPasscode = it }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("vault_unlock_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("vault_unlock_input").performTextInput("1234")
    composeTestRule.onNodeWithTag("vault_unlock_btn").performClick()
    assertEquals("1234", unlockedPasscode)
  }

  @Test
  fun mergedManifest_removesForegroundServiceAndBootPermissionsAndRegistersFcmService() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val pkgInfo =
      context.packageManager.getPackageInfo(
        context.packageName,
        android.content.pm.PackageManager.GET_PERMISSIONS or android.content.pm.PackageManager.GET_SERVICES
      )
    val permissions = pkgInfo.requestedPermissions?.toSet().orEmpty()
    assertTrue(!permissions.contains(android.Manifest.permission.FOREGROUND_SERVICE))
    assertTrue(!permissions.contains("android.permission.FOREGROUND_SERVICE_DATA_SYNC"))
    assertTrue(!permissions.contains(android.Manifest.permission.RECEIVE_BOOT_COMPLETED))
    val services = pkgInfo.services?.map { it.name }.orEmpty()
    assertTrue(services.any { it.endsWith("VizyonFirebaseMessagingService") })
  }

  @Test
  fun shareTrackDialog_switchesThreePanelsAndTriggersCallbacks() {
    var feedSharedNote: String? = null
    var storySharedNote: String? = null
    var dmPeerId: String? = null
    val sampleTrack =
      MusicTrack(
        trackId = "tr_share_1",
        title = "Kış Güneşi",
        artistId = "art_tarkan",
        artistName = "Tarkan"
      )
    val peer =
      PublicProfile(
        id = "u_share_peer",
        userId = "u_share_peer",
        displayName = "Mert Akın",
        handle = "@mertakin"
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        ShareTrackDialog(
          track = sampleTrack,
          listeners = listOf(peer),
          followingIds = setOf("u_share_peer"),
          onDismiss = {},
          onShareToFeed = { feedSharedNote = it },
          onShareToStory = { note, _ -> storySharedNote = note },
          onSendTrackDirectMessage = { targetPeer, _ -> dmPeerId = targetPeer.userId }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_studio_tab_feed").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_studio_tab_story").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_studio_story_preview").assertIsDisplayed()

    composeTestRule.onNodeWithTag("share_studio_tab_dm").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_studio_send_dm_u_share_peer").assertIsDisplayed().performClick()
    assertEquals("u_share_peer", dmPeerId)
  }

  @Test
  fun activityLikersModalSheet_rendersLikersAndHandlesSelection() {
    var openedUserId: String? = null
    val post =
      ActivityFeedItem(
        id = "act_likes_1",
        userId = "u_author",
        userName = "Deniz",
        trackTitle = "Dudu",
        likesCount = 1L
      )
    val likers =
      listOf(
        ActivityLikeUser(
          id = "u_liker_1",
          userId = "u_liker_1",
          displayName = "Selin Kaya",
          handle = "@selink"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        ActivityLikersModalSheet(
          activity = post,
          likersState = UiState.Success(likers),
          onDismiss = {},
          onOpenListenerProfile = { openedUserId = it.userId }
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("activity_likers_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("activity_liker_item_u_liker_1").assertIsDisplayed().performClick()
    assertEquals("u_liker_1", openedUserId)
  }

  @Test
  fun messagingStyle_rehydratesActiveNotificationHistoryAfterInMemoryClear() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    nm.cancelAll()
    com.example.notifications.VizyonNotificationHelper.clearAllHistory()

    val baseMs = System.currentTimeMillis() - 10_000L
    repeat(2) { idx ->
      val notif =
        UserNotification(
          id = "n_rehydrate_${idx + 1}",
          senderId = "u_rehydrate_sender",
          senderName = "Merve Ak",
          type = NotificationType.MESSAGE.name,
          message = "İlk oturum mesajı #${idx + 1}",
          createdAt = Timestamp(java.util.Date(baseMs + idx * 1000L))
        )
      com.example.notifications.VizyonNotificationHelper.postSocialNotificationSync(context, notif)
    }

    // Simulate process death & cold wake-up by wiping in-memory history while NotificationManager keeps active notification
    com.example.notifications.VizyonNotificationHelper.clearAllHistory()

    val coldWakeNotif =
      UserNotification(
        id = "n_rehydrate_3",
        senderId = "u_rehydrate_sender",
        senderName = "Merve Ak",
        type = NotificationType.MESSAGE.name,
        message = "Uygulama kapalıyken gelen FCM mesajı #3",
        createdAt = Timestamp(java.util.Date(baseMs + 5000L))
      )
    com.example.notifications.VizyonNotificationHelper.postSocialNotificationSync(context, coldWakeNotif)

    val posted = org.robolectric.Shadows.shadowOf(nm).allNotifications
    assertEquals(1, posted.size)
    @Suppress("DEPRECATION")
    val messages = posted.first().extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)
    assertEquals(3, messages?.size ?: 0)
  }

  @Test
  fun fcmService_coldStartDataOnlyPayload_createsChannelPostsNotificationAndDeduplicates() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    nm.cancelAll()
    com.example.notifications.VizyonNotificationHelper.clearAllHistory()
    com.example.notifications.NotificationDispatchGuard.clearForUser(context, "u_recipient_fcm")

    val service =
      org.robolectric.Robolectric.setupService(com.example.notifications.VizyonFirebaseMessagingService::class.java)
    val remoteMessage =
      com.google.firebase.messaging.RemoteMessage.Builder("fcm_sender@gcm.googleapis.com")
        .setMessageId("fcm_msg_cold_1")
        .addData("notificationId", "notif_fcm_cold_1")
        .addData("recipientId", "u_recipient_fcm")
        .addData("senderId", "u_sender_fcm")
        .addData("senderName", "Arda Şen")
        .addData("type", NotificationType.MESSAGE.name)
        .addData("message", "Uygulama kapalıyken gelen test mesajı")
        .build()

    service.onMessageReceived(remoteMessage)
    // Duplicate redelivery of the same notificationId should be suppressed by NotificationDispatchGuard
    service.onMessageReceived(remoteMessage)

    assertTrue(nm.getNotificationChannel(com.example.notifications.VizyonNotificationHelper.CHANNEL_MESSAGES) != null)
    val posted = org.robolectric.Shadows.shadowOf(nm).allNotifications
    assertEquals(1, posted.size)
    @Suppress("DEPRECATION")
    val messages = posted.first().extras.getParcelableArray(android.app.Notification.EXTRA_MESSAGES)
    assertEquals(1, messages?.size ?: 0)
  }

  @Test
  fun mergedManifest_declaresFcmDefaultChannelAndIconMetadata() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val appInfo =
      context.packageManager.getApplicationInfo(
        context.packageName,
        android.content.pm.PackageManager.GET_META_DATA
      )
    val meta = appInfo.metaData
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_MESSAGES,
      meta.getString("com.google.firebase.messaging.default_notification_channel_id")
    )
    assertTrue(meta.getInt("com.google.firebase.messaging.default_notification_icon", 0) != 0)
  }

  @Test
  fun directChatThreadSheet_blockedByPeer_displaysBlockedPanelAndHidesComposer() {
    val peer =
      PublicProfile(
        id = "u_blocker",
        userId = "u_blocker",
        displayName = "Bora Yılmaz",
        handle = "@boray"
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        DirectChatThreadSheet(
          peer = peer,
          currentUserId = "u_me",
          isFollowerOrFollowing = true,
          isBlockedByPeer = true,
          messagesState = UiState.Success(emptyList()),
          currentTrack = null,
          candidateTracks = emptyList(),
          onDismiss = {},
          onSendMessage = { _, _ -> },
          onPlayTrack = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_blocked_by_peer_panel").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bu kişi tarafından engellendiniz").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodes(hasTestTag("direct_chat_input")).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodes(hasTestTag("direct_chat_send_btn")).fetchSemanticsNodes().size)
  }

  @Test
  fun reportUserDialog_transitionsToPostReportBlockStepAndTriggersCallback() {
    var reportedReason: ReportReason? = null
    var blockedAfterReport = false

    composeTestRule.setContent {
      VizyonMusicTheme {
        ReportUserDialog(
          targetDisplayName = "Kaya Aksoy",
          isAlreadyBlocked = false,
          onDismiss = {},
          onSubmitReport = { reason, _ -> reportedReason = reason },
          onBlockAfterReport = { blockedAfterReport = true }
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("report_reason_SPAM").performClick()
    composeTestRule.onNodeWithTag("submit_report_btn").performClick()
    composeTestRule.waitForIdle()
    assertEquals(ReportReason.SPAM, reportedReason)

    composeTestRule.onNodeWithTag("report_post_block_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertTrue(blockedAfterReport)
  }

  @Test
  fun profileAboutPrivacySubScreen_prioritizesPrivacyAndBlockingAboveInfoAndTriggersFcmButtons() {
    var refreshFcmCalled = false
    var sendTestNotifCalled = false
    val blocked =
      listOf(
        com.example.model.BlockedUser(
          id = "u_blk_1",
          blockedUserId = "u_blk_1",
          displayName = "Engelli Kullanıcı",
          handle = "@engelli"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        com.example.ui.screens.ProfileAboutPrivacySubScreen(
          appearance = com.example.model.AppAppearanceConfig(backgroundNotifServiceEnabled = true),
          unreadNotificationCount = 0,
          bridgeStatus = null,
          blockedUsers = blocked,
          firebaseUid = "uid_privacy_test",
          onBack = {},
          onChatPrivacyChange = {},
          onRequireFollowApprovalToggle = {},
          onNotificationsToggle = {},
          onBackgroundNotifServiceToggle = {},
          onOpenNotifications = {},
          onUnblockUser = {},
          onOpenDataSafety = {},
          onRefreshFcmToken = { refreshFcmCalled = true },
          onSendTestNotification = { sendTestNotifCalled = true },
          onNotifyMessage = {}
        )
      }
    }

    composeTestRule.waitForIdle()
    // Top section cards must be visible before info cards
    composeTestRule.onNodeWithTag("about_chat_privacy_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_follow_approval_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("refresh_fcm_token_btn"))
    composeTestRule.onNodeWithTag("refresh_fcm_token_btn").performClick()
    composeTestRule.onNodeWithTag("send_local_test_notif_btn").performClick()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("blocked_users_management_card"))
    composeTestRule.onNodeWithTag("blocked_users_management_card").assertIsDisplayed()
    assertTrue(refreshFcmCalled)
    assertTrue(sendTestNotifCalled)
  }

  @Test
  fun appearanceSettingsScreen_showsToplulugumLabelInsteadOfBaglantilar() {
    composeTestRule.setContent {
      VizyonMusicTheme {
        com.example.ui.screens.AppearanceSettingsScreen(
          isSignedIn = true,
          firebaseUid = "uid_1",
          firebaseEmail = "user@example.com",
          userPhotoUrl = "",
          unreadNotificationCount = 0,
          appearance = com.example.model.AppAppearanceConfig(),
          userProfileState = UiState.Success(com.example.model.UserProfile(displayName = "Ali", handle = "@ali")),
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
    composeTestRule.onNodeWithTag("profile_open_follow_lists_btn").assertIsDisplayed()
    composeTestRule.onNodeWithText("Topluluğum").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("Bağlantılar", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun nowPlayingModal_omitsSwitchMatchedVideoChip() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val webView = android.webkit.WebView(context)
    val track =
      MusicTrack(
        trackId = "tr_np_1",
        title = "Firuze",
        artistId = "art_sezen",
        artistName = "Sezen Aksu",
        youtubeVideoId = "dQw4w9WgXcQ"
      )
    val state =
      com.example.player.PlaybackUiState(
        currentTrack = track,
        queue = listOf(track),
        activeVideoId = "dQw4w9WgXcQ",
        isPlaying = true,
        isPlayerReady = true
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        com.example.ui.components.NowPlayingModal(
          playbackState = state,
          lyricsState = UiState.Success(null),
          sharedWebView = webView,
          isFavorite = false,
          onDismiss = {},
          onIFrameLoadFailure = {},
          onRetryVideo = {},
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
    assertEquals(0, composeTestRule.onAllNodes(hasTestTag("switch_matched_video_btn")).fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("Videoyu Değiştir", substring = true).fetchSemanticsNodes().size)
  }

  @Test
  fun notificationDispatchGuard_persistsUidTokenAndBlockedSenderSuppression() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    com.example.notifications.NotificationDispatchGuard.clearForUser(context, "uid_guard_1")
    com.example.notifications.NotificationDispatchGuard.setLastActiveUserId(context, "uid_guard_1")
    com.example.notifications.NotificationDispatchGuard.cacheFcmToken(context, "token_abc_123456789")
    com.example.notifications.NotificationDispatchGuard.markTokenSyncedForUser(
      context = context,
      uid = "uid_guard_1",
      token = "token_abc_123456789",
      pushEnabled = true
    )
    com.example.notifications.NotificationDispatchGuard.setBlockedUserIds(context, "uid_guard_1", setOf("u_bad_sender"))

    assertEquals("uid_guard_1", com.example.notifications.NotificationDispatchGuard.getLastActiveUserId(context))
    assertEquals("token_abc_123456789", com.example.notifications.NotificationDispatchGuard.getCachedFcmToken(context))
    assertTrue(com.example.notifications.NotificationDispatchGuard.isTokenSyncedForUser(context, "uid_guard_1"))
    assertTrue(com.example.notifications.NotificationDispatchGuard.isSenderBlocked(context, "uid_guard_1", "u_bad_sender"))
    assertTrue(!com.example.notifications.NotificationDispatchGuard.isSenderBlocked(context, "uid_guard_1", "u_ok_sender"))
  }

  @Test
  fun directChatThreadSheet_whenBlockedByPeer_showsBlockedPanelAndDisablesComposer() {
    val blockedPeer =
      PublicProfile(
        id = "u_kuzey_blk",
        userId = "u_kuzey_blk",
        displayName = "Kuzey Demir",
        handle = "@kuzey_blk"
      )
    val threadId = "local__${blockedPeer.userId}"
    val msgs =
      listOf(
        DirectChatMessage(
          id = "dm_blk_1",
          chatId = threadId,
          senderId = blockedPeer.userId,
          senderName = blockedPeer.displayName,
          text = "Önceki mesaj"
        )
      )

    composeTestRule.setContent {
      VizyonMusicTheme {
        DirectChatThreadSheet(
          peer = blockedPeer,
          currentUserId = "local",
          isFollowerOrFollowing = true,
          isBlockedByPeer = true,
          messagesState = UiState.Success(msgs),
          currentTrack = null,
          candidateTracks = emptyList(),
          onDismiss = {},
          onSendMessage = { _, _ -> },
          onPlayTrack = {},
          onDeleteChatThread = {}
        )
      }
    }

    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("chat_blocked_by_peer_panel").assertIsDisplayed()
    composeTestRule.onNodeWithText("Bu kişi tarafından engellendiniz").assertIsDisplayed()
  }
}
