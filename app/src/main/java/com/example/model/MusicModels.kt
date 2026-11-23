package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TrackSource(val label: String) {
  YOUTUBE("Resmi Video"),
  ITUNES("iTunes Katalog"),
  HYBRID("Vizyon Katalog");

  companion object {
    fun fromRaw(raw: String?): TrackSource =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ITUNES
  }
}

enum class MusicCategoryGroup(val label: String) {
  TRENDING("Öne Çıkan"),
  TURKISH("Türk Müziği"),
  GLOBAL("Global Türler")
}

enum class MusicCategory(
  val label: String,
  val group: MusicCategoryGroup,
  val catalogQueryTr: String,
  val catalogQueryGlobal: String = catalogQueryTr
) {
  TRENDING("Trendler", MusicCategoryGroup.TRENDING, "türkçe hit şarkılar 2025", "top pop hits 2025"),
  TURKISH_POP("Türkçe Pop", MusicCategoryGroup.TURKISH, "türkçe pop tarkan sezen aksu mabel matiz", "turkish pop hits"),
  TURKISH_RAP("Türkçe Rap & Trap", MusicCategoryGroup.TURKISH, "türkçe rap ezhel ceza sagopa semİcenk", "turkish rap trap"),
  ANATOLIAN_ROCK("Anadolu Rock", MusicCategoryGroup.TURKISH, "anadolu rock barış manço cem karaca duman mor ve ötesi", "anatolian rock turkish"),
  ARABESK_FANTASY("Arabesk & Fantezi", MusicCategoryGroup.TURKISH, "arabesk müslüm gürses ibrahim tatlıses orhan gencebay", "turkish arabesk music"),
  TURKISH_FOLK("Türk Halk Müziği", MusicCategoryGroup.TURKISH, "türkü neşet ertaş selda bağcan musa eroğlu", "turkish folk bağlama"),
  TURKISH_CLASSICAL("Türk Sanat Müziği", MusicCategoryGroup.TURKISH, "türk sanat müziği zeki müren müzeyyen senar", "turkish classical art music"),
  TURKISH_ALTERNATIVE("Türkçe Alternatif", MusicCategoryGroup.TURKISH, "türkçe alternatif madrigal yüzyüzeyken konuşuruz dolu kadehi ters tut", "turkish indie alternative"),
  POP("Global Pop", MusicCategoryGroup.GLOBAL, "global pop hits billboard", "global billboard pop hits"),
  ELECTRONIC("Elektronik & Dans", MusicCategoryGroup.GLOBAL, "electronic synthwave house dance", "electronic synthwave house"),
  HIPHOP("Global Hip-Hop", MusicCategoryGroup.GLOBAL, "hip hop rap global hits", "hip hop rap hits"),
  ACOUSTIC("Akustik & Chill", MusicCategoryGroup.GLOBAL, "acoustic indie chill unplugged", "acoustic indie chill"),
  ROCK("Rock & Alternatif", MusicCategoryGroup.GLOBAL, "alternative rock classic rock", "alternative indie rock"),
  JAZZ_LOFI("Caz & Lo-Fi", MusicCategoryGroup.GLOBAL, "jazz lofi chill beats", "jazz lofi chill beats"),
  RNB_SOUL("R&B & Soul", MusicCategoryGroup.GLOBAL, "r&b soul modern neo soul", "r&b soul hits")
}

enum class PlaylistCoverStyle(val label: String) {
  CHROME("Sıvı Krom"),
  STAGE("Sahne Işığı"),
  STUDIO("Analog Stüdyo"),
  EMERALD("Zümrüt Dalga");

  companion object {
    fun fromRaw(raw: String?): PlaylistCoverStyle =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CHROME
  }
}

enum class ActivityActionType(val label: String) {
  LISTENING("Profil Dinlemesi"),
  SHARED_TRACK("Parça paylaştı"),
  CREATED_PLAYLIST("Çalma listesi oluşturdu"),
  FOLLOWED_ARTIST("Sanatçıyı takip etti"),
  REPOSTED_TRACK("Yeniden paylaştı");

  companion object {
    fun fromRaw(raw: String?): ActivityActionType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SHARED_TRACK
  }
}

enum class FanPostTag(val label: String) {
  APPLAUSE("Alkış"),
  REQUEST("İstek Parça"),
  CONCERT("Konser"),
  MIX("Sahne / Mix");

