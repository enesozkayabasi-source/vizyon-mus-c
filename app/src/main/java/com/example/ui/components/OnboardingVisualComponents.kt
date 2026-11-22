package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.StoryCardTheme
import com.example.util.DevicePerfHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class OnboardingStepMeta(
  val shortTitle: String,
  val headerTitle: String
)

/**
 * Full-bleed atmospheric backdrop featuring layered glowing orbs and three live
 * harmonic soundwaves that react subtly to the active onboarding step and genres.
 */
@Composable
fun AnimatedWaveformBackdrop(
  currentStep: Int,
  selectedGenreCount: Int,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }

  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val bg = MaterialTheme.colorScheme.background

  val bgBrush =
    remember(primary, bg) {
      Brush.verticalGradient(
        colors = listOf(primary.copy(alpha = 0.16f), bg, bg)
      )
    }

  if (isConstrained) {
    // Low-overhead static atmospheric backdrop for legacy Android / low-RAM devices
    val staticPath = remember { Path() }
    Canvas(
      modifier =
        modifier
          .fillMaxSize()
          .background(bgBrush)
    ) {
      val w = size.width
      val h = size.height
      val baseY = h * 0.19f
      val ampPx = 16.dp.toPx()
      staticPath.reset()
      val stepPx = 20f
      var x = 0f
      while (x <= w + stepPx) {
        val normX = (x / w.coerceAtLeast(1f))
        val envelope = sin(normX * PI).toFloat().coerceAtLeast(0.08f)
        val y = baseY + sin(normX * 2.5f * 2f * PI.toFloat()) * ampPx * envelope
        if (x == 0f) staticPath.moveTo(x, y) else staticPath.lineTo(x, y)
        x += stepPx
      }
      drawPath(
        path = staticPath,
        color = primary.copy(alpha = 0.24f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
      )
    }
    return
  }

  val transition = rememberInfiniteTransition(label = "onboarding_backdrop_wave")
  val wavePhase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 5200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "backdrop_wave_phase"
  )
  val orbDrift by transition.animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "backdrop_orb_drift"
  )
  val amplitudeScale by animateFloatAsState(
    targetValue = 0.85f + (currentStep * 0.12f) + (selectedGenreCount.coerceAtMost(5) * 0.06f),
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "backdrop_amp_scale"
  )
  val reusablePaths = remember { List(3) { Path() } }

  Canvas(
    modifier =
      modifier
        .fillMaxSize()
        .background(bgBrush)
  ) {
    val w = size.width
    val h = size.height

    val orb1 = Offset(w * (0.22f + 0.06f * orbDrift), h * 0.14f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(primary.copy(alpha = 0.30f), Color.Transparent),
        center = orb1,
        radius = w * 0.68f
      ),
      radius = w * 0.68f,
      center = orb1
    )

    val orb2 = Offset(w * (0.84f - 0.05f * orbDrift), h * 0.28f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(secondary.copy(alpha = 0.22f), Color.Transparent),
        center = orb2,
        radius = w * 0.56f
      ),
      radius = w * 0.56f,
      center = orb2
    )

    val orb3 = Offset(w * 0.5f, h * 0.86f)
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(tertiary.copy(alpha = 0.10f), Color.Transparent),
        center = orb3,
        radius = w * 0.62f
      ),
      radius = w * 0.62f,
      center = orb3
    )

    val waveConfigs = listOf(
      Triple(0.18f, 22.dp.toPx() * amplitudeScale, primary.copy(alpha = 0.32f)),
      Triple(0.205f, 16.dp.toPx() * amplitudeScale, secondary.copy(alpha = 0.26f)),
      Triple(0.23f, 11.dp.toPx() * amplitudeScale, tertiary.copy(alpha = 0.18f))
    )

    waveConfigs.forEachIndexed { idx, (yFactor, ampPx, color) ->
      val path = reusablePaths[idx]
      path.reset()
      val baseY = h * yFactor
      val stepPx = 14f
      val freq = 2.4f + idx * 0.75f
      val phaseOffset = wavePhase * (1f + idx * 0.25f) + idx * 1.1f
      var x = 0f
      while (x <= w + stepPx) {
        val normX = (x / w.coerceAtLeast(1f))
        val envelope = sin(normX * PI).toFloat().coerceAtLeast(0.08f)
        val y =
          baseY +
            sin(normX * freq * 2f * PI.toFloat() + phaseOffset) * ampPx * envelope +
            cos(normX * 1.3f * PI.toFloat() - wavePhase) * (ampPx * 0.35f) * envelope
        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
        x += stepPx
      }
      drawPath(
        path = path,
        color = color,
        style = Stroke(width = (2.2f - idx * 0.45f).dp.toPx(), cap = StrokeCap.Round)
      )
    }
  }
}

