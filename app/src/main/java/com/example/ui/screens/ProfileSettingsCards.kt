package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.StudioSoundEngine
import com.example.model.AccentPalette
import com.example.model.ApiBridgeStatus
import com.example.model.AppAppearanceConfig
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.CornerDensity
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioInteractiveChip

@Composable
fun ProfileNotificationSettingsCard(
  notificationsEnabled: Boolean,
  unreadCount: Int,
  hasDevicePermission: Boolean,
  onToggleNotifications: (Boolean) -> Unit,
  onRequestDevicePermission: () -> Unit,
  onOpenInbox: () -> Unit
) {
  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Kişisel Bildirim Ayarları",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }
        if (unreadCount > 0) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            shape = RoundedCornerShape(999.dp)
          ) {
            Text(
              text = "$unreadCount yeni",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Takipçi ve beğeni bildirimleri", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text(
            text = "Seni takip eden veya şarkını beğenen olunca anında bildirim mesajı al.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = notificationsEnabled,
          onCheckedChange = onToggleNotifications,
          modifier = Modifier.testTag("notifications_permission_switch")
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StudioGlassButton(
          text = "Bildirim Kutusu",
          onClick = onOpenInbox,
          modifier = Modifier.weight(1f).testTag("notifications_open_inbox_btn")
        )
        if (!hasDevicePermission) {
          StudioGlassButton(
            text = "Cihaz İzni Ver",
            onClick = onRequestDevicePermission,
            tintColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).testTag("request_device_notif_permission_btn")
          )
        }
      }
    }
  }
}

@Composable
fun ProfileSoundPreviewCard() {
  val context = LocalContext.current
  var lastTested by remember { mutableStateOf("") }

  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth().testTag("sound_preview_card")
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Bildirim & Mesaj Sesleri",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Mat su damlası ve yumuşak pıt tonlarını dene",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = "Su Damlası",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        SoundTestTileButton(
          label = "Gönderme",
          icon = Icons.AutoMirrored.Rounded.Send,
          selected = lastTested == "send",
          onClick = {
            lastTested = "send"
            StudioSoundEngine.playMessageSendChime(context)
          },
          modifier = Modifier.weight(1f).testTag("test_send_sound_btn")
        )
        SoundTestTileButton(
          label = "Mesaj Geldi",
          icon = Icons.Rounded.ChatBubbleOutline,
          selected = lastTested == "recv",
          onClick = {
            lastTested = "recv"
            StudioSoundEngine.playMessageReceiveChime(context)
          },
          modifier = Modifier.weight(1f).testTag("test_receive_sound_btn")
        )
        SoundTestTileButton(
          label = "Bildirim",
          icon = Icons.Rounded.NotificationsActive,
          selected = lastTested == "notif",
          onClick = {
            lastTested = "notif"
            StudioSoundEngine.playNotificationPreviewChime(context)
          },
          modifier = Modifier.weight(1f).testTag("test_notify_sound_btn")
        )
      }
    }
  }
}

