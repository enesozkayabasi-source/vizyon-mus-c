package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.model.ActivityActionType
import com.example.model.ActivityComment
import com.example.model.ActivityFeedItem
import com.example.model.ArtistFanPost
import com.example.model.ArtistSummary
import com.example.model.BlockedUser
import com.example.model.ChatPrivacy
import com.example.model.DirectChatMessage
import com.example.model.DirectChatThread
import com.example.model.DirectMessageType
import com.example.model.FanPostTag
import com.example.model.FollowConnection
import com.example.model.FollowRequest
import com.example.model.FollowedArtist
import com.example.model.MessageRequest
import com.example.model.MusicStoryItem
import com.example.model.MusicTrack
import com.example.model.NotificationType
import com.example.model.Playlist
import com.example.model.PlaylistCoverStyle
import com.example.model.PublicProfile
import com.example.model.RecentTrackSummary
import com.example.model.ReportReason
import com.example.model.RequestStatus
import com.example.model.SavedTrack
import com.example.model.StoryCardTheme
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.example.model.buildDirectChatId
import com.example.model.sanitizeFirestoreId
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VizyonFirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("Google ile giriş yapılmalıdır.")

  private fun safeAvatarUrl(raw: String?): String {
    val clean = raw?.trim().orEmpty()
    return when {
      clean.startsWith("http://") || clean.startsWith("https://") -> clean.take(500)
      clean.startsWith("data:image/") && clean.contains(";base64,") -> clean.take(24000)
      else -> ""
    }
  }

  private fun normalizeHandle(raw: String, fallbackName: String = "vizyon"): String {
    val cleaned =
      raw.trim()
        .ifBlank { fallbackName.lowercase().replace(Regex("[^a-z0-9]"), "") }
        .replace(Regex("\\s+"), "")
        .replace(Regex("[^a-zA-Z0-9_@]"), "")
    val withAt = if (cleaned.startsWith("@")) cleaned else "@$cleaned"
    return if (withAt.length >= 2) withAt.take(30) else "@vizyon"
  }

  private fun <T> Flow<T>.wrapFirestoreErrors(
    uid: String,
    op: OperationType,
    path: String
  ): Flow<T> = catch { error ->
    val firestoreError =
      generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val toLog = firestoreError ?: (error as? Exception)
    if (toLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(toLog, op, path)
    }
    throw error
  }

  // --- User Profile, Cloud Vault Backup & Public Profile ---

  suspend fun fetchUserProfileOnce(uid: String): Result<UserProfile?> {
    val path = "users/$uid"
    return try {
      val currentUid = requireUserId()
      check(currentUid == uid)
      val snap = db.collection("users").document(uid).get().await()
      val profile =
        if (snap.exists()) {
          snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else null
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun ensureUserProfileExists(
    uid: String,
    displayName: String,
    photoUrl: String,
    handle: String = "",
    bio: String = "",
    favoriteGenres: List<String> = emptyList(),
    shareActivity: Boolean = true,
    requireFollowApproval: Boolean = false,
    chatPrivacy: ChatPrivacy = ChatPrivacy.PUBLIC,
    defaultStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
    securityPasscodeHash: String = "",
    securityPasscodeSalt: String = "",
    rememberMe: Boolean = true,
    lockOnLaunch: Boolean = false,
    preferredThemeMode: String = "",
    preferredAccentPalette: String = "",
    preferredCornerDensity: String = "",
    preferredLanguage: String = "",
    onboardingCompleted: Boolean = false,
    recentTracks: List<RecentTrackSummary> = emptyList()
  ): Result<Unit> {
    val path = "users/$uid"
    return try {
      val currentUid = requireUserId()
      check(currentUid == uid)
      val userRef = db.collection("users").document(uid)
      val existing = userRef.get().await()
      val safeName = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }.take(50)
      val safeHandle = normalizeHandle(handle, safeName)
      val safePhoto = safeAvatarUrl(photoUrl)
      val safeBio = bio.trim().ifBlank { "Vizyon Music ile keşfediyor." }.take(200)
      val safeGenres = favoriteGenres.take(8).ifEmpty { listOf("Türkçe Pop", "Anadolu Rock") }

      if (!existing.exists()) {
        val privatePayload =
          mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to safeName,
            "handle" to safeHandle,
            "bio" to safeBio,
            "photoUrl" to safePhoto,
            "favoriteGenres" to safeGenres,
            "shareActivity" to shareActivity,
            "requireFollowApproval" to requireFollowApproval,
            "chatPrivacy" to chatPrivacy.name,
            "defaultStoryTheme" to defaultStoryTheme.name,
            "rememberMe" to rememberMe,
            "lockOnLaunch" to lockOnLaunch,
            "onboardingCompleted" to onboardingCompleted,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        if (securityPasscodeHash.isNotBlank() && securityPasscodeSalt.isNotBlank()) {
          privatePayload["securityPasscodeHash"] = securityPasscodeHash.take(128)
          privatePayload["securityPasscodeSalt"] = securityPasscodeSalt.take(64)
        }
        if (preferredThemeMode.isNotBlank()) privatePayload["preferredThemeMode"] = preferredThemeMode.take(32)
        if (preferredAccentPalette.isNotBlank()) privatePayload["preferredAccentPalette"] = preferredAccentPalette.take(32)
        if (preferredCornerDensity.isNotBlank()) privatePayload["preferredCornerDensity"] = preferredCornerDensity.take(32)
        if (preferredLanguage.isNotBlank()) privatePayload["preferredLanguage"] = preferredLanguage.take(32)
        if (recentTracks.isNotEmpty()) {
          privatePayload["recentTracks"] = recentTracks.take(5).map { it.toFirestoreMap() }
        }
        userRef.set(privatePayload).await()
      }

      val publicRef = db.collection("public_profiles").document(uid)
      val publicSnap = publicRef.get().await()
      if (!publicSnap.exists()) {
        val publicPayload =
          mapOf(
            "userId" to uid,
            "displayName" to safeName,
            "handle" to safeHandle,
            "bio" to safeBio.take(160),
            "photoUrl" to safePhoto,
            "discoverable" to true,
            "shareActivity" to shareActivity,
            "requireFollowApproval" to requireFollowApproval,
            "chatPrivacy" to chatPrivacy.name,
            "defaultStoryTheme" to defaultStoryTheme.name,
            "followerCount" to 0L,
            "followingCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        publicRef.set(publicPayload).await()
      } else if (publicSnap.getBoolean("discoverable") == false) {
        runCatching {
          publicRef.update(
            mapOf(
              "discoverable" to true,
              "shareActivity" to shareActivity,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db.collection("users").document(userId)
        .snapshots()
        .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
        .wrapFirestoreErrors(userId, OperationType.GET, path)
    )
  }

  suspend fun updateUserProfile(
    userId: String,
    displayName: String,
    handle: String,
    bio: String,
    shareActivity: Boolean,
    requireFollowApproval: Boolean? = null,
    favoriteGenres: List<String>? = null,
    photoUrl: String? = null,
    chatPrivacy: ChatPrivacy? = null,
    defaultStoryTheme: StoryCardTheme? = null,
    securityPasscodeHash: String? = null,
    securityPasscodeSalt: String? = null,
    rememberMe: Boolean? = null,
    lockOnLaunch: Boolean? = null,
    preferredThemeMode: String? = null,
    preferredAccentPalette: String? = null,
    preferredCornerDensity: String? = null,
    preferredLanguage: String? = null,
    onboardingCompleted: Boolean? = null,
    recentTracks: List<RecentTrackSummary>? = null
  ): Result<Unit> {
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val cleanName = displayName.trim().ifBlank { "Vizyon Dinleyicisi" }.take(50)
      val cleanHandle = normalizeHandle(handle, cleanName)
      val cleanBio = bio.trim().take(200)

      val userUpdates =
        mutableMapOf<String, Any>(
          "displayName" to cleanName,
          "handle" to cleanHandle,
          "bio" to cleanBio,
          "shareActivity" to shareActivity,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      if (requireFollowApproval != null) userUpdates["requireFollowApproval"] = requireFollowApproval
      if (favoriteGenres != null) userUpdates["favoriteGenres"] = favoriteGenres.take(8)
      val safePhoto = photoUrl?.let(::safeAvatarUrl)
      if (!safePhoto.isNullOrBlank()) userUpdates["photoUrl"] = safePhoto
      if (chatPrivacy != null) userUpdates["chatPrivacy"] = chatPrivacy.name
      if (defaultStoryTheme != null) userUpdates["defaultStoryTheme"] = defaultStoryTheme.name
      if (!securityPasscodeHash.isNullOrBlank() && !securityPasscodeSalt.isNullOrBlank()) {
        userUpdates["securityPasscodeHash"] = securityPasscodeHash.take(128)
        userUpdates["securityPasscodeSalt"] = securityPasscodeSalt.take(64)
      }
      if (rememberMe != null) userUpdates["rememberMe"] = rememberMe
      if (lockOnLaunch != null) userUpdates["lockOnLaunch"] = lockOnLaunch
      if (!preferredThemeMode.isNullOrBlank()) userUpdates["preferredThemeMode"] = preferredThemeMode.take(32)
      if (!preferredAccentPalette.isNullOrBlank()) userUpdates["preferredAccentPalette"] = preferredAccentPalette.take(32)
      if (!preferredCornerDensity.isNullOrBlank()) userUpdates["preferredCornerDensity"] = preferredCornerDensity.take(32)
      if (!preferredLanguage.isNullOrBlank()) userUpdates["preferredLanguage"] = preferredLanguage.take(32)
      if (onboardingCompleted != null) userUpdates["onboardingCompleted"] = onboardingCompleted
      if (recentTracks != null) userUpdates["recentTracks"] = recentTracks.take(5).map { it.toFirestoreMap() }

      db.collection("users").document(userId).update(userUpdates).await()

      val publicUpdates =
        mutableMapOf<String, Any>(
          "displayName" to cleanName,
          "handle" to cleanHandle,
          "bio" to cleanBio.take(160),
          "discoverable" to true,
          "shareActivity" to shareActivity,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      if (requireFollowApproval != null) publicUpdates["requireFollowApproval"] = requireFollowApproval
      if (!safePhoto.isNullOrBlank()) publicUpdates["photoUrl"] = safePhoto
      if (chatPrivacy != null) publicUpdates["chatPrivacy"] = chatPrivacy.name
      if (defaultStoryTheme != null) publicUpdates["defaultStoryTheme"] = defaultStoryTheme.name
      if (!shareActivity) {
        publicUpdates.putAll(clearedPublicTrackFields())
      }

      db.collection("public_profiles").document(userId)
        .update(publicUpdates)
        .await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  private fun clearedPublicTrackFields(): Map<String, Any> =
    mapOf(
      "currentTrackTitle" to "",
      "currentArtistName" to "",
      "lastTrackId" to "",
      "lastTrackTitle" to "",
      "lastArtistId" to "",
      "lastArtistName" to "",
      "lastTrackArtworkUrl" to "",
      "lastTrackYoutubeId" to "",
      "lastPlayedAtMillis" to 0L,
      "recentTracks" to emptyList<Map<String, Any>>()
    )

  suspend fun clearPublicListeningStatus(userId: String): Result<Unit> {
    val path = "public_profiles/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val updates = clearedPublicTrackFields().toMutableMap().apply {
        put("discoverable", true)
        put("shareActivity", false)
        put("updatedAt", FieldValue.serverTimestamp())
      }
      db.collection("public_profiles").document(userId).update(updates).await()
      runCatching {
        db.collection("users").document(userId).update(
          mapOf("shareActivity" to false, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateCloudBackupFields(
    userId: String,
    updates: Map<String, Any>
  ): Result<Unit> {
    if (updates.isEmpty()) return Result.success(Unit)
    val path = "users/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val payload = updates.toMutableMap().apply { put("updatedAt", FieldValue.serverTimestamp()) }
      db.collection("users").document(userId).update(payload).await()
      val publicUpdates = mutableMapOf<String, Any>()
      (updates["chatPrivacy"] as? String)?.let { publicUpdates["chatPrivacy"] = it }
      (updates["defaultStoryTheme"] as? String)?.let { publicUpdates["defaultStoryTheme"] = it }
      (updates["requireFollowApproval"] as? Boolean)?.let { publicUpdates["requireFollowApproval"] = it }
      (updates["shareActivity"] as? Boolean)?.let { share ->
        publicUpdates["shareActivity"] = share
        publicUpdates["discoverable"] = true
        if (!share) publicUpdates.putAll(clearedPublicTrackFields())
      }
      (updates["photoUrl"] as? String)?.let { safeAvatarUrl(it) }?.takeIf { it.isNotBlank() }?.let {
        publicUpdates["photoUrl"] = it
      }
      if (publicUpdates.isNotEmpty()) {
        publicUpdates["updatedAt"] = FieldValue.serverTimestamp()
        runCatching { db.collection("public_profiles").document(userId).update(publicUpdates).await() }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun updateLiveListeningStatus(
    userId: String,
    track: MusicTrack,
    recentTracks: List<RecentTrackSummary> = emptyList()
  ): Result<Unit> {
    val path = "public_profiles/$userId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val payload =
        mutableMapOf<String, Any>(
          "currentTrackTitle" to track.title.take(140),
          "currentArtistName" to track.artistName.take(100),
          "lastTrackId" to sanitizeFirestoreId(track.trackId).take(128),
          "lastTrackTitle" to track.title.take(160),
          "lastArtistId" to sanitizeFirestoreId(track.artistId).take(128),
          "lastArtistName" to track.artistName.take(120),
          "lastTrackArtworkUrl" to track.artworkUrl.take(500),
          "lastTrackYoutubeId" to track.youtubeVideoId.take(64),
          "lastPlayedAtMillis" to System.currentTimeMillis(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      if (recentTracks.isNotEmpty()) {
        val recentPayload = recentTracks.take(5).map { it.toFirestoreMap() }
        payload["recentTracks"] = recentPayload
        runCatching {
          db.collection("users").document(userId).update(
            mapOf(
              "recentTracks" to recentPayload,
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
      }
      db.collection("public_profiles").document(userId)
        .update(payload)
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteAllUserDataAndAccount(userId: String): Result<Unit> {
    val path = "users/$userId"
    return try {
      val currentUser = auth.currentUser ?: throw IllegalStateException("Aktif oturum bulunamadı.")
      check(currentUser.uid == userId)

      val userDoc = db.collection("users").document(userId)

      val playlistsSnap = userDoc.collection("playlists").get().await()
      for (pl in playlistsSnap.documents) {
        val tracksSnap = pl.reference.collection("tracks").get().await()
        tracksSnap.documents.forEach { it.reference.delete().await() }
        pl.reference.delete().await()
      }

      listOf(
        "favorites",
        "following",
        "followers",
        "followed_artists",
        "downloads",
        "notifications",
        "blocked_users",
        "follow_requests",
        "message_requests",
        "outgoing_requests"
      ).forEach { col ->
        val snap = userDoc.collection(col).get().await()
        snap.documents.forEach { it.reference.delete().await() }
      }

      val activitiesSnap =
        db.collection("activity_feed").whereEqualTo("userId", userId).get().await()
      for (actDoc in activitiesSnap.documents) {
        val cmtsSnap = actDoc.reference.collection("comments").get().await()
        cmtsSnap.documents.forEach { it.reference.delete().await() }
        actDoc.reference.delete().await()
      }
      val storiesSnap =
        db.collection("stories").whereEqualTo("userId", userId).get().await()
      storiesSnap.documents.forEach { it.reference.delete().await() }

      runCatching {
        val chatsSnap = db.collection("chats").whereArrayContains("participantIds", userId).get().await()
        chatsSnap.documents.forEach { chatDoc ->
          val msgs = chatDoc.reference.collection("messages").whereArrayContains("participantIds", userId).get().await()
          msgs.documents.forEach { it.reference.delete().await() }
          chatDoc.reference.delete().await()
        }
      }

      runCatching { db.collection("public_profiles").document(userId).delete().await() }
      runCatching { userDoc.delete().await() }

      currentUser.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- Discoverable Listeners, Following & Followers ---

  fun observePublicListeners(userId: String): Flow<List<PublicProfile>> = flow {
    val path = "public_profiles"
    emitAll(
      db.collection("public_profiles")
        .whereEqualTo("discoverable", true)
        .limit(40)
        .snapshots()
        .map { snap ->
          snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  fun observeFollowing(userId: String): Flow<List<FollowConnection>> = flow {
    val path = "users/$userId/following"
    emitAll(
      db.collection("users").document(userId).collection("following")
        .snapshots()
        .map { snap ->
          snap.toObjects(FollowConnection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  fun observeFollowers(userId: String): Flow<List<FollowConnection>> = flow {
    val path = "users/$userId/followers"
    emitAll(
      db.collection("users").document(userId).collection("followers")
        .snapshots()
        .map { snap ->
          snap.toObjects(FollowConnection::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun toggleFollowListener(
    userId: String,
    followerName: String,
    followerHandle: String,
    followerAvatarUrl: String,
    target: PublicProfile,
    currentlyFollowing: Boolean
  ): Result<Unit> {
    val targetId = sanitizeFirestoreId(target.userId.ifBlank { target.id })
    val path = "users/$userId/following/$targetId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val followingRef = db.collection("users").document(userId).collection("following").document(targetId)
      val followerRef = db.collection("users").document(targetId).collection("followers").document(userId)

      if (currentlyFollowing) {
        followingRef.delete().await()
        runCatching { followerRef.delete().await() }
        runCatching {
          val targetPublicRef = db.collection("public_profiles").document(targetId)
          db.runTransaction { tx ->
            val snap = tx.get(targetPublicRef)
            val cur = snap.getLong("followerCount") ?: 0L
            if (cur > 0L) {
              tx.update(
                targetPublicRef,
                mapOf(
                  "followerCount" to (cur - 1L),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            }
          }.await()
        }
        runCatching {
          val myPublicRef = db.collection("public_profiles").document(userId)
          db.runTransaction { tx ->
            val snap = tx.get(myPublicRef)
            val cur = snap.getLong("followingCount") ?: 0L
            if (cur > 0L) {
              tx.update(
                myPublicRef,
                mapOf(
                  "followingCount" to (cur - 1L),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            }
          }.await()
        }
      } else {
        followingRef.set(
          mapOf(
            "targetUserId" to targetId,
            "displayName" to target.displayName.ifBlank { "Dinleyici" }.take(50),
            "handle" to normalizeHandle(target.handle, target.displayName),
            "photoUrl" to safeAvatarUrl(target.photoUrl),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        runCatching {
          followerRef.set(
            mapOf(
              "targetUserId" to userId,
              "displayName" to followerName.ifBlank { "Dinleyici" }.take(50),
              "handle" to normalizeHandle(followerHandle, followerName),
              "photoUrl" to safeAvatarUrl(followerAvatarUrl),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
        runCatching {
          val targetPublicRef = db.collection("public_profiles").document(targetId)
          db.runTransaction { tx ->
            val snap = tx.get(targetPublicRef)
            if (snap.exists()) {
              val cur = snap.getLong("followerCount") ?: 0L
              tx.update(
                targetPublicRef,
                mapOf(
                  "followerCount" to (cur + 1L),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            }
          }.await()
        }
        runCatching {
          val myPublicRef = db.collection("public_profiles").document(userId)
          db.runTransaction { tx ->
            val snap = tx.get(myPublicRef)
            if (snap.exists()) {
              val cur = snap.getLong("followingCount") ?: 0L
              tx.update(
                myPublicRef,
                mapOf(
                  "followingCount" to (cur + 1L),
                  "updatedAt" to FieldValue.serverTimestamp()
                )
              )
            }
          }.await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // --- Blocking & Reporting ---

  fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
    val path = "users/$userId/blocked_users"
    emitAll(
      db.collection("users").document(userId).collection("blocked_users")
        .snapshots()
        .map { snap ->
          snap.toObjects(BlockedUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun blockUser(
    userId: String,
    targetUserId: String,
    targetName: String,
    targetHandle: String,
    targetAvatarUrl: String = ""
  ): Result<Unit> {
    val safeTarget = sanitizeFirestoreId(targetUserId)
    val path = "users/$userId/blocked_users/$safeTarget"
    return try {
      val uid = requireUserId()
      check(uid == userId && safeTarget != uid)
      db.collection("users").document(uid).collection("blocked_users").document(safeTarget)
        .set(
          mapOf(
            "blockedUserId" to safeTarget,
            "displayName" to targetName.ifBlank { "Dinleyici" }.take(50),
            "handle" to normalizeHandle(targetHandle, targetName),
            "photoUrl" to safeAvatarUrl(targetAvatarUrl),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      // Clean up active request docs if any
      runCatching { db.collection("users").document(uid).collection("follow_requests").document(safeTarget).delete().await() }
      runCatching { db.collection("users").document(uid).collection("message_requests").document(safeTarget).delete().await() }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun unblockUser(userId: String, blockedUserId: String): Result<Unit> {
    val safeTarget = sanitizeFirestoreId(blockedUserId)
    val path = "users/$userId/blocked_users/$safeTarget"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      db.collection("users").document(uid).collection("blocked_users").document(safeTarget).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun submitUserReport(
    reporterId: String,
    targetUserId: String,
    targetType: String,
    targetId: String,
    reason: ReportReason,
    details: String = ""
  ): Result<Unit> {
    val reportId = sanitizeFirestoreId("rep_${UUID.randomUUID().toString().take(12)}")
    val path = "user_reports/$reportId"
    return try {
      val uid = requireUserId()
      check(uid == reporterId)
      val safeTargetUser = sanitizeFirestoreId(targetUserId)
      val safeTargetDoc = sanitizeFirestoreId(targetId.ifBlank { safeTargetUser })
      db.collection("user_reports").document(reportId)
        .set(
          mapOf(
            "reporterId" to uid,
            "targetUserId" to safeTargetUser,
            "targetType" to targetType.ifBlank { "USER" }.take(32),
            "targetId" to safeTargetDoc,
            "reason" to reason.name,
            "details" to details.trim().take(300),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  // --- Follow Requests & Message Requests ---

  fun observeIncomingFollowRequests(userId: String): Flow<List<FollowRequest>> = flow {
    val path = "users/$userId/follow_requests"
    emitAll(
      db.collection("users").document(userId).collection("follow_requests")
        .whereEqualTo("status", RequestStatus.PENDING.name)
        .snapshots()
        .map { snap ->
          snap.toObjects(FollowRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  fun observeIncomingMessageRequests(userId: String): Flow<List<MessageRequest>> = flow {
    val path = "users/$userId/message_requests"
    emitAll(
      db.collection("users").document(userId).collection("message_requests")
        .whereEqualTo("status", RequestStatus.PENDING.name)
        .snapshots()
        .map { snap ->
          snap.toObjects(MessageRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  fun observeOutgoingRequestKeys(userId: String): Flow<Set<String>> = flow {
    val path = "users/$userId/outgoing_requests"
    emitAll(
      db.collection("users").document(userId).collection("outgoing_requests")
        .snapshots()
        .map { snap ->
          snap.documents.map { doc ->
            val st = doc.getString("status") ?: RequestStatus.PENDING.name
            "${doc.id}::$st"
          }.toSet()
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun fetchPendingRequestsCount(userId: String): Pair<List<FollowRequest>, List<MessageRequest>> =
    runCatching {
      val uid = requireUserId()
      if (uid != userId) return emptyList<FollowRequest>() to emptyList()
      val followReqs =
        db.collection("users").document(userId).collection("follow_requests")
          .whereEqualTo("status", RequestStatus.PENDING.name)
          .get(com.google.firebase.firestore.Source.SERVER)
          .await()
          .toObjects(FollowRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val msgReqs =
        db.collection("users").document(userId).collection("message_requests")
          .whereEqualTo("status", RequestStatus.PENDING.name)
          .get(com.google.firebase.firestore.Source.SERVER)
          .await()
          .toObjects(MessageRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      followReqs to msgReqs
    }.getOrDefault(emptyList<FollowRequest>() to emptyList())

  suspend fun sendFollowRequest(
    requesterId: String,
    requesterName: String,
    requesterHandle: String,
    requesterAvatarUrl: String,
    recipientId: String
  ): Result<Unit> {
    val safeRecipient = sanitizeFirestoreId(recipientId)
    val path = "users/$safeRecipient/follow_requests/$requesterId"
    return try {
      val uid = requireUserId()
      check(uid == requesterId && safeRecipient != uid)
      val safeHandle = normalizeHandle(requesterHandle, requesterName)
      val safeAvatar = safeAvatarUrl(requesterAvatarUrl)
      db.collection("users").document(safeRecipient).collection("follow_requests").document(uid)
        .set(
          mapOf(
            "requesterId" to uid,
            "requesterName" to requesterName.ifBlank { "Dinleyici" }.take(50),
            "requesterHandle" to safeHandle,
            "requesterAvatarUrl" to safeAvatar,
            "recipientId" to safeRecipient,
            "status" to RequestStatus.PENDING.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      db.collection("users").document(uid).collection("outgoing_requests").document("follow_$safeRecipient")
        .set(
          mapOf(
            "requesterId" to uid,
            "recipientId" to safeRecipient,
            "requestType" to "FOLLOW",
            "status" to RequestStatus.PENDING.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun respondToFollowRequest(
    recipientId: String,
    request: FollowRequest,
    accept: Boolean,
    recipientName: String,
    recipientHandle: String,
    recipientAvatarUrl: String
  ): Result<Unit> {
    val requesterId = sanitizeFirestoreId(request.requesterId.ifBlank { request.id })
    val path = "users/$recipientId/follow_requests/$requesterId"
    return try {
      val uid = requireUserId()
      check(uid == recipientId)
      val reqRef = db.collection("users").document(uid).collection("follow_requests").document(requesterId)
      if (accept) {
        val followerRef = db.collection("users").document(uid).collection("followers").document(requesterId)
        val requesterFollowingRef = db.collection("users").document(requesterId).collection("following").document(uid)
        followerRef.set(
          mapOf(
            "targetUserId" to requesterId,
            "displayName" to request.requesterName.ifBlank { "Dinleyici" }.take(50),
            "handle" to normalizeHandle(request.requesterHandle, request.requesterName),
            "photoUrl" to safeAvatarUrl(request.requesterAvatarUrl),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        requesterFollowingRef.set(
          mapOf(
            "targetUserId" to uid,
            "displayName" to recipientName.ifBlank { "Dinleyici" }.take(50),
            "handle" to normalizeHandle(recipientHandle, recipientName),
            "photoUrl" to safeAvatarUrl(recipientAvatarUrl),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
        runCatching {
          val myPubRef = db.collection("public_profiles").document(uid)
          db.runTransaction { tx ->
            val snap = tx.get(myPubRef)
            if (snap.exists()) {
              val cur = snap.getLong("followerCount") ?: 0L
              tx.update(myPubRef, mapOf("followerCount" to (cur + 1L), "updatedAt" to FieldValue.serverTimestamp()))
            }
          }.await()
        }
      }
      reqRef.delete().await()
      runCatching {
        db.collection("users").document(requesterId).collection("outgoing_requests").document("follow_$uid")
          .delete().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun sendMessageRequest(
    requesterId: String,
    requesterName: String,
    requesterHandle: String,
    requesterAvatarUrl: String,
    recipientId: String,
    previewText: String,
    messageType: DirectMessageType = DirectMessageType.TEXT,
    audioBase64: String = "",
    audioDurationMs: Long = 0L
  ): Result<Unit> {
    val safeRecipient = sanitizeFirestoreId(recipientId)
    val path = "users/$safeRecipient/message_requests/$requesterId"
    return try {
      val uid = requireUserId()
      check(uid == requesterId && safeRecipient != uid)
      val cleanPreview =
        previewText.trim().ifBlank {
          if (messageType == DirectMessageType.VOICE) {
            val sec = ((audioDurationMs + 500L) / 1000L).coerceIn(1L, 30L)
            "🎤 Sesli mesaj isteği (0:${sec.toString().padStart(2, '0')})"
          } else {
            "Merhaba! Sana mesaj göndermek istiyorum."
          }
        }.take(240)
      val payload =
        mutableMapOf<String, Any>(
          "requesterId" to uid,
          "requesterName" to requesterName.ifBlank { "Dinleyici" }.take(50),
          "requesterHandle" to normalizeHandle(requesterHandle, requesterName),
          "requesterAvatarUrl" to safeAvatarUrl(requesterAvatarUrl),
          "recipientId" to safeRecipient,
          "previewText" to cleanPreview,
          "messageType" to messageType.name,
          "status" to RequestStatus.PENDING.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      if (messageType == DirectMessageType.VOICE && audioBase64.isNotBlank()) {
        payload["audioBase64"] = audioBase64.take(220_000)
        payload["audioDurationMs"] = audioDurationMs.coerceIn(0L, 30_000L)
      }
      db.collection("users").document(safeRecipient).collection("message_requests").document(uid)
        .set(payload)
        .await()
      db.collection("users").document(uid).collection("outgoing_requests").document("message_$safeRecipient")
        .set(
          mapOf(
            "requesterId" to uid,
            "recipientId" to safeRecipient,
            "requestType" to "MESSAGE",
            "status" to RequestStatus.PENDING.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun respondToMessageRequest(
    recipientId: String,
    request: MessageRequest,
    accept: Boolean,
    recipientName: String,
    recipientHandle: String,
    recipientAvatarUrl: String
  ): Result<String?> {
    val requesterId = sanitizeFirestoreId(request.requesterId.ifBlank { request.id })
    val path = "users/$recipientId/message_requests/$requesterId"
    return try {
      val uid = requireUserId()
      check(uid == recipientId)
      val reqRef = db.collection("users").document(uid).collection("message_requests").document(requesterId)
      var createdChatId: String? = null
      if (accept) {
        val chatId = buildDirectChatId(uid, requesterId)
        createdChatId = chatId
        val chatRef = db.collection("chats").document(chatId)
        val participants = listOf(uid, requesterId).sorted()
        val nowMs = System.currentTimeMillis()
        val cleanPreview = request.previewText.trim().ifBlank { "Sohbet isteği kabul edildi." }.take(600)
        val names = mapOf(
          uid to recipientName.ifBlank { "Dinleyici" }.take(50),
          requesterId to request.requesterName.ifBlank { "Dinleyici" }.take(50)
        )
        val handles = mapOf(
          uid to normalizeHandle(recipientHandle, recipientName),
          requesterId to normalizeHandle(request.requesterHandle, request.requesterName)
        )
        val avatars = mapOf(
          uid to safeAvatarUrl(recipientAvatarUrl),
          requesterId to safeAvatarUrl(request.requesterAvatarUrl)
        )
        val chatSnap = chatRef.get().await()
        if (!chatSnap.exists()) {
          chatRef.set(
            mapOf(
              "chatId" to chatId,
              "participantIds" to participants,
              "participantNames" to names,
              "participantHandles" to handles,
              "participantAvatars" to avatars,
              "lastMessageText" to cleanPreview,
              "lastSenderId" to uid,
              "lastTrackTitle" to "",
              "lastMessageAtMillis" to nowMs,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          ).await()
        }
        val msgId = sanitizeFirestoreId("msg_req_${UUID.randomUUID().toString().take(10)}")
        val msgPayload =
          mutableMapOf<String, Any>(
            "chatId" to chatId,
            "participantIds" to participants,
            "senderId" to uid,
            "senderName" to recipientName.ifBlank { "Dinleyici" }.take(50),
            "senderHandle" to normalizeHandle(recipientHandle, recipientName),
            "text" to "Sohbet isteği kabul edildi • \"${cleanPreview.take(120)}\"",
            "messageType" to DirectMessageType.TEXT.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        val safeMyAvatar = safeAvatarUrl(recipientAvatarUrl)
        if (safeMyAvatar.isNotBlank()) msgPayload["senderAvatarUrl"] = safeMyAvatar
        chatRef.collection("messages").document(msgId).set(msgPayload).await()
      }
      reqRef.delete().await()
      runCatching {
        db.collection("users").document(requesterId).collection("outgoing_requests").document("message_$uid")
          .delete().await()
      }
      Result.success(createdChatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // --- Direct Messages (1-on-1 DM Chats) ---

  fun observeDirectChats(userId: String): Flow<List<DirectChatThread>> = flow {
    val path = "chats"
    emitAll(
      db.collection("chats")
        .whereArrayContains("participantIds", userId)
        .limit(40)
        .snapshots()
        .map { snap ->
          snap.toObjects(DirectChatThread::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.lastMessageAtMillis }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  fun observeChatMessages(chatId: String, userId: String): Flow<List<DirectChatMessage>> = flow {
    val safeChatId = sanitizeFirestoreId(chatId)
    val path = "chats/$safeChatId/messages"
    emitAll(
      db.collection("chats").document(safeChatId).collection("messages")
        .whereArrayContains("participantIds", userId)
        .limit(100)
        .snapshots()
        .map { snap ->
          snap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun sendDirectMessage(
    senderId: String,
    senderName: String,
    senderHandle: String,
    senderAvatarUrl: String,
    peer: PublicProfile,
    text: String,
    attachedTrack: MusicTrack? = null,
    messageType: DirectMessageType = DirectMessageType.TEXT,
    audioBase64: String = "",
    audioDurationMs: Long = 0L
  ): Result<String> {
    val peerId = sanitizeFirestoreId(peer.userId.ifBlank { peer.id })
    val chatId = buildDirectChatId(senderId, peerId)
    val messageId = sanitizeFirestoreId("msg_${UUID.randomUUID().toString().take(12)}")
    val path = "chats/$chatId/messages/$messageId"
    return try {
      val uid = requireUserId()
      check(uid == senderId)
      val resolvedType =
        when {
          messageType == DirectMessageType.VOICE && audioBase64.isNotBlank() -> DirectMessageType.VOICE
          attachedTrack != null -> DirectMessageType.TRACK
          else -> DirectMessageType.TEXT
        }
      val voiceDurationSec = ((audioDurationMs + 500L) / 1000L).coerceIn(1L, 30L)
      val cleanText =
        text.trim().ifBlank {
          when (resolvedType) {
            DirectMessageType.VOICE -> "🎤 Sesli mesaj (0:${voiceDurationSec.toString().padStart(2, '0')})"
            DirectMessageType.TRACK -> attachedTrack?.let { "♪ ${it.title} — ${it.artistName}" } ?: ""
            DirectMessageType.TEXT -> ""
          }
        }.take(600)
      require(cleanText.isNotEmpty()) { "Mesaj boş olamaz." }
      if (resolvedType == DirectMessageType.VOICE) {
        require(audioBase64.length <= 220_000) { "Sesli mesaj boyutu çok büyük (maks 30 sn)." }
      }

      val participants = listOf(uid, peerId).sorted()
      val safeSenderHandle = normalizeHandle(senderHandle, senderName)
      val safePeerHandle = normalizeHandle(peer.handle, peer.displayName)
      val safeSenderAvatar = safeAvatarUrl(senderAvatarUrl)
      val safePeerAvatar = safeAvatarUrl(peer.photoUrl)
      val nowMs = System.currentTimeMillis()

      val chatRef = db.collection("chats").document(chatId)
      val chatSnap = chatRef.get().await()
      val threadNames =
        mapOf(
          uid to senderName.ifBlank { "Dinleyici" }.take(50),
          peerId to peer.displayName.ifBlank { "Dinleyici" }.take(50)
        )
      val threadHandles = mapOf(uid to safeSenderHandle, peerId to safePeerHandle)
      val threadAvatars = mapOf(uid to safeSenderAvatar, peerId to safePeerAvatar)

      if (!chatSnap.exists()) {
        val chatCreatePayload =
          mutableMapOf<String, Any>(
            "chatId" to chatId,
            "participantIds" to participants,
            "participantNames" to threadNames,
            "participantHandles" to threadHandles,
            "participantAvatars" to threadAvatars,
            "lastMessageText" to cleanText,
            "lastSenderId" to uid,
            "lastTrackTitle" to (attachedTrack?.title?.take(160) ?: ""),
            "lastMessageAtMillis" to nowMs,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        chatRef.set(chatCreatePayload).await()
      } else {
        chatRef.update(
          mapOf(
            "participantNames" to threadNames,
            "participantHandles" to threadHandles,
            "participantAvatars" to threadAvatars,
            "lastMessageText" to cleanText,
            "lastSenderId" to uid,
            "lastTrackTitle" to (attachedTrack?.title?.take(160) ?: ""),
            "lastMessageAtMillis" to nowMs,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }

      val msgPayload =
        mutableMapOf<String, Any>(
          "chatId" to chatId,
          "participantIds" to participants,
          "senderId" to uid,
          "senderName" to senderName.ifBlank { "Dinleyici" }.take(50),
          "senderHandle" to safeSenderHandle,
          "text" to cleanText,
          "messageType" to resolvedType.name,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      if (safeSenderAvatar.isNotBlank()) msgPayload["senderAvatarUrl"] = safeSenderAvatar
      if (resolvedType == DirectMessageType.VOICE) {
        msgPayload["audioBase64"] = audioBase64.take(220_000)
        msgPayload["audioDurationMs"] = audioDurationMs.coerceIn(0L, 30_000L)
      }
      if (attachedTrack != null) {
        msgPayload["trackId"] = sanitizeFirestoreId(attachedTrack.trackId).take(128)
        msgPayload["trackTitle"] = attachedTrack.title.take(160)
        msgPayload["artistName"] = attachedTrack.artistName.take(120)
        msgPayload["albumArtUrl"] = attachedTrack.artworkUrl.take(500)
        msgPayload["youtubeVideoId"] = attachedTrack.youtubeVideoId.take(64)
        msgPayload["source"] = attachedTrack.source.name
      }
      chatRef.collection("messages").document(messageId).set(msgPayload).await()
      Result.success(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteDirectChatThread(userId: String, chatId: String): Result<Unit> {
    val safeChatId = sanitizeFirestoreId(chatId)
    val path = "chats/$safeChatId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val chatRef = db.collection("chats").document(safeChatId)
      val msgsSnap =
        chatRef.collection("messages")
          .whereArrayContains("participantIds", uid)
          .limit(200)
          .get()
          .await()
      msgsSnap.documents.chunked(250).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { batch.delete(it.reference) }
        batch.commit().await()
      }
      chatRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteDirectMessage(
    userId: String,
    chatId: String,
    messageId: String
  ): Result<Unit> {
    val safeChatId = sanitizeFirestoreId(chatId)
    val safeMsgId = sanitizeFirestoreId(messageId)
    val path = "chats/$safeChatId/messages/$safeMsgId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val chatRef = db.collection("chats").document(safeChatId)
      chatRef.collection("messages").document(safeMsgId).delete().await()

      val remainingSnap =
        runCatching {
          chatRef.collection("messages")
            .whereArrayContains("participantIds", uid)
            .limit(40)
            .get()
            .await()
        }.getOrNull()

      if (remainingSnap != null) {
        val remainingMsgs =
          remainingSnap.toObjects(DirectChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        val newest = remainingMsgs.maxByOrNull { it.createdAt?.toDate()?.time ?: 0L }
        runCatching {
          if (newest != null) {
            chatRef.update(
              mapOf(
                "lastMessageText" to newest.text.take(160),
                "lastSenderId" to newest.senderId,
                "lastTrackTitle" to newest.trackTitle.take(160),
                "lastMessageAtMillis" to (newest.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          } else {
            chatRef.update(
              mapOf(
                "lastMessageText" to "",
                "lastSenderId" to "",
                "lastTrackTitle" to "",
                "updatedAt" to FieldValue.serverTimestamp()
              )
            ).await()
          }
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- Favorites ---

  fun observeFavorites(userId: String): Flow<List<SavedTrack>> = flow {
    val path = "users/$userId/favorites"
    emitAll(
      db.collection("users").document(userId).collection("favorites")
        .snapshots()
        .map { snap ->
          snap.toObjects(SavedTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun toggleFavorite(
    userId: String,
    track: MusicTrack,
    currentlyFavorite: Boolean
  ): Result<Unit> {
    val safeTrackId = sanitizeFirestoreId(track.trackId)
    val path = "users/$userId/favorites/$safeTrackId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val ref = db.collection("users").document(userId).collection("favorites").document(safeTrackId)
      if (currentlyFavorite) {
        ref.delete().await()
      } else {
        ref.set(track.toFirestorePayload(safeTrackId)).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  // --- Customizable Playlists ---

  fun observePlaylists(userId: String): Flow<List<Playlist>> = flow {
    val path = "users/$userId/playlists"
    emitAll(
      db.collection("users").document(userId).collection("playlists")
        .snapshots()
        .map { snap ->
          snap.toObjects(Playlist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  fun observePlaylistTracks(userId: String, playlistId: String): Flow<List<SavedTrack>> = flow {
    val safePlId = sanitizeFirestoreId(playlistId)
    val path = "users/$userId/playlists/$safePlId/tracks"
    emitAll(
      db.collection("users").document(userId).collection("playlists").document(safePlId)
        .collection("tracks")
        .snapshots()
        .map { snap ->
          snap.toObjects(SavedTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun createPlaylist(
    userId: String,
    title: String,
    description: String,
    accentColorHex: String,
    coverStyle: PlaylistCoverStyle,
    sharedPublic: Boolean
  ): Result<String> {
    val playlistId = sanitizeFirestoreId("pl_${UUID.randomUUID().toString().take(12)}")
    val path = "users/$userId/playlists/$playlistId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val cleanTitle = title.trim().ifBlank { "Yeni Çalma Listem" }.take(80)
      db.collection("users").document(userId).collection("playlists").document(playlistId)
        .set(
          mapOf(
            "userId" to userId,
            "title" to cleanTitle,
            "description" to description.trim().take(240),
            "accentColorHex" to accentColorHex.take(16).ifBlank { "#2979FF" },
            "coverStyle" to coverStyle.name,
            "sharedPublic" to sharedPublic,
            "trackCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(playlistId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun addTrackToPlaylist(
    userId: String,
    playlistId: String,
    track: MusicTrack
  ): Result<Unit> {
    val safePlId = sanitizeFirestoreId(playlistId)
    val safeTrackId = sanitizeFirestoreId(track.trackId)
    val path = "users/$userId/playlists/$safePlId/tracks/$safeTrackId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val plRef = db.collection("users").document(userId).collection("playlists").document(safePlId)
      val trackRef = plRef.collection("tracks").document(safeTrackId)
      val alreadyExists = trackRef.get().await().exists()
      trackRef.set(track.toFirestorePayload(safeTrackId)).await()
      if (!alreadyExists) {
        plRef.update(
          mapOf(
            "trackCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  suspend fun removeTrackFromPlaylist(
    userId: String,
    playlistId: String,
    trackId: String
  ): Result<Unit> {
    val safePlId = sanitizeFirestoreId(playlistId)
    val safeTrackId = sanitizeFirestoreId(trackId)
    val path = "users/$userId/playlists/$safePlId/tracks/$safeTrackId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val plRef = db.collection("users").document(userId).collection("playlists").document(safePlId)
      val trackRef = plRef.collection("tracks").document(safeTrackId)
      val exists = trackRef.get().await().exists()
      trackRef.delete().await()
      if (exists) {
        val currentCount = plRef.get().await().getLong("trackCount") ?: 1L
        plRef.update(
          mapOf(
            "trackCount" to (currentCount - 1L).coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
    val safePlId = sanitizeFirestoreId(playlistId)
    val path = "users/$userId/playlists/$safePlId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val plRef = db.collection("users").document(userId).collection("playlists").document(safePlId)
      plRef.collection("tracks").get().await().documents.forEach { it.reference.delete().await() }
      plRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- Followed Artists & Interactive Artist Fan Wall ---

  fun observeFollowedArtists(userId: String): Flow<List<FollowedArtist>> = flow {
    val path = "users/$userId/followed_artists"
    emitAll(
      db.collection("users").document(userId).collection("followed_artists")
        .snapshots()
        .map { snap ->
          snap.toObjects(FollowedArtist::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun toggleFollowArtist(
    userId: String,
    artist: ArtistSummary,
    currentlyFollowing: Boolean
  ): Result<Unit> {
    val safeArtistId = sanitizeFirestoreId(artist.artistId)
    val path = "users/$userId/followed_artists/$safeArtistId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val ref = db.collection("users").document(userId).collection("followed_artists").document(safeArtistId)
      if (currentlyFollowing) {
        ref.delete().await()
      } else {
        ref.set(
          mapOf(
            "artistId" to safeArtistId,
            "name" to artist.name.ifBlank { "Sanatçı" }.take(120),
            "imageUrl" to artist.imageUrl.take(500),
            "genres" to artist.genres.take(6),
            "source" to artist.source.name,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, path)
      Result.failure(e)
    }
  }

  fun observeArtistFanPosts(userId: String, artistId: String): Flow<List<ArtistFanPost>> = flow {
    val safeArtistId = sanitizeFirestoreId(artistId)
    val path = "artists/$safeArtistId/fan_posts"
    emitAll(
      db.collection("artists").document(safeArtistId).collection("fan_posts")
        .whereEqualTo("artistId", safeArtistId)
        .snapshots()
        .map { snap ->
          snap.toObjects(ArtistFanPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun postArtistFanMessage(
    userId: String,
    artistId: String,
    authorName: String,
    authorHandle: String,
    message: String,
    tag: FanPostTag
  ): Result<Unit> {
    val safeArtistId = sanitizeFirestoreId(artistId)
    val postId = sanitizeFirestoreId("post_${UUID.randomUUID().toString().take(12)}")
    val path = "artists/$safeArtistId/fan_posts/$postId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val cleanMsg = message.trim().take(280)
      require(cleanMsg.isNotEmpty()) { "Mesaj boş olamaz." }
      db.collection("artists").document(safeArtistId).collection("fan_posts").document(postId)
        .set(
          mapOf(
            "artistId" to safeArtistId,
            "userId" to userId,
            "authorName" to authorName.ifBlank { "Dinleyici" }.take(50),
            "authorHandle" to normalizeHandle(authorHandle, authorName),
            "message" to cleanMsg,
            "tag" to tag.name,
            "cheersCount" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun cheerArtistFanPost(artistId: String, postId: String, currentCheers: Long): Result<Unit> {
    val safeArtistId = sanitizeFirestoreId(artistId)
    val safePostId = sanitizeFirestoreId(postId)
    val path = "artists/$safeArtistId/fan_posts/$safePostId"
    return try {
      requireUserId()
      db.collection("artists").document(safeArtistId).collection("fan_posts").document(safePostId)
        .update(
          mapOf(
            "cheersCount" to (currentCheers + 1L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  // --- Social Activity Feed ---

  fun observePublicActivityFeed(userId: String): Flow<List<ActivityFeedItem>> = flow {
    val path = "activity_feed"
    emitAll(
      db.collection("activity_feed")
        .whereEqualTo("visibility", "PUBLIC")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(50)
        .snapshots()
        .map { snap ->
          snap.toObjects(ActivityFeedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun publishActivity(
    userId: String,
    userName: String,
    userHandle: String,
    userAvatarUrl: String,
    actionType: ActivityActionType,
    track: MusicTrack,
    note: String,
    repostedFromName: String = "",
    repostedFromHandle: String = ""
  ): Result<Unit> {
    val activityId = sanitizeFirestoreId("act_${UUID.randomUUID().toString().take(12)}")
    val path = "activity_feed/$activityId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val safeHandle = normalizeHandle(userHandle, userName)
      val payload =
        mutableMapOf<String, Any>(
          "userId" to userId,
          "userName" to userName.ifBlank { "Vizyon Dinleyicisi" }.take(50),
          "userHandle" to safeHandle,
          "actionType" to actionType.name,
          "trackId" to sanitizeFirestoreId(track.trackId),
          "trackTitle" to track.title.ifBlank { "Bilinmeyen Parça" }.take(160),
          "artistId" to sanitizeFirestoreId(track.artistId),
          "artistName" to track.artistName.ifBlank { "Sanatçı" }.take(120),
          "albumArtUrl" to track.artworkUrl.take(500),
          "youtubeVideoId" to track.youtubeVideoId.take(64),
          "source" to track.source.name,
          "note" to note.trim().take(240),
          "visibility" to "PUBLIC",
          "likesCount" to 0L,
          "commentsCount" to 0L,
          "repostsCount" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      val safeAvatar = safeAvatarUrl(userAvatarUrl)
      if (safeAvatar.isNotBlank()) {
        payload["userAvatarUrl"] = safeAvatar
      }
      if (repostedFromName.isNotBlank()) {
        payload["repostedFromName"] = repostedFromName.take(50)
      }
      if (repostedFromHandle.isNotBlank()) {
        payload["repostedFromHandle"] = normalizeHandle(repostedFromHandle)
      }
      db.collection("activity_feed").document(activityId).set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun likeActivityItem(activityId: String, currentLikes: Long): Result<Unit> {
    val safeId = sanitizeFirestoreId(activityId)
    val path = "activity_feed/$safeId"
    return try {
      requireUserId()
      db.collection("activity_feed").document(safeId)
        .update(
          mapOf(
            "likesCount" to (currentLikes + 1L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  fun observeActivityComments(userId: String, activityId: String): Flow<List<ActivityComment>> = flow {
    val safeId = sanitizeFirestoreId(activityId)
    val path = "activity_feed/$safeId/comments"
    emitAll(
      db.collection("activity_feed").document(safeId).collection("comments")
        .whereEqualTo("activityId", safeId)
        .snapshots()
        .map { snap ->
          snap.toObjects(ActivityComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun addActivityComment(
    activityId: String,
    userId: String,
    userName: String,
    userHandle: String,
    userAvatarUrl: String,
    text: String
  ): Result<Unit> {
    val safeActivityId = sanitizeFirestoreId(activityId)
    val commentId = sanitizeFirestoreId("cmt_${UUID.randomUUID().toString().take(12)}")
    val path = "activity_feed/$safeActivityId/comments/$commentId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val cleanText = text.trim().take(280)
      require(cleanText.isNotEmpty()) { "Yorum boş olamaz." }
      val safeHandle = normalizeHandle(userHandle, userName)

      val parentRef = db.collection("activity_feed").document(safeActivityId)
      val commentRef = parentRef.collection("comments").document(commentId)
      val commentPayload =
        mutableMapOf<String, Any>(
          "activityId" to safeActivityId,
          "userId" to uid,
          "userName" to userName.ifBlank { "Vizyon Dinleyicisi" }.take(50),
          "userHandle" to safeHandle,
          "text" to cleanText,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      val safeAvatar = safeAvatarUrl(userAvatarUrl)
      if (safeAvatar.isNotBlank()) {
        commentPayload["userAvatarUrl"] = safeAvatar
      }

      commentRef.set(commentPayload).await()
      db.runTransaction { tx ->
        val snap = tx.get(parentRef)
        val curCount = snap.getLong("commentsCount") ?: 0L
        tx.update(
          parentRef,
          mapOf(
            "commentsCount" to (curCount + 1L),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }.await()

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun repostActivityItem(
    original: ActivityFeedItem,
    userId: String,
    userName: String,
    userHandle: String,
    userAvatarUrl: String,
    quoteNote: String
  ): Result<Unit> {
    val safeOriginalId = sanitizeFirestoreId(original.id)
    val track =
      MusicTrack(
        trackId = original.trackId,
        title = original.trackTitle,
        artistId = original.artistId,
        artistName = original.artistName,
        artworkUrl = original.albumArtUrl,
        youtubeVideoId = original.youtubeVideoId,
        source = com.example.model.TrackSource.fromRaw(original.source),
        durationMs = 180_000L
      )
    val publishRes =
      publishActivity(
        userId = userId,
        userName = userName,
        userHandle = userHandle,
        userAvatarUrl = userAvatarUrl,
        actionType = ActivityActionType.REPOSTED_TRACK,
        track = track,
        note = quoteNote.trim().ifBlank { original.note },
        repostedFromName = original.userName.ifBlank { "Dinleyici" },
        repostedFromHandle = original.userHandle.ifBlank { "@vizyon" }
      )
    if (publishRes.isFailure) return publishRes

    val path = "activity_feed/$safeOriginalId"
    return try {
      val parentRef = db.collection("activity_feed").document(safeOriginalId)
      db.runTransaction { tx ->
        val snap = tx.get(parentRef)
        if (snap.exists()) {
          val curReposts = snap.getLong("repostsCount") ?: 0L
          tx.update(
            parentRef,
            mapOf(
              "repostsCount" to (curReposts + 1L),
              "updatedAt" to FieldValue.serverTimestamp()
            )
          )
        }
      }.await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteActivityItem(userId: String, activityId: String): Result<Unit> {
    val safeId = sanitizeFirestoreId(activityId)
    val path = "activity_feed/$safeId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val docRef = db.collection("activity_feed").document(safeId)
      runCatching {
        docRef.collection("comments")
          .whereEqualTo("userId", uid)
          .get()
          .await()
          .documents
          .forEach { it.reference.delete().await() }
      }
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- Public Music Stories (24h TTL) ---

  fun observePublicStories(userId: String): Flow<List<MusicStoryItem>> = flow {
    val path = "stories"
    emitAll(
      db.collection("stories")
        .whereEqualTo("visibility", "PUBLIC")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .map { snap ->
          snap.toObjects(MusicStoryItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun publishStory(
    userId: String,
    userName: String,
    userHandle: String,
    userAvatarUrl: String,
    track: MusicTrack,
    note: String,
    cardTheme: StoryCardTheme
  ): Result<String> {
    val storyId = sanitizeFirestoreId("story_${UUID.randomUUID().toString().take(12)}")
    val path = "stories/$storyId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val safeHandle = normalizeHandle(userHandle, userName)
      val now = System.currentTimeMillis()
      val payload =
        mutableMapOf<String, Any>(
          "userId" to uid,
          "userName" to userName.ifBlank { "Vizyon Dinleyicisi" }.take(50),
          "userHandle" to safeHandle,
          "trackId" to sanitizeFirestoreId(track.trackId).take(128),
          "trackTitle" to track.title.ifBlank { "Parça" }.take(160),
          "artistId" to sanitizeFirestoreId(track.artistId).take(128),
          "artistName" to track.artistName.ifBlank { "Sanatçı" }.take(120),
          "albumArtUrl" to track.artworkUrl.take(500),
          "youtubeVideoId" to track.youtubeVideoId.take(64),
          "source" to track.source.name,
          "note" to note.trim().take(140),
          "cardTheme" to cardTheme.name,
          "visibility" to "PUBLIC",
          "expiresAtMillis" to (now + 86_400_000L),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      val safeAvatar = safeAvatarUrl(userAvatarUrl)
      if (safeAvatar.isNotBlank()) {
        payload["userAvatarUrl"] = safeAvatar
      }
      db.collection("stories").document(storyId).set(payload).await()
      Result.success(storyId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteStory(userId: String, storyId: String): Result<Unit> {
    val safeId = sanitizeFirestoreId(storyId)
    val path = "stories/$safeId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      db.collection("stories").document(safeId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  // --- Notifications Inbox ---

  fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
    val path = "users/$userId/notifications"
    emitAll(
      db.collection("users").document(userId).collection("notifications")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(40)
        .snapshots()
        .map { snap ->
          snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .wrapFirestoreErrors(userId, OperationType.LIST, path)
    )
  }

  suspend fun fetchLatestNotificationsOnce(
    userId: String,
    fromServerOnly: Boolean = false
  ): List<UserNotification> =
    runCatching {
      val uid = requireUserId()
      if (uid != userId) return emptyList()
      val query =
        db.collection("users").document(userId).collection("notifications")
          .orderBy("createdAt", Query.Direction.DESCENDING)
          .limit(30)
      val snap =
        if (fromServerOnly) {
          runCatching { query.get(com.google.firebase.firestore.Source.SERVER).await() }
            .getOrElse { query.get().await() }
        } else {
          query.get().await()
        }
      snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    }.getOrDefault(emptyList())

  suspend fun sendNotification(
    recipientUserId: String,
    senderId: String,
    senderName: String,
    senderHandle: String,
    senderAvatarUrl: String,
    type: NotificationType,
    trackId: String = "",
    trackTitle: String = "",
    message: String,
    targetType: String = "",
    targetId: String = ""
  ): Result<Unit> {
    val safeRecipient = sanitizeFirestoreId(recipientUserId)
    if (safeRecipient.isBlank() || safeRecipient == senderId) return Result.success(Unit)
    val notifId = sanitizeFirestoreId("notif_${UUID.randomUUID().toString().take(12)}")
    val path = "users/$safeRecipient/notifications/$notifId"
    return try {
      val uid = requireUserId()
      check(uid == senderId)
      val safeHandle = normalizeHandle(senderHandle, senderName)
      val payload =
        mutableMapOf<String, Any>(
          "recipientId" to safeRecipient,
          "senderId" to uid,
          "senderName" to senderName.ifBlank { "Dinleyici" }.take(50),
          "senderHandle" to safeHandle,
          "type" to type.name,
          "message" to message.ifBlank { type.label }.take(240),
          "read" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      val safeAvatar = safeAvatarUrl(senderAvatarUrl)
      if (safeAvatar.isNotBlank()) {
        payload["senderAvatarUrl"] = safeAvatar
      }
      if (trackId.isNotBlank()) {
        payload["trackId"] = sanitizeFirestoreId(trackId).take(128)
      }
      if (trackTitle.isNotBlank()) {
        payload["trackTitle"] = trackTitle.take(160)
      }
      if (targetType.isNotBlank()) {
        payload["targetType"] = targetType.take(40)
      }
      if (targetId.isNotBlank()) {
        payload["targetId"] = sanitizeFirestoreId(targetId).take(128)
      }
      db.collection("users").document(safeRecipient).collection("notifications").document(notifId)
        .set(payload)
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, path)
      Result.failure(e)
    }
  }

  suspend fun markNotificationRead(userId: String, notificationId: String): Result<Unit> {
    val safeId = sanitizeFirestoreId(notificationId)
    val path = "users/$userId/notifications/$safeId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      db.collection("users").document(userId).collection("notifications").document(safeId)
        .update(
          mapOf(
            "read" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun markAllNotificationsRead(userId: String, notificationIds: List<String>): Result<Unit> {
    if (notificationIds.isEmpty()) return Result.success(Unit)
    val path = "users/$userId/notifications"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val batch = db.batch()
      notificationIds.take(40).forEach { rawId ->
        val safeId = sanitizeFirestoreId(rawId)
        val ref = db.collection("users").document(userId).collection("notifications").document(safeId)
        batch.update(
          ref,
          mapOf(
            "read" to true,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        )
      }
      batch.commit().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, path)
      Result.failure(e)
    }
  }

  suspend fun deleteNotification(userId: String, notificationId: String): Result<Unit> {
    val safeId = sanitizeFirestoreId(notificationId)
    val path = "users/$userId/notifications/$safeId"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      db.collection("users").document(userId).collection("notifications").document(safeId)
        .delete()
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  suspend fun clearAllNotifications(userId: String, notificationIds: List<String>): Result<Unit> {
    val path = "users/$userId/notifications"
    return try {
      val uid = requireUserId()
      check(uid == userId)
      val notifCollection = db.collection("users").document(userId).collection("notifications")
      val fetchedIds =
        runCatching { notifCollection.limit(200).get().await().documents.map { it.id } }
          .getOrDefault(emptyList())
      val targetIds =
        (notificationIds.map { sanitizeFirestoreId(it) } + fetchedIds)
          .filter { it.isNotBlank() }
          .distinct()
      if (targetIds.isEmpty()) return Result.success(Unit)
      targetIds.chunked(400).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { safeId ->
          batch.delete(notifCollection.document(safeId))
        }
        batch.commit().await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  private fun MusicTrack.toFirestorePayload(safeTrackId: String): Map<String, Any> =
    mapOf(
      "trackId" to safeTrackId,
      "title" to title.ifBlank { "Parça" }.take(160),
      "artistId" to sanitizeFirestoreId(artistId).take(128),
      "artistName" to artistName.ifBlank { "Sanatçı" }.take(120),
      "albumName" to albumName.take(140),
      "artworkUrl" to artworkUrl.take(500),
      "youtubeVideoId" to youtubeVideoId.take(64),
      "durationMs" to durationMs.coerceAtLeast(0L),
      "source" to source.name,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
}
