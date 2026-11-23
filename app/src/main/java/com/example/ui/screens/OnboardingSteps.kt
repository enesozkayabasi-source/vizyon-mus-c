package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AccentPalette
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.MusicCategory
import com.example.model.MusicCategoryGroup
import com.example.model.StoryCardTheme
import com.example.security.PasscodeSecurityHelper
import com.example.ui.components.HolographicConcertVipTicket
import com.example.ui.components.LiveSoundwaveEqualizer
import com.example.ui.components.MechanicalVaultDialStage
import com.example.ui.components.SonicRadarScanStage
import com.example.ui.components.SpinningVinylAvatarStage
import com.example.ui.components.StageRadialFrequencyHalo
import com.example.ui.components.StudioAvatar
import com.example.ui.components.StudioInteractiveChip
import java.util.Locale

@Composable
private fun ConcertStageHeadline(
  kicker: String,
  title: String,
  subtitle: String,
  centered: Boolean = false
) {
  Column(
    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = kicker,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.secondary,
      fontWeight = FontWeight.ExtraBold,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.ExtraBold,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = if (centered) TextAlign.Center else TextAlign.Start
    )
  }
}

@Composable
private fun MixerChannelToggleStrip(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  switchTag: String,
  accentColor: Color = MaterialTheme.colorScheme.primary
) {
  val railColor by animateColorAsState(
    targetValue = if (checked) accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    label = "mixer_rail_color"
  )
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(
          if (checked) accentColor.copy(alpha = 0.10f)
          else MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
        )
        .clickable { onCheckedChange(!checked) }
        .padding(vertical = 10.dp, horizontal = 12.dp)
  ) {
    Box(
      modifier =
        Modifier.width(4.dp)
          .height(34.dp)
          .clip(CircleShape)
          .background(railColor)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(switchTag)
    )
  }
}

@Composable
private fun stageTextFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.34f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f)
  )

// =====================================================================
// SAHNE 1: Konser Sahnesi Portalı & Bulut Kurtarma Feneri
// =====================================================================
@Composable
internal fun OnboardingStep1WelcomeAndRestore(
  selectedCorner: CornerDensity,
  isSignedInWithGoogle: Boolean,
  googleConnecting: Boolean,
  googleAuthError: String?,
  onRestoreFromGoogleClick: () -> Unit
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    // Borderless Arch Concert Stage Hero with radial spotlight fade
    Box(
      contentAlignment = Alignment.BottomCenter,
      modifier =
        Modifier.fillMaxWidth()
          .height(216.dp)
          .clip(RoundedCornerShape(topStart = 120.dp, topEnd = 120.dp, bottomStart = 28.dp, bottomEnd = 28.dp))
          .border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.75f), Color.Transparent)),
            shape = RoundedCornerShape(topStart = 120.dp, topEnd = 120.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
          )
    ) {
      Image(
        painter = painterResource(R.drawable.img_hero_stage),
        contentDescription = "Vizyon Music Konser Sahnesi",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors = listOf(Color(0x22040814), Color(0x99060B18), Color(0xF2050812))
              )
            )
      )
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)
      ) {
        Surface(
          color = primary.copy(alpha = 0.28f),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.border(1.dp, secondary.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
          ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = secondary, modifier = Modifier.size(14.dp))
            Text(
              text = "CANLI KONSER STÜDYOSU",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
        Text(
          text = "Sahne Senin İçin Kuruluyor",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center
        )
        Text(
          text = "Plak kimliği, sahne ışıkları, güvenlik kasa kadranı ve sonik radar ile benzersiz stüdyonu hazırla.",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFD0DEFF),
          textAlign = TextAlign.Center
        )
      }
    }

    // Floating Stage Milestones Teaser (no box container)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf("Vinil Plak", "Işık Konsolu", "Kasa Kadranı", "VIP Bilet").forEachIndexed { idx, tag ->
        Surface(
          color = (if (idx % 2 == 0) primary else secondary).copy(alpha = 0.14f),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
        ) {
          Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }

    // Orbital Cloud Restore Beacon Ring
    if (isSignedInWithGoogle) {
      Surface(
        color = secondary.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.border(1.2.dp, secondary.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
          Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = secondary)
          Text(
            text = "Google Bulut Yedeğin Bağlandı ✓",
            style = MaterialTheme.typography.labelLarge,
            color = secondary,
            fontWeight = FontWeight.Bold
          )
        }
      }
    } else {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          color = primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .border(
                width = 1.4.dp,
                brush = Brush.horizontalGradient(listOf(primary, secondary)),
                shape = RoundedCornerShape(999.dp)
              )
              .clickable(enabled = !googleConnecting, onClick = onRestoreFromGoogleClick)
              .testTag("onboarding_cloud_restore_btn")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp)
          ) {
            Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = secondary)
            Text(
              text = if (googleConnecting) "Bulut Kontrol Ediliyor..." else "Önceki Hesabımı Geri Yükle (Google)",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Text(
          text = "Yeni başlıyorsan aşağıdaki butonla doğrudan 2. sahneye geç.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )
      }
    }

    googleAuthError?.let { err ->
      Text(text = err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
    }
  }
}

