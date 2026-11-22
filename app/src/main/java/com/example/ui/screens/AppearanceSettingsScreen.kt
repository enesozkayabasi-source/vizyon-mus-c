package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.model.AccentPalette
import com.example.model.ApiBridgeStatus
import com.example.model.AppAppearanceConfig
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.MusicTrack
import com.example.model.RecentTrackSummary
import com.example.model.StoryCardTheme
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.auth.clearCredentialStateSafely
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.auth.reauthenticateWithGoogle
import com.example.ui.auth.signOutUser
import com.example.ui.components.DeleteAccountSecurityWallDialog
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.ReauthenticateAccountDialog
import com.example.ui.components.StudioAvatar
import com.example.ui.components.StudioCircleIconButton
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioGradientCtaButton
import kotlinx.coroutines.launch

enum class ProfileSubPage {
  OVERVIEW,
  EDIT_PROFILE,
  ABOUT_PRIVACY,
  SETTINGS
}

@Composable
fun AppearanceSettingsScreen(
  isSignedIn: Boolean,
  firebaseUid: String?,
  firebaseEmail: String?,
  userPhotoUrl: String,
  unreadNotificationCount: Int,
  appearance: AppAppearanceConfig,
  userProfileState: UiState<UserProfile?>,
  recentTracks: List<RecentTrackSummary> = emptyList(),
  bridgeStatus: ApiBridgeStatus?,
  isDeletingAccount: Boolean,
  followersCount: Int = 0,
  followingCount: Int = 0,
  favoritesCount: Int = 0,
  onThemeModeChange: (AppThemeMode) -> Unit,
  onAccentChange: (AccentPalette) -> Unit,
  onCornerDensityChange: (CornerDensity) -> Unit,
  onLanguageChange: (AppLanguage) -> Unit,
  onShareActivityToggle: (Boolean) -> Unit,
  onNotificationsToggle: (Boolean) -> Unit,
  onPickProfilePhoto: (Uri) -> Unit,
  onOpenNotifications: () -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit = { _, _ -> },
  onShareTrackAsStory: (MusicTrack) -> Unit = {},
  onSaveProfile: (String, String, String, Boolean) -> Unit,
  onSaveVaultPasscode: (String, Boolean, Boolean) -> Unit = { _, _, _ -> },
  onUpdateVaultSettings: (Boolean, Boolean) -> Unit = { _, _ -> },
  onChatPrivacyChange: (ChatPrivacy) -> Unit = {},
  onDefaultStoryThemeChange: (StoryCardTheme) -> Unit = {},
  onUpdateFavoriteGenres: (List<String>) -> Unit = {},
  onOpenFollowLists: () -> Unit = {},
  onSyncCloudBackupNow: () -> Unit = {},
  onDeleteAccount: (
    bypassFreshnessPreCheck: Boolean,
    onReauthRequired: () -> Unit,
    onCompleted: () -> Unit
  ) -> Unit,
  onNotifyMessage: (String) -> Unit,
  onSignedOut: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { runCatching { CredentialManager.create(context) }.getOrNull() }

  val profile = (userProfileState as? UiState.Success)?.data
  val displayName = profile?.displayName?.ifBlank { "Vizyon Dinleyicisi" } ?: "Vizyon Dinleyicisi"
  val handle = profile?.handle?.ifBlank { "@vizyon" } ?: "@vizyon"
  val bio = profile?.bio.orEmpty()
  val shareActivity = profile?.shareActivity ?: appearance.shareListeningActivity

  var activeSubPage by remember { mutableStateOf(ProfileSubPage.OVERVIEW) }
  var authConnecting by remember { mutableStateOf(false) }
  var authError by remember { mutableStateOf<String?>(null) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showReauthDialog by remember { mutableStateOf(false) }
  var isReauthenticating by remember { mutableStateOf(false) }
  var reauthError by remember { mutableStateOf<String?>(null) }

  val subPageEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = activeSubPage,
      transitionSpec = {
        val enteringSubpage = targetState != ProfileSubPage.OVERVIEW
        val slideSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 160, easing = subPageEase)
        val fadeInSpec = tween<Float>(durationMillis = 160, easing = subPageEase)
        val fadeOutSpec = tween<Float>(durationMillis = 105, easing = subPageEase)
        val enterOffset: (Int) -> Int = { w -> if (enteringSubpage) w / 14 else -w / 14 }
        (slideInHorizontally(animationSpec = slideSpec, initialOffsetX = enterOffset) + fadeIn(fadeInSpec))
          .togetherWith(fadeOut(fadeOutSpec))
      },
      label = "profile_subpage_transition"
    ) { subPage ->
      when (subPage) {
        ProfileSubPage.OVERVIEW -> {
          InstagramStyleProfileOverview(
            isSignedIn = isSignedIn,
            userPhotoUrl = userPhotoUrl,
            displayName = displayName,
            handle = handle,
            bio = bio,
            favoritesCount = favoritesCount,
            followersCount = followersCount,
            followingCount = followingCount,
            unreadNotificationCount = unreadNotificationCount,
            appearance = appearance,
            recentTracks = recentTracks,
            onOpenNotifications = onOpenNotifications,
            onOpenEditProfile = { activeSubPage = ProfileSubPage.EDIT_PROFILE },
            onOpenAboutPrivacy = { activeSubPage = ProfileSubPage.ABOUT_PRIVACY },
            onOpenSettings = { activeSubPage = ProfileSubPage.SETTINGS },
            onOpenFollowLists = onOpenFollowLists,
            onPlayTrack = onPlayTrack,
            onShareTrackAsStory = onShareTrackAsStory
          )
        }
        ProfileSubPage.EDIT_PROFILE -> {
          ProfileEditSubScreen(
            userPhotoUrl = userPhotoUrl,
            initialDisplayName = profile?.displayName.orEmpty(),
            initialHandle = profile?.handle.orEmpty(),
            initialBio = profile?.bio.orEmpty(),
            initialShareActivity = shareActivity,
            onBack = { activeSubPage = ProfileSubPage.OVERVIEW },
            onPickProfilePhoto = onPickProfilePhoto,
            onShareActivityToggle = onShareActivityToggle,
            onSaveProfile = onSaveProfile
          )
        }
        ProfileSubPage.ABOUT_PRIVACY -> {
          ProfileAboutPrivacySubScreen(
            appearance = appearance,
            unreadNotificationCount = unreadNotificationCount,
            bridgeStatus = bridgeStatus,
            onBack = { activeSubPage = ProfileSubPage.OVERVIEW },
            onChatPrivacyChange = onChatPrivacyChange,
            onNotificationsToggle = onNotificationsToggle,
            onOpenNotifications = onOpenNotifications,
            onNotifyMessage = onNotifyMessage
          )
        }
        ProfileSubPage.SETTINGS -> {
          ProfileSettingsSubScreen(
            isSignedIn = isSignedIn,
            displayName = displayName,
            handle = handle,
            firebaseUid = firebaseUid,
            firebaseEmail = firebaseEmail,
            appearance = appearance,
            isConnectingAuth = authConnecting,
            authError = authError,
            isDeletingAccount = isDeletingAccount,
            followersCount = followersCount,
            followingCount = followingCount,
            onBack = { activeSubPage = ProfileSubPage.OVERVIEW },
            onGoogleAuthClick = {
              val cm = credentialManager
              if (cm == null) {
                onNotifyMessage("Kimlik yöneticisi bu cihazda başlatılamadı.")
                return@ProfileSettingsSubScreen
              }
              authConnecting = true
              authError = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = cm,
                onAuthSuccess = {
                  authConnecting = false
                  authError = null
                  onNotifyMessage("Google bulut kaydı tamamlandı ve profil eşitlendi!")
                },
                onAuthError = { msg ->
                  authConnecting = false
                  authError = msg
                  onNotifyMessage(msg)
                },
                scope = scope,
                onAuthCancelled = { authConnecting = false }
              )
            },
            onSignOutClick = {
              val cm = credentialManager
              if (cm != null) {
                signOutUser(cm, scope) {
                  onNotifyMessage("Güvenli çıkış yapıldı.")
                  onSignedOut()
                }
              } else {
                onSignedOut()
              }
            },
            onSyncCloudBackupNow = onSyncCloudBackupNow,
            onThemeModeChange = onThemeModeChange,
            onAccentChange = onAccentChange,
            onCornerDensityChange = onCornerDensityChange,
            onLanguageChange = onLanguageChange,
            onDefaultStoryThemeChange = onDefaultStoryThemeChange,
            onUpdateFavoriteGenres = onUpdateFavoriteGenres,
            onSaveVaultPasscode = onSaveVaultPasscode,
            onUpdateVaultSettings = onUpdateVaultSettings,
            onOpenFollowLists = onOpenFollowLists,
            onOpenDeleteWall = { showDeleteDialog = true }
          )
        }
      }
    }

    if (showDeleteDialog) {
      DeleteAccountSecurityWallDialog(
        isSignedIn = isSignedIn,
        displayName = displayName,
        handle = handle,
        firebaseEmail = firebaseEmail,
        isDeletingAccount = isDeletingAccount,
        onDismiss = { showDeleteDialog = false },
        onConfirmDelete = {
          showDeleteDialog = false
          onDeleteAccount(
            false,
            {
              reauthError = null
              showReauthDialog = true
            },
            {
              credentialManager?.let { cm ->
                scope.launch { clearCredentialStateSafely(cm) }
              }
            }
          )
        }
      )
    }

    if (showReauthDialog) {
      ReauthenticateAccountDialog(
        firebaseEmail = firebaseEmail,
        isReauthenticating = isReauthenticating,
        reauthError = reauthError,
        onDismiss = { showReauthDialog = false },
        onConfirmReauth = {
          val cm = credentialManager
          if (cm == null) {
            reauthError = "Kimlik yöneticisi başlatılamadı."
            return@ReauthenticateAccountDialog
          }
          isReauthenticating = true
          reauthError = null
          reauthenticateWithGoogle(
            context = context,
            credentialManager = cm,
            onReauthSuccess = {
              isReauthenticating = false
              showReauthDialog = false
              onDeleteAccount(
                true,
                { onNotifyMessage("Yeniden doğrulama başarısız oldu, lütfen tekrar deneyin.") },
                { scope.launch { clearCredentialStateSafely(cm) } }
              )
            },
            onReauthError = { msg ->
              isReauthenticating = false
              reauthError = msg
            },
            scope = scope,
            onReauthCancelled = { isReauthenticating = false }
          )
        }
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstagramStyleProfileOverview(
  isSignedIn: Boolean,
  userPhotoUrl: String,
  displayName: String,
  handle: String,
  bio: String,
  favoritesCount: Int,
  followersCount: Int,
  followingCount: Int,
  unreadNotificationCount: Int,
  appearance: AppAppearanceConfig,
  recentTracks: List<RecentTrackSummary>,
  onOpenNotifications: () -> Unit,
  onOpenEditProfile: () -> Unit,
  onOpenAboutPrivacy: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenFollowLists: () -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
  onShareTrackAsStory: (MusicTrack) -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("studio_settings_screen")
  ) {
    // 1. Clean Instagram-style Top Bar (@handle + Cloud Pill + Bell + Gear)
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = handle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Surface(
            color =
              if (isSignedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(999.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              if (isSignedIn) {
                Icon(
                  Icons.Rounded.VerifiedUser,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(13.dp)
                )
              }
              Text(
                text = if (isSignedIn) "Bulut" else "Cihaz",
                style = MaterialTheme.typography.labelSmall,
                color = if (isSignedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          StudioCircleIconButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "Bildirimler",
            badgeCount = unreadNotificationCount,
            highlighted = unreadNotificationCount > 0,
            size = 48.dp,
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("profile_notifications_bell_btn")
          )
          StudioCircleIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Profil Ayarları",
            size = 48.dp,
            onClick = onOpenSettings,
            modifier = Modifier.testTag("profile_top_settings_icon_btn")
          )
        }
      }
    }

    // 2. Instagram-style Hero Card (Avatar + 3 Stat Columns + Name/Bio + Genre Chips + Action Buttons)
    item {
      GlassStudioCard(
        highlighted = true,
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth().testTag("profile_overview_hero_card")
      ) {
        Column(
          modifier =
            Modifier.background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    Color.Transparent
                  )
                )
              )
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            StudioAvatar(
              photoUrlOrPath = userPhotoUrl,
              contentDescription = displayName,
              size = 84.dp
            )
            Row(
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              ProfileStatColumn(
                count = favoritesCount.toString(),
                label = "Favori",
                onClick = null
              )
              ProfileStatColumn(
                count = followersCount.toString(),
                label = "Takipçi",
                onClick = onOpenFollowLists
              )
              ProfileStatColumn(
                count = followingCount.toString(),
                label = "Takip",
                onClick = onOpenFollowLists
              )
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = displayName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = bio.ifBlank { "Müzik biyografin henüz boş — Profili Düzenle ile ekleyebilirsin." },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (appearance.favoriteGenres.isNotEmpty()) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              appearance.favoriteGenres.take(4).forEach { genre ->
                Surface(
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  shape = RoundedCornerShape(999.dp),
                  modifier = Modifier.border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                ) {
                  Text(
                    text = genre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
              if (appearance.favoriteGenres.size > 4) {
                Surface(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                  shape = RoundedCornerShape(999.dp)
                ) {
                  Text(
                    text = "+${appearance.favoriteGenres.size - 4}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }

          // Primary Instagram-style Action Buttons Row 1: Profili Düzenle + Profil Ayarları
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StudioGradientCtaButton(
              text = "Profili Düzenle",
              icon = Icons.Rounded.Edit,
              onClick = onOpenEditProfile,
              modifier = Modifier.weight(1f).testTag("open_edit_profile_btn")
            )
            StudioGlassButton(
              text = "Profil Ayarları",
              icon = Icons.Rounded.Tune,
              onClick = onOpenSettings,
              modifier = Modifier.weight(1f).testTag("open_profile_settings_btn")
            )
          }

          // Action Row 2: Hakkında & Gizlilik + Takipçiler/Takip
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StudioGlassButton(
              text = "Hakkında & Gizlilik",
              icon = Icons.Rounded.PrivacyTip,
              onClick = onOpenAboutPrivacy,
              modifier = Modifier.weight(1f).testTag("open_about_privacy_btn")
            )
            StudioGlassButton(
              text = "Bağlantılar",
              icon = Icons.Rounded.People,
              onClick = onOpenFollowLists,
              modifier = Modifier.weight(1f).testTag("profile_open_follow_lists_btn")
            )
          }
        }
      }
    }

    // 3. Last 5 Listened Tracks Card (cleanly showcased directly on the profile!)
    item {
      ProfileRecentTracksCard(
        recentTracks = recentTracks,
        onPlayTrack = onPlayTrack,
        onShareAsStory = onShareTrackAsStory
      )
    }
  }
}

@Composable
private fun ProfileStatColumn(
  count: String,
  label: String,
  onClick: (() -> Unit)?
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
        .clip(RoundedCornerShape(12.dp))
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 6.dp, vertical = 4.dp)
  ) {
    Text(
      text = count,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1
    )
  }
}
