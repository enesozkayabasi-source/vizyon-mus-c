package com.example.data.local

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccentPalette
import com.example.model.AppAppearanceConfig
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ChatPrivacy
import com.example.model.CornerDensity
import com.example.model.MusicTrack
import com.example.model.RecentTrackSummary
import com.example.model.StoryCardTheme
import com.example.model.TrackSource
import com.example.model.UserProfile
import com.example.model.sanitizeFirestoreId
import com.example.security.PasscodeSecurityHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

private val Context.appearanceDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "vizyon_appearance_prefs")

class AppearancePreferences(context: Context) {
  private val appContext = context.applicationContext
  private val dataStore = appContext.appearanceDataStore

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    val CORNER_DENSITY = stringPreferencesKey("corner_density")
    val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
    val SHARE_LISTENING_ACTIVITY = booleanPreferencesKey("share_listening_activity")
    val COMPACT_CARDS = booleanPreferencesKey("compact_cards")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val AGE_VERIFIED = booleanPreferencesKey("age_verified")
    val FAVORITE_GENRES = stringPreferencesKey("favorite_genres")
    val LOCAL_DISPLAY_NAME = stringPreferencesKey("local_display_name")
    val LOCAL_HANDLE = stringPreferencesKey("local_handle")
    val LOCAL_BIO = stringPreferencesKey("local_bio")
    val LOCAL_AVATAR_PATH = stringPreferencesKey("local_avatar_path")
    val CLOUD_AVATAR_DATA_URL = stringPreferencesKey("cloud_avatar_data_url")
    val DEFAULT_STORY_THEME = stringPreferencesKey("default_story_theme")
    val CHAT_PRIVACY = stringPreferencesKey("chat_privacy")
    val REMEMBER_ME = booleanPreferencesKey("remember_me")
    val LOCK_ON_LAUNCH = booleanPreferencesKey("lock_on_launch")
    val VAULT_PASSCODE_HASH = stringPreferencesKey("vault_passcode_hash")
    val VAULT_PASSCODE_SALT = stringPreferencesKey("vault_passcode_salt")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val REQUIRE_FOLLOW_APPROVAL = booleanPreferencesKey("require_follow_approval")
    val BACKGROUND_NOTIF_SERVICE_ENABLED = booleanPreferencesKey("background_notif_service_enabled")
    val LAST_CATALOG_REFRESH_MS = longPreferencesKey("last_catalog_refresh_ms")
    val RECENT_TRACKS_JSON = stringPreferencesKey("recent_tracks_json")
  }

  val configFlow: Flow<AppAppearanceConfig> =
    dataStore.data.map { prefs ->
      val genres =
        prefs[Keys.FAVORITE_GENRES]
          ?.split("|")
          ?.map { it.trim() }
          ?.filter { it.isNotBlank() }
          .orEmpty()
      AppAppearanceConfig(
        themeMode =
          enumValueOrDefault(prefs[Keys.THEME_MODE], AppThemeMode.NIGHT_AMOLED),
        accentPalette =
          enumValueOrDefault(prefs[Keys.ACCENT_PALETTE], AccentPalette.VIZYON_BLUE),
        cornerDensity =
          enumValueOrDefault(prefs[Keys.CORNER_DENSITY], CornerDensity.BALANCED),
        preferredLanguage =
          enumValueOrDefault(prefs[Keys.PREFERRED_LANGUAGE], AppLanguage.TURKISH),
        shareListeningActivity = prefs[Keys.SHARE_LISTENING_ACTIVITY] ?: true,
        compactCards = prefs[Keys.COMPACT_CARDS] ?: false,
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        ageVerified = prefs[Keys.AGE_VERIFIED] ?: false,
        favoriteGenres = genres,
        localAvatarPath = prefs[Keys.LOCAL_AVATAR_PATH] ?: "",
        cloudAvatarDataUrl = prefs[Keys.CLOUD_AVATAR_DATA_URL] ?: "",
        defaultStoryTheme =
          enumValueOrDefault(prefs[Keys.DEFAULT_STORY_THEME], StoryCardTheme.NEON_WAVE),
        chatPrivacy =
          enumValueOrDefault(prefs[Keys.CHAT_PRIVACY], ChatPrivacy.PUBLIC),
        rememberMe = prefs[Keys.REMEMBER_ME] ?: true,
        lockOnLaunch = prefs[Keys.LOCK_ON_LAUNCH] ?: false,
        hasVaultPasscode = !prefs[Keys.VAULT_PASSCODE_HASH].isNullOrBlank(),
        notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
        requireFollowApproval = prefs[Keys.REQUIRE_FOLLOW_APPROVAL] ?: false,
        backgroundNotifServiceEnabled = prefs[Keys.BACKGROUND_NOTIF_SERVICE_ENABLED] ?: true,
        lastCatalogRefreshMs = prefs[Keys.LAST_CATALOG_REFRESH_MS] ?: 0L
      )
    }

  val recentTracksFlow: Flow<List<RecentTrackSummary>> =
    dataStore.data.map { prefs ->
      parseRecentTracksJson(prefs[Keys.RECENT_TRACKS_JSON].orEmpty())
    }

  val localProfileFlow: Flow<UserProfile> =
    dataStore.data.map { prefs ->
      val genres =
        prefs[Keys.FAVORITE_GENRES]
          ?.split("|")
          ?.map { it.trim() }
          ?.filter { it.isNotBlank() }
          .orEmpty()
      val resolvedPhoto =
        prefs[Keys.CLOUD_AVATAR_DATA_URL]?.takeIf { it.isNotBlank() }
          ?: (prefs[Keys.LOCAL_AVATAR_PATH] ?: "")
      UserProfile(
        userId = "guest",
        displayName = prefs[Keys.LOCAL_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Vizyon Dinleyicisi",
        handle = prefs[Keys.LOCAL_HANDLE]?.takeIf { it.isNotBlank() } ?: "@vizyon",
        bio = prefs[Keys.LOCAL_BIO] ?: "Kişisel müzik keşif profili",
        photoUrl = resolvedPhoto,
        favoriteGenres = genres,
        shareActivity = prefs[Keys.SHARE_LISTENING_ACTIVITY] ?: true,
        requireFollowApproval = prefs[Keys.REQUIRE_FOLLOW_APPROVAL] ?: false,
        chatPrivacy = enumValueOrDefault(prefs[Keys.CHAT_PRIVACY], ChatPrivacy.PUBLIC).name,
        defaultStoryTheme = enumValueOrDefault(prefs[Keys.DEFAULT_STORY_THEME], StoryCardTheme.NEON_WAVE).name,
        securityPasscodeHash = prefs[Keys.VAULT_PASSCODE_HASH] ?: "",
        securityPasscodeSalt = prefs[Keys.VAULT_PASSCODE_SALT] ?: "",
        rememberMe = prefs[Keys.REMEMBER_ME] ?: true,
        lockOnLaunch = prefs[Keys.LOCK_ON_LAUNCH] ?: false,
        preferredThemeMode = enumValueOrDefault(prefs[Keys.THEME_MODE], AppThemeMode.NIGHT_AMOLED).name,
        preferredAccentPalette = enumValueOrDefault(prefs[Keys.ACCENT_PALETTE], AccentPalette.VIZYON_BLUE).name,
        preferredCornerDensity = enumValueOrDefault(prefs[Keys.CORNER_DENSITY], CornerDensity.BALANCED).name,
        preferredLanguage = enumValueOrDefault(prefs[Keys.PREFERRED_LANGUAGE], AppLanguage.TURKISH).name,
        onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
      )
    }

  suspend fun completeOnboarding(
    displayName: String,
    handle: String,
    bio: String,
    favoriteGenres: List<String>,
    ageVerified: Boolean,
    preferredLanguage: AppLanguage,
    themeMode: AppThemeMode,
    shareActivity: Boolean,
    accentPalette: AccentPalette = AccentPalette.VIZYON_BLUE,
    cornerDensity: CornerDensity = CornerDensity.BALANCED,
    defaultStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
    chatPrivacy: ChatPrivacy = ChatPrivacy.PUBLIC,
    passcode: String = "",
    rememberMe: Boolean = true,
    lockOnLaunch: Boolean = false,
    requireFollowApproval: Boolean = false,
    backgroundNotifServiceEnabled: Boolean = true
  ) {
    val cleanHandle = normalizeHandle(handle)
    val digest =
      if (passcode.trim().length >= 4) {
        PasscodeSecurityHelper.createPasscodeDigest(passcode)
      } else null
    dataStore.edit { prefs ->
      prefs[Keys.LOCAL_DISPLAY_NAME] = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }.take(50)
      prefs[Keys.LOCAL_HANDLE] = cleanHandle
      prefs[Keys.LOCAL_BIO] = bio.trim().ifBlank { "Vizyon Music dinleyicisi" }.take(200)
      prefs[Keys.FAVORITE_GENRES] = favoriteGenres.distinct().take(8).joinToString("|")
      prefs[Keys.AGE_VERIFIED] = ageVerified
      prefs[Keys.PREFERRED_LANGUAGE] = preferredLanguage.name
      prefs[Keys.THEME_MODE] = themeMode.name
      prefs[Keys.ACCENT_PALETTE] = accentPalette.name
      prefs[Keys.CORNER_DENSITY] = cornerDensity.name
      prefs[Keys.DEFAULT_STORY_THEME] = defaultStoryTheme.name
      prefs[Keys.CHAT_PRIVACY] = chatPrivacy.name
      prefs[Keys.REQUIRE_FOLLOW_APPROVAL] = requireFollowApproval
      prefs[Keys.BACKGROUND_NOTIF_SERVICE_ENABLED] = backgroundNotifServiceEnabled
      prefs[Keys.REMEMBER_ME] = rememberMe
      prefs[Keys.LOCK_ON_LAUNCH] = lockOnLaunch && (digest != null || !prefs[Keys.VAULT_PASSCODE_HASH].isNullOrBlank())
      if (digest != null) {
        prefs[Keys.VAULT_PASSCODE_HASH] = digest.hashHex
        prefs[Keys.VAULT_PASSCODE_SALT] = digest.saltBase64
      }
      prefs[Keys.SHARE_LISTENING_ACTIVITY] = shareActivity
      prefs[Keys.ONBOARDING_COMPLETED] = true
    }
  }

  suspend fun updateLocalProfile(
    displayName: String,
    handle: String,
    bio: String,
    shareActivity: Boolean
  ) {
    val cleanHandle = normalizeHandle(handle)
    dataStore.edit { prefs ->
      prefs[Keys.LOCAL_DISPLAY_NAME] = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }.take(50)
      prefs[Keys.LOCAL_HANDLE] = cleanHandle
      prefs[Keys.LOCAL_BIO] = bio.trim().take(200)
      prefs[Keys.SHARE_LISTENING_ACTIVITY] = shareActivity
    }
  }

  suspend fun savePickedAvatarUri(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val dir = File(context.applicationContext.filesDir, "avatars")
        if (!dir.exists()) dir.mkdirs()
        dir.listFiles()?.forEach { oldFile -> runCatching { oldFile.delete() } }
        val destFile = File(dir, "profile_avatar_${System.currentTimeMillis()}.jpg")
        context.applicationContext.contentResolver.openInputStream(uri)?.use { input ->
          destFile.outputStream().use { output ->
            input.copyTo(output)
          }
        } ?: return@withContext null
        val path = destFile.absolutePath
        val cloudDataUrl = PasscodeSecurityHelper.compressAvatarToDataUrl(context.applicationContext, uri).orEmpty()
        dataStore.edit { prefs ->
          prefs[Keys.LOCAL_AVATAR_PATH] = path
          if (cloudDataUrl.isNotBlank()) {
            prefs[Keys.CLOUD_AVATAR_DATA_URL] = cloudDataUrl
          }
        }
        cloudDataUrl.ifBlank { path }
      }.getOrNull()
    }

  suspend fun saveCloudAvatarDataUrl(dataUrl: String) {
    dataStore.edit { it[Keys.CLOUD_AVATAR_DATA_URL] = dataUrl }
  }

  suspend fun currentCloudAvatarDataUrl(): String =
    runCatching { dataStore.data.first()[Keys.CLOUD_AVATAR_DATA_URL].orEmpty() }.getOrDefault("")

  suspend fun currentVaultPasscodePair(): Pair<String, String> =
    runCatching {
      val prefs = dataStore.data.first()
      (prefs[Keys.VAULT_PASSCODE_HASH] ?: "") to (prefs[Keys.VAULT_PASSCODE_SALT] ?: "")
    }.getOrDefault("" to "")

  suspend fun verifyLocalPasscode(input: String): Boolean {
    val prefs = dataStore.data.first()
    val hash = prefs[Keys.VAULT_PASSCODE_HASH].orEmpty()
    val salt = prefs[Keys.VAULT_PASSCODE_SALT].orEmpty()
    if (hash.isBlank() || salt.isBlank()) return true
    return PasscodeSecurityHelper.verifyPasscode(input, hash, salt)
  }

  suspend fun updateVaultPasscode(
    newPasscode: String,
    rememberMe: Boolean,
    lockOnLaunch: Boolean
  ): Boolean {
    val trimmed = newPasscode.trim()
    if (trimmed.length < 4) return false
    val digest = PasscodeSecurityHelper.createPasscodeDigest(trimmed)
    dataStore.edit { prefs ->
      prefs[Keys.VAULT_PASSCODE_HASH] = digest.hashHex
      prefs[Keys.VAULT_PASSCODE_SALT] = digest.saltBase64
      prefs[Keys.REMEMBER_ME] = rememberMe
      prefs[Keys.LOCK_ON_LAUNCH] = lockOnLaunch
    }
    return true
  }

  suspend fun updateVaultSettings(rememberMe: Boolean, lockOnLaunch: Boolean) {
    dataStore.edit { prefs ->
      prefs[Keys.REMEMBER_ME] = rememberMe
      val hasHash = !prefs[Keys.VAULT_PASSCODE_HASH].isNullOrBlank()
      prefs[Keys.LOCK_ON_LAUNCH] = lockOnLaunch && hasHash
    }
  }

  suspend fun updateChatPrivacy(privacy: ChatPrivacy) {
    dataStore.edit { it[Keys.CHAT_PRIVACY] = privacy.name }
  }

  suspend fun updateDefaultStoryTheme(theme: StoryCardTheme) {
    dataStore.edit { it[Keys.DEFAULT_STORY_THEME] = theme.name }
  }

  suspend fun updateFavoriteGenres(genres: List<String>) {
    dataStore.edit { it[Keys.FAVORITE_GENRES] = genres.distinct().take(8).joinToString("|") }
  }

  suspend fun updateLocalAvatarPath(path: String) {
    dataStore.edit { it[Keys.LOCAL_AVATAR_PATH] = path }
  }

  suspend fun updateNotificationsEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
  }

  suspend fun updateRequireFollowApproval(enabled: Boolean) {
    dataStore.edit { it[Keys.REQUIRE_FOLLOW_APPROVAL] = enabled }
  }

  suspend fun updateBackgroundNotifServiceEnabled(enabled: Boolean) {
    dataStore.edit { it[Keys.BACKGROUND_NOTIF_SERVICE_ENABLED] = enabled }
  }

  suspend fun updateThemeMode(mode: AppThemeMode) {
    dataStore.edit { it[Keys.THEME_MODE] = mode.name }
  }

  suspend fun updateAccentPalette(palette: AccentPalette) {
    dataStore.edit { it[Keys.ACCENT_PALETTE] = palette.name }
  }

  suspend fun updateCornerDensity(density: CornerDensity) {
    dataStore.edit { it[Keys.CORNER_DENSITY] = density.name }
  }

  suspend fun updatePreferredLanguage(language: AppLanguage) {
    dataStore.edit { it[Keys.PREFERRED_LANGUAGE] = language.name }
  }

  suspend fun updateShareListeningActivity(enabled: Boolean) {
    dataStore.edit { it[Keys.SHARE_LISTENING_ACTIVITY] = enabled }
  }

  suspend fun updateCompactCards(compact: Boolean) {
    dataStore.edit { it[Keys.COMPACT_CARDS] = compact }
  }

  suspend fun updateLastCatalogRefreshMs(timestampMs: Long) {
    dataStore.edit { it[Keys.LAST_CATALOG_REFRESH_MS] = timestampMs }
  }

  suspend fun restoreFromCloudUserProfile(profile: UserProfile) {
    dataStore.edit { prefs ->
      if (profile.displayName.isNotBlank()) prefs[Keys.LOCAL_DISPLAY_NAME] = profile.displayName.take(50)
      if (profile.handle.isNotBlank()) prefs[Keys.LOCAL_HANDLE] = normalizeHandle(profile.handle)
      if (profile.bio.isNotBlank()) prefs[Keys.LOCAL_BIO] = profile.bio.take(200)
      if (profile.photoUrl.isNotBlank()) {
        prefs[Keys.CLOUD_AVATAR_DATA_URL] = profile.photoUrl
      }
      if (profile.favoriteGenres.isNotEmpty()) {
        prefs[Keys.FAVORITE_GENRES] = profile.favoriteGenres.distinct().take(8).joinToString("|")
      }
      prefs[Keys.SHARE_LISTENING_ACTIVITY] = profile.shareActivity
      prefs[Keys.REQUIRE_FOLLOW_APPROVAL] = profile.requireFollowApproval
      prefs[Keys.CHAT_PRIVACY] =
        enumValueOrDefault(profile.chatPrivacy, ChatPrivacy.PUBLIC).name
      prefs[Keys.DEFAULT_STORY_THEME] =
        enumValueOrDefault(profile.defaultStoryTheme, StoryCardTheme.NEON_WAVE).name
      if (profile.securityPasscodeHash.isNotBlank() && profile.securityPasscodeSalt.isNotBlank()) {
        prefs[Keys.VAULT_PASSCODE_HASH] = profile.securityPasscodeHash
        prefs[Keys.VAULT_PASSCODE_SALT] = profile.securityPasscodeSalt
      }
      prefs[Keys.REMEMBER_ME] = profile.rememberMe
      prefs[Keys.LOCK_ON_LAUNCH] = profile.lockOnLaunch
      if (profile.preferredThemeMode.isNotBlank()) {
        prefs[Keys.THEME_MODE] =
          enumValueOrDefault(profile.preferredThemeMode, AppThemeMode.NIGHT_AMOLED).name
      }
      if (profile.preferredAccentPalette.isNotBlank()) {
        prefs[Keys.ACCENT_PALETTE] =
          enumValueOrDefault(profile.preferredAccentPalette, AccentPalette.VIZYON_BLUE).name
      }
      if (profile.preferredCornerDensity.isNotBlank()) {
        prefs[Keys.CORNER_DENSITY] =
          enumValueOrDefault(profile.preferredCornerDensity, CornerDensity.BALANCED).name
      }
      if (profile.preferredLanguage.isNotBlank()) {
        prefs[Keys.PREFERRED_LANGUAGE] =
          enumValueOrDefault(profile.preferredLanguage, AppLanguage.TURKISH).name
      }
      if (profile.recentTracks.isNotEmpty()) {
        prefs[Keys.RECENT_TRACKS_JSON] = encodeRecentTracksJson(profile.recentTracks.take(5))
      }
      prefs[Keys.AGE_VERIFIED] = true
      prefs[Keys.ONBOARDING_COMPLETED] = true
    }
  }

  suspend fun recordRecentTrack(track: MusicTrack): List<RecentTrackSummary> {
    val safeId = sanitizeFirestoreId(track.trackId)
    val entry = RecentTrackSummary.fromMusicTrack(track, System.currentTimeMillis())
    val updated =
      dataStore.edit { prefs ->
        val existing = parseRecentTracksJson(prefs[Keys.RECENT_TRACKS_JSON].orEmpty())
        val merged =
          (listOf(entry) + existing.filterNot { it.trackId == safeId || it.title.equals(track.title, ignoreCase = true) })
            .take(5)
        prefs[Keys.RECENT_TRACKS_JSON] = encodeRecentTracksJson(merged)
      }
    return parseRecentTracksJson(updated[Keys.RECENT_TRACKS_JSON].orEmpty())
  }

  suspend fun currentRecentTracks(): List<RecentTrackSummary> =
    runCatching { recentTracksFlow.first() }.getOrDefault(emptyList())

  suspend fun resetAllForAccountDeletion() {
    runCatching {
      File(appContext.filesDir, "avatars").deleteRecursively()
    }
    dataStore.edit { prefs ->
      prefs.clear()
    }
  }

  private fun parseRecentTracksJson(raw: String): List<RecentTrackSummary> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(raw)
      buildList {
        for (i in 0 until arr.length()) {
          val obj = arr.optJSONObject(i) ?: continue
          val title = obj.optString("title", "")
          if (title.isBlank()) continue
          add(
            RecentTrackSummary(
              trackId = obj.optString("trackId", ""),
              title = title,
              artistId = obj.optString("artistId", ""),
              artistName = obj.optString("artistName", ""),
              artworkUrl = obj.optString("artworkUrl", ""),
              youtubeVideoId = obj.optString("youtubeVideoId", ""),
              source = obj.optString("source", TrackSource.HYBRID.name),
              playedAtMillis = obj.optLong("playedAtMillis", 0L)
            )
          )
        }
      }.take(5)
    }.getOrDefault(emptyList())
  }

  private fun encodeRecentTracksJson(items: List<RecentTrackSummary>): String {
    val arr = JSONArray()
    items.take(5).forEach { item ->
      arr.put(
        JSONObject().apply {
          put("trackId", item.trackId)
          put("title", item.title)
          put("artistId", item.artistId)
          put("artistName", item.artistName)
          put("artworkUrl", item.artworkUrl)
          put("youtubeVideoId", item.youtubeVideoId)
          put("source", item.source)
          put("playedAtMillis", item.playedAtMillis)
        }
      )
    }
    return arr.toString()
  }

  private fun normalizeHandle(raw: String): String {
    val cleaned = raw.trim().replace(Regex("\\s+"), "").replace(Regex("[^a-zA-Z0-9_@]"), "")
    val withAt = if (cleaned.startsWith("@")) cleaned else "@$cleaned"
    return if (withAt.length >= 2) withAt.take(30) else "@vizyon"
  }

  private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == raw } ?: default
}