/**
 * Glassmorphic 3D depth card with frosted translucent surface, soft luminous
 * outer aura (zero muddy hardware shadow bleed-through), and crisp clipped border.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassStudioCard(
  modifier: Modifier = Modifier,
  highlighted: Boolean = false,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  cornerRadius: Dp = 22.dp,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val shape = RoundedCornerShape(cornerRadius)
  val secondaryColor = MaterialTheme.colorScheme.secondary
  val borderBrush =
    Brush.linearGradient(
      colors =
        if (highlighted) {
          listOf(
            accentColor.copy(alpha = 0.85f),
            secondaryColor.copy(alpha = 0.45f),
            accentColor.copy(alpha = 0.25f)
          )
        } else {
          listOf(
            Color.White.copy(alpha = 0.24f),
            MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
            accentColor.copy(alpha = 0.18f)
          )
        }
    )
  val containerColor =
    if (highlighted) {
      MaterialTheme.colorScheme.surface.copy(alpha = if (isConstrained) 0.94f else 0.88f)
    } else {
      MaterialTheme.colorScheme.surface.copy(alpha = if (isConstrained) 0.88f else 0.78f)
    }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .then(
          if (isConstrained) {
            Modifier
          } else {
            Modifier.drawBehind {
              // Soft shape-matched luminous halo — never leaves a dark rectangular shadow under translucent glass
              val radiusPx = cornerRadius.toPx()
              val haloAlpha1 = if (highlighted) 0.14f else 0.05f
              val haloAlpha2 = if (highlighted) 0.07f else 0.025f
              val pad1 = 1.5.dp.toPx()
              val pad2 = 4.dp.toPx()
              drawRoundRect(
                color = accentColor.copy(alpha = haloAlpha2),
                topLeft = Offset(-pad2, -pad2),
                size = Size(size.width + pad2 * 2f, size.height + pad2 * 2f),
                cornerRadius = CornerRadius(radiusPx + pad2, radiusPx + pad2),
                style = Stroke(width = 4.dp.toPx())
              )
              drawRoundRect(
                color = accentColor.copy(alpha = haloAlpha1),
                topLeft = Offset(-pad1, -pad1),
                size = Size(size.width + pad1 * 2f, size.height + pad1 * 2f),
                cornerRadius = CornerRadius(radiusPx + pad1, radiusPx + pad1),
                style = Stroke(width = 2.dp.toPx())
              )
            }
          }
        )
        .clip(shape)
        .background(containerColor)
        .background(
          Brush.verticalGradient(
            colors = listOf(
              accentColor.copy(alpha = if (highlighted) 0.11f else 0.045f),
              Color.Transparent
            )
          )
        )
        .border(
          width = if (highlighted) 1.35.dp else 1.dp,
          brush = borderBrush,
          shape = shape
        )
        .then(
          when {
            onLongClick != null ->
              Modifier.combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
              )
            onClick != null -> Modifier.clickable(onClick = onClick)
            else -> Modifier
          }
        )
  ) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
      Column(content = content)
    }
  }
}

/**
 * Interactive 22-bar live animated soundwave equalizer rendered smoothly on Canvas.
 */
@Composable
fun LiveSoundwaveEqualizer(
  modifier: Modifier = Modifier,
  barCount: Int = 22,
  energyBoost: Float = 1f,
  height: Dp = 32.dp
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary

  val mainBrush = remember(primary, secondary) { Brush.verticalGradient(listOf(secondary, primary)) }
  val accentBrush = remember(primary, tertiary) { Brush.verticalGradient(listOf(tertiary, primary)) }

  val phase =
    if (isConstrained) {
      1.15f
    } else {
      val transition = rememberInfiniteTransition(label = "live_eq")
      val animatedPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
      )
      animatedPhase
    }

  val effectiveBars = if (isConstrained) barCount.coerceAtMost(14) else barCount

  Canvas(
    modifier =
      modifier
        .fillMaxWidth()
        .height(height)
        .testTag("onboarding_live_waveform")
  ) {
    val totalW = size.width
    val totalH = size.height
    val slotW = totalW / effectiveBars.coerceAtLeast(1)
    val barW = (slotW * 0.46f).coerceIn(2.5.dp.toPx(), 6.dp.toPx())

    for (i in 0 until effectiveBars) {
      val centerDist = 1f - kotlin.math.abs((i - (effectiveBars - 1) / 2f) / ((effectiveBars - 1) / 2f).coerceAtLeast(1f))
      val wave1 = (sin(phase + i * 0.55f) + 1f) * 0.5f
      val wave2 = (cos(phase * 1.4f - i * 0.35f) + 1f) * 0.5f
      val factor = ((wave1 * 0.65f + wave2 * 0.35f) * (0.45f + 0.55f * centerDist) * energyBoost)
        .coerceIn(0.18f, 1f)
      val barH = (totalH * factor).coerceAtLeast(4.dp.toPx())
      val left = i * slotW + (slotW - barW) * 0.5f
      val top = (totalH - barH) * 0.5f

      drawRoundRect(
        brush = if (i % 5 == 0) accentBrush else mainBrush,
        topLeft = Offset(left, top),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(barW * 0.5f, barW * 0.5f)
      )
    }
  }
}