  companion object {
    fun fromRaw(raw: String?): FanPostTag =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: APPLAUSE
  }
}

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class MusicTrack(
  val trackId: String,
  val title: String,
  val artistId: String,
  val artistName: String,
  val albumName: String = "",
  val artworkUrl: String = "",
  val youtubeVideoId: String = "",
  val durationMs: Long = 0L,
  val source: TrackSource = TrackSource.ITUNES,
  val viewCountLabel: String = "",
  val categoryTag: String = MusicCategory.TRENDING.label
)

data class ArtistSummary(
  val artistId: String,
  val name: String,
  val imageUrl: String = "",
  val followersCount: Long = 0L,
  val genres: List<String> = emptyList(),
  val source: TrackSource = TrackSource.ITUNES,
  val bio: String = ""
)

data class ApiBridgeStatus(
  val itunesConnected: Boolean,
  val youtubeApiConfigured: Boolean,
  val youtubeConnected: Boolean,
  val lyricsReady: Boolean,
  val activeMessage: String
)

data class LyricLine(
  val timeMs: Long,
  val text: String
)

data class TrackLyrics(
  val trackId: String,
  val trackName: String,
  val artistName: String,
  val syncedLines: List<LyricLine> = emptyList(),
  val plainLines: List<String> = emptyList(),
  val isInstrumental: Boolean = false
) {
  val isSynced: Boolean get() = syncedLines.isNotEmpty()
  val hasAnyLyrics: Boolean get() = syncedLines.isNotEmpty() || plainLines.isNotEmpty()
}

@Keep
@IgnoreExtraProperties
data class RecentTrackSummary(
  val trackId: String = "",
  val title: String = "",
  val artistId: String = "",
  val artistName: String = "",
  val artworkUrl: String = "",
  val youtubeVideoId: String = "",
  val source: String = TrackSource.HYBRID.name,
  val playedAtMillis: Long = 0L
) {
  fun toMusicTrack(): MusicTrack =
    MusicTrack(
      trackId = trackId.ifBlank { sanitizeFirestoreId("rec_${title}_${artistName}") },
      title = title.ifBlank { "Parça" },
      artistId = artistId.ifBlank { sanitizeFirestoreId("art_$artistName") },
      artistName = artistName.ifBlank { "Sanatçı" },
      artworkUrl = artworkUrl,
      youtubeVideoId = youtubeVideoId,
      durationMs = 180_000L,
      source = if (youtubeVideoId.isNotBlank()) TrackSource.YOUTUBE else TrackSource.fromRaw(source)
    )

  fun toFirestoreMap(): Map<String, Any> =
    mapOf(
      "trackId" to sanitizeFirestoreId(trackId).take(128),
      "title" to title.ifBlank { "Parça" }.take(160),
      "artistId" to sanitizeFirestoreId(artistId).take(128),
      "artistName" to artistName.ifBlank { "Sanatçı" }.take(120),
      "artworkUrl" to artworkUrl.take(500),
      "youtubeVideoId" to youtubeVideoId.take(64),
      "source" to TrackSource.fromRaw(source).name,
      "playedAtMillis" to playedAtMillis.coerceAtLeast(0L)
    )

  companion object {
    fun fromMusicTrack(track: MusicTrack, playedAtMillis: Long = System.currentTimeMillis()): RecentTrackSummary =
      RecentTrackSummary(
        trackId = sanitizeFirestoreId(track.trackId).take(128),
        title = track.title.ifBlank { "Parça" }.take(160),
        artistId = sanitizeFirestoreId(track.artistId).take(128),
        artistName = track.artistName.ifBlank { "Sanatçı" }.take(120),
        artworkUrl = track.artworkUrl.take(500),
        youtubeVideoId = track.youtubeVideoId.take(64),
        source = track.source.name,
        playedAtMillis = playedAtMillis
      )
  }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val bio: String = "",
  val photoUrl: String = "",
  val favoriteGenres: List<String> = emptyList(),
  val shareActivity: Boolean = true,
  val requireFollowApproval: Boolean = false,
  val chatPrivacy: String = ChatPrivacy.PUBLIC.name,
  val defaultStoryTheme: String = StoryCardTheme.NEON_WAVE.name,
  val securityPasscodeHash: String = "",
  val securityPasscodeSalt: String = "",
  val rememberMe: Boolean = true,
  val lockOnLaunch: Boolean = false,
  val preferredThemeMode: String = "",
  val preferredAccentPalette: String = "",
  val preferredCornerDensity: String = "",
  val preferredLanguage: String = "",
  val onboardingCompleted: Boolean = false,
  val recentTracks: List<RecentTrackSummary> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val bio: String = "",
  val photoUrl: String = "",
  val discoverable: Boolean = true,
  val shareActivity: Boolean = true,
  val requireFollowApproval: Boolean = false,
  val chatPrivacy: String = ChatPrivacy.PUBLIC.name,
  val defaultStoryTheme: String = StoryCardTheme.NEON_WAVE.name,
  val currentTrackTitle: String = "",
  val currentArtistName: String = "",
  val lastTrackId: String = "",
  val lastTrackTitle: String = "",
  val lastArtistId: String = "",
  val lastArtistName: String = "",
  val lastTrackArtworkUrl: String = "",
  val lastTrackYoutubeId: String = "",
  val lastPlayedAtMillis: Long = 0L,
  val recentTracks: List<RecentTrackSummary> = emptyList(),
  val followerCount: Long = 0L,
  val followingCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toLastListenedTrack(): MusicTrack? {
    if (!shareActivity) return null
    val resolvedTitle = lastTrackTitle.ifBlank { currentTrackTitle }.trim()
    if (resolvedTitle.isBlank()) return recentTracks.firstOrNull()?.toMusicTrack()
    val resolvedArtist = lastArtistName.ifBlank { currentArtistName }.ifBlank { "Sanatçı" }
    val resolvedTrackId =
      lastTrackId.ifBlank {
        sanitizeFirestoreId("pub_${userId}_${resolvedTitle}_${resolvedArtist}")
      }
    val resolvedArtistId =
      lastArtistId.ifBlank {
        sanitizeFirestoreId("art_$resolvedArtist")
      }
    return MusicTrack(
      trackId = resolvedTrackId,
      title = resolvedTitle,
      artistId = resolvedArtistId,
      artistName = resolvedArtist,
      artworkUrl = lastTrackArtworkUrl,
      youtubeVideoId = lastTrackYoutubeId,
      durationMs = 180_000L,
      source = if (lastTrackYoutubeId.isNotBlank()) TrackSource.YOUTUBE else TrackSource.HYBRID
    )
  }

  fun resolvedRecentTracks(): List<RecentTrackSummary> {
    if (!shareActivity) return emptyList()
    if (recentTracks.isNotEmpty()) {
      return recentTracks
        .filter { it.title.isNotBlank() }
        .distinctBy { it.trackId.ifBlank { it.title } }
        .take(5)
    }
    val fallback = toLastListenedTrack() ?: return emptyList()
    return listOf(RecentTrackSummary.fromMusicTrack(fallback, lastPlayedAtMillis))
  }
}

