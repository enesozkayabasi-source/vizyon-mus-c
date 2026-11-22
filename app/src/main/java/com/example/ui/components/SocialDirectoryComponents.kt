package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.PublicProfile

@Composable
fun ListenerDirectoryCard(
  person: PublicProfile,
  isFollowing: Boolean,
  onOpenProfile: () -> Unit,
  onToggleFollow: () -> Unit,
  onOpenChat: () -> Unit = {}
) {
  val lastTrackTitle = person.lastTrackTitle.ifBlank { person.currentTrackTitle }
  val hasRecentActivity = lastTrackTitle.isNotBlank() || person.recentTracks.isNotEmpty()
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.985f else 1f,
    animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
    label = "listener_card_scale"
  )

  GlassStudioCard(
    highlighted = hasRecentActivity,
    cornerRadius = 22.dp,
    onClick = onOpenProfile,
    modifier = Modifier.scale(scale).testTag("listener_card_${person.userId}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.padding(14.dp)
    ) {
      Box(contentAlignment = Alignment.BottomEnd) {
        StudioAvatar(
          photoUrlOrPath = person.photoUrl,
          contentDescription = person.displayName,
          size = 50.dp,
          ringColor =
            if (hasRecentActivity) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primary
        )
        if (hasRecentActivity) {
          Box(
            modifier =
              Modifier.size(13.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
          )
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = person.displayName.ifBlank { "Vizyon Dinleyicisi" },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${person.handle.ifBlank { "@vizyon" }} • ${person.followerCount} takipçi",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1
        )
        if (person.bio.isNotBlank()) {
          Text(
            text = person.bio,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        } else {
          Text(
            text = "Son 5 şarkı ve mesaj için profile dokun",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
          )
        }
      }
      StudioCircleIconButton(
        icon = Icons.Rounded.ChatBubbleOutline,
        contentDescription = "Mesaj gönder",
        onClick = onOpenChat,
        size = 48.dp,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.testTag("chat_user_btn_${person.userId}")
      )
      StudioFollowToggleButton(
        isFollowing = isFollowing,
        onClick = onToggleFollow,
        modifier = Modifier.testTag("follow_user_btn_${person.userId}")
      )
    }
  }
}

@Composable
fun CloudSignInPromptCard(onOpenStudioForCloudSync: () -> Unit) {
  GlassStudioCard(
    highlighted = true,
    cornerRadius = 20.dp,
    onClick = onOpenStudioForCloudSync
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.padding(14.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(38.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        ) {
          Icon(
            Icons.Rounded.Groups,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Diğer dinleyicileri takip etmek, son dinlediklerini görmek ve gerçek zamanlı bildirimler almak için Google ile kayıt olabilirsin.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
      StudioGradientCtaButton(
        text = "Profil'den Google ile Kayıt Ol",
        onClick = onOpenStudioForCloudSync,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

@Composable
fun EmptySocialCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null
) {
  GlassStudioCard(cornerRadius = 22.dp) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(56.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                  MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                )
              )
            )
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
      ) {
        Icon(
          icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp)
        )
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
      if (!actionLabel.isNullOrBlank() && onAction != null) {
        StudioGradientCtaButton(
          text = actionLabel,
          onClick = onAction
        )
      }
    }
  }
}
