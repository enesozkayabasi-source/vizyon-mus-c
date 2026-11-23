package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppearancePreferences
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores Vizyon background notification scheduling and foreground sync listener
 * automatically after device reboot or app updates.
 */
class VizyonNotificationRestartReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (action != Intent.ACTION_BOOT_COMPLETED &&
      action != Intent.ACTION_MY_PACKAGE_REPLACED &&
      action != "android.intent.action.QUICKBOOT_POWERON"
    ) {
      return
    }

    val pendingResult = goAsync()
    val appCtx = context.applicationContext
    CoroutineScope(Dispatchers.IO).launch {
      try {
        if (FirebaseApp.getApps(appCtx).isEmpty()) {
          runCatching { FirebaseApp.initializeApp(appCtx) }
        }
        VizyonNotificationHelper.ensureChannel(appCtx)
        BackgroundNotificationJobService.schedule(appCtx)
        val config = runCatching { AppearancePreferences(appCtx).configFlow.first() }.getOrNull()
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (!uid.isNullOrBlank() && config != null) {
          VizyonRealtimeNotificationService.syncServiceState(
            context = appCtx,
            uid = uid,
            notificationsEnabled = config.notificationsEnabled,
            backgroundNotifServiceEnabled = config.backgroundNotifServiceEnabled
          )
        }
      } finally {
        runCatching { pendingResult.finish() }
      }
    }
  }
}
