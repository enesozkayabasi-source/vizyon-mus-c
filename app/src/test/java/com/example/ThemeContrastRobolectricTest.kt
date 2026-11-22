package com.example

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.AppAppearanceConfig
import com.example.model.AppThemeMode
import com.example.ui.theme.VizyonMusicTheme
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeContrastRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `amoled and dark themes never emit black LocalContentColor`() {
    var observedAmoledContentColor: Color = Color.Black
    var observedDarkContentColor: Color = Color.Black
    var observedAmoledOnBackground: Color = Color.Black
    var observedDarkOnBackground: Color = Color.Black
    composeTestRule.setContent {
      VizyonMusicTheme(appearance = AppAppearanceConfig(themeMode = AppThemeMode.NIGHT_AMOLED)) {
        observedAmoledContentColor = LocalContentColor.current
        observedAmoledOnBackground = MaterialTheme.colorScheme.onBackground
      }
      VizyonMusicTheme(appearance = AppAppearanceConfig(themeMode = AppThemeMode.DARK)) {
        observedDarkContentColor = LocalContentColor.current
        observedDarkOnBackground = MaterialTheme.colorScheme.onBackground
      }
    }
    assertNotEquals(Color.Black, observedAmoledContentColor)
    assertNotEquals(Color.Black, observedDarkContentColor)
    assertNotEquals(Color.Black, observedAmoledOnBackground)
    assertNotEquals(Color.Black, observedDarkOnBackground)
  }
}
