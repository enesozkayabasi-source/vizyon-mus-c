package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccentPalette
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.StoryCardTheme
import com.example.util.DevicePerfHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-bleed Live Concert Stage & Neon Depth atmospheric backdrop.
 * Renders sweeping volumetric spotlights from top corners, horizon laser floor perspective lines,
 * glowing plasma orbs, and seamless continuous harmonic laser curves.
 */
@Composable
fun AnimatedStageConcertBackdrop(
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
  val isLightBg = bg.luminance() > 0.45f

  val stageGradient =
    remember(primary, secondary, bg, isLightBg) {
      Brush.verticalGradient(
        colors =
          if (isLightBg) {
            listOf(primary.copy(alpha = 0.14f), bg, secondary.copy(alpha = 0.08f))
          } else {
            listOf(Color(0xFF0A1024), bg, Color(0xFF060914))
          }
      )
    }

  if (isConstrained) {
    val staticPath = remember { Path() }
    Canvas(modifier = modifier.fillMaxSize().background(stageGradient)) {
      val w = size.width
      val h = size.height
      if (w <= 0f || h <= 0f) return@Canvas
      val beamPath = Path().apply {
        moveTo(w * 0.12f, 0f)
        lineTo(w * 0.72f, h * 0.62f)
        lineTo(w * 0.35f, h * 0.68f)
        close()
      }
      drawPath(beamPath, color = primary.copy(alpha = 0.09f))
      staticPath.reset()
      val baseY = h * 0.22f
      for (i in 0..48) {
        val nx = i / 48f
        val env = sin(nx * PI).toFloat()
        val y = baseY + sin(nx * 4f * PI.toFloat()) * 14.dp.toPx() * env
        if (i == 0) staticPath.moveTo(w * nx, y) else staticPath.lineTo(w * nx, y)
      }
      drawPath(staticPath, color = secondary.copy(alpha = 0.22f), style = Stroke(width = 2.dp.toPx()))
    }
    return
  }

  val infinite = rememberInfiniteTransition(label = "concert_stage_backdrop")
  val wavePhase by infinite.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
    label = "stage_wave_phase"
  )
  val beamSweep by infinite.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(tween(7800, easing = LinearEasing), RepeatMode.Restart),
    label = "stage_beam_sweep"
  )
  val stageEnergy by animateFloatAsState(
    targetValue = 0.85f + (currentStep * 0.06f) + (selectedGenreCount.coerceAtMost(5) * 0.04f),
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "stage_energy"
  )
  val laserPaths = remember { List(3) { Path() } }
  val beamPathLeft = remember { Path() }
  val beamPathRight = remember { Path() }

  Canvas(modifier = modifier.fillMaxSize().background(stageGradient)) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    val sweepL = sin(beamSweep.toDouble()).toFloat()
    val sweepR = cos(beamSweep.toDouble()).toFloat()
    val beamAlpha = if (isLightBg) 0.08f else 0.14f

    // Volumetric left spotlight cone
    beamPathLeft.reset()
    beamPathLeft.moveTo(w * 0.08f, -20f)
    beamPathLeft.lineTo(w * (0.64f + 0.16f * sweepL), h * 0.68f)
    beamPathLeft.lineTo(w * (0.24f + 0.10f * sweepL), h * 0.72f)
    beamPathLeft.close()
    drawPath(
      path = beamPathLeft,
      brush =
        Brush.verticalGradient(
          colors = listOf(primary.copy(alpha = beamAlpha * 1.4f), primary.copy(alpha = 0.03f), Color.Transparent),
          startY = 0f,
          endY = h * 0.72f
        )
    )

    // Volumetric right spotlight cone
    beamPathRight.reset()
    beamPathRight.moveTo(w * 0.92f, -20f)
    beamPathRight.lineTo(w * (0.76f + 0.12f * sweepR), h * 0.70f)
    beamPathRight.lineTo(w * (0.36f + 0.16f * sweepR), h * 0.66f)
    beamPathRight.close()
    drawPath(
      path = beamPathRight,
      brush =
        Brush.verticalGradient(
          colors = listOf(secondary.copy(alpha = beamAlpha * 1.2f), secondary.copy(alpha = 0.03f), Color.Transparent),
          startY = 0f,
          endY = h * 0.70f
        )
    )

    // Ambient stage nebula glow orbs
    val topOrbCenter = Offset(w * (0.5f + 0.12f * sweepL), h * 0.12f)
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(primary.copy(alpha = if (isLightBg) 0.14f else 0.24f), Color.Transparent),
          center = topOrbCenter,
          radius = w * 0.68f
        ),
      radius = w * 0.68f,
      center = topOrbCenter
    )
    val floorOrbCenter = Offset(w * 0.5f, h * 0.92f)
    drawCircle(
      brush =
        Brush.radialGradient(
          colors = listOf(secondary.copy(alpha = if (isLightBg) 0.10f else 0.16f), Color.Transparent),
          center = floorOrbCenter,
          radius = w * 0.78f
        ),
      radius = w * 0.78f,
      center = floorOrbCenter
    )

    // Subtle concert stage laser floor perspective lines
    val horizonY = h * 0.80f
    val floorLineColor = primary.copy(alpha = if (isLightBg) 0.08f else 0.13f)
    for (r in 0..3) {
      val ry = horizonY + (h - horizonY) * (r / 4f)
      drawLine(
        color = floorLineColor,
        start = Offset(0f, ry),
        end = Offset(w, ry),
        strokeWidth = 1.dp.toPx()
      )
    }
    for (c in -2..2) {
      val topX = w * 0.5f + c * (w * 0.11f)
      val botX = w * 0.5f + c * (w * 0.34f)
      drawLine(
        color = secondary.copy(alpha = if (isLightBg) 0.06f else 0.10f),
        start = Offset(topX, horizonY),
        end = Offset(botX, h),
        strokeWidth = 1.dp.toPx()
      )
    }

    // Three seamless looping harmonic laser waves with zero edge clipping
    val yFactors = floatArrayOf(0.17f, 0.195f, 0.22f)
    val amps = floatArrayOf(18f, 13f, 9f)
    val colors = arrayOf(primary.copy(alpha = 0.34f), secondary.copy(alpha = 0.26f), tertiary.copy(alpha = 0.20f))
    val spatialCycles = floatArrayOf(2f, 3f, 4f)
    val speeds = floatArrayOf(1f, 2f, 1f)
    val phaseShifts = floatArrayOf(0f, 1.1f, 2.3f)
    val samples = 64

    for (idx in 0 until 3) {
      val path = laserPaths[idx]
      path.reset()
      val baseY = h * yFactors[idx]
      val ampPx = amps[idx].dp.toPx() * stageEnergy
      val cycles = spatialCycles[idx]
      val phaseOffset = wavePhase * speeds[idx] + phaseShifts[idx]
      for (s in 0..samples) {
        val nx = s.toFloat() / samples.toFloat()
        val x = w * nx
        val edge = sin(nx * PI).toFloat().coerceIn(0f, 1f)
        val env = edge * edge
        val p1 = sin(nx * cycles * 2f * PI.toFloat() + phaseOffset)
        val p2 = cos(nx * (cycles + 1f) * 2f * PI.toFloat() - wavePhase)
        val y = baseY + (p1 + p2 * 0.28f) * ampPx * env
        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }
      drawPath(
        path = path,
        color = colors[idx],
        style = Stroke(width = (2.1f - idx * 0.4f).dp.toPx(), cap = StrokeCap.Round)
      )
    }
  }
}