// =====================================================================
// SAHNE 2: Dönen Vinil Plak & Sahne Kimliği
// =====================================================================
@Composable
internal fun OnboardingStep2IdentityAndAvatar(
  displayName: String,
  handle: String,
  bio: String,
  avatarPath: String,
  selectedCorner: CornerDensity,
  onPickPhotoClick: () -> Unit,
  onDisplayNameChange: (String) -> Unit,
  onHandleChange: (String) -> Unit,
  onBioChange: (String) -> Unit
) {
  val radius = selectedCorner.cardCornerDp.dp
  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "PLAK KAPAĞI & KİMLİK",
      title = "Sanatçı Plak Kimliğin",
      subtitle = "Fotoğrafını plağın merkezine bas ve sahne adını belirle.",
      centered = true
    )

    SpinningVinylAvatarStage(
      displayName = displayName,
      handle = handle,
      avatarPath = avatarPath,
      onPickPhotoClick = onPickPhotoClick
    )

    OutlinedTextField(
      value = displayName,
      onValueChange = { if (it.length <= 40) onDisplayNameChange(it) },
      label = { Text("Görünen Ad *") },
      placeholder = { Text("Örn: Deniz Kaya") },
      singleLine = true,
      colors = stageTextFieldColors(),
      shape = RoundedCornerShape(radius),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_name_input")
    )

    OutlinedTextField(
      value = handle,
      onValueChange = { raw ->
        val clean =
          raw.lowercase(Locale.ROOT)
            .replace("@", "")
            .replace(Regex("[^a-z0-9_.]"), "")
            .take(24)
        onHandleChange(clean)
      },
      label = { Text("Kullanıcı Adı *") },
      prefix = { Text("@") },
      placeholder = { Text("denizmusic") },
      singleLine = true,
      colors = stageTextFieldColors(),
      shape = RoundedCornerShape(radius),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_handle_input")
    )

    OutlinedTextField(
      value = bio,
      onValueChange = { if (it.length <= 160) onBioChange(it) },
      label = { Text("Müzik Biyografisi (İsteğe bağlı)") },
      placeholder = { Text("Örn: Gece sürüşü & Anadolu Rock") },
      maxLines = 2,
      colors = stageTextFieldColors(),
      shape = RoundedCornerShape(radius),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_bio_input")
    )
  }
}

