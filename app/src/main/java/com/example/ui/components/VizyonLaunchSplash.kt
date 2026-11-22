package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * Branded Vizyon Music splash gate shown briefly while DataStore appearance
 * preferences resolve on cold launch. Prevents the onboarding screen from flashing
 * on screen when reopening the app after setup has already been completed.
 */
@Composable
fun VizyonLaunchSplash(modifier: Modifier = Modifier) {
  val transition = rememberInfiniteTransition(label = "vizyon_splash_motion")
  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "splash_wave_phase"
  )
  val pulse by transition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "splash_ring_pulse"
  )

  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF050811), Color(0xFF091224), Color(0xFF04060D))
          )
        )
        .testTag("vizyon_launch_splash")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width * 0.5f, size.height * 0.46f)
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.24f), Color.Transparent),
            center = center,
            radius = size.minDimension * 0.55f * pulse
          ),
        radius = size.minDimension * 0.55f * pulse,
        center = center
      )
      drawCircle(
        color = primary.copy(alpha = 0.20f),
        radius = 86.dp.toPx() * pulse,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
      )
      drawCircle(
        color = secondary.copy(alpha = 0.14f),
        radius = 124.dp.toPx() * (2f - pulse),
        center = center,
        style = Stroke(width = 1.dp.toPx())
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      Surface(
        color = Color(0xFF0E1729),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier =
          Modifier.size(92.dp)
            .border(
              width = 1.5.dp,
              brush = Brush.linearGradient(listOf(primary, secondary)),
              shape = RoundedCornerShape(28.dp)
            )
      ) {
        Image(
          painter = painterResource(R.drawable.ic_vizyon_logo),
          contentDescription = "Vizyon Music",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp))
        )
      }

      Spacer(modifier = Modifier.height(22.dp))

      Text(
        text = "VIZYON MUSIC",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.4.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Stüdyo Deneyimi Hazırlanıyor",
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF9EB0D0)
      )

      Spacer(modifier = Modifier.height(22.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        for (i in 0 until 7) {
          val amp = 0.35f + 0.65f * ((sin(phase + i * 0.75f) + 1f) * 0.5f)
          Box(
            modifier =
              Modifier.width(4.dp)
                .height((10f + 18f * amp).dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                  Brush.verticalGradient(listOf(secondary, primary))
                )
          )
        }
      }
    }
  }
}
