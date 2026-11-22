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
import com.example.model.UserNotification
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Lightweight Android JobScheduler service (API 24+ compatible, zero extra dependencies)
 * that periodically fetches unread cloud notifications even if the app process was swiped
 * away or paused in background on older devices.
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
          FirebaseApp.initializeApp(ctx)
        }
        val uid = Firebase.auth.currentUser?.uid
        if (uid.isNullOrBlank()) return@launch
        if (!VizyonNotificationHelper.hasNotificationPermission(ctx)) return@launch

        val config = runCatching { AppearancePreferences(ctx).configFlow.first() }.getOrNull()
        if (config?.notificationsEnabled == false) return@launch

        val databaseId = getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        val snapshot =
          db.collection("users")
            .document(uid)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .await()

        val allRecent =
          snapshot.documents
            .mapNotNull { doc ->
              doc.toObject(UserNotification::class.java)?.let {
                if (it.id.isBlank()) it.copy(id = doc.id) else it
              }
            }
        val unread =
          allRecent
            .filter { !it.read }
            .sortedBy { it.createdAt?.toDate()?.time ?: 0L }

        if (allRecent.isEmpty()) return@launch

        if (!NotificationDispatchGuard.isBaselineInitialized(ctx, uid)) {
          NotificationDispatchGuard.markBaselineSeen(ctx, uid, allRecent.map { it.id })
          return@launch
        }

        if (unread.isEmpty()) return@launch

        val unseenIds =
          NotificationDispatchGuard.filterAndMarkUnseen(ctx, uid, unread.map { it.id }).toSet()
        unread
          .filter { it.id in unseenIds }
          .forEach { notif ->
            VizyonNotificationHelper.postSocialNotificationSync(ctx, notif)
          }
      } catch (_: Throwable) {
        // Silent resilience in background job
      } finally {
        jobFinished(params, false)
      }
    }
    return true
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          builder.setRequiresBatteryNotLow(false)
        }
        scheduler.schedule(builder.build())
      }
    }
  }
}
