package com.example.notifications

import android.content.Context

/**
 * Thread-safe persistent deduplication store shared between the ViewModel realtime
 * observer and BackgroundNotificationJobService so unread notifications fire reliably
 * in background without dropping alerts after 2-3 messages or duplicating across restarts.
 */
object NotificationDispatchGuard {
  private const val PREFS_NAME = "vizyon_notif_dispatch_guard"
  private const val KEY_BASELINE_PREFIX = "baseline_init_"
  private const val KEY_SEEN_IDS_PREFIX = "seen_ids_"
  private const val MAX_STORED_IDS = 260
  private val lock = Any()

  fun isBaselineInitialized(context: Context, uidKey: String): Boolean {
    synchronized(lock) {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getBoolean("$KEY_BASELINE_PREFIX$uidKey", false)
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
        .commit()
    }
  }

  private fun readOrderedIds(raw: String?): List<String> =
    raw.orEmpty()
      .split(',')
      .map { it.trim() }
      .filter { it.isNotBlank() }
}
