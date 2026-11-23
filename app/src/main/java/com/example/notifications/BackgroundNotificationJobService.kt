package com.example.notifications

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Persisted periodic Android JobScheduler fallback (API 24+ compatible)
 * that fetches unread cloud notifications and pending requests from Firestore server
 * even if the app process was swiped away or paused by battery optimization.
 */
class BackgroundNotificationJobService : JobService() {
  private var serviceScope: CoroutineScope? = null

  override fun onStartJob(params: JobParameters?): Boolean {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    serviceScope = scope
    scope.launch {
      try {
        val ctx = applicationContext
        if (FirebaseApp.getApps(ctx).isEmpty()) {
          runCatching { FirebaseApp.initializeApp(ctx) }
        }
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (uid.isNullOrBlank()) return@launch
        if (!VizyonNotificationHelper.hasNotificationPermission(ctx)) return@launch

        val config = runCatching { AppearancePreferences(ctx).configFlow.first() }.getOrNull()
        if (config?.notificationsEnabled == false) return@launch

        val databaseId = getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val userDoc = db.collection("users").document(uid)

        val notifQuery =
          userDoc.collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
        val snapshot =
          runCatching { notifQuery.get(Source.SERVER).await() }
            .getOrElse { notifQuery.get(Source.DEFAULT).await() }

        val allRecent =
          snapshot.documents.mapNotNull { doc ->
            doc.toObject(UserNotification::class.java)?.let {
              if (it.id.isBlank()) it.copy(id = doc.id) else it
            }
          }
        if (allRecent.isNotEmpty()) {
          if (!NotificationDispatchGuard.isBaselineInitialized(ctx, uid)) {
            NotificationDispatchGuard.markBaselineSeen(ctx, uid, allRecent.map { it.id })
          } else {
            val unread =
              allRecent
                .filter { !it.read }
                .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
            val unseenIds =
              NotificationDispatchGuard.filterAndMarkUnseen(ctx, uid, unread.map { it.id }).toSet()
            for (notif in unread.filter { it.id in unseenIds }) {
              VizyonNotificationHelper.postSocialNotificationSuspend(ctx, notif)
            }
          }
        }

        // Also check pending follow & message requests in case a request wasn't paired with a notif doc
        val followQuery =
          userDoc.collection("follow_requests").whereEqualTo("status", RequestStatus.PENDING.name)
        val followSnap =
          runCatching { followQuery.get(Source.SERVER).await() }
            .getOrElse { followQuery.get(Source.DEFAULT).await() }
        val followReqs =
          followSnap.documents.mapNotNull { doc ->
            doc.toObject(FollowRequest::class.java)?.let {
              if (it.id.isBlank()) it.copy(id = doc.id) else it
            }
          }.map { req ->
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
        dispatchSyntheticBatch(ctx, uid + "_req_follow", followReqs)

        val msgReqQuery =
          userDoc.collection("message_requests").whereEqualTo("status", RequestStatus.PENDING.name)
        val msgReqSnap =
          runCatching { msgReqQuery.get(Source.SERVER).await() }
            .getOrElse { msgReqQuery.get(Source.DEFAULT).await() }
        val msgReqs =
          msgReqSnap.documents.mapNotNull { doc ->
            doc.toObject(MessageRequest::class.java)?.let {
              if (it.id.isBlank()) it.copy(id = doc.id) else it
            }
          }.map { req ->
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
        dispatchSyntheticBatch(ctx, uid + "_req_msg", msgReqs)
      } catch (_: Throwable) {
        // Silent resilience in background job
      } finally {
        jobFinished(params, false)
      }
    }
    return true
  }

  private suspend fun dispatchSyntheticBatch(
    ctx: Context,
    guardKey: String,
    items: List<UserNotification>
  ) {
    if (items.isEmpty()) return
    if (!NotificationDispatchGuard.isBaselineInitialized(ctx, guardKey)) {
      NotificationDispatchGuard.markBaselineSeen(ctx, guardKey, items.map { it.id })
      return
    }
    val unseen = NotificationDispatchGuard.filterAndMarkUnseen(ctx, guardKey, items.map { it.id }).toSet()
    for (notif in items.filter { it.id in unseen }) {
      VizyonNotificationHelper.postSocialNotificationSuspend(ctx, notif)
    }
  }

  override fun onStopJob(params: JobParameters?): Boolean {
    serviceScope?.cancel()
    serviceScope = null
    return true
  }

  companion object {
    private const val JOB_ID = 7042
    private const val PERIODIC_INTERVAL_MS = 15L * 60L * 1000L

    fun schedule(context: Context) {
      val appCtx = context.applicationContext
      runCatching {
        val scheduler = appCtx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
        val alreadyPending =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            scheduler.getPendingJob(JOB_ID) != null
          } else {
            scheduler.allPendingJobs.any { it.id == JOB_ID }
          }
        if (alreadyPending) return

        val builder =
          JobInfo.Builder(JOB_ID, ComponentName(appCtx, BackgroundNotificationJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(PERIODIC_INTERVAL_MS)
            .setPersisted(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          builder.setRequiresBatteryNotLow(false)
        }
        scheduler.schedule(builder.build())
      }
    }
  }
}
