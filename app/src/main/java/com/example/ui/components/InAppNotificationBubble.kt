package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.NotificationType
import com.example.model.UserNotification
import com.example.notifications.VizyonNotificationHelper
import kotlinx.coroutines.delay

/**
 * Floating top pill notification bubble shown silently only while the user is inside the app
 * (suppressed when already chatting with the sender). Shows only the sender's display name.
 */
@Composable
fun InAppNotificationBubble(
  notification: UserNotification?,
  onOpen: (UserNotification) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  LaunchedEffect(notification?.id) {
    if (notification != null) {
      delay(4200L)
      onDismiss()
    }
  }

  Box(
    contentAlignment = Alignment.TopCenter,
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    AnimatedVisibility(
      visible = notification != null,
      enter =
        slideInVertically(
          animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
          initialOffsetY = { -it * 2 }
        ) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { -it * 2 }) + fadeOut()
    ) {
      notification?.let { notif ->
        val nType = NotificationType.fromRaw(notif.type)
        val displayName = VizyonNotificationHelper.cleanDisplayNameOnly(notif.senderName)
        val cleanPreview = VizyonNotificationHelper.sanitizeNotificationMessage(notif)
        val badgeIcon =
          when (nType) {
            NotificationType.LIKE -> Icons.Rounded.Favorite
            NotificationType.COMMENT -> Icons.Rounded.ChatBubble
            NotificationType.REPOST -> Icons.Rounded.Repeat
            NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST -> Icons.Rounded.PersonAdd
            NotificationType.STORY -> Icons.Rounded.AutoAwesome
            NotificationType.VOICE_MESSAGE -> Icons.Rounded.GraphicEq
            NotificationType.MESSAGE, NotificationType.MESSAGE_REQUEST -> Icons.Rounded.ChatBubble
          }
        val badgeColor =
          when (nType) {
            NotificationType.LIKE -> MaterialTheme.colorScheme.secondary
            NotificationType.REPOST, NotificationType.STORY -> MaterialTheme.colorScheme.tertiary
            NotificationType.MESSAGE,
            NotificationType.VOICE_MESSAGE,
            NotificationType.MESSAGE_REQUEST -> MaterialTheme.colorScheme.secondary
            NotificationType.COMMENT,
            NotificationType.FOLLOW,
            NotificationType.FOLLOW_REQUEST -> MaterialTheme.colorScheme.primary
          }

        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
          shadowElevation = 14.dp,
          modifier =
            Modifier
              .widthIn(max = 520.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(999.dp))
              .border(
                width = 1.2.dp,
                brush =
                  Brush.horizontalGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                      MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                    )
                  ),
                shape = RoundedCornerShape(999.dp)
              )
              .pointerInput(notif.id) {
                detectVerticalDragGestures { _, dragAmount ->
                  if (dragAmount < -18f) onDismiss()
                }
              }
              .clickable { onOpen(notif) }
              .testTag("in_app_notification_bubble")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
          ) {
            Box(contentAlignment = Alignment.BottomEnd) {
              StudioAvatar(
                photoUrlOrPath = notif.senderAvatarUrl,
                contentDescription = displayName,
                size = 40.dp,
                ringColor = badgeColor
              )
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
              ) {
                Icon(
                  imageVector = badgeIcon,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(10.dp)
                )
              }
            }

            Column(
              verticalArrangement = Arrangement.spacedBy(2.dp),
              modifier = Modifier.weight(1f)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                  color = badgeColor.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(999.dp)
                ) {
                  Text(
                    text = nType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text =
                  if (notif.trackTitle.isNotBlank() && nType == NotificationType.MESSAGE) {
                    "♪ ${notif.trackTitle} • $cleanPreview"
                  } else {
                    cleanPreview
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(48.dp).testTag("in_app_notification_dismiss_btn")
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Baloncuğu kapat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }
    }
  }
}
