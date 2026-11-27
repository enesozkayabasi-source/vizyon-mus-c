package com.example

import android.content.Context
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppAppearanceConfig
import com.example.model.AppThemeMode
import com.example.model.DISCOVER_AUTO_REFRESH_INTERVAL_MS
import com.example.ui.theme.VizyonMusicTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Vizyon Music", appName)
    assertEquals(30L * 60L * 1000L, DISCOVER_AUTO_REFRESH_INTERVAL_MS)
  }

  @Test
  fun `first launch 8 step onboarding, pure stories, requests sheet, data safety, long press delete, and top settings work end to end`() {
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithTag("onboarding_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()

    // Step 0 (1. Hoş Geldin & Bulut Geri Yükleme)
    composeTestRule.onNodeWithTag("onboarding_cloud_restore_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 1 (2. Kimlik & Avatar)
    composeTestRule.onNodeWithTag("onboarding_pick_photo_btn").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_name_input").performScrollTo().performTextInput("Deniz Kaya")
    composeTestRule.onNodeWithTag("onboarding_handle_input").performScrollTo().performTextInput("denizkaya")
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 2 (3. Tema & Canlı Önizleme)
    composeTestRule.onNodeWithTag("onboarding_live_preview_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_theme_NIGHT_AMOLED").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 3 (4. Hikaye Stili, Sohbet & Takip Onayı Gizliliği)
    composeTestRule.onNodeWithTag("onboarding_story_theme_AMOLED_GOLD").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_chat_privacy_PUBLIC").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_share_switch").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_follow_approval_switch").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 4 (5. Dil & Müzik Tarzları)
    composeTestRule.onNodeWithTag("onboarding_live_waveform").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_lang_card_BILINGUAL").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("genre_chip_ARABESK_FANTASY").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("genre_chip_ELECTRONIC").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 5 (6. Güvenlik Kasası Şifresi)
    composeTestRule.onNodeWithTag("onboarding_passcode_input").performScrollTo().performTextInput("123456")
    composeTestRule.onNodeWithTag("onboarding_passcode_confirm_input").performScrollTo().performTextInput("123456")
    composeTestRule.onNodeWithTag("onboarding_remember_me_switch").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 6 (7. Ağ Kontrolü, 13+ Yaş Onayı & Arka Plan Bildirimleri)
    composeTestRule.onNodeWithTag("check_network_btn").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_age_switch").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("onboarding_bg_notif_switch").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_next_btn").performClick()
    composeTestRule.waitForIdle()

    // Step 7 (8. Kurulum Özeti & Tamamla -> DiscoverScreen)
    composeTestRule.onNodeWithTag("onboarding_setup_summary_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("onboarding_complete_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithTag("discover_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("discover_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notifications_bell_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("discover_refresh_status_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("category_group_TURKISH").assertIsDisplayed()
    composeTestRule.onNodeWithTag("category_group_TURKISH").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("category_chip_ANATOLIAN_ROCK").assertIsDisplayed()

    // Dismiss onboarding snackbar & verify Notification Inbox sheet + filter chips
    composeTestRule.mainClock.advanceTimeBy(4500L)
    composeTestRule.onNodeWithTag("notifications_bell_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("notification_inbox_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_filter_ALL").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_filter_UNREAD").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_filter_STORY").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_filter_MESSAGE").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notif_more_menu_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("mark_all_notifications_read_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("clear_all_notifications_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_notification_inbox_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()

    // Verify predictive Search Autocomplete ("müs" -> "Müslüm Gürses") on Ara tab
    composeTestRule.onNodeWithTag("nav_tab_Ara").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_input").performTextInput("müs")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_autocomplete_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Müslüm Gürses").assertIsDisplayed()

    // Navigate to Akış tab -> verify header Profile shortcut AND pure Stories strip (no "Sen/Profilim" ring!)
    composeTestRule.onNodeWithTag("nav_tab_Akış").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("social_feed_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_profile_btn").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("story_ring_self").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithTag("feed_search_toggle_btn").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("feed_user_search_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_messages_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_tab_ALL").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_tab_FOLLOWING").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_tab_LISTENERS").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_tab_CHATS").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_requests_btn").assertIsDisplayed().performClick()
    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("requests_modal_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("requests_tab_follow").assertIsDisplayed()
    composeTestRule.onNodeWithTag("requests_tab_message").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_requests_sheet_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("feed_stories_strip").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_quick_composer").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithText("EN SON DİNLEDİĞİ").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithText("CANLI").fetchSemanticsNodes().size)

    // Switch across tabs smoothly including Sohbet tab
    composeTestRule.onNodeWithTag("feed_tab_CHATS").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("direct_chats_list_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("feed_tab_LISTENERS").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("feed_tab_ALL").performClick()
    composeTestRule.waitForIdle()

    // Tap Header Profile Shortcut -> Detailed Listener Profile Sheet showing followers/following & Son 5 Dinlenen Şarkı
    composeTestRule.onNodeWithTag("feed_profile_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("listener_profile_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_followers_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_following_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_recent_tracks_section").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_listener_profile_sheet_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()

    // Open Story Studio Sheet -> publish a story -> open StoryViewerDialog & verify bottom action buttons are visible!
    composeTestRule.onNodeWithTag("social_feed_screen").performScrollToNode(hasTestTag("open_story_studio_btn"))
    composeTestRule.onNodeWithTag("open_story_studio_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("story_studio_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_studio_sticky_footer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_story_png_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("publish_story_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_card_preview").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_theme_AMOLED_GOLD").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("story_note_input").performScrollTo().performTextInput("Gece modu sahnede")
    composeTestRule.onNodeWithTag("publish_story_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithText("Hikayen paylaşıldı!").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.mainClock.advanceTimeBy(4500L)
    composeTestRule.waitForIdle()

    val storyItemMatcher =
      SemanticsMatcher("story item") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("story_item_") == true
      }
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodes(storyItemMatcher).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onAllNodes(storyItemMatcher)[0].performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("story_viewer_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_bottom_bar").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_play_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("story_viewer_share_png_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_story_viewer_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()

    // Publish quick post via compact single-line composer -> test Comment, Repost & Long-Press Delete!
    composeTestRule.onNodeWithTag("social_feed_screen").performScrollToNode(hasTestTag("feed_composer_input"))
    composeTestRule.onNodeWithTag("feed_composer_input").performTextInput("Gece stüdyo seansı başlıyor!")
    composeTestRule.onNodeWithTag("social_feed_screen").performScrollToNode(hasTestTag("feed_publish_post_btn"))
    composeTestRule.onNodeWithTag("feed_publish_post_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithText("Akışta paylaşıldı!").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.mainClock.advanceTimeBy(4500L)
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.mainClock.advanceTimeBy(50L)
      composeTestRule.onAllNodesWithText("“Gece stüdyo seansı başlıyor!”").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("social_feed_screen")
      .performScrollToNode(hasText("“Gece stüdyo seansı başlıyor!”"))
    composeTestRule.onNodeWithText("“Gece stüdyo seansı başlıyor!”").assertIsDisplayed()

    val commentBtnMatcher =
      SemanticsMatcher("comment btn") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("comment_activity_btn_") == true
      }
    val postCardMatcher =
      SemanticsMatcher("post card") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("activity_post_card_") == true
      }

    // Open Comments sheet on the published post and submit a comment
    composeTestRule.onNodeWithTag("social_feed_screen").performScrollToNode(commentBtnMatcher)
    composeTestRule.onAllNodes(commentBtnMatcher)[0].performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.mainClock.advanceTimeBy(50L)
      composeTestRule.onAllNodesWithTag("close_comments_sheet_btn").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("close_comments_sheet_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comment_input").performTextInput("Çok iyi parça!")
    composeTestRule.onNodeWithTag("submit_comment_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithText("Çok iyi parça!").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("close_comments_sheet_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(4500L)
    composeTestRule.waitForIdle()

    // Long-press published post -> opens bottom action sheet -> Delete Post removes it cleanly
    composeTestRule.onNodeWithTag("social_feed_screen").performScrollToNode(postCardMatcher)
    composeTestRule.onAllNodes(postCardMatcher)[0].performTouchInput { longClick() }
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.mainClock.advanceTimeBy(50L)
      composeTestRule.onAllNodesWithTag("post_actions_bottom_sheet").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("post_action_delete_btn").assertIsDisplayed().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithText("“Gece stüdyo seansı başlıyor!”").fetchSemanticsNodes().isEmpty()
    }
    composeTestRule.mainClock.advanceTimeBy(4500L)
    composeTestRule.waitForIdle()

    // Navigate to Profil -> Instagram-style overview (no exposed edit fields or vault inputs!)
    composeTestRule.onNodeWithTag("nav_tab_Profil").performClick()
    composeTestRule.mainClock.advanceTimeBy(1000L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_overview_hero_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_edit_profile_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_about_privacy_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("open_profile_settings_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_open_follow_lists_btn").assertIsDisplayed()
    assertEquals(0, composeTestRule.onAllNodesWithTag("save_profile_btn").fetchSemanticsNodes().size)
    assertEquals(0, composeTestRule.onAllNodesWithTag("profile_vault_privacy_card").fetchSemanticsNodes().size)
    composeTestRule.onNodeWithTag("studio_settings_screen")
      .performScrollToNode(hasTestTag("own_profile_recent_tracks_card"))
    composeTestRule.onNodeWithTag("own_profile_recent_tracks_card").assertIsDisplayed()

    // Sub-page 1: Profili Düzenle
    composeTestRule.onNodeWithTag("studio_settings_screen")
      .performScrollToNode(hasTestTag("open_edit_profile_btn"))
    composeTestRule.onNodeWithTag("open_edit_profile_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_edit_subscreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pick_profile_photo_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_edit_subscreen").performScrollToNode(hasTestTag("save_profile_btn"))
    composeTestRule.onNodeWithTag("save_profile_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_subpage_back_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()

    // Sub-page 2: Hakkında & Gizlilik -> Data Safety, Follow Approval, Background Service & Blocked Users
    composeTestRule.onNodeWithTag("open_about_privacy_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("about_chat_privacy_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("about_vizyon_card"))
    composeTestRule.onNodeWithTag("about_vizyon_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("open_data_safety_btn"))
    composeTestRule.onNodeWithTag("open_data_safety_btn").assertIsDisplayed().performClick()
    composeTestRule.mainClock.advanceTimeBy(350L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("data_safety_modal_sheet").assertIsDisplayed()
    composeTestRule.onNodeWithTag("close_data_safety_sheet_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(300L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("profile_follow_approval_switch"))
    composeTestRule.onNodeWithTag("profile_follow_approval_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("notifications_permission_switch"))
    composeTestRule.onNodeWithTag("notifications_permission_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("profile_bg_notif_switch"))
    composeTestRule.onNodeWithTag("profile_bg_notif_switch").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("blocked_users_management_card"))
    composeTestRule.onNodeWithTag("blocked_users_management_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_about_privacy_subscreen")
      .performScrollToNode(hasTestTag("profile_subpage_back_btn"))
    composeTestRule.onNodeWithTag("profile_subpage_back_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()

    // Sub-page 3: Profil Ayarları -> Theme & Personalization Panel is immediately visible AT THE TOP!
    composeTestRule.onNodeWithTag("open_profile_settings_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("profile_settings_subscreen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_personalization_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_settings_subscreen")
      .performScrollToNode(hasTestTag("profile_story_theme_card"))
    composeTestRule.onNodeWithTag("profile_story_theme_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_settings_subscreen")
      .performScrollToNode(hasTestTag("profile_auth_card"))
    composeTestRule.onNodeWithTag("profile_auth_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_google_auth_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_settings_subscreen")
      .performScrollToNode(hasTestTag("profile_vault_privacy_card"))
    composeTestRule.onNodeWithTag("profile_vault_privacy_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_settings_subscreen")
      .performScrollToNode(hasTestTag("profile_genres_card"))
    composeTestRule.onNodeWithTag("profile_genres_card").assertIsDisplayed()

    // Open Account Deletion Security Wall
    composeTestRule.onNodeWithTag("profile_settings_subscreen")
      .performScrollToNode(hasTestTag("open_delete_account_dialog_btn"))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("open_delete_account_dialog_btn").performClick()
    composeTestRule.mainClock.advanceTimeBy(400L)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("delete_account_dialog").assertIsDisplayed()

    // Confirm button must stay disabled until BOTH ack checkbox & SİL keyword pass
    composeTestRule.onNodeWithTag("confirm_delete_account_btn").assertIsNotEnabled()
    composeTestRule.onNodeWithTag("delete_account_ack_checkbox").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("confirm_delete_account_btn").assertIsNotEnabled()

    composeTestRule.onNodeWithTag("delete_account_confirm_input").performScrollTo().performTextInput("SİL")
    composeTestRule.onNodeWithTag("confirm_delete_account_btn").assertIsEnabled()

    // Confirm wipe -> resets cleanly back to 4-step Onboarding Setup Schema
    composeTestRule.onNodeWithTag("confirm_delete_account_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5000L) {
      composeTestRule.onAllNodesWithTag("onboarding_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag("onboarding_screen").assertIsDisplayed()
  }
}