@Composable
private fun SoundTestTileButton(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color =
      if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
    shape = RoundedCornerShape(16.dp),
    modifier =
      modifier
        .height(64.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(
          width = 1.dp,
          color =
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
          shape = RoundedCornerShape(16.dp)
        )
        .clickable(onClick = onClick)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun ProfileThemeModeCard(
  appearance: AppAppearanceConfig,
  onThemeModeChange: (AppThemeMode) -> Unit
) {
  GlassStudioCard(
    highlighted = true,
    cornerRadius = 24.dp,
    modifier = Modifier.fillMaxWidth().testTag("profile_personalization_panel")
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(Icons.Rounded.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("Tema & Kişiselleştirme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
              text = "Görünümü, renk paletini ve atmosferi anında özelleştir.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = appearance.themeMode.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppThemeMode.entries.chunked(2).forEach { rowItems ->
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            rowItems.forEach { mode ->
              val selected = appearance.themeMode == mode
              val swatchColor =
                when (mode) {
                  AppThemeMode.LIGHT -> Color(0xFFF3F6FC)
                  AppThemeMode.DARK -> Color(0xFF141B29)
                  AppThemeMode.NIGHT_AMOLED -> Color(0xFF030508)
                  AppThemeMode.SYSTEM -> MaterialTheme.colorScheme.primary
                }
              Surface(
                color =
                  if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
                modifier =
                  Modifier.weight(1f)
                    .border(
                      width = if (selected) 1.5.dp else 0.8.dp,
                      color =
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                      shape = MaterialTheme.shapes.medium
                    )
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onThemeModeChange(mode) }
                    .testTag("theme_mode_${mode.name}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(12.dp)
                ) {
                  Box(
                    modifier =
                      Modifier.size(16.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                  )
                  Column {
                    Text(
                      mode.label,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      mode.subtitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAccentAndLanguageCard(
  appearance: AppAppearanceConfig,
  onAccentChange: (AccentPalette) -> Unit,
  onCornerDensityChange: (CornerDensity) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit
) {
  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Kişisel Renk, Form & Dil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      }

      Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        AccentPalette.entries.forEach { palette ->
          val selected = appearance.accentPalette == palette
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onAccentChange(palette) }.testTag("accent_palette_${palette.name}")
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

      Text("Köşe Yumuşaklığı", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CornerDensity.entries.forEach { density ->
          StudioInteractiveChip(
            label = density.label,
            selected = appearance.cornerDensity == density,
            onClick = { onCornerDensityChange(density) }
          )
        }
      }

      Text("Dil & Katalog Bölgesi", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppLanguage.entries.forEach { lang ->
          StudioInteractiveChip(
            label = lang.label,
            selected = appearance.preferredLanguage == lang,
            onClick = { onLanguageChange(lang) }
          )
        }
      }
    }
  }
}

@Composable
fun ProfilePlatformStatusCard(bridgeStatus: ApiBridgeStatus?) {
  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Yasal Platform & Oynatıcı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
      }
      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          Text(
            text = bridgeStatus?.activeMessage ?: "iTunes Katalog & Resmi YouTube IFrame Oynatıcı",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "• Katalog: Kotasız iTunes Search API (Çocuk içerikleri filtrelendi)",
            style = MaterialTheme.typography.bodySmall
          )
          Text(
            text =
              if (bridgeStatus?.youtubeApiConfigured == true)
                "• Video Eşleştirme: YouTube Data API v3 & Canlı IFrame Aktif"
              else
                "• Video Eşleştirme: YOUTUBE_API_KEY Secrets panelinden bekleniyor",
            style = MaterialTheme.typography.bodySmall,
            color =
              if (bridgeStatus?.youtubeApiConfigured == true) MaterialTheme.colorScheme.onSurface
              else MaterialTheme.colorScheme.error
          )
          Text(
            text = "• Kesintisiz Mini Video: Gezinme ve aramada albüm kapağı yerinde canlı video",
            style = MaterialTheme.typography.bodySmall
          )
          Text(
            text = "• Şarkı Sözleri: LRCLIB Zaman Senkronlu Aktif",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

@Composable
fun ProfileRecentTracksCard(
  recentTracks: List<com.example.model.RecentTrackSummary>,
  onPlayTrack: (com.example.model.MusicTrack, List<com.example.model.MusicTrack>) -> Unit,
  onShareAsStory: (com.example.model.MusicTrack) -> Unit
) {
  val queue = recentTracks.map { it.toMusicTrack() }
  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth().testTag("own_profile_recent_tracks_card")
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Son 5 Dinlenen Şarkım",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = "${recentTracks.size}/5",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
          )
        }
      }

      if (recentTracks.isEmpty()) {
        Text(
          text = "Henüz dinlediğin bir parça kaydedilmedi. Keşfet'ten bir şarkı başlattığında son 5 parçan burada listelenir.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        recentTracks.take(5).forEachIndexed { idx, summary ->
          val track = summary.toMusicTrack()
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onPlayTrack(track, queue) }
                .testTag("own_recent_track_row_$idx")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
              Text(
                text = "${idx + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = summary.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1
                )
                Text(
                  text = summary.artistName.ifBlank { "Sanatçı" },
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1
                )
              }
              StudioGlassButton(
                text = "Hikaye",
                onClick = { onShareAsStory(track) },
                modifier = Modifier.testTag("own_recent_story_btn_$idx")
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileVaultAndPrivacyCard(
  appearance: AppAppearanceConfig,
  isSignedIn: Boolean,
  followersCount: Int,
  followingCount: Int,
  onSaveVaultPasscode: (String, Boolean, Boolean) -> Unit,
  onUpdateVaultSettings: (Boolean, Boolean) -> Unit,
  onChatPrivacyChange: (com.example.model.ChatPrivacy) -> Unit,
  onDefaultStoryThemeChange: (com.example.model.StoryCardTheme) -> Unit,
  onOpenFollowLists: () -> Unit,
  onSyncCloudBackupNow: () -> Unit
) {
  var newPasscode by remember { mutableStateOf("") }
  var rememberMe by remember(appearance.rememberMe) {
    mutableStateOf(appearance.rememberMe)
  }
  var lockOnLaunch by remember(appearance.lockOnLaunch) {
    mutableStateOf(appearance.lockOnLaunch)
  }

  GlassStudioCard(
    highlighted = appearance.hasVaultPasscode,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth().testTag("profile_vault_privacy_card")
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = "Veri Koruma, Şifre & Sohbet",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = if (appearance.hasVaultPasscode) "Şifre Aktif ✓" else "Şifresiz",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StudioGlassButton(
          text = "Takipçi ($followersCount) & Takip ($followingCount)",
          onClick = onOpenFollowLists,
          modifier = Modifier.weight(1f).testTag("profile_open_follow_lists_btn")
        )
      }

      Text("Sohbet Gizliliği", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.model.ChatPrivacy.entries.forEach { privacy ->
          StudioInteractiveChip(
            label = privacy.label,
            selected = appearance.chatPrivacy == privacy,
            onClick = { onChatPrivacyChange(privacy) },
            modifier = Modifier.testTag("profile_chat_privacy_${privacy.name}")
          )
        }
      }

      Text("Varsayılan Hikaye Kartı Teması", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        com.example.model.StoryCardTheme.entries.forEach { theme ->
          StudioInteractiveChip(
            label = theme.label,
            selected = appearance.defaultStoryTheme == theme,
            onClick = { onDefaultStoryThemeChange(theme) },
            modifier = Modifier.testTag("profile_story_theme_${theme.name}")
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Beni Hatırla", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
          Text(
            text = "Oturumun ve kişisel tercihlerin cihaz & bulutta korunsun.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = rememberMe,
          onCheckedChange = {
            rememberMe = it
            onUpdateVaultSettings(rememberMe, lockOnLaunch)
          }
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
            text = "Uygulama açılışında kasa şifresi sor.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = lockOnLaunch,
          enabled = appearance.hasVaultPasscode || newPasscode.trim().length >= 4,
          onCheckedChange = {
            lockOnLaunch = it
            onUpdateVaultSettings(rememberMe, lockOnLaunch)
          }
        )
      }

      androidx.compose.material3.OutlinedTextField(
        value = newPasscode,
        onValueChange = { if (it.length <= 24) newPasscode = it },
        label = { Text(if (appearance.hasVaultPasscode) "Yeni Stüdyo Şifresi (en az 4)" else "Stüdyo Şifresi Oluştur (en az 4)") },
        singleLine = true,
        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("profile_passcode_input")
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StudioGlassButton(
          text = "Şifreyi Kaydet",
          enabled = newPasscode.trim().length >= 4,
          onClick = {
            onSaveVaultPasscode(newPasscode, rememberMe, lockOnLaunch)
            newPasscode = ""
          },
          modifier = Modifier.weight(1f).testTag("profile_save_passcode_btn")
        )
        if (isSignedIn) {
          StudioGlassButton(
            text = "Buluta Yedekle",
            onClick = onSyncCloudBackupNow,
            tintColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).testTag("sync_cloud_backup_btn")
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileFavoriteGenresCard(
  selectedGenres: List<String>,
  onUpdateGenres: (List<String>) -> Unit
) {
  val turkishCategories =
    com.example.model.MusicCategory.entries.filter { it.group == com.example.model.MusicCategoryGroup.TURKISH }
  val globalCategories =
    com.example.model.MusicCategory.entries.filter { it.group == com.example.model.MusicCategoryGroup.GLOBAL }

  GlassStudioCard(
    highlighted = false,
    cornerRadius = 22.dp,
    modifier = Modifier.fillMaxWidth().testTag("profile_genres_card")
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Favori Müzik Türlerim", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
          text = "${selectedGenres.size}/8",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
      }

      Text("Türk Müziği", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        turkishCategories.forEach { cat ->
          val active = cat.label in selectedGenres
          StudioInteractiveChip(
            label = cat.label,
            selected = active,
            onClick = {
              val next =
                if (active) {
                  if (selectedGenres.size > 1) selectedGenres - cat.label else selectedGenres
                } else {
                  (selectedGenres + cat.label).distinct().take(8)
                }
              onUpdateGenres(next)
            }
          )
        }
      }

      Text("Global Türler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        globalCategories.forEach { cat ->
          val active = cat.label in selectedGenres
          StudioInteractiveChip(
            label = cat.label,
            selected = active,
            onClick = {
              val next =
                if (active) {
                  if (selectedGenres.size > 1) selectedGenres - cat.label else selectedGenres
                } else {
                  (selectedGenres + cat.label).distinct().take(8)
                }
              onUpdateGenres(next)
            }
          )
        }
      }
    }
  }
}