// =====================================================================
// SAHNE 3: Sahne Işık Konsolu & Hologram Vitrini
// =====================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingStep3ThemeAndVisuals(
  displayName: String,
  handle: String,
  bio: String,
  avatarPath: String,
  selectedLanguage: AppLanguage,
  selectedTheme: AppThemeMode,
  selectedAccent: AccentPalette,
  selectedCorner: CornerDensity,
  selectedStoryTheme: StoryCardTheme,
  selectedChatPrivacy: ChatPrivacy,
  selectedGenres: List<String>,
  isSignedInWithGoogle: Boolean,
  ageVerified: Boolean,
  shareActivity: Boolean,
  passcodeConfigured: Boolean,
  onSelectTheme: (AppThemeMode) -> Unit,
  onSelectAccent: (AccentPalette) -> Unit,
  onSelectCorner: (CornerDensity) -> Unit
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val cleanName = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }
  val cleanHandle = "@${handle.trim().ifBlank { "vizyon" }}"

  Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "SAHNE AYDINLATMA KONSOLU",
      title = "Atmosfer & Neon Renk",
      subtitle = "Her dokunuşta tüm konser sahnesinin ışıkları anlık değişir."
    )

    // Unboxed Floating Hologram Showcase Preview
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(selectedCorner.cardCornerDp.dp))
          .background(
            Brush.linearGradient(
              listOf(primary.copy(alpha = 0.18f), secondary.copy(alpha = 0.08f), Color.Transparent)
            )
          )
          .border(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(listOf(primary.copy(alpha = 0.7f), secondary.copy(alpha = 0.45f))),
            shape = RoundedCornerShape(selectedCorner.cardCornerDp.dp)
          )
          .padding(14.dp)
          .testTag("onboarding_live_preview_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          StudioAvatar(photoUrlOrPath = avatarPath, contentDescription = cleanName, size = 50.dp, ringColor = primary)
          Column(modifier = Modifier.weight(1f)) {
            Text(text = cleanName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text(text = cleanHandle, style = MaterialTheme.typography.labelMedium, color = primary, fontWeight = FontWeight.Bold)
          }
          Surface(color = primary.copy(alpha = 0.22f), shape = RoundedCornerShape(999.dp)) {
            Text(
              text = "${selectedTheme.label} • ${selectedLanguage.badge}",
              style = MaterialTheme.typography.labelSmall,
              color = primary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
            )
          }
        }
        LiveSoundwaveEqualizer(barCount = 22, energyBoost = 0.9f, height = 22.dp)
      }
    }

    // 2x2 Stage Spotlight Mode Pads
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      AppThemeMode.entries.chunked(2).forEach { pair ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          pair.forEach { mode ->
            val selected = selectedTheme == mode
            val padGlow =
              when (mode) {
                AppThemeMode.LIGHT -> Color(0xFFF3F6FC)
                AppThemeMode.DARK -> Color(0xFF1A263D)
                AppThemeMode.NIGHT_AMOLED -> Color(0xFF04060D)
                AppThemeMode.SYSTEM -> primary
              }
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(selectedCorner.cardCornerDp.dp))
                  .background(
                    if (selected) primary.copy(alpha = 0.22f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.30f)
                  )
                  .border(
                    width = if (selected) 1.6.dp else 1.dp,
                    color = if (selected) primary else Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(selectedCorner.cardCornerDp.dp)
                  )
                  .clickable { onSelectTheme(mode) }
                  .padding(12.dp)
                  .testTag("onboarding_theme_${mode.name}")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                  modifier =
                    Modifier.size(22.dp)
                      .clip(CircleShape)
                      .background(padGlow)
                      .border(1.5.dp, if (selected) secondary else Color.White.copy(alpha = 0.4f), CircleShape)
                )
                Column {
                  Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) primary else MaterialTheme.colorScheme.onBackground
                  )
                  Text(
                    text = mode.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
      }
    }

    // Glowing Stage Light Orbs for Accent Palette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Lazer Vurgu Rengi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        AccentPalette.entries.forEach { palette ->
          val selected = selectedAccent == palette
          val orbScale by animateFloatAsState(
            targetValue = if (selected) 1.18f else 0.94f,
            animationSpec = spring(dampingRatio = 0.55f),
            label = "accent_orb_scale"
          )
          val color = Color(palette.primaryHex)
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
              Modifier.clickable { onSelectAccent(palette) }
                .padding(vertical = 4.dp)
                .testTag("onboarding_accent_${palette.name}")
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.size(48.dp).scale(orbScale)
            ) {
              if (selected) {
                Box(
                  modifier =
                    Modifier.size(46.dp)
                      .clip(CircleShape)
                      .border(2.dp, color.copy(alpha = 0.75f), CircleShape)
                )
              }
              Box(
                modifier =
                  Modifier.size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.35f), color)))
                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
              )
            }
            Text(
              text = palette.label.substringBefore(" "),
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
            )
          }
        }
      }
    }

    // Corner Density Floating Pill Bar
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      CornerDensity.entries.forEach { corner ->
        StudioInteractiveChip(
          label = corner.label,
          selected = selectedCorner == corner,
          onClick = { onSelectCorner(corner) },
          modifier = Modifier.testTag("onboarding_corner_${corner.name}")
        )
      }
    }
  }
}