@Keep
@IgnoreExtraProperties
data class FollowConnection(
  @DocumentId val id: String = "",
  val targetUserId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val photoUrl: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectChatThread(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val participantIds: List<String> = emptyList(),
  val participantNames: Map<String, String> = emptyMap(),
  val participantHandles: Map<String, String> = emptyMap(),
  val participantAvatars: Map<String, String> = emptyMap(),
  val lastMessageText: String = "",
  val lastTrackTitle: String = "",
  val lastSenderId: String = "",
  val lastMessageAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun peerIdFor(myUid: String): String =
    participantIds.firstOrNull { it != myUid } ?: participantIds.firstOrNull().orEmpty()

  fun peerName(myUid: String): String =
    participantNames[peerIdFor(myUid)]?.ifBlank { "Dinleyici" } ?: "Dinleyici"

  fun peerHandle(myUid: String): String =
    participantHandles[peerIdFor(myUid)]?.ifBlank { "@vizyon" } ?: "@vizyon"

  fun peerAvatar(myUid: String): String =
    participantAvatars[peerIdFor(myUid)].orEmpty()
}

enum class DirectMessageType {
  TEXT,
  TRACK,
  VOICE;

  companion object {
    fun fromRaw(raw: String?): DirectMessageType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TEXT
  }
}

@Keep
@IgnoreExtraProperties
data class DirectChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val senderHandle: String = "",
  val senderAvatarUrl: String = "",
  val participantIds: List<String> = emptyList(),
  val text: String = "",
  val messageType: String = DirectMessageType.TEXT.name,
  val audioBase64: String = "",
  val audioDurationMs: Long = 0L,
  val trackId: String = "",
  val trackTitle: String = "",
  val artistName: String = "",
  val albumArtUrl: String = "",
  val youtubeVideoId: String = "",
  val source: String = TrackSource.HYBRID.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val isVoiceMessage: Boolean
    get() = DirectMessageType.fromRaw(messageType) == DirectMessageType.VOICE && audioBase64.isNotBlank()

  fun toAttachedTrack(): MusicTrack? {
    if (trackTitle.isBlank()) return null
    return MusicTrack(
      trackId = trackId.ifBlank { sanitizeFirestoreId("chat_${trackTitle}_${artistName}") },
      title = trackTitle,
      artistId = sanitizeFirestoreId("art_${artistName.ifBlank { "sanatci" }}"),
      artistName = artistName.ifBlank { "Sanatçı" },
      artworkUrl = albumArtUrl,
      youtubeVideoId = youtubeVideoId,
      durationMs = 180_000L,
      source = if (youtubeVideoId.isNotBlank()) TrackSource.YOUTUBE else TrackSource.fromRaw(source)
    )
  }
}

