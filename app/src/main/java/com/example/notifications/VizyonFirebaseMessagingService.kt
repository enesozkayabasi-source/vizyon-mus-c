package com.example.notifications

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.AppearancePreferences
import com.example.model.NotificationType
import com.example.model.UserNotification
import com.example.model.sanitizeFirestoreId
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Receives push notifications via Firebase Cloud Messaging (FCM) when the app is in background or closed,
 * forwarding them to VizyonNotificationHelper (MessagingStyle + 1150ms sound cooldown, zero FGS).
 */
class VizyonFirebaseMessagingService : FirebaseMessagingService() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
  override fun onNewToken(token: String) {
    super.onNewToken(token)
    if (token.isBlank()) return
    val appCtx = applicationContext
    NotificationDispatchGuard.cacheFcmToken(appCtx, token)
    serviceScope.launch {
      val cfg =
        withTimeoutOrNull(1500L) {
          runCatching { AppearancePreferences(appCtx).configFlow.first() }.getOrNull()
        }
      val uid =
        runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
          ?.takeIf { it.isNotBlank() }
          ?: NotificationDispatchGuard.getLastActiveUserId(appCtx)
      if (!uid.isNullOrBlank()) {
        val pushAllowed =
          if (cfg != null) {
            NotificationDispatchGuard.setNotificationsEnabled(appCtx, cfg.notificationsEnabled)
            NotificationDispatchGuard.setBackgroundPushEnabled(appCtx, cfg.backgroundNotifServiceEnabled)
            cfg.notificationsEnabled && cfg.backgroundNotifServiceEnabled
          } else {
            NotificationDispatchGuard.areNotificationsAllowed(appCtx)
          }
        VizyonFcmTokenSync.persistTokenToFirestore(
          context = appCtx,
          uid = uid,
          token = if (pushAllowed) token else "",
          pushEnabled = pushAllowed
        )
      }
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    val appCtx = applicationContext
    if (!NotificationDispatchGuard.areNotificationsAllowed(appCtx)) {
      return
    }
    if (com.google.firebase.FirebaseApp.getApps(appCtx).isEmpty()) {
      runCatching { com.google.firebase.FirebaseApp.initializeApp(appCtx) }
    }

    val data = remoteMessage.data
    val notifFallback = remoteMessage.notification

    val currentUid =
      runCatching { Firebase.auth.currentUser?.uid }.getOrNull().orEmpty()
        .ifBlank { NotificationDispatchGuard.getLastActiveUserId(appCtx).orEmpty() }
    val recipientId = data["recipientId"]?.takeIf { it.isNotBlank() } ?: currentUid
    val resolvedUid = currentUid.ifBlank { recipientId }
    val uidKey = resolvedUid.ifBlank { "guest" }

    val rawNotifId =
      data["notificationId"]?.takeIf { it.isNotBlank() }
        ?: data["id"]?.takeIf { it.isNotBlank() }
        ?: remoteMessage.messageId?.takeIf { it.isNotBlank() }
        ?: "fcm_${System.currentTimeMillis()}"
    val notifId = sanitizeFirestoreId(rawNotifId)

    val senderId = data["senderId"].orEmpty()
    if (senderId.isNotBlank() && resolvedUid.isNotBlank() && senderId == resolvedUid) {
      return
    }
    if (senderId.isNotBlank() && NotificationDispatchGuard.isSenderBlocked(appCtx, uidKey, senderId)) {
      return
    }

    val rawType =
      data["type"]?.takeIf { it.isNotBlank() }
        ?: data["notificationType"]?.takeIf { it.isNotBlank() }
        ?: NotificationType.MESSAGE.name
    val type = NotificationType.fromRaw(rawType)
    val expectedChannelId =
      data["channelId"]?.takeIf { it.isNotBlank() }
        ?: VizyonNotificationHelper.channelForType(type)
    VizyonNotificationHelper.ensureChannel(appCtx, expectedChannelId)

    val senderName =
      data["senderName"]?.ifBlank { null }
        ?: data["title"]?.ifBlank { null }
        ?: notifFallback?.title?.ifBlank { null }
        ?: "Vizyon Dinleyicisi"
    val body =
      data["message"]?.ifBlank { null }
        ?: data["body"]?.ifBlank { null }
        ?: notifFallback?.body?.ifBlank { null }
        ?: type.label

    val sentTimeMs =
      data["sentTimeMs"]?.toLongOrNull()?.takeIf { it > 0L }
        ?: remoteMessage.sentTime.takeIf { it > 0L }
        ?: System.currentTimeMillis()
    val notification =
      UserNotification(
        id = notifId,
        recipientId = recipientId,
        senderId = senderId,
        senderName = senderName,
        senderHandle = data["senderHandle"].orEmpty(),
        senderAvatarUrl = data["senderAvatarUrl"].orEmpty(),
        type = type.name,
        trackId = data["trackId"].orEmpty(),
        trackTitle = data["trackTitle"].orEmpty(),
        message = body,
        targetType = data["targetType"].orEmpty(),
        targetId = data["targetId"].orEmpty(),
        read = false,
        createdAt = Timestamp(java.util.Date(sentTimeMs))
      )

    val baselineInitialized = NotificationDispatchGuard.isBaselineInitialized(appCtx, uidKey)
    if (baselineInitialized && NotificationDispatchGuard.isNotificationSeen(appCtx, uidKey, notifId)) {
      return
    }

    val posted = VizyonNotificationHelper.postFcmNotificationFast(appCtx, notification)
    if (posted) {
      NotificationDispatchGuard.markNotificationSeen(appCtx, uidKey, notifId)
    }
  }
}