// =====================================================================
// SAHNE 4: Paralaks Sahne Afişi Destesi & Ses Mikseri Gizlilik Rayları
// =====================================================================
@Composable
internal fun OnboardingStep4StoryAndPrivacy(
  selectedCorner: CornerDensity,
  selectedStoryTheme: StoryCardTheme,
  selectedChatPrivacy: ChatPrivacy,
  shareActivity: Boolean,
  requireFollowApproval: Boolean,
  onSelectStoryTheme: (StoryCardTheme) -> Unit,
  onSelectChatPrivacy: (ChatPrivacy) -> Unit,
  onShareActivityChange: (Boolean) -> Unit,
  onRequireFollowApprovalChange: (Boolean) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "PARALAKS AFİŞ & MİKSER KONSOLU",
      title = "Hikaye Stili & Gizlilik",
      subtitle = "Varsayılan sahne posterini seç ve gizlilik kanallarını ayarla."
    )

    // 3D Fanning Poster Deck for StoryCardTheme
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
      itemsIndexed(StoryCardTheme.entries) { idx, theme ->
        val selected = selectedStoryTheme == theme
        val scale by animateFloatAsState(
          targetValue = if (selected) 1.04f else 0.94f,
          animationSpec = spring(dampingRatio = 0.65f),
          label = "poster_scale"
        )
        val tilt = if (selected) 0f else if (idx % 2 == 0) -1.8f else 1.8f
        val startC = Color(theme.startColorHex)
        val accentC = Color(theme.accentColorHex)

        Box(
          modifier =
            Modifier.width(126.dp)
              .height(142.dp)
              .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = tilt
              }
              .clip(RoundedCornerShape(20.dp))
              .background(Brush.verticalGradient(listOf(startC, accentC.copy(alpha = 0.48f), Color(0xFF070A12))))
              .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accentC else Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp)
              )
              .clickable { onSelectStoryTheme(theme) }
              .padding(12.dp)
              .testTag("onboarding_story_theme_${theme.name}")
        ) {
          Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
          ) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(accentC))
              if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = theme.label,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2
              )
              Text(
                text = if (selected) "SEÇİLİ POSTER" else "Afişi Seç",
                style = MaterialTheme.typography.labelSmall,
                color = accentC,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // Channel Sliders for DM Privacy
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Direkt Mesaj (DM) Kanal İzni", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
      ChatPrivacy.entries.forEach { privacy ->
        val selected = selectedChatPrivacy == privacy
        val primary = MaterialTheme.colorScheme.primary
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(if (selected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.26f))
              .border(
                width = if (selected) 1.4.dp else 0.8.dp,
                color = if (selected) primary else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
              )
              .clickable { onSelectChatPrivacy(privacy) }
              .padding(horizontal = 14.dp, vertical = 12.dp)
              .testTag("onboarding_chat_privacy_${privacy.name}")
        ) {
          Box(
            modifier =
              Modifier.size(12.dp)
                .clip(CircleShape)
                .background(if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = privacy.label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (selected) primary else MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = privacy.subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Studio Mixer Toggle Rails (No Box Card)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      MixerChannelToggleStrip(
        title = "Profilimde Son Dinlediğimi Göster",
        subtitle = "Kapatsan bile kullanıcı adı aramalarında profilin görünür kalır.",
        checked = shareActivity,
        onCheckedChange = onShareActivityChange,
        switchTag = "onboarding_share_switch"
      )
      MixerChannelToggleStrip(
        title = "Takip İstek Onayı Gereksin",
        subtitle = "Açıkken yeni takipçiler İstekler panelinden onayını bekler.",
        checked = requireFollowApproval,
        onCheckedChange = onRequireFollowApprovalChange,
        switchTag = "onboarding_follow_approval_switch",
        accentColor = MaterialTheme.colorScheme.secondary
      )
    }
  }
}

// =====================================================================
// SAHNE 5: 360° Akustik Frekans Halesi & Tür Takımyıldızı
// =====================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingStep5LanguageAndGenres(
  selectedLanguage: AppLanguage,
  selectableGenres: List<MusicCategory>,
  selectedGenres: List<String>,
  onSelectLanguage: (AppLanguage) -> Unit,
  onToggleGenre: (String) -> Unit
) {
  val turkishGenres = selectableGenres.filter { it.group == MusicCategoryGroup.TURKISH }
  val globalGenres = selectableGenres.filter { it.group == MusicCategoryGroup.GLOBAL }
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "360° AKUSTİK FREKANS SPEKTRUMU",
      title = "Dil & Müzik Tarzların",
      subtitle = "Seçtiğin her müzik türü merkezdeki ses halesini canlandırır.",
      centered = true
    )

    StageRadialFrequencyHalo(
      selectedCount = selectedGenres.size,
      languageBadge = selectedLanguage.badge
    )

    // Floating Neon Language Ribbons (Zero Card Box)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Rounded.Public, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
        Text("Dil & Katalog Bölgesi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      }
      AppLanguage.entries.forEach { lang ->
        val selected = selectedLanguage == lang
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(if (selected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
              .border(
                width = if (selected) 1.4.dp else 0.8.dp,
                color = if (selected) primary else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
              )
              .clickable { onSelectLanguage(lang) }
              .padding(horizontal = 14.dp, vertical = 11.dp)
              .testTag("onboarding_lang_card_${lang.name}")
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = lang.label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (selected) primary else MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = lang.subtitle,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Surface(
            color = if (selected) primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = lang.badge,
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    // Genre Constellation Clusters
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Türk Müziği Seçkisi",
        style = MaterialTheme.typography.labelLarge,
        color = primary,
        fontWeight = FontWeight.ExtraBold
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        turkishGenres.forEach { cat ->
          val isSelected = cat.label in selectedGenres
          FilterChip(
            selected = isSelected,
            onClick = { onToggleGenre(cat.label) },
            leadingIcon =
              if (isSelected) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null,
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = primary,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
              ),
            label = { Text(cat.label) },
            modifier = Modifier.testTag("genre_chip_${cat.name}")
          )
        }
      }

      Text(
        text = "Global Türler",
        style = MaterialTheme.typography.labelLarge,
        color = secondary,
        fontWeight = FontWeight.ExtraBold
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        globalGenres.forEach { cat ->
          val isSelected = cat.label in selectedGenres
          FilterChip(
            selected = isSelected,
            onClick = { onToggleGenre(cat.label) },
            leadingIcon =
              if (isSelected) {
                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null,
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = primary,
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
              ),
            label = { Text(cat.label) },
            modifier = Modifier.testTag("genre_chip_${cat.name}")
          )
        }
      }
    }
  }
}

