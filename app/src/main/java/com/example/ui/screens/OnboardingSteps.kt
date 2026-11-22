package com.example.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.LiveProfilePreviewCard
import com.example.ui.components.LiveSoundwaveEqualizer
import com.example.ui.components.SchemaFeatureCard
import com.example.ui.components.StudioAvatar
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioInteractiveChip
import java.util.Locale

@Composable
internal fun OnboardingStepOneIdentity(
  displayName: String,
  handle: String,
  bio: String,
  avatarPath: String,
  selectedCorner: CornerDensity,
  isSignedInWithGoogle: Boolean,
  googleConnecting: Boolean,
  googleAuthError: String?,
  onPickPhotoClick: () -> Unit,
  onDisplayNameChange: (String) -> Unit,
  onHandleChange: (String) -> Unit,
  onBioChange: (String) -> Unit,
  onRestoreFromGoogleClick: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Rounded.PersonAdd,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "1. Kimlik & Profilin",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "İlk kurulum yalnızca başlangıçta gösterilir; profilini oluştur veya buluttan geri yükle.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Reinstall / Cross-device Cloud Restore Banner
    GlassStudioCard(
      highlighted = isSignedInWithGoogle,
      cornerRadius = selectedCorner.cardCornerDp.dp
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(
            imageVector = if (isSignedInWithGoogle) Icons.Rounded.CloudDone else Icons.Rounded.CloudDownload,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text =
                if (isSignedInWithGoogle) "Google Bulut Yedeği Bağlandı ✓"
                else "Daha Önce Hesabın Var mı?",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text =
                if (isSignedInWithGoogle) "Verilerin bulutla eşitleniyor; silip yüklesen bile geri döner."
                else "Uygulamayı silip yüklediysen Google hesabınla profil, fotoğraf, mesaj ve tercihlerini geri al.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        if (!isSignedInWithGoogle) {
          StudioGlassButton(
            text = if (googleConnecting) "Bulut Kontrol Ediliyor..." else "Mevcut Hesabımla Geri Yükle (Google)",
            icon = Icons.Rounded.CloudDownload,
            enabled = !googleConnecting,
            onClick = onRestoreFromGoogleClick,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_cloud_restore_btn")
          )
        }
        googleAuthError?.let { err ->
          Text(text = err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
      }
    }

    // Profile Identity + Avatar Picker Card
    GlassStudioCard(highlighted = true, cornerRadius = selectedCorner.cardCornerDp.dp) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          StudioAvatar(
            photoUrlOrPath = avatarPath,
            contentDescription = "Profil fotoğrafı seç",
            size = 64.dp
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Profil Fotoğrafı (Bulut Eşitli)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            StudioGlassButton(
              text = if (avatarPath.isBlank()) "Galeriden Fotoğraf Seç" else "Fotoğrafı Değiştir",
              icon = Icons.Rounded.AddAPhoto,
              onClick = onPickPhotoClick,
              modifier = Modifier.testTag("onboarding_pick_photo_btn")
            )
          }
        }

        OutlinedTextField(
          value = displayName,
          onValueChange = { if (it.length <= 40) onDisplayNameChange(it) },
          label = { Text("Görünen Ad *") },
          placeholder = { Text("Örn: Deniz Kaya") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
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
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_handle_input")
        )

        OutlinedTextField(
          value = bio,
          onValueChange = { if (it.length <= 160) onBioChange(it) },
          label = { Text("Müzik Biyografisi (İsteğe bağlı)") },
          placeholder = { Text("Örn: Gece sürüşü & Anadolu Rock") },
          maxLines = 2,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_bio_input")
        )
      }
    }

    // Schema Overview Card highlighting core app pillars
    SchemaFeatureCard(
      icon = Icons.Rounded.AutoAwesome,
      title = "4 Adımlı Stüdyo Kurulum Şeması",
      subtitle = "Kimlik → Tema & Hikaye Stili → Türk & Global Tarzlar → Güvenlik Kasası & Özet",
      badge = "İLK KURULUM"
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingStepTwoThemeAndStyle(
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
  onSelectCorner: (CornerDensity) -> Unit,
  onSelectStoryTheme: (StoryCardTheme) -> Unit,
  onSelectChatPrivacy: (ChatPrivacy) -> Unit,
  onShareActivityChange: (Boolean) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Rounded.Palette,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "2. Tema, Hikaye & Sohbet Stili",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Renk paletini, varsayılan hikaye kartını ve DM gizliliğini canlı önizlemeyle ayarla.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    LiveProfilePreviewCard(
      displayName = displayName,
      handle = handle,
      bio = bio,
      selectedLanguage = selectedLanguage,
      selectedTheme = selectedTheme,
      selectedGenres = selectedGenres,
      isSignedInWithGoogle = isSignedInWithGoogle,
      ageVerified = ageVerified,
      shareActivity = shareActivity,
      localAvatarPath = avatarPath,
      cornerRadiusDp = selectedCorner.cardCornerDp.dp,
      selectedStoryTheme = selectedStoryTheme,
      selectedChatPrivacy = selectedChatPrivacy,
      passcodeConfigured = passcodeConfigured
    )

    // Live Studio Theme, Accent Color & Corner Style Customizer
    GlassStudioCard(highlighted = true, cornerRadius = selectedCorner.cardCornerDp.dp) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Tema Modu, Vurgu Rengi & Kart Formu",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          AppThemeMode.entries.chunked(2).forEach { pair ->
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              pair.forEach { mode ->
                val selected = selectedTheme == mode
                val swatchColor =
                  when (mode) {
                    AppThemeMode.LIGHT -> Color(0xFFF3F6FC)
                    AppThemeMode.DARK -> Color(0xFF141B29)
                    AppThemeMode.NIGHT_AMOLED -> Color(0xFF030508)
                    AppThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
                  }
                Surface(
                  color =
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  shape = RoundedCornerShape(14.dp),
                  modifier =
                    Modifier.weight(1f)
                      .border(
                        width = if (selected) 1.5.dp else 1.dp,
                        color =
                          if (selected) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp)
                      )
                      .clip(RoundedCornerShape(14.dp))
                      .clickable { onSelectTheme(mode) }
                      .testTag("onboarding_theme_${mode.name}")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                  ) {
                    Box(
                      modifier =
                        Modifier.size(16.dp)
                          .clip(CircleShape)
                          .background(swatchColor)
                          .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        text = mode.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

        Text("Vurgu Rengi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
          AccentPalette.entries.forEach { palette ->
            val selected = selectedAccent == palette
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier =
                Modifier.clickable { onSelectAccent(palette) }
                  .testTag("onboarding_accent_${palette.name}")
            ) {
              Box(
                modifier =
                  Modifier.size(38.dp)
                    .clip(CircleShape)
                    .background(Color(palette.primaryHex))
                    .border(
                      width = if (selected) 3.dp else 1.dp,
                      color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                      shape = CircleShape
                    )
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = palette.label.substringBefore(" "),
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }

        Text("Köşe Formu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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

    // Story Card Default Theme & Chat Privacy Style
    GlassStudioCard(cornerRadius = selectedCorner.cardCornerDp.dp) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Hikaye Kartı Teması & Sohbet Gizliliği",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        }

        Text("Varsayılan Hikaye Kartı Stili", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(StoryCardTheme.entries) { theme ->
            val selected = selectedStoryTheme == theme
            val accent = Color(theme.accentColorHex)
            Surface(
              color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(999.dp),
              modifier =
                Modifier.clip(RoundedCornerShape(999.dp))
                  .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(999.dp)
                  )
                  .clickable { onSelectStoryTheme(theme) }
                  .testTag("onboarding_story_theme_${theme.name}")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
              ) {
                Box(
                  modifier =
                    Modifier.size(12.dp)
                      .clip(CircleShape)
                      .background(Brush.linearGradient(listOf(Color(theme.startColorHex), accent)))
                )
                Text(
                  text = theme.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Kimler Doğrudan Mesaj (DM) Gönderebilir?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          ChatPrivacy.entries.forEach { privacy ->
            val selected = selectedChatPrivacy == privacy
            Surface(
              color =
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color =
                      if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(14.dp)
                  )
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { onSelectChatPrivacy(privacy) }
                  .testTag("onboarding_chat_privacy_${privacy.name}")
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = privacy.label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Profilimde Son Dinlediğimi Göster",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Akışa otomatik kart düşmez; sadece profiline girenler son 5 şarkını görür.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Switch(
            checked = shareActivity,
            onCheckedChange = onShareActivityChange,
            modifier = Modifier.testTag("onboarding_share_switch")
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OnboardingStepThreeLanguageAndGenres(
  selectedLanguage: AppLanguage,
  selectableGenres: List<MusicCategory>,
  selectedGenres: List<String>,
  onSelectLanguage: (AppLanguage) -> Unit,
  onToggleGenre: (String) -> Unit
) {
  val turkishGenres = selectableGenres.filter { it.group == MusicCategoryGroup.TURKISH }
  val globalGenres = selectableGenres.filter { it.group == MusicCategoryGroup.GLOBAL }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Rounded.MusicNote,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(26.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "3. Dil & Müzik Tarzları",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Günün Vitrini ve Keşfet seçkisi burada seçtiğin bölge ve türlere göre şekillenir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    GlassStudioCard(highlighted = true, cornerRadius = 22.dp) {
      Box(modifier = Modifier.fillMaxWidth()) {
        Image(
          painter = painterResource(R.drawable.img_hero_stage),
          contentDescription = "Vizyon Music Kurulum Sahnesi",
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize()
        )
        Box(
          modifier =
            Modifier.matchParentSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color(0x66061024), Color(0xCC060F24), Color(0xF5050A14))
                )
              )
        )
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.GraphicEq,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "TÜRK MÜZİĞİ & GLOBAL KATALOG",
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFF93C5FD),
              fontWeight = FontWeight.Bold
            )
          }
          LiveSoundwaveEqualizer(barCount = 24, energyBoost = 1f, height = 26.dp)
        }
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Rounded.Public,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "Dil & Katalog Bölgesi",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      AppLanguage.entries.forEach { lang ->
        val selected = selectedLanguage == lang
        GlassStudioCard(
          highlighted = selected,
          cornerRadius = 18.dp,
          modifier =
            Modifier.clickable { onSelectLanguage(lang) }
              .testTag("onboarding_lang_card_${lang.name}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = lang.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = lang.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
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
    }

    GlassStudioCard(cornerRadius = 20.dp) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Müzik Tarzlarını Seç (En az 1)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = "${selectedGenres.size}/8 seçili",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        Text(
          text = "Türk Müziği Seçkisi",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
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
          color = MaterialTheme.colorScheme.secondary,
          fontWeight = FontWeight.Bold
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
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
}

@Composable
internal fun OnboardingStepFourSecurityAndSummary(
  displayName: String,
  handle: String,
  selectedTheme: AppThemeMode,
  selectedAccent: AccentPalette,
  selectedStoryTheme: StoryCardTheme,
  selectedChatPrivacy: ChatPrivacy,
  selectedLanguage: AppLanguage,
  selectedGenres: List<String>,
  passcode: String,
  passcodeConfirm: String,
  rememberMe: Boolean,
  lockOnLaunch: Boolean,
  isNetworkAvailable: Boolean,
  ageVerified: Boolean,
  hasNotifPermission: Boolean,
  isSignedInWithGoogle: Boolean,
  googleConnecting: Boolean,
  googleAuthError: String?,
  onPasscodeChange: (String) -> Unit,
  onPasscodeConfirmChange: (String) -> Unit,
  onRememberMeChange: (Boolean) -> Unit,
  onLockOnLaunchChange: (Boolean) -> Unit,
  onVerifyNetwork: () -> Unit,
  onAgeVerifiedChange: (Boolean) -> Unit,
  onRequestNotifPermission: () -> Unit,
  onGoogleAuthClick: () -> Unit
) {
  var showPasscode by remember { mutableStateOf(false) }
  val (strengthLabel, strengthProgress) = remember(passcode) {
    PasscodeSecurityHelper.passcodeStrengthLabel(passcode)
  }
  val passcodeReady = passcode.trim().length >= 4 && passcode == passcodeConfirm

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Rounded.GppGood,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "4. Güvenlik Kasası & Özet",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Stüdyo şifreni belirle, gerekli izinleri tamamla ve tüm kurulum ayarlarını gözden geçir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Security Passcode, Remember Me & Data Protection Vault Card
    GlassStudioCard(highlighted = true, cornerRadius = 20.dp) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Hesap Güvenliği & Beni Hatırla *",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        }
        Text(
          text = "Profilin, mesajların ve yedekli verilerin için 4+ karakterli bir stüdyo şifresi oluştur.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
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
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_passcode_input")
        )

        OutlinedTextField(
          value = passcodeConfirm,
          onValueChange = { if (it.length <= 24) onPasscodeConfirmChange(it) },
          label = { Text("Şifre Tekrar *") },
          singleLine = true,
          visualTransformation = if (showPasscode) VisualTransformation.None else PasswordVisualTransformation(),
          isError = passcodeConfirm.isNotEmpty() && passcode != passcodeConfirm,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_passcode_confirm_input")
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Şifre Gücü: $strengthLabel",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.SemiBold
            )
            if (passcodeReady) {
              Text(
                text = "Eşleşti ✓",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
              )
            }
          }
          LinearProgressIndicator(
            progress = { strengthProgress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color =
              if (strengthProgress >= 0.7f) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.primary
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Beni Hatırla", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Profil ve tercihlerim cihaz + Google bulutta koruma altında kalsın.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = rememberMe,
            onCheckedChange = onRememberMeChange,
            modifier = Modifier.testTag("onboarding_remember_me_switch")
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Açılışta Şifre Kilidi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
              text = "Uygulamayı açarken stüdyo şifresi doğrulaması iste.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = lockOnLaunch,
            onCheckedChange = onLockOnLaunchChange,
            modifier = Modifier.testTag("onboarding_lock_on_launch_switch")
          )
        }
      }
    }

    // 13+ Mandatory Safety Gate
    GlassStudioCard(
      highlighted = true,
      accentColor = if (ageVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
      cornerRadius = 20.dp
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "13+ Yaş & Güvenlik Duvarı Onayı *",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Platform genel ve yetişkin dinleyicilere yöneliktir; çocuk içerikleri filtrelenmiştir.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Switch(
            checked = ageVerified,
            onCheckedChange = onAgeVerifiedChange,
            modifier = Modifier.testTag("onboarding_age_switch")
          )
        }
        Text(
          text =
            if (ageVerified) "✓ 13 yaşından büyük olduğum ve güvenlik kuralları onaylandı."
            else "Tamamlamak için zorunlu 13+ yaş güvenlik onayını aktifleştirin.",
          style = MaterialTheme.typography.labelMedium,
          color = if (ageVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
      }
    }

    // Network Readiness & Notification Permission
    GlassStudioCard(cornerRadius = 18.dp) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Ağ & Resmi Medya Akışı",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
          Surface(
            color =
              if (isNetworkAvailable) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
              else MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = if (isNetworkAvailable) "Aktif ✓" else "Bağlantı Yok",
              style = MaterialTheme.typography.labelSmall,
              color = if (isNetworkAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
        Text(
          text = "Kotasız iTunes katalog aramaları, sohbet ve Resmi YouTube IFrame oynatıcı için internet erişimi doğrulanır.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedButton(
          onClick = onVerifyNetwork,
          modifier = Modifier.heightIn(min = 48.dp).testTag("check_network_btn")
        ) {
          Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Bağlantıyı Doğrula")
        }
      }
    }

    GlassStudioCard(highlighted = hasNotifPermission, cornerRadius = 18.dp) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(14.dp)
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sohbet & Sosyal Bildirim İzni",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = if (hasNotifPermission) "Cihaz bildirimleri aktif ✓" else "Yeni mesaj, takip, beğeni ve hikaye bildirimlerini anında al.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (!hasNotifPermission) {
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedButton(
            onClick = onRequestNotifPermission,
            modifier = Modifier.heightIn(min = 48.dp).testTag("onboarding_notif_permission_btn")
          ) {
            Text("İzin Ver")
          }
        }
      }
    }

    if (!isSignedInWithGoogle) {
      GlassStudioCard(cornerRadius = 18.dp) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Rounded.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Google Hesabı ile Bulut Korumasını Aç",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
          }
          OutlinedButton(
            onClick = onGoogleAuthClick,
            enabled = !googleConnecting,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("onboarding_google_signin_btn")
          ) {
            Text(if (googleConnecting) "Yetkilendiriliyor..." else "Google Hesabı ile Yetkilendir")
          }
          googleAuthError?.let { err ->
            Text(text = err, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
          }
        }
      }
    }

    // Complete Professional Setup Summary Card
    GlassStudioCard(
      highlighted = true,
      cornerRadius = 22.dp,
      modifier = Modifier.fillMaxWidth().testTag("onboarding_setup_summary_card")
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "Kurulum Özeti",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
        }
        SetupSummaryRow("Profil Kimliği", "${displayName.trim().ifBlank { "Vizyon Dinleyicisi" }} (@${handle.trim().ifBlank { "vizyon" }})")
        SetupSummaryRow("Tema & Renk", "${selectedTheme.label} • ${selectedAccent.label}")
        SetupSummaryRow("Hikaye & DM", "${selectedStoryTheme.label} • ${selectedChatPrivacy.label}")
        SetupSummaryRow("Katalog & Tarzlar", "${selectedLanguage.badge} • ${selectedGenres.size} müzik türü")
        SetupSummaryRow(
          "Güvenlik & Bulut",
          "${if (passcodeReady) "Şifre Aktif ✓" else "Şifre Bekliyor"} • ${if (isSignedInWithGoogle) "Google Bulut" else "Cihaz Kasası"}"
        )
      }
    }
  }
}

@Composable
private fun SetupSummaryRow(label: String, value: String) {
  Row(
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

internal fun checkActiveNetwork(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
  val network = cm.activeNetwork ?: return false
  val caps = cm.getNetworkCapabilities(network) ?: return false
  return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