/**
 * Interactive real-time listener card preview reflecting displayName, @handle,
 * language/catalog region, theme, authentication badge, selected music genres,
 * and a live animated waveform.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveProfilePreviewCard(
  displayName: String,
  handle: String,
  bio: String,
  selectedLanguage: AppLanguage,
  selectedTheme: AppThemeMode,
  selectedGenres: List<String>,
  isSignedInWithGoogle: Boolean,
  ageVerified: Boolean,
  shareActivity: Boolean,
  modifier: Modifier = Modifier,
  localAvatarPath: String = "",
  cornerRadiusDp: Dp = 24.dp,
  selectedStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
  selectedChatPrivacy: ChatPrivacy = ChatPrivacy.PUBLIC,
  passcodeConfigured: Boolean = false
) {
  val cleanName = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }
  val cleanHandle = "@${handle.trim().ifBlank { "vizyon" }}"
  val initials =
    cleanName
      .split(Regex("\\s+"))
      .filter { it.isNotBlank() }
      .take(2)
      .joinToString("") { it.first().uppercaseChar().toString() }
      .ifBlank { "VM" }

  GlassStudioCard(
    highlighted = true,
    cornerRadius = cornerRadiusDp,
    modifier = modifier.testTag("onboarding_live_preview_card")
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "CANLI PROFİL & VİTRİN ÖNİZLEMESİ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = "${selectedLanguage.badge} • ${selectedTheme.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (localAvatarPath.isNotBlank()) {
          StudioAvatar(
            photoUrlOrPath = localAvatarPath,
            contentDescription = cleanName,
            size = 54.dp,
            ringColor = MaterialTheme.colorScheme.primary
          )
        } else {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(54.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                  )
                )
                .border(2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
          ) {
            Text(
              text = initials,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = cleanName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = cleanHandle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (bio.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = bio.trim(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        selectedGenres.forEach { genre ->
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                RoundedCornerShape(999.dp)
              )
          ) {
            Text(
              text = genre,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          LiveSoundwaveEqualizer(
            barCount = 24,
            energyBoost = (0.70f + selectedGenres.size * 0.08f).coerceAtMost(1.1f),
            height = 26.dp
          )
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isSignedInWithGoogle) Icons.Rounded.CloudDone else Icons.Rounded.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text =
                  buildString {
                    append(if (ageVerified) "13+ Onaylı" else "Doğrulama Bekleniyor")
                    append(" • ")
                    append(if (isSignedInWithGoogle) "Firebase Auth" else "Yerel Mod")
                  },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Text(
              text = "${selectedStoryTheme.label} • ${if (selectedChatPrivacy == ChatPrivacy.PUBLIC) "Açık DM" else "Takipçi DM"}",
              style = MaterialTheme.typography.labelSmall,
              color = Color(selectedStoryTheme.accentColorHex),
              fontWeight = FontWeight.Bold
            )
          }
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = if (passcodeConfigured) "Şifre Korumalı Kasa ✓" else "Şifre Kurulumu Bekleniyor",
              style = MaterialTheme.typography.labelSmall,
              color = if (passcodeConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (shareActivity) "Akış Açık" else "Gizli Dinleme",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
fun SchemaStepperHeader(
  steps: List<OnboardingStepMeta>,
  currentStep: Int
) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
    shape = RoundedCornerShape(20.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          Color.White.copy(alpha = 0.14f),
          RoundedCornerShape(20.dp)
        )
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
      steps.forEachIndexed { index, meta ->
        val isCompleted = index < currentStep
        val isCurrent = index == currentStep
        val scale by animateFloatAsState(
          targetValue = if (isCurrent) 1.08f else 1f,
          animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
          label = "stepper_node_scale"
        )
        val nodeBg by animateColorAsState(
          targetValue =
            when {
              isCompleted || isCurrent -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            },
          label = "stepper_node_bg"
        )
        val labelColor by animateColorAsState(
          targetValue =
            when {
              isCurrent -> MaterialTheme.colorScheme.primary
              isCompleted -> MaterialTheme.colorScheme.onSurface
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
          label = "stepper_label_color"
        )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.width(62.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(32.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(nodeBg)
                .border(
                  width = if (isCurrent) 2.dp else 0.dp,
                  color = if (isCurrent) MaterialTheme.colorScheme.secondary else Color.Transparent,
                  shape = CircleShape
                )
          ) {
            if (isCompleted) {
              Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            } else {
              Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = meta.shortTitle,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (index < steps.lastIndex) {
          val lineColor by animateColorAsState(
            targetValue =
              if (index < currentStep) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            label = "stepper_line_color"
          )
          Box(
            modifier =
              Modifier.weight(1f)
                .padding(bottom = 18.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(lineColor)
          )
        }
      }
    }
  }
}

@Composable
fun SchemaFeatureCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  badge: String = "HAZIR"
) {
  GlassStudioCard(cornerRadius = 18.dp) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(14.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                  MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                )
              )
            )
            .border(
              1.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
              RoundedCornerShape(14.dp)
            )
      ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.weight(1f)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = badge,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
          }
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