// =====================================================================
// SAHNE 6: Mekanik Neon Kasa Kadranı & Güvenlik Şifresi
// =====================================================================
@Composable
internal fun OnboardingStep6SecurityVault(
  passcode: String,
  passcodeConfirm: String,
  rememberMe: Boolean,
  lockOnLaunch: Boolean,
  onPasscodeChange: (String) -> Unit,
  onPasscodeConfirmChange: (String) -> Unit,
  onRememberMeChange: (Boolean) -> Unit,
  onLockOnLaunchChange: (Boolean) -> Unit
) {
  var showPasscode by remember { mutableStateOf(false) }
  val (strengthLabel, strengthProgress) = remember(passcode) {
    PasscodeSecurityHelper.passcodeStrengthLabel(passcode)
  }
  val passcodeReady = passcode.trim().length >= 4 && passcode == passcodeConfirm

  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "MEKANİK KASA KADRANI • SHA-256",
      title = "Güvenlik Kasası Kilidi",
      subtitle = "Her tuşta kasa dişlileri döner ve eşleştiğinde kilit mekanizması mühürlenir.",
      centered = true
    )

    MechanicalVaultDialStage(
      passcodeLength = passcode.length,
      passcodeReady = passcodeReady,
      strengthProgress = strengthProgress,
      strengthLabel = strengthLabel
    )

    OutlinedTextField(
      value = passcode,
      onValueChange = { if (it.length <= 24) onPasscodeChange(it) },
      label = { Text("Stüdyo Güvenlik Şifresi (en az 4) *") },
      singleLine = true,
      visualTransformation = if (showPasscode) VisualTransformation.None else PasswordVisualTransformation(),
      trailingIcon = {
        IconButton(onClick = { showPasscode = !showPasscode }) {
          Icon(
            imageVector = if (showPasscode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = if (showPasscode) "Şifreyi gizle" else "Şifreyi göster"
          )
        }
      },
      colors = stageTextFieldColors(),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_passcode_input")
    )

    OutlinedTextField(
      value = passcodeConfirm,
      onValueChange = { if (it.length <= 24) onPasscodeConfirmChange(it) },
      label = { Text("Şifre Tekrar *") },
      singleLine = true,
      visualTransformation = if (showPasscode) VisualTransformation.None else PasswordVisualTransformation(),
      isError = passcodeConfirm.isNotEmpty() && passcode != passcodeConfirm,
      colors = stageTextFieldColors(),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_passcode_confirm_input")
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Kasa Gücü: $strengthLabel",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        if (passcodeReady) {
          Text(
            text = "Mühürlendi ✓",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
      LinearProgressIndicator(
        progress = { strengthProgress },
        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
        color = if (strengthProgress >= 0.7f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      MixerChannelToggleStrip(
        title = "Beni Hatırla",
        subtitle = "Profil ve tercihlerim cihaz + Google bulutta koruma altında kalsın.",
        checked = rememberMe,
        onCheckedChange = onRememberMeChange,
        switchTag = "onboarding_remember_me_switch"
      )
      MixerChannelToggleStrip(
        title = "Açılışta Şifre Kilidi",
        subtitle = "Uygulamayı açarken stüdyo şifresi doğrulaması iste.",
        checked = lockOnLaunch,
        onCheckedChange = onLockOnLaunchChange,
        switchTag = "onboarding_lock_on_launch_switch",
        accentColor = MaterialTheme.colorScheme.secondary
      )
    }
  }
}

// =====================================================================
// SAHNE 7: Canlı Sonik Radar & Güvenlik İzin Konsolu
// =====================================================================
@Composable
internal fun OnboardingStep7SafetyNotificationsAndCloud(
  isNetworkAvailable: Boolean,
  ageVerified: Boolean,
  hasNotifPermission: Boolean,
  backgroundNotifServiceEnabled: Boolean,
  isSignedInWithGoogle: Boolean,
  googleConnecting: Boolean,
  googleAuthError: String?,
  onVerifyNetwork: () -> Unit,
  onAgeVerifiedChange: (Boolean) -> Unit,
  onRequestNotifPermission: () -> Unit,
  onBackgroundNotifServiceChange: (Boolean) -> Unit,
  onGoogleAuthClick: () -> Unit
) {
  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary
  val errorColor = MaterialTheme.colorScheme.error

  Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "CANLI SONİK RADAR & İZİNLER",
      title = "Radar, 13+ Onay & Bildirim",
      subtitle = "Ağ bağlantını tara, 13+ güvenlik geçidini aç ve arka plan canlı bildirimini etkinleştir.",
      centered = true
    )

    SonicRadarScanStage(
      isNetworkAvailable = isNetworkAvailable,
      ageVerified = ageVerified,
      onVerifyNetwork = onVerifyNetwork
    )

    // Laser Age Gate Strip
    MixerChannelToggleStrip(
      title = "13+ Yaş & Güvenlik Duvarı Onayı *",
      subtitle =
        if (ageVerified) "13 yaşından büyük olduğum ve güvenlik ilkeleri onaylandı ✓"
        else "Devam etmek için zorunlu 13+ yaş güvenlik onayını aktifleştirin.",
      checked = ageVerified,
      onCheckedChange = onAgeVerifiedChange,
      switchTag = "onboarding_age_switch",
      accentColor = if (ageVerified) secondary else errorColor
    )

    // Notification Permission Action Rail
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f))
          .border(
            1.dp,
            if (hasNotifPermission) secondary.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.14f),
            RoundedCornerShape(16.dp)
          )
          .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(
          Icons.Rounded.NotificationsActive,
          contentDescription = null,
          tint = if (hasNotifPermission) secondary else primary
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Cihaz Bildirim İzni", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(
            text = if (hasNotifPermission) "Cihaz bildirimleri aktif ✓" else "Sesli mesaj ve takip isteklerini kaçırma.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
      if (!hasNotifPermission) {
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
          color = primary.copy(alpha = 0.22f),
          shape = RoundedCornerShape(999.dp),
          modifier =
            Modifier.defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .border(1.dp, primary, RoundedCornerShape(999.dp))
              .clickable(onClick = onRequestNotifPermission)
              .testTag("onboarding_notif_permission_btn")
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text("İzin Ver", style = MaterialTheme.typography.labelLarge, color = primary, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    MixerChannelToggleStrip(
      title = "Arka Plan Canlı Bildirim Servisi",
      subtitle = "Uygulama arka planda veya kapalıyken mesaj ve ses bildirimlerinin kesilmesini önler.",
      checked = backgroundNotifServiceEnabled,
      onCheckedChange = onBackgroundNotifServiceChange,
      switchTag = "onboarding_bg_notif_switch",
      accentColor = primary
    )

    if (!isSignedInWithGoogle) {
      Surface(
        color = primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, primary.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable(enabled = !googleConnecting, onClick = onGoogleAuthClick)
            .testTag("onboarding_google_signin_btn")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
          Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (googleConnecting) "Yetkilendiriliyor..." else "Google Hesabı ile Bulut Korumasını Aç",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
      googleAuthError?.let { err ->
        Text(text = err, style = MaterialTheme.typography.labelSmall, color = errorColor)
      }
    }
  }
}

// =====================================================================
// SAHNE 8: Perforasyonlu Holografik Konser VIP Bileti (Final)
// =====================================================================
@Composable
internal fun OnboardingStep8FinalSummary(
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
  ageVerified: Boolean,
  shareActivity: Boolean,
  requireFollowApproval: Boolean,
  backgroundNotifServiceEnabled: Boolean,
  isSignedInWithGoogle: Boolean
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
    ConcertStageHeadline(
      kicker = "SAHNE AÇILIŞ BİLETİN HAZIR",
      title = "Vizyon VIP Sahne Geçişi",
      subtitle = "Tüm ayarların tek bir holografik VIP konser biletinde mühürlendi.",
      centered = true
    )

    HolographicConcertVipTicket(
      displayName = displayName,
      handle = handle,
      bio = bio,
      avatarPath = avatarPath,
      selectedTheme = selectedTheme,
      selectedAccent = selectedAccent,
      selectedCorner = selectedCorner,
      selectedStoryTheme = selectedStoryTheme,
      selectedChatPrivacy = selectedChatPrivacy,
      selectedLanguage = selectedLanguage,
      selectedGenres = selectedGenres,
      passcodeConfigured = passcodeConfigured,
      shareActivity = shareActivity,
      requireFollowApproval = requireFollowApproval,
      backgroundNotifServiceEnabled = backgroundNotifServiceEnabled,
      isSignedInWithGoogle = isSignedInWithGoogle
    )
  }
}

internal fun checkActiveNetwork(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
  val network = cm.activeNetwork ?: return false
  val caps = cm.getNetworkCapabilities(network) ?: return false
  return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
