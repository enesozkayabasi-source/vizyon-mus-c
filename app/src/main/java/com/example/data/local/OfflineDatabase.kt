package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ActivityComment
import com.example.model.ActivityFeedItem
import com.example.model.ArtistFanPost
import com.example.model.DirectChatMessage
import com.example.model.FollowConnection
import com.example.model.FollowedArtist
import com.example.model.MusicStoryItem
import com.example.model.Playlist
import com.example.model.SavedTrack
import com.example.model.StoryCardTheme
import com.example.model.UserNotification
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Entity(tableName = "local_favorites")
data class LocalFavoriteEntity(
  @PrimaryKey val trackId: String,
  val title: String,
  val artistId: String,
  val artistName: String,
  val albumName: String,
  val artworkUrl: String,
  val youtubeVideoId: String,
  val durationMs: Long,
  val source: String,
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toSavedTrack(): SavedTrack =
    SavedTrack(
      id = trackId,
      trackId = trackId,
      title = title,
      artistId = artistId,
      artistName = artistName,
      albumName = albumName,
      artworkUrl = artworkUrl,
      youtubeVideoId = youtubeVideoId,
      durationMs = durationMs,
      source = source,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_playlists")
data class LocalPlaylistEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val accentColorHex: String,
  val coverStyle: String,
  val sharedPublic: Boolean,
  val trackCount: Long,
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toPlaylist(): Playlist =
    Playlist(
      id = id,
      userId = "local",
      title = title,
      description = description,
      accentColorHex = accentColorHex,
      coverStyle = coverStyle,
      sharedPublic = sharedPublic,
      trackCount = trackCount,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class LocalPlaylistTrackEntity(
  val playlistId: String,
  val trackId: String,
  val title: String,
  val artistId: String,
  val artistName: String,
  val albumName: String,
  val artworkUrl: String,
  val youtubeVideoId: String,
  val durationMs: Long,
  val source: String,
  val addedAtMs: Long = System.currentTimeMillis()
) {
  fun toSavedTrack(): SavedTrack =
    SavedTrack(
      id = trackId,
      trackId = trackId,
      title = title,
      artistId = artistId,
      artistName = artistName,
      albumName = albumName,
      artworkUrl = artworkUrl,
      youtubeVideoId = youtubeVideoId,
      durationMs = durationMs,
      source = source,
      createdAt = Timestamp(Date(addedAtMs)),
      updatedAt = Timestamp(Date(addedAtMs))
    )
}

@Entity(tableName = "local_followed_artists")
data class LocalFollowedArtistEntity(
  @PrimaryKey val artistId: String,
  val name: String,
  val imageUrl: String,
  val genresJoined: String,
  val source: String,
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toFollowedArtist(): FollowedArtist =
    FollowedArtist(
      id = artistId,
      artistId = artistId,
      name = name,
      imageUrl = imageUrl,
      genres = genresJoined.split("|").filter { it.isNotBlank() },
      source = source,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_activity_feed")
data class LocalActivityEntity(
  @PrimaryKey val id: String,
  val userName: String,
  val userHandle: String,
  val userAvatarUrl: String = "",
  val actionType: String,
  val trackId: String,
  val trackTitle: String,
  val artistId: String,
  val artistName: String,
  val albumArtUrl: String,
  val youtubeVideoId: String,
  val source: String,
  val note: String,
  val likesCount: Long,
  val commentsCount: Long = 0L,
  val repostsCount: Long = 0L,
  val repostedFromName: String = "",
  val repostedFromHandle: String = "",
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toActivityFeedItem(): ActivityFeedItem =
    ActivityFeedItem(
      id = id,
      userId = "local",
      userName = userName,
      userHandle = userHandle,
      userAvatarUrl = userAvatarUrl,
      actionType = actionType,
      trackId = trackId,
      trackTitle = trackTitle,
      artistId = artistId,
      artistName = artistName,
      albumArtUrl = albumArtUrl,
      youtubeVideoId = youtubeVideoId,
      source = source,
      note = note,
      visibility = "PUBLIC",
      likesCount = likesCount,
      commentsCount = commentsCount,
      repostsCount = repostsCount,
      repostedFromName = repostedFromName,
      repostedFromHandle = repostedFromHandle,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_activity_comments")
data class LocalActivityCommentEntity(
  @PrimaryKey val id: String,
  val activityId: String,
  val userName: String,
  val userHandle: String,
  val userAvatarUrl: String,
  val text: String,
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toActivityComment(): ActivityComment =
    ActivityComment(
      id = id,
      activityId = activityId,
      userId = "local",
      userName = userName,
      userHandle = userHandle,
      userAvatarUrl = userAvatarUrl,
      text = text,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_artist_fan_posts")
data class LocalFanPostEntity(
  @PrimaryKey val id: String,
  val artistId: String,
  val authorName: String,
  val authorHandle: String,
  val message: String,
  val tag: String,
  val cheersCount: Long,
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toArtistFanPost(): ArtistFanPost =
    ArtistFanPost(
      id = id,
      artistId = artistId,
      userId = "local",
      authorName = authorName,
      authorHandle = authorHandle,
      message = message,
      tag = tag,
      cheersCount = cheersCount,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_stories")
data class LocalStoryEntity(
  @PrimaryKey val id: String,
  val userName: String,
  val userHandle: String,
  val userAvatarUrl: String,
  val trackId: String,
  val trackTitle: String,
  val artistId: String,
  val artistName: String,
  val albumArtUrl: String,
  val youtubeVideoId: String,
  val source: String,
  val note: String,
  val cardTheme: String = StoryCardTheme.NEON_WAVE.name,
  val createdAtMs: Long = System.currentTimeMillis(),
  val expiresAtMillis: Long = System.currentTimeMillis() + 86_400_000L
) {
  fun toMusicStoryItem(): MusicStoryItem =
    MusicStoryItem(
      id = id,
      userId = "local",
      userName = userName,
      userHandle = userHandle,
      userAvatarUrl = userAvatarUrl,
      trackId = trackId,
      trackTitle = trackTitle,
      artistId = artistId,
      artistName = artistName,
      albumArtUrl = albumArtUrl,
      youtubeVideoId = youtubeVideoId,
      source = source,
      note = note,
      cardTheme = cardTheme,
      visibility = "PUBLIC",
      expiresAtMillis = expiresAtMillis,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_notifications")
data class LocalNotificationEntity(
  @PrimaryKey val id: String,
  val senderName: String,
  val senderHandle: String,
  val senderAvatarUrl: String,
  val type: String,
  val trackId: String,
  val trackTitle: String,
  val message: String,
  val targetType: String = "",
  val targetId: String = "",
  val read: Boolean,
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toUserNotification(): UserNotification =
    UserNotification(
      id = id,
      recipientId = "local",
      senderId = "local_peer",
      senderName = senderName,
      senderHandle = senderHandle,
      senderAvatarUrl = senderAvatarUrl,
      type = type,
      trackId = trackId,
      trackTitle = trackTitle,
      message = message,
      targetType = targetType,
      targetId = targetId,
      read = read,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_following")
data class LocalFollowingEntity(
  @PrimaryKey val targetUserId: String,
  val displayName: String,
  val handle: String,
  val photoUrl: String = "",
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toFollowConnection(): FollowConnection =
    FollowConnection(
      id = targetUserId,
      targetUserId = targetUserId,
      displayName = displayName,
      handle = handle,
      photoUrl = photoUrl,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Entity(tableName = "local_direct_messages")
data class LocalDirectMessageEntity(
  @PrimaryKey val id: String,
  val chatId: String,
  val peerId: String,
  val peerName: String,
  val peerHandle: String,
  val peerAvatarUrl: String = "",
  val senderId: String,
  val senderName: String,
  val senderHandle: String,
  val senderAvatarUrl: String = "",
  val text: String,
  val trackId: String = "",
  val trackTitle: String = "",
  val artistName: String = "",
  val albumArtUrl: String = "",
  val youtubeVideoId: String = "",
  val source: String = "",
  val createdAtMs: Long = System.currentTimeMillis()
) {
  fun toDirectChatMessage(): DirectChatMessage =
    DirectChatMessage(
      id = id,
      chatId = chatId,
      participantIds = listOf("local", peerId),
      senderId = senderId,
      senderName = senderName,
      senderHandle = senderHandle,
      senderAvatarUrl = senderAvatarUrl,
      text = text,
      trackId = trackId,
      trackTitle = trackTitle,
      artistName = artistName,
      albumArtUrl = albumArtUrl,
      youtubeVideoId = youtubeVideoId,
      source = source,
      createdAt = Timestamp(Date(createdAtMs)),
      updatedAt = Timestamp(Date(createdAtMs))
    )
}

@Dao
interface LocalLibraryDao {
  @Query("SELECT * FROM local_favorites ORDER BY createdAtMs DESC")
  fun observeFavorites(): Flow<List<LocalFavoriteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFavorite(fav: LocalFavoriteEntity)

  @Query("DELETE FROM local_favorites WHERE trackId = :trackId")
  suspend fun deleteFavorite(trackId: String)

  @Query("DELETE FROM local_favorites")
  suspend fun deleteAllFavorites()

  @Query("SELECT * FROM local_playlists ORDER BY createdAtMs DESC")
  fun observePlaylists(): Flow<List<LocalPlaylistEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPlaylist(playlist: LocalPlaylistEntity)

  @Query("UPDATE local_playlists SET trackCount = :count WHERE id = :playlistId")
  suspend fun updatePlaylistCount(playlistId: String, count: Long)

  @Query("DELETE FROM local_playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: String)

  @Query("DELETE FROM local_playlists")
  suspend fun deleteAllPlaylists()

  @Query("SELECT * FROM local_playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAtMs DESC")
  fun observePlaylistTracks(playlistId: String): Flow<List<LocalPlaylistTrackEntity>>

  @Query("SELECT COUNT(*) FROM local_playlist_tracks WHERE playlistId = :playlistId")
  suspend fun countPlaylistTracks(playlistId: String): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPlaylistTrack(item: LocalPlaylistTrackEntity)

  @Query("DELETE FROM local_playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
  suspend fun deletePlaylistTrack(playlistId: String, trackId: String)

  @Query("DELETE FROM local_playlist_tracks")
  suspend fun deleteAllPlaylistTracks()

  @Query("SELECT * FROM local_followed_artists ORDER BY createdAtMs DESC")
  fun observeFollowedArtists(): Flow<List<LocalFollowedArtistEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFollowedArtist(artist: LocalFollowedArtistEntity)

  @Query("DELETE FROM local_followed_artists WHERE artistId = :artistId")
  suspend fun deleteFollowedArtist(artistId: String)

  @Query("DELETE FROM local_followed_artists")
  suspend fun deleteAllFollowedArtists()

  @Query("SELECT * FROM local_activity_feed ORDER BY createdAtMs DESC LIMIT 50")
  fun observeActivityFeed(): Flow<List<LocalActivityEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivity(item: LocalActivityEntity)

  @Query("UPDATE local_activity_feed SET likesCount = likesCount + 1 WHERE id = :id")
  suspend fun incrementActivityLike(id: String)

  @Query("UPDATE local_activity_feed SET commentsCount = commentsCount + 1 WHERE id = :id")
  suspend fun incrementActivityComment(id: String)

  @Query("UPDATE local_activity_feed SET repostsCount = repostsCount + 1 WHERE id = :id")
  suspend fun incrementActivityRepost(id: String)

  @Query("DELETE FROM local_activity_feed WHERE id = :id")
  suspend fun deleteActivity(id: String)

  @Query("DELETE FROM local_activity_feed")
  suspend fun deleteAllActivities()

  @Query("SELECT * FROM local_activity_comments WHERE activityId = :activityId ORDER BY createdAtMs ASC")
  fun observeActivityComments(activityId: String): Flow<List<LocalActivityCommentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertActivityComment(comment: LocalActivityCommentEntity)

  @Query("DELETE FROM local_activity_comments WHERE activityId = :activityId")
  suspend fun deleteCommentsForActivity(activityId: String)

  @Query("DELETE FROM local_activity_comments")
  suspend fun deleteAllActivityComments()

  @Query("SELECT * FROM local_artist_fan_posts WHERE artistId = :artistId ORDER BY createdAtMs DESC")
  fun observeFanPosts(artistId: String): Flow<List<LocalFanPostEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFanPost(post: LocalFanPostEntity)

  @Query("UPDATE local_artist_fan_posts SET cheersCount = cheersCount + 1 WHERE id = :id")
  suspend fun incrementFanPostCheer(id: String)

  @Query("DELETE FROM local_artist_fan_posts")
  suspend fun deleteAllFanPosts()

  @Query("SELECT * FROM local_stories ORDER BY createdAtMs DESC LIMIT 30")
  fun observeStories(): Flow<List<LocalStoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStory(story: LocalStoryEntity)

  @Query("DELETE FROM local_stories WHERE id = :storyId")
  suspend fun deleteStory(storyId: String)

  @Query("DELETE FROM local_stories")
  suspend fun deleteAllStories()

  @Query("SELECT * FROM local_notifications ORDER BY createdAtMs DESC LIMIT 50")
  fun observeNotifications(): Flow<List<LocalNotificationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNotification(item: LocalNotificationEntity)

  @Query("UPDATE local_notifications SET read = 1 WHERE id = :id")
  suspend fun markNotificationRead(id: String)

  @Query("UPDATE local_notifications SET read = 1 WHERE read = 0")
  suspend fun markAllNotificationsRead()

  @Query("DELETE FROM local_notifications WHERE id = :id")
  suspend fun deleteNotificationById(id: String)

  @Query("DELETE FROM local_notifications")
  suspend fun deleteAllNotifications()

  @Query("SELECT * FROM local_following ORDER BY createdAtMs DESC")
  fun observeFollowing(): Flow<List<LocalFollowingEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFollowing(item: LocalFollowingEntity)

  @Query("DELETE FROM local_following WHERE targetUserId = :targetUserId")
  suspend fun deleteFollowing(targetUserId: String)

  @Query("DELETE FROM local_following")
  suspend fun deleteAllFollowing()

  @Query("SELECT * FROM local_direct_messages ORDER BY createdAtMs DESC")
  fun observeAllDirectMessages(): Flow<List<LocalDirectMessageEntity>>

  @Query("SELECT * FROM local_direct_messages WHERE chatId = :chatId ORDER BY createdAtMs ASC")
  fun observeDirectMessagesForChat(chatId: String): Flow<List<LocalDirectMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDirectMessage(msg: LocalDirectMessageEntity)

  @Query("DELETE FROM local_direct_messages WHERE chatId = :chatId")
  suspend fun deleteDirectMessagesForChat(chatId: String)

  @Query("DELETE FROM local_direct_messages WHERE id = :messageId")
  suspend fun deleteDirectMessageById(messageId: String)

  @Query("DELETE FROM local_direct_messages")
  suspend fun deleteAllDirectMessages()

  suspend fun clearAllUserData() {
    deleteAllPlaylistTracks()
    deleteAllPlaylists()
    deleteAllFavorites()
    deleteAllFollowedArtists()
    deleteAllActivities()
    deleteAllActivityComments()
    deleteAllFanPosts()
    deleteAllStories()
    deleteAllNotifications()
    deleteAllFollowing()
    deleteAllDirectMessages()
  }
}

@Database(
  entities = [
    LocalFavoriteEntity::class,
    LocalPlaylistEntity::class,
    LocalPlaylistTrackEntity::class,
    LocalFollowedArtistEntity::class,
    LocalActivityEntity::class,
    LocalActivityCommentEntity::class,
    LocalFanPostEntity::class,
    LocalStoryEntity::class,
    LocalNotificationEntity::class,
    LocalFollowingEntity::class,
    LocalDirectMessageEntity::class
  ],
  version = 7,
  exportSchema = false
)
abstract class VizyonDatabase : RoomDatabase() {
  abstract fun localLibraryDao(): LocalLibraryDao

  companion object {
    @Volatile private var INSTANCE: VizyonDatabase? = null

    fun getInstance(context: Context): VizyonDatabase =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: Room.databaseBuilder(
                context.applicationContext,
                VizyonDatabase::class.java,
                "vizyon_music_library.db"
              )
              .fallbackToDestructiveMigration(dropAllTables = true)
              .build()
              .also { INSTANCE = it }
        }
  }
}
