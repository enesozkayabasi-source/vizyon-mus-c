package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.google.firebase.Timestamp
import com.example.model.DirectChatMessage
import com.example.model.DirectChatThread
import com.example.model.MusicStoryItem
import com.example.model.NotificationType
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserNotification
import com.example.ui.components.DirectChatThreadSheet
import com.example.ui.components.DirectChatsListSection
import com.example.ui.components.InAppNotificationBubble
import com.example.ui.components.NotificationInboxSheet
import com.example.ui.components.SocialFeedTab
import com.example.ui.components.SocialSegmentedTabs
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.StoryViewerOverlay
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
  fun consecutiveBackgroundNotifications_postAllFiveWithoutSilentGroupCollapse() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    nm.cancelAll()

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
    assertTrue(posted.size >= 5)
    assertTrue(posted.none { (it.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0 })
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
}
