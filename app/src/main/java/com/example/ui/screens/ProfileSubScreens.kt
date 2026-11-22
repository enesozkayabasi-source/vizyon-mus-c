package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AccentPalette
import com.example.model.ApiBridgeStatus
import com.example.model.AppAppearanceConfig
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.StoryCardTheme
import com.example.notifications.VizyonNotificationHelper
import com.example.ui.components.AccountDeletionSecurityCard
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.StudioAvatar
import com.example.ui.components.StudioCircleIconButton
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioGradientCtaButton
import com.example.ui.components.StudioInteractiveChip

@Composable
internal fun ProfileSubPageTopBar(
  title: String,
  subtitle: String,
  onBack: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    StudioCircleIconButton(
      icon = Icons.AutoMirrored.Rounded.ArrowBack,
      contentDescription = "Geri",
      onClick = onBack,
      size = 48.dp,
      modifier = Modifier.testTag("profile_subpage_back_btn")
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
internal fun ProfileEditSubScreen(
  userPhotoUrl: String,
  initialDisplayName: String,
  initialHandle: String,
  initialBio: String,
  initialShareActivity: Boolean,
  onBack: () -> Unit,
  onPickProfilePhoto: (Uri) -> Unit,
  onShareActivityToggle: (Boolean) -> Unit,
  onSaveProfile: (String, String, String, Boolean) -> Unit
) {
  BackHandler(onBack = onBack)

  var displayName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
  var handle by remember(initialHandle) { mutableStateOf(initialHandle) }
  var bio by remember(initialBio) { mutableStateOf(initialBio) }
  var shareActivity by remember(initialShareActivity) { mutableStateOf(initialShareActivity) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) onPickProfilePhoto(uri)
    }

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("profile_edit_subscreen")
  ) {
    item {
      ProfileSubPageTopBar(
        title = "Profili Düzenle",
        subtitle = "KİMLİK & GÖRÜNÜM",
        onBack = onBack
      )
    }

    item {
      GlassStudioCard(
        highlighted = true,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth().testTag("profile_editor_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            StudioAvatar(
              photoUrlOrPath = userPhotoUrl,
              contentDescription = "Profil fotoğrafı",
              size = 80.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = displayName.ifBlank { "Vizyon Dinleyicisi" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Seçilen fotoğraf hikayelerde ve paylaşımlarda eşitlenir.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              StudioGlassButton(
                text = "Fotoğraf Seç",
                icon = Icons.Rounded.AddAPhoto,
                onClick = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                modifier = Modifier.testTag("pick_profile_photo_btn")
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kişisel Bilgiler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }

          OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= 50) displayName = it },
            label = { Text("Görünen Ad") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
          )
          OutlinedTextField(
            value = handle,
            onValueChange = { if (it.length <= 30) handle = it },
            label = { Text("Kullanıcı Adı (@handle)") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_handle_input")
          )
          OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 200) bio = it },
            label = { Text("Hakkında / Müzik Biyografisi") },
            maxLines = 3,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input")
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Dinlediğim son müziği profilimde göster",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Sadece profil kartında görünür, akışı kalabalıklaştırmaz.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = shareActivity,
              onCheckedChange = {
                shareActivity = it
                onShareActivityToggle(it)
              },
              modifier = Modifier.testTag("edit_profile_share_activity_switch")
            )
          }

          StudioGradientCtaButton(
            text = "Değişiklikleri Kaydet",
            icon = Icons.Rounded.CheckCircle,
            onClick = {
              onSaveProfile(displayName, handle, bio, shareActivity)
              onBack()
            },
            modifier = Modifier.fillMaxWidth().testTag("save_profile_btn")
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileAboutPrivacySubScreen(
  appearance: AppAppearanceConfig,
  unreadNotificationCount: Int,
  bridgeStatus: ApiBridgeStatus?,
  onBack: () -> Unit,
  onChatPrivacyChange: (ChatPrivacy) -> Unit,
  onNotificationsToggle: (Boolean) -> Unit,
  onOpenNotifications: () -> Unit,
  onNotifyMessage: (String) -> Unit
) {
  BackHandler(onBack = onBack)
  val context = LocalContext.current

  var hasNotificationPermission by remember {
    mutableStateOf(VizyonNotificationHelper.hasNotificationPermission(context))
  }
  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasNotificationPermission = granted
      onNotifyMessage(
        if (granted) "Cihaz bildirim izni verildi!"
        else "Cihaz bildirimi kapalı; bildirimlerin uygulama içi kutuya düşmeye devam edecek."
      )
    }

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("profile_about_privacy_subscreen")
  ) {
    item {
      ProfileSubPageTopBar(
        title = "Hakkında & Gizlilik",
        subtitle = "UYGULAMA & GÜVENLİK",
        onBack = onBack
      )
    }

    item {
      GlassStudioCard(
        highlighted = true,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth().testTag("about_vizyon_card")
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Vizyon Music", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
              shape = RoundedCornerShape(999.dp)
            ) {
              Text(
                text = "v2.6 Studio",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
          Text(
            text = "Türk ve global müzik keşfi, 24 saatlik görsel müzik hikayeleri, takipçilerle birebir sohbet ve gerçek zamanlı senkronlu şarkı sözleri için tasarlanmış modern stüdyo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.56f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Text("Gizlilik & Veri İlkeleri", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
              }
              Text("• Fotoğraf seçimi sıfır depolama izniyle Android Fotoğraf Seçici üzerinden yapılır.", style = MaterialTheme.typography.bodySmall)
              Text("• Stüdyo kasa şifresi cihazda SHA-256 + rastgele tuz ile korunur.", style = MaterialTheme.typography.bodySmall)
              Text("• Google bulut yedeği isteğe bağlıdır; tüm verilerini istediğin an kalıcı silebilirsin.", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    item {
      GlassStudioCard(
        highlighted = false,
        cornerRadius = 22.dp,
        modifier = Modifier.fillMaxWidth().testTag("about_chat_privacy_card")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Sohbet Gizliliği", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          }
          Text(
            text = "Sana kimlerin doğrudan müzik ve mesaj gönderebileceğini belirle.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChatPrivacy.entries.forEach { privacy ->
              StudioInteractiveChip(
                label = privacy.label,
                selected = appearance.chatPrivacy == privacy,
                onClick = { onChatPrivacyChange(privacy) },
                modifier = Modifier.testTag("profile_chat_privacy_${privacy.name}")
              )
            }
          }
        }
      }
    }

    item {
      ProfileNotificationSettingsCard(
        notificationsEnabled = appearance.notificationsEnabled,
        unreadCount = unreadNotificationCount,
        hasDevicePermission = hasNotificationPermission,
        onToggleNotifications = onNotificationsToggle,
        onRequestDevicePermission = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          } else {
            hasNotificationPermission = true
            onNotifyMessage("Cihaz bildirimleri etkin!")
          }
        },
        onOpenInbox = onOpenNotifications
      )
    }

    item {
      ProfilePlatformStatusCard(bridgeStatus = bridgeStatus)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProfileSettingsSubScreen(
  isSignedIn: Boolean,
  displayName: String,
  handle: String,
  firebaseUid: String?,
  firebaseEmail: String?,
  appearance: AppAppearanceConfig,
  isConnectingAuth: Boolean,
  authError: String?,
  isDeletingAccount: Boolean,
  followersCount: Int,
  followingCount: Int,
  onBack: () -> Unit,
  onGoogleAuthClick: () -> Unit,
  onSignOutClick: () -> Unit,
  onSyncCloudBackupNow: () -> Unit,
  onThemeModeChange: (AppThemeMode) -> Unit,
  onAccentChange: (AccentPalette) -> Unit,
  onCornerDensityChange: (CornerDensity) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onDefaultStoryThemeChange: (StoryCardTheme) -> Unit,
  onUpdateFavoriteGenres: (List<String>) -> Unit,
  onSaveVaultPasscode: (String, Boolean, Boolean) -> Unit,
  onUpdateVaultSettings: (Boolean, Boolean) -> Unit,
  onOpenFollowLists: () -> Unit,
  onOpenDeleteWall: () -> Unit
) {
  BackHandler(onBack = onBack)

  var newPasscode by remember { mutableStateOf("") }
  var rememberMe by remember(appearance.rememberMe) { mutableStateOf(appearance.rememberMe) }
  var lockOnLaunch by remember(appearance.lockOnLaunch) { mutableStateOf(appearance.lockOnLaunch) }

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("profile_settings_subscreen")
  ) {
    item {
      ProfileSubPageTopBar(
        title = "Profil Ayarları",
        subtitle = "TEMA, BULUT & KASA",
        onBack = onBack
      )
    }

    // 1. Prominent Theme, Accent Palette, Corner Style & Language right at the top!
    item { ProfileThemeModeCard(appearance = appearance, onThemeModeChange = onThemeModeChange) }
    item {
      ProfileAccentAndLanguageCard(
        appearance = appearance,
        onAccentChange = onAccentChange,
        onCornerDensityChange = onCornerDensityChange,
        onLanguageChange = onLanguageChange
      )
    }

    // 2. Default Story Card Theme Selector right below personalization
    item {
      GlassStudioCard(
        highlighted = false,
        cornerRadius = 22.dp,
        modifier = Modifier.fillMaxWidth().testTag("profile_story_theme_card")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Varsayılan Hikaye Kartı Teması", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          }
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StoryCardTheme.entries.forEach { theme ->
              StudioInteractiveChip(
                label = theme.label,
                selected = appearance.defaultStoryTheme == theme,
                onClick = { onDefaultStoryThemeChange(theme) },
                modifier = Modifier.testTag("profile_story_theme_${theme.name}")
              )
            }
          }
        }
      }
    }

    // 2.5 Sound Preview & Test Card
    item {
      ProfileSoundPreviewCard()
    }

    // 3. Onboarding-grade Google Cloud Backup & Identity Card
    item {
      OnboardingMatchedCloudCard(
        isSignedIn = isSignedIn,
        displayName = displayName,
        handle = handle,
        firebaseUid = firebaseUid,
        firebaseEmail = firebaseEmail,
        ageVerified = appearance.ageVerified,
        isConnecting = isConnectingAuth,
        authError = authError,
        onSignInOrRegisterClick = onGoogleAuthClick,
        onSyncBackupNow = onSyncCloudBackupNow,
        onSignOutClick = onSignOutClick
      )
    }

    // 4. Studio Security Vault Card
    item {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text("Güvenlik Kasası & Şifre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
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

          StudioGlassButton(
            text = "Takipçi ($followersCount) & Takip ($followingCount) Bağlantıları",
            onClick = onOpenFollowLists,
            modifier = Modifier.fillMaxWidth().testTag("profile_open_follow_lists_btn")
          )

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

          OutlinedTextField(
            value = newPasscode,
            onValueChange = { if (it.length <= 24) newPasscode = it },
            label = { Text(if (appearance.hasVaultPasscode) "Yeni Stüdyo Şifresi (en az 4)" else "Stüdyo Şifresi Oluştur (en az 4)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_passcode_input")
          )

          StudioGlassButton(
            text = "Şifreyi Kaydet",
            enabled = newPasscode.trim().length >= 4,
            onClick = {
              onSaveVaultPasscode(newPasscode, rememberMe, lockOnLaunch)
              newPasscode = ""
            },
            modifier = Modifier.fillMaxWidth().testTag("profile_save_passcode_btn")
          )
        }
      }
    }

    // 5. Favorite Genres
    item {
      ProfileFavoriteGenresCard(
        selectedGenres = appearance.favoriteGenres,
        onUpdateGenres = onUpdateFavoriteGenres
      )
    }

    // 6. Account Deletion Security Wall
    item {
      AccountDeletionSecurityCard(
        isSignedIn = isSignedIn,
        ageVerified = appearance.ageVerified,
        firebaseEmail = firebaseEmail,
        isDeletingAccount = isDeletingAccount,
        onOpenDeleteWall = onOpenDeleteWall
      )
    }
  }
}