/**
 * Unboxed animated Vinyl Disc + Progress Ring + Live Spectrum Equalizer header replacing plain card steppers.
 */
@Composable
fun VinylEqualizerProgressHeader(
  steps: List<OnboardingStepMeta>,
  currentStep: Int,
  languageBadge: String
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val activeMeta = steps.getOrElse(currentStep) { OnboardingStepMeta("Kurulum", "Stüdyo") }
  val totalSteps = steps.size.coerceAtLeast(1)
  val targetFraction = (currentStep + 1).toFloat() / totalSteps.toFloat()
  val animatedFraction by animateFloatAsState(
    targetValue = targetFraction,
    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
    label = "vinyl_progress_fraction"
  )
  val progressPct = (targetFraction * 100f).toInt()

  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val onBg = MaterialTheme.colorScheme.onBackground
  val surfaceVar = MaterialTheme.colorScheme.surfaceVariant

  val spinAngle: Float
  val eqPhase: Float
  if (isConstrained) {
    spinAngle = 0f
    eqPhase = 0.9f
  } else {
    val inf = rememberInfiniteTransition(label = "vinyl_header_anim")
    val rot by inf.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
      label = "mini_vinyl_spin"
    )
    val eq by inf.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
      label = "mini_eq_phase"
    )
    spinAngle = rot
    eqPhase = eq
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
  ) {
    // Rotating Vinyl Disc + Neon Progress Ring
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(62.dp)
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = size.minDimension * 0.45f
        val ringPad = 4.dp.toPx()
        val ringArcSize = Size(size.width - ringPad * 2f, size.height - ringPad * 2f)

        // Track ring
        drawArc(
          color = surfaceVar.copy(alpha = 0.45f),
          startAngle = -90f,
          sweepAngle = 360f,
          useCenter = false,
          topLeft = Offset(ringPad, ringPad),
          size = ringArcSize,
          style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
        )
        // Neon progress ring
        drawArc(
          brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
          startAngle = -90f,
          sweepAngle = 360f * animatedFraction,
          useCenter = false,
          topLeft = Offset(ringPad, ringPad),
          size = ringArcSize,
          style = Stroke(width = 3.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Spinning Vinyl Inner Disc
        val discRadius = radius - 6.dp.toPx()
        rotate(spinAngle, pivot = center) {
          drawCircle(color = Color(0xFF10131C), radius = discRadius, center = center)
          drawCircle(
            color = Color.White.copy(alpha = 0.14f),
            radius = discRadius * 0.78f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
          )
          drawCircle(
            brush = Brush.sweepGradient(listOf(Color.Transparent, primary.copy(alpha = 0.42f), Color.Transparent)),
            radius = discRadius * 0.64f,
            center = center,
            style = Stroke(width = 2.dp.toPx())
          )
          drawCircle(color = primary.copy(alpha = 0.85f), radius = discRadius * 0.34f, center = center)
          drawCircle(color = Color(0xFF060910), radius = 2.8.dp.toPx(), center = center)
        }
      }
      Text(
        text = "%$progressPct",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = Color.White,
        fontWeight = FontWeight.ExtraBold
      )
    }

    // Center Kinetic Stage Label (Zero outer card)
    Column(
      verticalArrangement = Arrangement.spacedBy(2.dp),
      modifier = Modifier.weight(1f)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "SAHNE ${currentStep + 1} / $totalSteps",
          style = MaterialTheme.typography.labelSmall,
          color = primary,
          fontWeight = FontWeight.ExtraBold
        )
        Box(
          modifier =
            Modifier.size(4.dp)
              .clip(CircleShape)
              .background(secondary)
        )
        Text(
          text = activeMeta.shortTitle.uppercase(),
          style = MaterialTheme.typography.labelSmall,
          color = secondary,
          fontWeight = FontWeight.Bold,
          maxLines = 1
        )
      }
      Text(
        text = activeMeta.headerTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = onBg,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Right: Mini Live Spectrum Equalizer + Language Pill
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Canvas(modifier = Modifier.width(48.dp).height(18.dp)) {
        val bars = 8
        val slot = size.width / bars
        val bw = slot * 0.52f
        for (i in 0 until bars) {
          val factor = ((sin(eqPhase + i * 0.7f) + 1f) * 0.38f + 0.24f).coerceIn(0.22f, 1f)
          val bh = size.height * factor
          drawRoundRect(
            brush = Brush.verticalGradient(listOf(secondary, primary)),
            topLeft = Offset(i * slot + (slot - bw) * 0.5f, (size.height - bh) * 0.5f),
            size = Size(bw, bh),
            cornerRadius = CornerRadius(bw, bw)
          )
        }
      }
      Surface(
        color = primary.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.border(1.dp, primary.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
      ) {
        Text(
          text = languageBadge,
          style = MaterialTheme.typography.labelSmall,
          color = primary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }
  }
}

/**
 * Stage 2 Centerpiece: Spinning Vinyl Disc with the listener's photo / initials mounted in the center spindle
 * and an orbital tonearm photo picker pill (`onboarding_pick_photo_btn`).
 */
@Composable
fun SpinningVinylAvatarStage(
  displayName: String,
  handle: String,
  avatarPath: String,
  onPickPhotoClick: () -> Unit
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val cleanName = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }
  val cleanHandle = "@${handle.trim().ifBlank { "vizyon" }}"

  val vinylSpin: Float =
    if (isConstrained) {
      18f
    } else {
      val inf = rememberInfiniteTransition(label = "vinyl_avatar_spin")
      val deg by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "vinyl_rot"
      )
      deg
    }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.size(226.dp)
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val r = size.minDimension * 0.46f
        // Outer neon vinyl aura
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(primary.copy(alpha = 0.28f), secondary.copy(alpha = 0.08f), Color.Transparent),
              center = center,
              radius = r * 1.18f
            ),
          radius = r * 1.18f,
          center = center
        )

        rotate(vinylSpin, pivot = center) {
          // Deep obsidian vinyl platter
          drawCircle(color = Color(0xFF0A0D16), radius = r, center = center)
          // Metallic light-reflection sweep across grooves
          drawCircle(
            brush =
              Brush.sweepGradient(
                listOf(
                  Color.Transparent,
                  Color.White.copy(alpha = 0.16f),
                  Color.Transparent,
                  primary.copy(alpha = 0.26f),
                  Color.Transparent
                )
              ),
            radius = r * 0.94f,
            center = center
          )
          // Concentric vinyl grooves
          for (g in 4..9) {
            val gr = r * (g / 10f)
            drawCircle(
              color = Color.White.copy(alpha = if (g % 2 == 0) 0.14f else 0.07f),
              radius = gr,
              center = center,
              style = Stroke(width = 1.dp.toPx())
            )
          }
          // Glowing neon label bezel
          drawCircle(
            brush = Brush.linearGradient(listOf(primary, secondary)),
            radius = r * 0.48f,
            center = center,
            style = Stroke(width = 2.5.dp.toPx())
          )
        }

        // Metallic tonearm needle resting smoothly onto the disc
        val pivotPt = Offset(size.width * 0.90f, size.height * 0.14f)
        val armKnee = Offset(size.width * 0.84f, size.height * 0.42f)
        val needleTip = Offset(size.width * 0.72f, size.height * 0.56f)
        drawCircle(color = secondary.copy(alpha = 0.9f), radius = 8.dp.toPx(), center = pivotPt)
        drawLine(
          color = Color(0xFFD6E2FF),
          start = pivotPt,
          end = armKnee,
          strokeWidth = 3.2.dp.toPx(),
          cap = StrokeCap.Round
        )
        drawLine(
          color = primary,
          start = armKnee,
          end = needleTip,
          strokeWidth = 2.6.dp.toPx(),
          cap = StrokeCap.Round
        )
      }

      // User photo or initials mounted in center spindle label
      StudioAvatar(
        photoUrlOrPath = avatarPath,
        contentDescription = cleanName,
        size = 86.dp,
        ringColor = primary
      )

      // Floating Orbital Photo Picker Pill
      Surface(
        color = primary,
        shape = RoundedCornerShape(999.dp),
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.4.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable(onClick = onPickPhotoClick)
            .testTag("onboarding_pick_photo_btn")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Icon(
            imageVector = Icons.Rounded.AddAPhoto,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = if (avatarPath.isBlank()) "Plak Kapağı Seç" else "Fotoğrafı Değiştir",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Text(
      text = "$cleanName  •  $cleanHandle",
      style = MaterialTheme.typography.titleSmall,
      color = primary,
      fontWeight = FontWeight.Bold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * Stage 5 Centerpiece: 360-degree Radial Acoustic Frequency Halo carrying `onboarding_live_waveform`.
 */
@Composable
fun StageRadialFrequencyHalo(
  selectedCount: Int,
  languageBadge: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val tertiary = MaterialTheme.colorScheme.tertiary

  val phase: Float =
    if (isConstrained) {
      0.8f
    } else {
      val inf = rememberInfiniteTransition(label = "radial_freq_halo")
      val p by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "radial_phase"
      )
      p
    }
  val energyBoost by animateFloatAsState(
    targetValue = (0.55f + (selectedCount.coerceIn(1, 8) / 8f) * 0.55f),
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "radial_energy_boost"
  )

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.fillMaxWidth().height(178.dp)
  ) {
    Canvas(
      modifier = Modifier.size(176.dp).testTag("onboarding_live_waveform")
    ) {
      val innerR = size.minDimension * 0.27f
      val maxBarLen = size.minDimension * 0.20f
      val rayCount = 40

      drawCircle(
        brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.22f), Color.Transparent)),
        radius = innerR * 1.9f,
        center = center
      )
      drawCircle(
        color = primary.copy(alpha = 0.45f),
        radius = innerR,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      for (i in 0 until rayCount) {
        val angleRad = (i.toFloat() / rayCount.toFloat()) * 2f * PI.toFloat()
        val wave = ((sin(phase + i * 0.45f) + 1f) * 0.5f) * 0.65f +
          ((cos(phase * 1.5f - i * 0.3f) + 1f) * 0.5f) * 0.35f
        val barLen = (maxBarLen * (0.28f + 0.72f * wave) * energyBoost).coerceAtLeast(6.dp.toPx())
        val start = Offset(center.x + cos(angleRad) * innerR, center.y + sin(angleRad) * innerR)
        val end = Offset(center.x + cos(angleRad) * (innerR + barLen), center.y + sin(angleRad) * (innerR + barLen))
        drawLine(
          color =
            when {
              i % 6 == 0 -> tertiary
              i % 2 == 0 -> secondary
              else -> primary
            },
          start = start,
          end = end,
          strokeWidth = 3.dp.toPx(),
          cap = StrokeCap.Round
        )
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "$selectedCount/8",
        style = MaterialTheme.typography.titleLarge,
        color = primary,
        fontWeight = FontWeight.ExtraBold
      )
      Text(
        text = languageBadge,
        style = MaterialTheme.typography.labelSmall,
        color = secondary,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

/**
 * Stage 6 Centerpiece: Mechanical Neon Combination Vault Dial reacting dynamically to passcode length,
 * strength, and exact match confirmation.
 */
@Composable
fun MechanicalVaultDialStage(
  passcodeLength: Int,
  passcodeReady: Boolean,
  strengthProgress: Float,
  strengthLabel: String
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  val dialColor by animateColorAsState(
    targetValue = if (passcodeReady) secondary else primary,
    label = "vault_dial_color"
  )
  val animatedStrength by animateFloatAsState(
    targetValue = strengthProgress.coerceIn(0f, 1f),
    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    label = "vault_strength_arc"
  )
  val dialRotation by animateFloatAsState(
    targetValue = (passcodeLength * 32f) + if (passcodeReady) 180f else 0f,
    animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
    label = "vault_dial_turn"
  )

  val ambientTurn: Float =
    if (isConstrained) {
      0f
    } else {
      val inf = rememberInfiniteTransition(label = "vault_bezel")
      val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "vault_ambient_turn"
      )
      t
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.fillMaxWidth().height(186.dp)
  ) {
    Canvas(modifier = Modifier.size(182.dp)) {
      val outerR = size.minDimension * 0.46f
      val middleR = size.minDimension * 0.36f
      val innerR = size.minDimension * 0.25f

      drawCircle(
        brush = Brush.radialGradient(listOf(dialColor.copy(alpha = 0.24f), Color.Transparent)),
        radius = outerR * 1.15f,
        center = center
      )

      // Outer notched dial ring
      rotate(ambientTurn, pivot = center) {
        val ticks = 24
        for (i in 0 until ticks) {
          val a = (i.toFloat() / ticks) * 2f * PI.toFloat()
          val rStart = outerR - if (i % 4 == 0) 9.dp.toPx() else 5.dp.toPx()
          drawLine(
            color = dialColor.copy(alpha = if (i % 4 == 0) 0.78f else 0.30f),
            start = Offset(center.x + cos(a) * rStart, center.y + sin(a) * rStart),
            end = Offset(center.x + cos(a) * outerR, center.y + sin(a) * outerR),
            strokeWidth = if (i % 4 == 0) 2.5.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }

      // Strength arc around middle ring
      val pad = (size.minDimension * 0.5f) - middleR
      drawArc(
        color = Color.White.copy(alpha = 0.12f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = Size(middleR * 2f, middleR * 2f),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
      )
      drawArc(
        brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
        startAngle = -90f,
        sweepAngle = 360f * animatedStrength,
        useCenter = false,
        topLeft = Offset(pad, pad),
        size = Size(middleR * 2f, middleR * 2f),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
      )

      // Inner gear ring turning on each digit typed
      rotate(dialRotation, pivot = center) {
        for (b in 0 until 4) {
          val a = (b * 90f) * (PI / 180f).toFloat()
          val boltStart = Offset(center.x + cos(a) * innerR, center.y + sin(a) * innerR)
          val boltEnd =
            Offset(
              center.x + cos(a) * (innerR + if (passcodeReady) 12.dp.toPx() else 6.dp.toPx()),
              center.y + sin(a) * (innerR + if (passcodeReady) 12.dp.toPx() else 6.dp.toPx())
            )
          drawLine(
            color = dialColor,
            start = boltStart,
            end = boltEnd,
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }

      drawCircle(color = Color(0xFF0B101D), radius = innerR, center = center)
      drawCircle(
        color = dialColor.copy(alpha = 0.65f),
        radius = innerR,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = if (passcodeReady) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
        contentDescription = null,
        tint = dialColor,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = if (passcodeReady) "KİLİT HAZIR" else strengthLabel.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = dialColor,
        fontWeight = FontWeight.ExtraBold
      )
    }
  }
}

/**
 * Stage 7 Centerpiece: Sweeping Neon Sonic Radar Scanner with active status pings and embedded
 * network check trigger (`check_network_btn`).
 */
@Composable
fun SonicRadarScanStage(
  isNetworkAvailable: Boolean,
  ageVerified: Boolean,
  onVerifyNetwork: () -> Unit
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val errorColor = MaterialTheme.colorScheme.error

  val radarColor by animateColorAsState(
    targetValue =
      when {
        !isNetworkAvailable -> errorColor
        ageVerified -> secondary
        else -> primary
      },
    label = "sonic_radar_color"
  )

  val sweepDeg: Float =
    if (isConstrained) {
      35f
    } else {
      val inf = rememberInfiniteTransition(label = "sonic_radar")
      val deg by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "radar_sweep"
      )
      deg
    }

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(176.dp)) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val maxR = size.minDimension * 0.46f
        // Range rings
        for (ring in 1..3) {
          drawCircle(
            color = radarColor.copy(alpha = 0.24f),
            radius = maxR * (ring / 3f),
            center = center,
            style = Stroke(width = 1.4.dp.toPx())
          )
        }
        // Crosshairs
        drawLine(
          color = radarColor.copy(alpha = 0.16f),
          start = Offset(center.x - maxR, center.y),
          end = Offset(center.x + maxR, center.y),
          strokeWidth = 1.dp.toPx()
        )
        drawLine(
          color = radarColor.copy(alpha = 0.16f),
          start = Offset(center.x, center.y - maxR),
          end = Offset(center.x, center.y + maxR),
          strokeWidth = 1.dp.toPx()
        )
        // Rotating sonar beam wedge
        rotate(sweepDeg, pivot = center) {
          drawArc(
            brush =
              Brush.sweepGradient(
                0f to Color.Transparent,
                0.78f to Color.Transparent,
                0.98f to radarColor.copy(alpha = 0.45f),
                1f to Color.Transparent
              ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = true,
            topLeft = Offset(center.x - maxR, center.y - maxR),
            size = Size(maxR * 2f, maxR * 2f)
          )
          drawLine(
            color = radarColor,
            start = center,
            end = Offset(center.x + maxR, center.y),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
        // Sonar target blips
        drawCircle(
          color = if (isNetworkAvailable) secondary else errorColor,
          radius = 4.5.dp.toPx(),
          center = Offset(center.x + maxR * 0.42f, center.y - maxR * 0.34f)
        )
        drawCircle(
          color = if (ageVerified) secondary else primary.copy(alpha = 0.55f),
          radius = 4.dp.toPx(),
          center = Offset(center.x - maxR * 0.48f, center.y + maxR * 0.30f)
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
          imageVector = Icons.Rounded.Wifi,
          contentDescription = null,
          tint = radarColor,
          modifier = Modifier.size(24.dp)
        )
        Text(
          text = if (isNetworkAvailable) "SONAR AKTİF" else "AĞ YOK",
          style = MaterialTheme.typography.labelSmall,
          color = radarColor,
          fontWeight = FontWeight.ExtraBold
        )
      }
    }

    Surface(
      color = radarColor.copy(alpha = 0.16f),
      shape = RoundedCornerShape(999.dp),
      modifier =
        Modifier.defaultMinSize(minHeight = 48.dp)
          .clip(RoundedCornerShape(999.dp))
          .border(1.dp, radarColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
          .clickable(onClick = onVerifyNetwork)
          .testTag("check_network_btn")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          tint = radarColor,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = if (isNetworkAvailable) "Ağ Bağlantısı Doğrulandı ✓" else "Bağlantıyı Yeniden Tara",
          style = MaterialTheme.typography.labelLarge,
          color = radarColor,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * Stage 8 Centerpiece: Perforated Holographic Concert VIP Pass (`onboarding_setup_summary_card`)
 * with notched ticket cutouts, tear line perforation, laser foil shimmer, and credential summary.
 */
@Composable
fun HolographicConcertVipTicket(
  displayName: String,
  handle: String,
  bio: String,
  avatarPath: String,
  selectedTheme: AppThemeMode,
  selectedAccent: AccentPalette,
  selectedCorner: CornerDensity,
  selectedStoryTheme: StoryCardTheme,
  selectedChatPrivacy: ChatPrivacy,
  selectedLanguage: AppLanguage,
  selectedGenres: List<String>,
  passcodeConfigured: Boolean,
  shareActivity: Boolean,
  requireFollowApproval: Boolean,
  backgroundNotifServiceEnabled: Boolean,
  isSignedInWithGoogle: Boolean
) {
  val context = LocalContext.current
  val isConstrained = remember(context) { DevicePerfHelper.isConstrainedDevice(context) }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val surfaceColor = MaterialTheme.colorScheme.surface

  val shimmerX: Float =
    if (isConstrained) {
      0.5f
    } else {
      val inf = rememberInfiniteTransition(label = "vip_ticket_shimmer")
      val x by inf.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "foil_sweep"
      )
      x
    }

  val cleanName = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }
  val cleanHandle = "@${handle.trim().ifBlank { "vizyon" }}"

  Box(
    modifier =
      Modifier.fillMaxWidth()
        .testTag("onboarding_setup_summary_card")
        .drawBehind {
          val w = size.width
          val h = size.height
          val cornerR = 24.dp.toPx()
          val notchY = h * 0.34f
          val notchR = 13.dp.toPx()

          // Build notched Concert VIP Ticket silhouette
          val ticketPath = Path().apply {
            moveTo(cornerR, 0f)
            lineTo(w - cornerR, 0f)
            arcTo(Rect(w - 2 * cornerR, 0f, w, 2 * cornerR), -90f, 90f, false)
            lineTo(w, notchY - notchR)
            arcTo(Rect(w - notchR, notchY - notchR, w + notchR, notchY + notchR), -90f, -180f, false)
            lineTo(w, h - cornerR)
            arcTo(Rect(w - 2 * cornerR, h - 2 * cornerR, w, h), 0f, 90f, false)
            lineTo(cornerR, h)
            arcTo(Rect(0f, h - 2 * cornerR, 2 * cornerR, h), 90f, 90f, false)
            lineTo(0f, notchY + notchR)
            arcTo(Rect(-notchR, notchY - notchR, notchR, notchY + notchR), 90f, -180f, false)
            lineTo(0f, cornerR)
            arcTo(Rect(0f, 0f, 2 * cornerR, 2 * cornerR), 180f, 90f, false)
            close()
          }

          drawPath(
            path = ticketPath,
            color = surfaceColor.copy(alpha = 0.88f)
          )
          // Diagonal holographic foil sheen sweep
          val bandX = w * shimmerX
          drawPath(
            path = ticketPath,
            brush =
              Brush.linearGradient(
                colors =
                  listOf(
                    Color.Transparent,
                    primary.copy(alpha = 0.14f),
                    secondary.copy(alpha = 0.16f),
                    Color.Transparent
                  ),
                start = Offset(bandX - w * 0.25f, 0f),
                end = Offset(bandX + w * 0.25f, h)
              )
          )
          drawPath(
            path = ticketPath,
            brush = Brush.linearGradient(listOf(primary.copy(alpha = 0.78f), secondary.copy(alpha = 0.60f))),
            style = Stroke(width = 1.4.dp.toPx())
          )
          // Dashed tear perforation across waist
          drawLine(
            color = primary.copy(alpha = 0.45f),
            start = Offset(notchR + 8.dp.toPx(), notchY),
            end = Offset(w - notchR - 8.dp.toPx(), notchY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
          )
        }
        .padding(horizontal = 20.dp, vertical = 18.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
      // Upper VIP Pass Header
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = Icons.Rounded.ConfirmationNumber,
              contentDescription = null,
              tint = secondary,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "VİZYON STUDIO VIP PASS • #2026",
              style = MaterialTheme.typography.labelSmall,
              color = secondary,
              fontWeight = FontWeight.ExtraBold
            )
          }
          Surface(
            color = primary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = "8/8 HAZIR",
              style = MaterialTheme.typography.labelSmall,
              color = primary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          StudioAvatar(
            photoUrlOrPath = avatarPath,
            contentDescription = cleanName,
            size = 56.dp,
            ringColor = primary
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = cleanName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = cleanHandle,
              style = MaterialTheme.typography.labelMedium,
              color = primary,
              fontWeight = FontWeight.Bold
            )
            if (bio.isNotBlank()) {
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
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Lower Stub Credentials
      Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        VipTicketRow("Tema & Form", "${selectedTheme.label} • ${selectedAccent.label} • ${selectedCorner.label}")
        VipTicketRow("Hikaye & DM", "${selectedStoryTheme.label} • ${selectedChatPrivacy.label}")
        VipTicketRow(
          "Takip & Aktivite",
          "${if (requireFollowApproval) "Onaylı Takip" else "Açık Takip"} • ${if (shareActivity) "Son Müzik Görünür" else "Gizli"}"
        )
        VipTicketRow("Katalog & Tarzlar", "${selectedLanguage.badge} • ${selectedGenres.size} müzik türü")
        VipTicketRow(
          "Güvenlik & Bildirim",
          "${if (passcodeConfigured) "Kasa Aktif ✓" else "Bekliyor"} • ${if (backgroundNotifServiceEnabled) "Canlı Bildirim" else "Ön Plan"}"
        )
        VipTicketRow("Bulut Yedeği", if (isSignedInWithGoogle) "Google Bulut Bağlı ✓" else "Cihaz Kasası")
      }

      // Laser Ticket Barcode Strip
      Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
        val barCount = 38
        val slot = size.width / barCount
        for (i in 0 until barCount) {
          val wRatio = when (i % 4) {
            0 -> 0.70f
            1 -> 0.35f
            2 -> 0.55f
            else -> 0.25f
          }
          drawRoundRect(
            color = (if (i % 7 == 0) secondary else primary).copy(alpha = 0.55f),
            topLeft = Offset(i * slot, 2.dp.toPx()),
            size = Size(slot * wRatio, size.height - 4.dp.toPx()),
            cornerRadius = CornerRadius(2f, 2f)
          )
        }
      }
    }
  }
}

@Composable
private fun VipTicketRow(label: String, value: String) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(14.dp)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.End,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f, fill = false)
    )
  }
}
