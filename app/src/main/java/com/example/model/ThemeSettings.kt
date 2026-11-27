package com.example.model

enum class AppThemeMode(val label: String, val subtitle: String) {
  SYSTEM("Sistem", "Cihaz ayarını takip et"),
  LIGHT("Aydınlık", "Gündüz stüdyo teması"),
  DARK("Karanlık", "Obsidyen stüdyo modu"),
  NIGHT_AMOLED("Gece Modu", "Saf siyah AMOLED tasarruf")
}

enum class AppLanguage(
  val label: String,
  val subtitle: String,
  val itunesCountry: String,
  val badge: String
) {
  TURKISH("Türkçe", "Türkiye müzik kataloğu & Türkçe arayüz", "TR", "TR"),
  ENGLISH("English", "Global & US catalog focus", "US", "EN"),
  BILINGUAL("TR & Global", "Türkçe ve uluslararası karma seçki", "TR", "TR/EN")
}

enum class AccentPalette(
  val label: String,
  val primaryHex: Long,
  val secondaryHex: Long,
  val hexString: String
) {
  VIZYON_BLUE("Vizyon Mavi", 0xFF2979FF, 0xFF38BDF8, "#2979FF"),
  CYAN("Buz Turkuaz", 0xFF00B4D8, 0xFF60A5FA, "#00B4D8"),
  VIOLET("Gece Moru", 0xFF8B5CF6, 0xFF38BDF8, "#8B5CF6"),
  CORAL("Mercan Ateşi", 0xFFFB7185, 0xFFF59E0B, "#FB7185"),
  EMERALD("Stüdyo Yeşil", 0xFF1ED760, 0xFF38BDF8, "#1ED760")
}

enum class CornerDensity(val label: String, val cardCornerDp: Int) {
  SHARP("Keskin", 10),
  BALANCED("Dengeli", 20),
  PILL("Yumuşak", 28)
}

enum class ChatPrivacy(val label: String, val subtitle: String) {
  PUBLIC("Herkes (Açık DM)", "Tüm Vizyon dinleyicileri sana doğrudan mesaj gönderebilir"),
  FOLLOWERS_ONLY("Yalnızca Takip Ettiklerim", "Sadece takip ettiğin kişiler sana mesaj gönderebilir");

  companion object {
    fun fromRaw(raw: String?): ChatPrivacy =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PUBLIC
  }
}

const val DISCOVER_AUTO_REFRESH_INTERVAL_MS: Long = 30L * 60L * 1000L

data class AppAppearanceConfig(
  val themeMode: AppThemeMode = AppThemeMode.NIGHT_AMOLED,
  val accentPalette: AccentPalette = AccentPalette.VIZYON_BLUE,
  val cornerDensity: CornerDensity = CornerDensity.BALANCED,
  val preferredLanguage: AppLanguage = AppLanguage.TURKISH,
  val defaultStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
  val chatPrivacy: ChatPrivacy = ChatPrivacy.PUBLIC,
  val shareListeningActivity: Boolean = true,
  val compactCards: Boolean = false,
  val onboardingCompleted: Boolean = false,
  val ageVerified: Boolean = false,
  val favoriteGenres: List<String> = emptyList(),
  val localAvatarPath: String = "",
  val cloudAvatarDataUrl: String = "",
  val notificationsEnabled: Boolean = true,
  val lastCatalogRefreshMs: Long = 0L,
  val rememberMe: Boolean = true,
  val lockOnLaunch: Boolean = false,
  val hasVaultPasscode: Boolean = false,
  val requireFollowApproval: Boolean = false,
  val backgroundNotifServiceEnabled: Boolean = true,
  val notifPermissionPrompted: Boolean = false
)
