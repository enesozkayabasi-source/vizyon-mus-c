package com.example.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.model.NotificationType
import com.example.model.UserNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object VizyonNotificationHelper {
  const val CHANNEL_ID = "vizyon_social_channel_v6"
  const val CHANNEL_FOLLOWS = "vizyon_social_follows_v6"
  const val CHANNEL_REACTIONS = "vizyon_social_reactions_v6"
  const val CHANNEL_STORIES = "vizyon_social_stories_v6"
  const val CHANNEL_MESSAGES = "vizyon_social_messages_v6"
  const val CHANNEL_BG_SYNC_STATUS = "vizyon_bg_sync_status_v1"

  const val BG_SYNC_NOTIFICATION_ID = 9001

  private val LEGACY_CHANNEL_IDS = listOf(
    "vizyon_social_channel",
    "vizyon_social_follows",
    "vizyon_social_reactions",
    "vizyon_social_stories",
    "vizyon_social_messages",
    "vizyon_social_channel_v2",
    "vizyon_social_follows_v2",
    "vizyon_social_reactions_v2",
    "vizyon_social_stories_v2",
    "vizyon_social_messages_v2",
    "vizyon_social_channel_v3",
    "vizyon_social_follows_v3",
    "vizyon_social_reactions_v3",
    "vizyon_social_stories_v3",
    "vizyon_social_messages_v3",
    "vizyon_social_channel_v4",
    "vizyon_social_follows_v4",
    "vizyon_social_reactions_v4",
    "vizyon_social_stories_v4",
    "vizyon_social_messages_v4",
    "vizyon_social_channel_v5",
    "vizyon_social_follows_v5",
    "vizyon_social_reactions_v5",
    "vizyon_social_stories_v5",
    "vizyon_social_messages_v5"
  )

  private const val CHANNEL_GROUP_SOCIAL = "vizyon_group_social"

  const val EXTRA_NOTIF_ID = "extra_notif_id"
  const val EXTRA_NOTIF_TARGET_TYPE = "extra_notif_target_type"
  const val EXTRA_NOTIF_TARGET_ID = "extra_notif_target_id"
  const val EXTRA_NOTIF_TRACK_ID = "extra_notif_track_id"
  const val EXTRA_NOTIF_SENDER_ID = "extra_notif_sender_id"
  const val EXTRA_NOTIF_TYPE = "extra_notif_type"

  private val postMutex = Mutex()
  @Volatile private var lastPostedWallClockMs: Long = 0L
  private val notifScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val clockFormatter = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
  }

  private fun customSoundUri(context: Context): Uri {
    val hasRawChime =
      runCatching {
        context.resources.openRawResourceFd(R.raw.vizyon_notify_chime)?.use { true } ?: false
      }.getOrDefault(false)
    return if (hasRawChime) {
      Uri.parse("android.resource://${context.packageName}/${R.raw.vizyon_notify_chime}")
    } else {
      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
  }

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java) ?: return
      runCatching {
        manager.createNotificationChannelGroup(
          NotificationChannelGroup(CHANNEL_GROUP_SOCIAL, "Vizyon Sosyal Topluluk")
        )
      }
      LEGACY_CHANNEL_IDS.forEach { oldId ->
        runCatching { manager.deleteNotificationChannel(oldId) }
      }
      val chimeUri = customSoundUri(context)
      val audioAttrs =
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_NOTIFICATION)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()

      val channels = listOf(
        NotificationChannel(
          CHANNEL_ID,
          "Genel Sosyal Bildirimler",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          group = CHANNEL_GROUP_SOCIAL
          description = "Genel dinleyici etkileşimleri"
          setSound(chimeUri, audioAttrs)
          enableVibration(true)
          setShowBadge(true)
        },
        NotificationChannel(
          CHANNEL_FOLLOWS,
          "Takipçiler ve İstekler",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          group = CHANNEL_GROUP_SOCIAL
          description = "Seni takip edenler ve gelen takip istekleri"
          setSound(chimeUri, audioAttrs)
          enableVibration(true)
          setShowBadge(true)
        },
        NotificationChannel(
          CHANNEL_REACTIONS,
          "Beğeni, Yorum ve Paylaşımlar",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          group = CHANNEL_GROUP_SOCIAL
          description = "Paylaşımlarına gelen beğeni, yorum ve yeniden paylaşımlar"
          setSound(chimeUri, audioAttrs)
          enableVibration(true)
          setShowBadge(true)
        },
        NotificationChannel(
          CHANNEL_STORIES,
          "Müzik Hikayeleri",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          group = CHANNEL_GROUP_SOCIAL
          description = "Takip ettiğin dinleyicilerin paylaştığı yeni hikaye kartları"
          setSound(chimeUri, audioAttrs)
          enableVibration(true)
          setShowBadge(true)
        },
        NotificationChannel(
          CHANNEL_MESSAGES,
          "Doğrudan ve Sesli Mesajlar",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          group = CHANNEL_GROUP_SOCIAL
          description = "Dinleyicilerden gelen metin, şarkı, sesli mesaj ve mesaj istekleri"
          setSound(chimeUri, audioAttrs)
          enableVibration(true)
          setShowBadge(true)
        },
        NotificationChannel(
          CHANNEL_BG_SYNC_STATUS,
          "Arka Plan Bildirim Bağlantısı",
          NotificationManager.IMPORTANCE_LOW
        ).apply {
          description = "Uygulama kapalıyken anlık mesaj ve istek bildirimlerini sessizce dinler"
          setSound(null, null)
          enableVibration(false)
          setShowBadge(false)
        }
      )
      channels.forEach { ch ->
        if (manager.getNotificationChannel(ch.id) == null) {
          runCatching { manager.createNotificationChannel(ch) }
        }
      }
    }
  }

  fun buildForegroundSyncNotification(context: Context): Notification {
    val appCtx = context.applicationContext
    ensureChannel(appCtx)
    val openIntent =
      Intent(appCtx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        appCtx,
        BG_SYNC_NOTIFICATION_ID,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    return NotificationCompat.Builder(appCtx, CHANNEL_BG_SYNC_STATUS)
      .setSmallIcon(R.drawable.ic_notif_vizyon)
      .setContentTitle("Vizyon Music")
      .setContentText("Anlık mesaj ve sosyal bildirim bağlantısı etkin")
      .setOngoing(true)
      .setSilent(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setContentIntent(pendingIntent)
      .build()
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun cleanDisplayNameOnly(senderName: String): String {
    val cleaned =
      senderName
        .replace(Regex("\\s*\\(@[^)]*\\)"), "")
        .replace(Regex("@[A-Za-z0-9_.-]+"), "")
        .trim()
    return cleaned.ifBlank { "Vizyon Dinleyicisi" }
  }

  fun sanitizeNotificationMessage(notification: UserNotification): String {
    val displayName = cleanDisplayNameOnly(notification.senderName)
    var body =
      notification.message
        .replace(Regex("\\s*\\(@[^)]*\\)"), "")
        .trim()

    if (notification.senderHandle.isNotBlank()) {
      body = body.replace(notification.senderHandle, "").trim()
    }
    body = body.replace(Regex("\\s{2,}"), " ").trim()

    val colonPrefix = "$displayName:"
    val commaPrefix = "$displayName,"
    body =
      when {
        body.startsWith(colonPrefix, ignoreCase = true) ->
          body.removeRange(0, colonPrefix.length).trim()
        body.startsWith(commaPrefix, ignoreCase = true) ->
          body.removeRange(0, commaPrefix.length).trim().replaceFirstChar { it.uppercase() }
        body.startsWith("$displayName ", ignoreCase = true) ->
          body.removeRange(0, displayName.length).trim().replaceFirstChar { it.uppercase() }
        else -> body
      }

    if (body.isBlank()) {
      val type = NotificationType.fromRaw(notification.type)
      body =
        when (type) {
          NotificationType.FOLLOW -> "Seni takip etmeye başladı"
          NotificationType.FOLLOW_REQUEST -> "Sana takip isteği gönderdi"
          NotificationType.LIKE -> "Paylaştığın parçayı beğendi"
          NotificationType.COMMENT -> "Paylaşımına yorum yaptı"
          NotificationType.REPOST -> "Paylaşımını yeniden paylaştı"
          NotificationType.STORY -> "Yeni bir müzik hikayesi paylaştı"
          NotificationType.MESSAGE_REQUEST -> "Sana bir mesaj isteği gönderdi"
          NotificationType.VOICE_MESSAGE -> "Sana bir sesli mesaj gönderdi"
          NotificationType.MESSAGE ->
            if (notification.trackTitle.isNotBlank()) "Sana bir şarkı önerdi"
            else "Sana yeni bir mesaj gönderdi"
        }
    }
    return body
  }

  fun notificationSystemId(notificationId: String): Int =
    abs(notificationId.hashCode()).coerceAtLeast(100)

  fun cancelSystemNotification(context: Context, notificationId: String) {
    if (notificationId.isBlank()) return
    runCatching {
      NotificationManagerCompat.from(context.applicationContext)
        .cancel(notificationSystemId(notificationId))
    }
  }

  fun cancelAllSystemNotifications(context: Context) {
    runCatching {
      NotificationManagerCompat.from(context.applicationContext).cancelAll()
    }
  }

  fun postSocialNotification(context: Context, notification: UserNotification) {
    val appCtx = context.applicationContext
    if (!hasNotificationPermission(appCtx)) return
    ensureChannel(appCtx)

    notifScope.launch {
      postSocialNotificationSuspend(appCtx, notification)
    }
  }

  suspend fun postSocialNotificationSuspend(context: Context, notification: UserNotification) {
    val appCtx = context.applicationContext
    if (!hasNotificationPermission(appCtx)) return
    ensureChannel(appCtx)

    // Inter-alert cooldown of 1150ms prevents Android AudioFlinger / NotificationManager
    // from throttling or muting alert sound & banners after 3 rapid consecutive notifications.
    postMutex.withLock {
      val elapsed = System.currentTimeMillis() - lastPostedWallClockMs
      if (elapsed in 0L until 1150L) {
        delay(1150L - elapsed)
      }
      postSocialNotificationInternal(appCtx, notification)
      lastPostedWallClockMs = System.currentTimeMillis()
    }
  }

  fun postSocialNotificationSync(context: Context, notification: UserNotification) {
    val appCtx = context.applicationContext
    if (!hasNotificationPermission(appCtx)) return
    ensureChannel(appCtx)
    runBlocking {
      postSocialNotificationSuspend(appCtx, notification)
    }
  }

  private fun postSocialNotificationInternal(appCtx: Context, notification: UserNotification) {
    val type = NotificationType.fromRaw(notification.type)
    val channelId =
      when (type) {
        NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST -> CHANNEL_FOLLOWS
        NotificationType.STORY -> CHANNEL_STORIES
        NotificationType.MESSAGE,
        NotificationType.VOICE_MESSAGE,
        NotificationType.MESSAGE_REQUEST -> CHANNEL_MESSAGES
        NotificationType.LIKE, NotificationType.COMMENT, NotificationType.REPOST -> CHANNEL_REACTIONS
      }

    val displayNameOnly = cleanDisplayNameOnly(notification.senderName)
    val cleanBody = sanitizeNotificationMessage(notification)
    val badgeLabel = type.label
    val timestampMs = notification.createdAt?.toDate()?.time ?: System.currentTimeMillis()
    val clockLabel =
      runCatching { clockFormatter.get()?.format(Date(timestampMs)).orEmpty() }.getOrDefault("")

    val resolvedTargetType =
      notification.targetType.ifBlank {
        when (type) {
          NotificationType.FOLLOW -> "PROFILE"
          NotificationType.FOLLOW_REQUEST, NotificationType.MESSAGE_REQUEST -> "REQUESTS"
          NotificationType.COMMENT, NotificationType.REPOST, NotificationType.LIKE -> "POST"
          NotificationType.STORY -> "STORY"
          NotificationType.MESSAGE, NotificationType.VOICE_MESSAGE -> "CHAT"
        }
      }

    val uniqueNotifId =
      notificationSystemId(notification.id.ifBlank { "${notification.senderId}_${System.nanoTime()}" })

    val intent =
      Intent(appCtx, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_NOTIF_ID, notification.id)
        putExtra(EXTRA_NOTIF_TARGET_TYPE, resolvedTargetType)
        putExtra(EXTRA_NOTIF_TARGET_ID, notification.targetId)
        putExtra(EXTRA_NOTIF_TRACK_ID, notification.trackId)
        putExtra(EXTRA_NOTIF_SENDER_ID, notification.senderId)
        putExtra(EXTRA_NOTIF_TYPE, type.name)
      }

    val pendingIntent =
      PendingIntent.getActivity(
        appCtx,
        uniqueNotifId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

    // Keep avatar lightweight (64px) and NEVER use multi-bitmap RemoteViews or group bundling,
    // which triggers Android SystemUI 1MB Binder transaction drops & auto-bundle silencing after #3.
    val avatarBitmap = loadCircularSenderBitmap(notification.senderAvatarUrl, displayNameOnly, sizePx = 64)
    val isMessagingCategory =
      type == NotificationType.MESSAGE ||
        type == NotificationType.VOICE_MESSAGE ||
        type == NotificationType.MESSAGE_REQUEST

    val builder =
      NotificationCompat.Builder(appCtx, channelId)
        .setSmallIcon(R.drawable.ic_notif_vizyon)
        .setLargeIcon(avatarBitmap)
        .setContentTitle(displayNameOnly)
        .setContentText(cleanBody)
        .setSubText(if (clockLabel.isNotBlank()) "$badgeLabel • $clockLabel" else badgeLabel)
        .setSound(customSoundUri(appCtx))
        .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(
          if (isMessagingCategory) NotificationCompat.CATEGORY_MESSAGE
          else NotificationCompat.CATEGORY_SOCIAL
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOnlyAlertOnce(false)
        .setWhen(timestampMs)
        .setShowWhen(true)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    if (isMessagingCategory) {
      val mePerson = Person.Builder().setName("Sen").build()
      val senderPerson =
        Person.Builder()
          .setName(displayNameOnly)
          .setIcon(IconCompat.createWithBitmap(avatarBitmap))
          .build()
      val msgText =
        if (notification.trackTitle.isNotBlank()) "$cleanBody (♪ ${notification.trackTitle})"
        else cleanBody
      builder.setStyle(
        NotificationCompat.MessagingStyle(mePerson)
          .addMessage(msgText, timestampMs, senderPerson)
      )
    } else {
      val bigText =
        if (notification.trackTitle.isNotBlank()) "$cleanBody\n♪ ${notification.trackTitle}"
        else cleanBody
      builder.setStyle(
        NotificationCompat.BigTextStyle()
          .setBigContentTitle(displayNameOnly)
          .bigText(bigText)
          .setSummaryText(badgeLabel)
      )
    }

    runCatching {
      val manager = NotificationManagerCompat.from(appCtx)
      manager.notify(uniqueNotifId, builder.build())
    }
  }

  private fun loadCircularSenderBitmap(
    avatarUrlOrData: String,
    displayName: String,
    sizePx: Int = 64
  ): Bitmap {
    val rawBitmap =
      try {
        when {
          avatarUrlOrData.startsWith("data:image", ignoreCase = true) -> {
            val base64Part = avatarUrlOrData.substringAfter("base64,", "")
            if (base64Part.isNotBlank()) {
              val bytes = Base64.decode(base64Part, Base64.DEFAULT)
              decodeSampledBytes(bytes, sizePx)
            } else null
          }
          avatarUrlOrData.startsWith("http://", ignoreCase = true) ||
            avatarUrlOrData.startsWith("https://", ignoreCase = true) -> {
            val conn = (URL(avatarUrlOrData).openConnection() as HttpURLConnection).apply {
              connectTimeout = 1500
              readTimeout = 1500
              doInput = true
            }
            val bytes = conn.inputStream.use { it.readBytes() }
            decodeSampledBytes(bytes, sizePx)
          }
          avatarUrlOrData.isNotBlank() -> {
            val file = File(avatarUrlOrData)
            if (file.exists()) {
              val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
              BitmapFactory.decodeFile(file.absolutePath, bounds)
              val opts = BitmapFactory.Options().apply {
                inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, sizePx)
                inPreferredConfig = Bitmap.Config.RGB_565
              }
              BitmapFactory.decodeFile(file.absolutePath, opts)
            } else null
          }
          else -> null
        }
      } catch (_: Throwable) {
        null
      }

    return if (rawBitmap != null && !rawBitmap.isRecycled && rawBitmap.width > 0 && rawBitmap.height > 0) {
      try {
        cropToCircle(rawBitmap, sizePx)
      } catch (_: Throwable) {
        createMonogramAvatarBitmap(displayName, sizePx)
      }
    } else {
      createMonogramAvatarBitmap(displayName, sizePx)
    }
  }

  private fun decodeSampledBytes(bytes: ByteArray, targetPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val opts = BitmapFactory.Options().apply {
      inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, targetPx)
      inPreferredConfig = Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
  }

  private fun computeSampleSize(width: Int, height: Int, targetPx: Int): Int {
    var sample = 1
    while (width / (sample * 2) >= targetPx && height / (sample * 2) >= targetPx) {
      sample *= 2
    }
    return sample
  }

  private fun cropToCircle(source: Bitmap, sizePx: Int): Bitmap {
    val scaled = Bitmap.createScaledBitmap(source, sizePx, sizePx, true)
    val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaled, Rect(0, 0, sizePx, sizePx), Rect(0, 0, sizePx, sizePx), paint)
    return output
  }

  private fun createMonogramAvatarBitmap(displayName: String, sizePx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = sizePx / 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      shader =
        LinearGradient(
          0f,
          0f,
          sizePx.toFloat(),
          sizePx.toFloat(),
          Color.parseColor("#2979FF"),
          Color.parseColor("#38BDF8"),
          Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(radius, radius, radius, bgPaint)

    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = sizePx * 0.44f
      textAlign = Paint.Align.CENTER
      isFakeBoldText = true
    }
    val textBounds = Rect()
    textPaint.getTextBounds(initial, 0, initial.length, textBounds)
    val y = radius - textBounds.exactCenterY()
    canvas.drawText(initial, radius, y, textPaint)
    return bitmap
  }
}
