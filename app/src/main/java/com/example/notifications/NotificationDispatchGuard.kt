package com.example.notifications

import android.content.Context
import com.example.model.sanitizeFirestoreId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe persistent deduplication and killed-state metadata store shared between
 * the ViewModel realtime Firestore snapshot listener and VizyonFirebaseMessagingService (FCM).
 */
object NotificationDispatchGuard {
  private const val PREFS_NAME = "vizyon_notif_dispatch_guard"
  private const val KEY_BASELINE_PREFIX = "baseline_init_"
  private const val KEY_SEEN_IDS_PREFIX = "seen_ids_"
  private const val KEY_BLOCKED_IDS_PREFIX = "blocked_ids_"
  private const val KEY_LAST_ACTIVE_UID = "last_active_uid"
  private const val KEY_CACHED_FCM_TOKEN = "cached_fcm_token"
  private const val KEY_TOKEN_SYNCED_UID_PREFIX = "fcm_token_synced_uid_"
  private const val KEY_TOKEN_SYNCED_VAL_PREFIX = "fcm_token_synced_val_"
  private const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
  private const val KEY_BG_PUSH_ENABLED = "pref_bg_push_enabled"
  private const val KEY_LEGACY_CHANNELS_PRUNED = "legacy_channels_pruned_v6"
  private const val MAX_STORED_IDS = 260
  private val lock = Any()
  private val _tokenSyncVersion = MutableStateFlow(0L)
  val tokenSyncVersion: StateFlow<Long> = _tokenSyncVersion.asStateFlow()

