package com.example.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.AppearancePreferences
import com.example.model.FollowRequest
import com.example.model.MessageRequest
import com.example.model.NotificationType
import com.example.model.RequestStatus
import com.example.model.UserNotification
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Play-compliant foreground dataSync service that maintains active Firestore listeners
 * for social notifications, messages, follow requests, and message requests even when
 * the app is backgrounded or swiped from recents.
 */
class VizyonRealtimeNotificationService : Service() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private var notifListener: ListenerRegistration? = null
  private var followReqListener: ListenerRegistration? = null
  private var messageReqListener: ListenerRegistration? = null
  @Volatile private var activeUid: String? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    startForegroundSafely()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForegroundSafely()
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }

    serviceScope.launch {
      if (FirebaseApp.getApps(applicationContext).isEmpty()) {
        runCatching { FirebaseApp.initializeApp(applicationContext) }
      }
      val uid = Firebase.auth.currentUser?.uid
      val config = runCatching { AppearancePreferences(applicationContext).configFlow.first() }.getOrNull()
      val eligible =
        !uid.isNullOrBlank() &&
          config?.notificationsEnabled != false &&
          config?.backgroundNotifServiceEnabled != false &&
          VizyonNotificationHelper.hasNotificationPermission(applicationContext)

      if (!eligible) {
        stopSelf()
        return@launch
      }
      attachListenersIfNeeded(uid!!)
    }
    return START_STICKY
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    BackgroundNotificationJobService.schedule(applicationContext)
    super.onTaskRemoved(rootIntent)
  }

  override fun onDestroy() {
    detachListeners()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun startForegroundSafely() {
    runCatching {
      val notification = VizyonNotificationHelper.buildForegroundSyncNotification(applicationContext)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          VizyonNotificationHelper.BG_SYNC_NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      } else {
        startForeground(VizyonNotificationHelper.BG_SYNC_NOTIFICATION_ID, notification)
      }
    }
  }

  private fun attachListenersIfNeeded(uid: String) {
    if (activeUid == uid && notifListener != null) return
    detachListeners()
    activeUid = uid

    val databaseId = getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(databaseId)
    val userDoc = db.collection("users").document(uid)

    notifListener =
      userDoc.collection("notifications")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .limit(30)
        .addSnapshotListener { snap, error ->
          if (error != null || snap == null) return@addSnapshotListener
          val list =
            snap.documents.mapNotNull { doc ->
              doc.toObject(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
            }
          dispatchUnreadNotifications(uid, list)
        }

    followReqListener =
      userDoc.collection("follow_requests")
        .whereEqualTo("status", RequestStatus.PENDING.name)
        .addSnapshotListener { snap, error ->
          if (error != null || snap == null) return@addSnapshotListener
          val reqs =
            snap.documents.mapNotNull { doc ->
              doc.toObject(FollowRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
            }
          if (reqs.isEmpty()) return@addSnapshotListener
          val synthetic =
            reqs.map { req ->
              UserNotification(
                id = "req_follow_${req.requesterId.ifBlank { req.id }}",
                recipientId = uid,
                senderId = req.requesterId,
                senderName = req.requesterName,
                senderHandle = req.requesterHandle,
                senderAvatarUrl = req.requesterAvatarUrl,
                type = NotificationType.FOLLOW_REQUEST.name,
                message = "Sana takip isteği gönderdi",
                targetType = "REQUESTS",
                targetId = req.requesterId,
                read = false,
                createdAt = req.createdAt,
                updatedAt = req.updatedAt
              )
            }
          dispatchUnreadNotifications(uid, synthetic, keySuffix = "_req_follow")
        }

    messageReqListener =
      userDoc.collection("message_requests")
        .whereEqualTo("status", RequestStatus.PENDING.name)
        .addSnapshotListener { snap, error ->
          if (error != null || snap == null) return@addSnapshotListener
          val reqs =
            snap.documents.mapNotNull { doc ->
              doc.toObject(MessageRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?.let { if (it.id.isBlank()) it.copy(id = doc.id) else it }
            }
          if (reqs.isEmpty()) return@addSnapshotListener
          val synthetic =
            reqs.map { req ->
              UserNotification(
                id = "req_msg_${req.requesterId.ifBlank { req.id }}_${req.updatedAt?.seconds ?: 0L}",
                recipientId = uid,
                senderId = req.requesterId,
                senderName = req.requesterName,
                senderHandle = req.requesterHandle,
                senderAvatarUrl = req.requesterAvatarUrl,
                type = NotificationType.MESSAGE_REQUEST.name,
                message = req.previewText.ifBlank { "Sana bir mesaj isteği gönderdi" },
                targetType = "REQUESTS",
                targetId = req.requesterId,
                read = false,
                createdAt = req.createdAt,
                updatedAt = req.updatedAt
              )
            }
          dispatchUnreadNotifications(uid, synthetic, keySuffix = "_req_msg")
        }
  }

  private fun dispatchUnreadNotifications(
    uid: String,
    items: List<UserNotification>,
    keySuffix: String = ""
  ) {
    if (items.isEmpty()) return
    val key = uid + keySuffix
    val appCtx = applicationContext
    if (!NotificationDispatchGuard.isBaselineInitialized(appCtx, key)) {
      NotificationDispatchGuard.markBaselineSeen(appCtx, key, items.map { it.id })
      return
    }
    val unread = items.filter { !it.read }.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
    if (unread.isEmpty()) return
    val unseenIds = NotificationDispatchGuard.filterAndMarkUnseen(appCtx, key, unread.map { it.id }).toSet()
    val pending = unread.filter { it.id in unseenIds }
    if (pending.isEmpty()) return

    serviceScope.launch {
      pending.forEach { notif ->
        VizyonNotificationHelper.postSocialNotificationSuspend(appCtx, notif)
      }
    }
  }

  private fun detachListeners() {
    runCatching { notifListener?.remove() }
    runCatching { followReqListener?.remove() }
    runCatching { messageReqListener?.remove() }
    notifListener = null
    followReqListener = null
    messageReqListener = null
    activeUid = null
  }

  companion object {
    private const val ACTION_START = "com.vizyon.music.action.START_NOTIF_SYNC"
    private const val ACTION_STOP = "com.vizyon.music.action.STOP_NOTIF_SYNC"

    fun syncServiceState(
      context: Context,
      uid: String?,
      notificationsEnabled: Boolean,
      backgroundNotifServiceEnabled: Boolean
    ) {
      val appCtx = context.applicationContext
      val shouldRun =
        !uid.isNullOrBlank() &&
          notificationsEnabled &&
          backgroundNotifServiceEnabled &&
          VizyonNotificationHelper.hasNotificationPermission(appCtx)
      if (!shouldRun) {
        stop(appCtx)
        return
      }
      BackgroundNotificationJobService.schedule(appCtx)
      runCatching {
        val intent = Intent(appCtx, VizyonRealtimeNotificationService::class.java).setAction(ACTION_START)
        ContextCompat.startForegroundService(appCtx, intent)
      }.onFailure {
        // Fallback smoothly on API 34+ restricted background starts without crashing
        BackgroundNotificationJobService.schedule(appCtx)
      }
    }

    fun stop(context: Context) {
      val appCtx = context.applicationContext
      runCatching {
        appCtx.stopService(Intent(appCtx, VizyonRealtimeNotificationService::class.java))
      }
    }
  }
}