@Composable
private fun OnboardingMatchedCloudCard(
  isSignedIn: Boolean,
  displayName: String,
  handle: String,
  firebaseUid: String?,
  firebaseEmail: String?,
  ageVerified: Boolean,
  isConnecting: Boolean,
  authError: String?,
  onSignInOrRegisterClick: () -> Unit,
  onSyncBackupNow: () -> Unit,
  onSignOutClick: () -> Unit
) {
  GlassStudioCard(
    highlighted = true,
    cornerRadius = 24.dp,
    modifier = Modifier.fillMaxWidth().testTag("profile_auth_card")
  ) {
    Column(
      modifier =
        Modifier.background(
            Brush.linearGradient(
              listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                Color.Transparent
              )
            )
          )
          .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(44.dp)
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                  )
                )
          ) {
            Icon(
              imageVector = if (isSignedIn) Icons.Rounded.CloudDone else Icons.Rounded.CloudUpload,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = if (isSignedIn) "Google Bulut Kasası Aktif" else "Bulut Yedeği & Hesap Koruma",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text =
                if (isSignedIn) "Silip yüklesen bile verilerin geri yüklenir"
                else "Profil, fotoğraf, mesaj ve tercihlerini bulutta koru",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        Surface(
          color =
            if (isSignedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = if (isSignedIn) "Kayıtlı ✓" else "Cihaz Modu",
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isSignedIn) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
          if (isSignedIn) {
            Text(
              text = "${displayName.ifBlank { "Vizyon Dinleyicisi" }} (${handle.ifBlank { "@vizyon" }})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (!firebaseEmail.isNullOrBlank()) {
              Text(
                text = "Bağlı Google Hesabı: $firebaseEmail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            val maskedUid =
              if (!firebaseUid.isNullOrBlank() && firebaseUid.length > 10) {
                "${firebaseUid.take(6)}…${firebaseUid.takeLast(4)}"
              } else {
                firebaseUid.orEmpty()
              }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
              Text(
                text = "Bulut UID: $maskedUid • ${if (ageVerified) "13+ Onaylı" else "Eşitleme Aktif"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
              )
            }
          } else {
            Text("✓ Profil fotoğrafı, biyografi ve tema tercihleri buluta yazılır", style = MaterialTheme.typography.bodySmall)
            Text("✓ Takipçiler, direkt mesajlar ve favori parçalar korunur", style = MaterialTheme.typography.bodySmall)
            Text("✓ İlk kurulumdaki hesabını tek dokunuşla geri yükleyebilirsin", style = MaterialTheme.typography.bodySmall)
          }
        }
      }

      if (isSignedIn) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          StudioGradientCtaButton(
            text = "Buluta Yedekle",
            icon = Icons.Rounded.CloudSync,
            onClick = onSyncBackupNow,
            modifier = Modifier.weight(1f).testTag("sync_cloud_backup_btn")
          )
          StudioGlassButton(
            text = "Güvenli Çıkış",
            icon = Icons.AutoMirrored.Rounded.Logout,
            onClick = onSignOutClick,
            modifier = Modifier.weight(1f).testTag("sign_out_btn")
          )
        }
      } else {
        StudioGradientCtaButton(
          text = if (isConnecting) "Google Bağlanıyor..." else "Google ile Kayıt Ol / Bağlan",
          icon = Icons.Rounded.Lock,
          isLoading = isConnecting,
          enabled = !isConnecting,
          onClick = onSignInOrRegisterClick,
          modifier = Modifier.fillMaxWidth().testTag("profile_google_auth_btn")
        )
      }

      if (!authError.isNullOrBlank()) {
        Text(
          text = authError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error
        )
      }
    }
  }
}
