package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AppAppearanceConfig
import com.example.model.AppThemeMode

val LocalAppearanceConfig = staticCompositionLocalOf { AppAppearanceConfig() }

@Composable
fun VizyonMusicTheme(
  appearance: AppAppearanceConfig = AppAppearanceConfig(),
  content: @Composable () -> Unit
) {
  val systemDark = isSystemInDarkTheme()
  val primaryAccent = Color(appearance.accentPalette.primaryHex)
  val secondaryAccent = Color(appearance.accentPalette.secondaryHex)

  val isAmoled = appearance.themeMode == AppThemeMode.NIGHT_AMOLED
  val useDark =
    when (appearance.themeMode) {
      AppThemeMode.SYSTEM -> systemDark
      AppThemeMode.LIGHT -> false
      AppThemeMode.DARK, AppThemeMode.NIGHT_AMOLED -> true
    }

  val colorScheme =
    if (useDark) {
      darkColorScheme(
        primary = primaryAccent,
        onPrimary = Color.White,
        primaryContainer = primaryAccent.copy(alpha = 0.20f),
        onPrimaryContainer = primaryAccent,
        secondary = secondaryAccent,
        onSecondary = Color(0xFF04101C),
        secondaryContainer = secondaryAccent.copy(alpha = 0.16f),
        onSecondaryContainer = secondaryAccent,
        tertiary = WarmAmber,
        background = if (isAmoled) AmoledBlack else ObsidianBg,
        onBackground = Color(0xFFF3F6FC),
        surface = if (isAmoled) AmoledSurface else ObsidianSurface,
        onSurface = Color(0xFFF3F6FC),
        surfaceVariant = if (isAmoled) AmoledSurfaceHigh else ObsidianSurfaceVariant,
        onSurfaceVariant = Color(0xFF9EB0D0),
        outline = if (isAmoled) AmoledOutline else ObsidianOutline,
        error = Color(0xFFFF5A79),
        onError = Color(0xFF1A0005)
      )
    } else {
      lightColorScheme(
        primary = primaryAccent,
        onPrimary = Color.White,
        primaryContainer = primaryAccent.copy(alpha = 0.14f),
        onPrimaryContainer = Color(0xFF0A2458),
        secondary = secondaryAccent,
        onSecondary = Color.White,
        secondaryContainer = secondaryAccent.copy(alpha = 0.14f),
        onSecondaryContainer = Color(0xFF082438),
        tertiary = WarmAmber,
        background = DaylightBg,
        onBackground = DaylightOnBg,
        surface = DaylightSurface,
        onSurface = DaylightOnBg,
        surfaceVariant = DaylightSurfaceVariant,
        onSurfaceVariant = Color(0xFF475878),
        outline = DaylightOutline,
        error = Color(0xFFD92D4E),
        onError = Color.White
      )
    }

  val baseCorner = appearance.cornerDensity.cardCornerDp.dp
  val shapes =
    Shapes(
      small = RoundedCornerShape((baseCorner.value * 0.55f).dp),
      medium = RoundedCornerShape(baseCorner),
      large = RoundedCornerShape((baseCorner.value * 1.35f).dp)
    )

  CompositionLocalProvider(LocalAppearanceConfig provides appearance) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = VizyonTypography,
      shapes = shapes
    ) {
      CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
        content()
      }
    }
  }
}