object VizyonFcmTokenSync {
  private const val TAG = "VizyonFcmTokenSync"
  @Volatile var lastSyncError: String? = null
    private set

  suspend fun syncCurrentUserToken(
    context: Context,
    uid: String?,
    notificationsEnabled: Boolean,
    fcmPushEnabled: Boolean
  ) {
    syncCurrentUserTokenWithStatus(context, uid, notificationsEnabled, fcmPushEnabled)
  }

  suspend fun syncCurrentUserTokenWithStatus(
    context: Context,
    uid: String?,
    notificationsEnabled: Boolean,
    fcmPushEnabled: Boolean
  ): Boolean {
    val appCtx = context.applicationContext
    NotificationDispatchGuard.setNotificationsEnabled(appCtx, notificationsEnabled)
    NotificationDispatchGuard.setBackgroundPushEnabled(appCtx, fcmPushEnabled)
    if (uid.isNullOrBlank()) return false
    NotificationDispatchGuard.setLastActiveUserId(appCtx, uid)
    val enabled = notificationsEnabled && fcmPushEnabled
    if (!enabled) {
      return persistTokenToFirestore(appCtx, uid, token = "", pushEnabled = false)
    }
    var retrievedToken: String? = null
    var lastRetrieveErr: Throwable? = null
    for (attempt in 0 until 3) {
      val tokenAttempt =
        runCatching {
          @Suppress("DEPRECATION")
          FirebaseMessaging.getInstance().token.await()
        }
      retrievedToken = tokenAttempt.getOrNull()?.takeIf { it.isNotBlank() }
      if (retrievedToken != null) {
        lastRetrieveErr = null
        break
      }
      lastRetrieveErr = tokenAttempt.exceptionOrNull()
      if (lastRetrieveErr != null) {
        Log.d(TAG, "FCM token retrieval attempt=${attempt + 1} skipped/unavailable: ${lastRetrieveErr.message}")
      }
      if (attempt < 2) delay(600L * (attempt + 1))
    }
    val token =
      retrievedToken?.takeIf { it.isNotBlank() }
        ?: NotificationDispatchGuard.getCachedFcmToken(appCtx).orEmpty()
    if (token.isNotBlank()) {
      NotificationDispatchGuard.cacheFcmToken(appCtx, token)
      return persistTokenToFirestore(appCtx, uid, token = token, pushEnabled = true)
    }
    lastSyncError = lastRetrieveErr?.localizedMessage ?: "FCM cihaz token'ı alınamadı."
    return false
  }

  suspend fun clearTokenOnSignOutOrDelete(context: Context, uid: String?) {
    if (uid.isNullOrBlank()) return
    val appCtx = context.applicationContext
    persistTokenToFirestore(appCtx, uid, token = "", pushEnabled = false)
    NotificationDispatchGuard.markTokenSyncedForUser(appCtx, uid, "", false)
    NotificationDispatchGuard.setLastActiveUserId(appCtx, null)
  }

  private suspend fun resolveAuthenticatedUidForWrite(targetUid: String): String? {
    val auth = runCatching { Firebase.auth }.getOrNull() ?: return null
    auth.currentUser?.uid?.let { return it }
    return withTimeoutOrNull(2500L) {
      callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
          val resolved = firebaseAuth.currentUser?.uid
          if (!resolved.isNullOrBlank()) {
            trySend(resolved)
          }
        }
        auth.addAuthStateListener(listener)
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }?.let { trySend(it) }
        awaitClose { auth.removeAuthStateListener(listener) }
      }.first()
    }
  }

  suspend fun persistTokenToFirestore(
    context: Context,
    uid: String,
    token: String,
    pushEnabled: Boolean
  ): Boolean {
    val appCtx = context.applicationContext
    if (token.isNotBlank()) {
      NotificationDispatchGuard.cacheFcmToken(appCtx, token)
    }
    val currentAuthUid = resolveAuthenticatedUidForWrite(uid)
    if (currentAuthUid != uid) {
      lastSyncError = "Google oturumu henüz doğrulanamadı."
      return false
    }
    NotificationDispatchGuard.setLastActiveUserId(appCtx, uid)
    val dbId = appCtx.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(dbId)
    val payload =
      mapOf(
        "fcmToken" to token.take(512),
        "fcmPushEnabled" to pushEnabled,
        "fcmUpdatedAtMillis" to System.currentTimeMillis(),
        "updatedAt" to FieldValue.serverTimestamp()
      )

    repeat(4) { attempt ->
      val res =
        runCatching {
          db.collection("users").document(uid).update(payload).await()
        }
      if (res.isSuccess) {
        lastSyncError = null
        NotificationDispatchGuard.markTokenSyncedForUser(appCtx, uid, token, pushEnabled)
        return true
      }
      val err = res.exceptionOrNull()
      val isNotFound =
        err is FirebaseFirestoreException && err.code == FirebaseFirestoreException.Code.NOT_FOUND
      if (isNotFound && attempt < 3) {
        delay(700L * (attempt + 1))
      } else {
        lastSyncError = err?.localizedMessage ?: "Firestore token güncellemesi başarısız."
        Log.d(TAG, "FCM token Firestore sync skipped: ${err?.message}")
        return false
      }
    }
    return false
  }
}
