package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppearancePreferences
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ReopenLaunchRouteTest {

  @get:Rule
  val composeTestRule = createEmptyComposeRule()

  @Test
  fun `reopening app after completed onboarding skips onboarding without flash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = AppearancePreferences(context)
    runBlocking {
      prefs.completeOnboarding(
        displayName = "Deniz Kaya",
        handle = "@denizkaya",
        bio = "Test dinleyici",
        favoriteGenres = listOf("Pop", "Türkçe"),
        ageVerified = true,
        preferredLanguage = AppLanguage.TURKISH,
        themeMode = AppThemeMode.NIGHT_AMOLED,
        shareActivity = true
      )
    }

    val scenario = ActivityScenario.launch(MainActivity::class.java)
    try {
      // Immediately after Activity launch, onboarding_screen must NEVER appear
      assertTrue(
        "Onboarding screen must never flash when onboardingCompleted == true",
        composeTestRule.onAllNodesWithTag("onboarding_screen").fetchSemanticsNodes().isEmpty()
      )

      composeTestRule.waitUntil(timeoutMillis = 5000L) {
        composeTestRule.onAllNodesWithTag("discover_screen").fetchSemanticsNodes().isNotEmpty()
      }
      composeTestRule.onNodeWithTag("discover_screen").assertIsDisplayed()
      assertTrue(
        composeTestRule.onAllNodesWithTag("onboarding_screen").fetchSemanticsNodes().isEmpty()
      )
    } finally {
      scenario.close()
      runBlocking { prefs.resetAllForAccountDeletion() }
    }
  }
}