@Keep
@IgnoreExtraProperties
data class SavedTrack(
  @DocumentId val id: String = "",
  val trackId: String = "",
  val title: String = "",
  val artistId: String = "",
  val artistName: String = "",
  val albumName: String = "",
  val artworkUrl: String = "",
  val youtubeVideoId: String = "",
  val durationMs: Long = 0L,
  val source: String = TrackSource.ITUNES.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMusicTrack(): MusicTrack =
    MusicTrack(
      trackId = trackId.ifBlank { id },
      title = title,
      artistId = artistId,
      artistName = artistName,
      albumName = albumName,
      artworkUrl = artworkUrl,
      youtubeVideoId = youtubeVideoId,
      durationMs = durationMs,
      source = TrackSource.fromRaw(source)
    )
}

@Keep
@IgnoreExtraProperties
data class Playlist(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val description: String = "",
  val accentColorHex: String = "#2979FF",
  val coverStyle: String = PlaylistCoverStyle.CHROME.name,
  val sharedPublic: Boolean = true,
  val trackCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowedArtist(
  @DocumentId val id: String = "",
  val artistId: String = "",
  val name: String = "",
  val imageUrl: String = "",
  val genres: List<String> = emptyList(),
  val source: String = TrackSource.ITUNES.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityFeedItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val userHandle: String = "",
  val userAvatarUrl: String = "",
  val actionType: String = ActivityActionType.SHARED_TRACK.name,
  val trackId: String = "",
  val trackTitle: String = "",
  val artistId: String = "",
  val artistName: String = "",
  val albumArtUrl: String = "",
  val youtubeVideoId: String = "",
  val source: String = TrackSource.ITUNES.name,
  val note: String = "",
  val visibility: String = "PUBLIC",
  val likesCount: Long = 0L,
  val commentsCount: Long = 0L,
  val repostsCount: Long = 0L,
  val repostedFromName: String = "",
  val repostedFromHandle: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ActivityComment(
  @DocumentId val id: String = "",
  val activityId: String = "",
  val userId: String = "",
  val userName: String = "",
  val userHandle: String = "",
  val userAvatarUrl: String = "",
  val text: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ArtistFanPost(
  @DocumentId val id: String = "",
  val artistId: String = "",
  val userId: String = "",
  val authorName: String = "",
  val authorHandle: String = "",
  val message: String = "",
  val tag: String = FanPostTag.APPLAUSE.name,
  val cheersCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class StoryCardTheme(
  val label: String,
  val startColorHex: Long,
  val endColorHex: Long,
  val accentColorHex: Long
) {
  NEON_WAVE("Neon Dalga", 0xFF0A1432, 0xFF1D4ED8, 0xFF38BDF8),
  AMOLED_GOLD("AMOLED Altın", 0xFF030508, 0xFF1A1407, 0xFFF59E0B),
  GLASS_STUDIO("Stüdyo Camı", 0xFF0D172B, 0xFF1E293B, 0xFF60A5FA),
  SUNSET_VINYL("Gün Batımı Plak", 0xFF280B1C, 0xFF7C2D12, 0xFFFB7185),
  MIDNIGHT_AURORA("Gece Aurora", 0xFF061B1A, 0xFF0F3D3E, 0xFF1ED760);

  companion object {
    fun fromRaw(raw: String?): StoryCardTheme =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NEON_WAVE
  }
}

@Keep
@IgnoreExtraProperties
data class MusicStoryItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val userName: String = "",
  val userHandle: String = "",
  val userAvatarUrl: String = "",
  val trackId: String = "",
  val trackTitle: String = "",
  val artistId: String = "",
  val artistName: String = "",
  val albumArtUrl: String = "",
  val youtubeVideoId: String = "",
  val source: String = TrackSource.HYBRID.name,
  val note: String = "",
  val cardTheme: String = StoryCardTheme.NEON_WAVE.name,
  val visibility: String = "PUBLIC",
  val expiresAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun toMusicTrack(): MusicTrack =
    MusicTrack(
      trackId = trackId.ifBlank { sanitizeFirestoreId("story_${trackTitle}_${artistName}") },
      title = trackTitle.ifBlank { "Parça" },
      artistId = artistId.ifBlank { sanitizeFirestoreId("art_$artistName") },
      artistName = artistName.ifBlank { "Sanatçı" },
      artworkUrl = albumArtUrl,
      youtubeVideoId = youtubeVideoId,
      durationMs = 180_000L,
      source = TrackSource.fromRaw(source)
    )
}

enum class NotificationType(val label: String) {
  FOLLOW("Yeni Takipçi"),
  LIKE("Parça Beğenisi"),
  COMMENT("Yeni Yorum"),
  REPOST("Yeniden Paylaşım"),
  STORY("Yeni Hikaye"),
  MESSAGE("Yeni Mesaj"),
  FOLLOW_REQUEST("Takip İsteği"),
  MESSAGE_REQUEST("Mesaj İsteği"),
  VOICE_MESSAGE("Sesli Mesaj");

  companion object {
    fun fromRaw(raw: String?): NotificationType =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: FOLLOW
  }
}

enum class RequestStatus {
  PENDING,
  ACCEPTED,
  REJECTED;

  companion object {
    fun fromRaw(raw: String?): RequestStatus =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: PENDING
  }
}

enum class ReportReason(val label: String) {
  SPAM("Spam veya Reklam"),
  HARASSMENT("Taciz veya Zorbalık"),
  INAPPROPRIATE("Uygunsuz İçerik"),
  IMPERSONATION("Taklit veya Sahte Hesap"),
  OTHER("Diğer");

  companion object {
    fun fromRaw(raw: String?): ReportReason =
      entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OTHER
  }
}

@Keep
@IgnoreExtraProperties
data class BlockedUser(
  @DocumentId val id: String = "",
  val blockedUserId: String = "",
  val displayName: String = "",
  val handle: String = "",
  val photoUrl: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetUserId: String = "",
  val targetType: String = "USER",
  val targetId: String = "",
  val reason: String = ReportReason.INAPPROPRIATE.name,
  val details: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRequest(
  @DocumentId val id: String = "",
  val requesterId: String = "",
  val requesterName: String = "",
  val requesterHandle: String = "",
  val requesterAvatarUrl: String = "",
  val recipientId: String = "",
  val status: String = RequestStatus.PENDING.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MessageRequest(
  @DocumentId val id: String = "",
  val requesterId: String = "",
  val requesterName: String = "",
  val requesterHandle: String = "",
  val requesterAvatarUrl: String = "",
  val recipientId: String = "",
  val previewText: String = "",
  val messageType: String = DirectMessageType.TEXT.name,
  val audioBase64: String = "",
  val audioDurationMs: Long = 0L,
  val status: String = RequestStatus.PENDING.name,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
  @DocumentId val id: String = "",
  val recipientId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val senderHandle: String = "",
  val senderAvatarUrl: String = "",
  val type: String = NotificationType.FOLLOW.name,
  val trackId: String = "",
  val trackTitle: String = "",
  val message: String = "",
  val targetType: String = "",
  val targetId: String = "",
  val read: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

data class SearchAutocompleteSuggestion(
  val text: String,
  val subtitle: String,
  val isLiveApi: Boolean = false
)

fun buildDirectChatId(uidA: String, uidB: String): String {
  val a = sanitizeFirestoreId(uidA)
  val b = sanitizeFirestoreId(uidB)
  return if (a <= b) "${a}__${b}" else "${b}__${a}"
}

fun sanitizeFirestoreId(raw: String): String {
  val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_')
  return cleaned.ifBlank { "item_${raw.hashCode().toUInt()}" }.take(120)
}

fun formatRelativeTimeTr(timestamp: Timestamp?, nowMillis: Long = System.currentTimeMillis()): String {
  val millis = timestamp?.toDate()?.time ?: return "az önce"
  val diffSec = ((nowMillis - millis) / 1000L).coerceAtLeast(0L)
  return when {
    diffSec < 60L -> "az önce"
    diffSec < 3600L -> "${diffSec / 60L} dk önce"
    diffSec < 86_400L -> "${diffSec / 3600L} sa önce"
    diffSec < 604_800L -> "${diffSec / 86_400L} gün önce"
    else -> "${diffSec / 604_800L} hf önce"
  }
}