  fun setLastActiveUserId(context: Context, uid: String?) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val clean = uid?.trim().orEmpty()
      if (clean.isBlank()) {
        prefs.edit().remove(KEY_LAST_ACTIVE_UID).commit()
      } else {
        prefs.edit().putString(KEY_LAST_ACTIVE_UID, clean).commit()
      }
    }
  }

  fun getLastActiveUserId(context: Context): String? {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_LAST_ACTIVE_UID, null)?.takeIf { it.isNotBlank() }
    }
  }

  fun cacheFcmToken(context: Context, token: String) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val clean = token.trim().take(512)
      val previous = prefs.getString(KEY_CACHED_FCM_TOKEN, "") ?: ""
      val editor = prefs.edit().putString(KEY_CACHED_FCM_TOKEN, clean)
      if (clean != previous) {
        // Force re-sync marker reset if token changed
        prefs.all.keys
          .filter { it.startsWith(KEY_TOKEN_SYNCED_UID_PREFIX) }
          .forEach { editor.remove(it) }
      }
      editor.commit()
      _tokenSyncVersion.value = _tokenSyncVersion.value + 1L
    }
  }

  fun getCachedFcmToken(context: Context): String {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_CACHED_FCM_TOKEN, "") ?: ""
    }
  }

  fun markTokenSyncedForUser(context: Context, uid: String, token: String, pushEnabled: Boolean) {
    if (uid.isBlank()) return
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putBoolean("$KEY_TOKEN_SYNCED_UID_PREFIX$uid", pushEnabled && token.isNotBlank())
        .putString("$KEY_TOKEN_SYNCED_VAL_PREFIX$uid", token.take(512))
        .commit()
      _tokenSyncVersion.value = _tokenSyncVersion.value + 1L
    }
  }

  fun isTokenSyncedForUser(context: Context, uid: String?): Boolean {
    if (uid.isNullOrBlank()) return false
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val synced = prefs.getBoolean("$KEY_TOKEN_SYNCED_UID_PREFIX$uid", false)
      val valToken = prefs.getString("$KEY_TOKEN_SYNCED_VAL_PREFIX$uid", "").orEmpty()
      return synced && valToken.isNotBlank()
    }
  }

  fun setNotificationsEnabled(context: Context, enabled: Boolean) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).commit()
    }
  }

  fun setBackgroundPushEnabled(context: Context, enabled: Boolean) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().putBoolean(KEY_BG_PUSH_ENABLED, enabled).commit()
    }
  }

  fun isNotificationsEnabled(context: Context): Boolean {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }
  }

  fun areNotificationsAllowed(context: Context): Boolean {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val notifOn = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
      val bgPushOn = prefs.getBoolean(KEY_BG_PUSH_ENABLED, true)
      return notifOn && bgPushOn
    }
  }

  fun isLegacyChannelsPruned(context: Context): Boolean {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getBoolean(KEY_LEGACY_CHANNELS_PRUNED, false)
    }
  }

  fun markLegacyChannelsPruned(context: Context) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().putBoolean(KEY_LEGACY_CHANNELS_PRUNED, true).commit()
    }
  }

  fun setBlockedUserIds(context: Context, uidKey: String, blockedIds: Set<String>) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val clean = blockedIds.mapNotNull { id -> sanitizeFirestoreId(id).takeIf { it.isNotBlank() } }
      prefs.edit()
        .putString("$KEY_BLOCKED_IDS_PREFIX$uidKey", clean.joinToString(","))
        .commit()
    }
  }

  fun isSenderBlocked(context: Context, uidKey: String, senderId: String): Boolean {
    if (senderId.isBlank()) return false
    val safeSender = sanitizeFirestoreId(senderId)
    if (safeSender.isBlank()) return false
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val blocked = readOrderedIds(prefs.getString("$KEY_BLOCKED_IDS_PREFIX$uidKey", ""))
      return safeSender in blocked
    }
  }

  fun isBaselineInitialized(context: Context, uidKey: String): Boolean {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getBoolean("$KEY_BASELINE_PREFIX$uidKey", false)
    }
  }

  fun isNotificationSeen(context: Context, uidKey: String, id: String): Boolean {
    if (id.isBlank()) return true
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val existing = readOrderedIds(prefs.getString("$KEY_SEEN_IDS_PREFIX$uidKey", ""))
      return id in existing
    }
  }

  fun markNotificationSeen(context: Context, uidKey: String, id: String) {
    if (id.isBlank()) return
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val existing = readOrderedIds(prefs.getString("$KEY_SEEN_IDS_PREFIX$uidKey", ""))
      val merged = (listOf(id) + existing).distinct().take(MAX_STORED_IDS)
      prefs.edit()
        .putBoolean("$KEY_BASELINE_PREFIX$uidKey", true)
        .putString("$KEY_SEEN_IDS_PREFIX$uidKey", merged.joinToString(","))
        .commit()
    }
  }

  fun markBaselineSeen(context: Context, uidKey: String, ids: List<String>) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val existing = readOrderedIds(prefs.getString("$KEY_SEEN_IDS_PREFIX$uidKey", ""))
      val merged = (ids + existing).distinct().take(MAX_STORED_IDS)
      prefs.edit()
        .putBoolean("$KEY_BASELINE_PREFIX$uidKey", true)
        .putString("$KEY_SEEN_IDS_PREFIX$uidKey", merged.joinToString(","))
        .commit()
    }
  }

  fun filterAndMarkUnseen(
    context: Context,
    uidKey: String,
    unreadIds: List<String>
  ): List<String> {
    if (unreadIds.isEmpty()) return emptyList()
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val existing = readOrderedIds(prefs.getString("$KEY_SEEN_IDS_PREFIX$uidKey", ""))
      val existingSet = existing.toHashSet()
      val unseen = unreadIds.filter { it.isNotBlank() && it !in existingSet }
      if (unseen.isNotEmpty()) {
        val merged = (unseen + existing).distinct().take(MAX_STORED_IDS)
        prefs.edit()
          .putBoolean("$KEY_BASELINE_PREFIX$uidKey", true)
          .putString("$KEY_SEEN_IDS_PREFIX$uidKey", merged.joinToString(","))
          .commit()
      }
      return unseen
    }
  }

  fun clearForUser(context: Context, uidKey: String) {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .remove("$KEY_BASELINE_PREFIX$uidKey")
        .remove("$KEY_SEEN_IDS_PREFIX$uidKey")
        .remove("$KEY_BLOCKED_IDS_PREFIX$uidKey")
        .remove("$KEY_TOKEN_SYNCED_UID_PREFIX$uidKey")
        .remove("$KEY_TOKEN_SYNCED_VAL_PREFIX$uidKey")
        .commit()
      _tokenSyncVersion.value = _tokenSyncVersion.value + 1L
    }
  }

  private fun readOrderedIds(raw: String?): List<String> =
    raw.orEmpty()
      .split(',')
      .map { it.trim() }
      .filter { it.isNotBlank() }
}
