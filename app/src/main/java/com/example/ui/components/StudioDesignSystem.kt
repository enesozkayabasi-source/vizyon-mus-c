package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import java.io.File

@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1f,
    animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
    label = "btn_press_scale"
  )
  return scale
}

/**
 * Primary CTA capsule with luminous studio gradient, glow shadow, and tactile spring press.
 */
@Composable
fun StudioGradientCtaButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  gradientColors: List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary
  ),
  contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberPressScale(interactionSource)
  val shape = RoundedCornerShape(999.dp)
  val effectiveColors =
    if (enabled) gradientColors
    else listOf(
      MaterialTheme.colorScheme.surfaceVariant,
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    )

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .scale(scale)
        .defaultMinSize(minHeight = 48.dp)
        .drawBehind {
          if (enabled) {
            val haloPad = 2.dp.toPx()
            val r = size.height / 2f + haloPad
            drawRoundRect(
              color = gradientColors.first().copy(alpha = 0.20f),
              topLeft = Offset(-haloPad, -haloPad),
              size = Size(size.width + haloPad * 2f, size.height + haloPad * 2f),
              cornerRadius = CornerRadius(r, r),
              style = Stroke(width = 3.5.dp.toPx())
            )
          }
        }
        .clip(shape)
        .background(Brush.horizontalGradient(effectiveColors))
        .border(
          width = 1.dp,
          color = if (enabled) Color.White.copy(alpha = 0.34f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
          shape = shape
        )
        .clickable(
          enabled = enabled && !isLoading,
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        )
        .padding(contentPadding)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (isLoading) {
        CircularProgressIndicator(
          color = Color.White,
          strokeWidth = 2.dp,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
      } else if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Secondary frosted glass pill button with translucent sheen & hairline border.
 */
@Composable
fun StudioGlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  tintColor: Color = MaterialTheme.colorScheme.onSurface,
  borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
  containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 11.dp)
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberPressScale(interactionSource)
  val shape = RoundedCornerShape(999.dp)

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .scale(scale)
        .defaultMinSize(minHeight = 48.dp)
        .clip(shape)
        .background(
          Brush.linearGradient(
            listOf(
              containerColor,
              tintColor.copy(alpha = 0.08f)
            )
          )
        )
        .border(1.dp, if (enabled) borderColor else borderColor.copy(alpha = 0.3f), shape)
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        )
        .padding(contentPadding)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (enabled) tintColor else tintColor.copy(alpha = 0.45f),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) tintColor else tintColor.copy(alpha = 0.45f),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Circular frosted glass icon button for headers and toolbars, supporting an unread counter badge.
 */
@Composable
fun StudioCircleIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  highlighted: Boolean = false,
  badgeCount: Int = 0,
  tint: Color = MaterialTheme.colorScheme.primary
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberPressScale(interactionSource)

  Box(modifier = modifier.scale(scale)) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(size)
          .clip(CircleShape)
          .background(
            if (highlighted) tint.copy(alpha = 0.20f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
          )
          .border(
            width = 1.dp,
            color = if (highlighted) tint.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.20f),
            shape = CircleShape
          )
          .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
          )
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(21.dp)
      )
    }
    if (badgeCount > 0) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .offset(x = 3.dp, y = (-3).dp)
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error)
            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
            .padding(horizontal = 4.dp)
      ) {
        Text(
          text = if (badgeCount > 9) "9+" else badgeCount.toString(),
          style = MaterialTheme.typography.labelSmall,
          color = Color.White,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * Animated stateful Follow / Following toggle pill.
 */
@Composable
fun StudioFollowToggleButton(
  isFollowing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  followLabel: String = "Takip Et",
  followingLabel: String = "Takipte"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberPressScale(interactionSource)
  val bg by animateColorAsState(
    targetValue =
      if (isFollowing) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
      else MaterialTheme.colorScheme.primary,
    label = "follow_pill_bg"
  )
  val border by animateColorAsState(
    targetValue =
      if (isFollowing) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
      else Color.White.copy(alpha = 0.30f),
    label = "follow_pill_border"
  )
  val contentColor = if (isFollowing) MaterialTheme.colorScheme.primary else Color.White
  val shape = RoundedCornerShape(999.dp)

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .scale(scale)
        .defaultMinSize(minHeight = 40.dp)
        .clip(shape)
        .background(bg)
        .border(1.dp, border, shape)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        )
        .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
      Icon(
        imageVector = if (isFollowing) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (isFollowing) followingLabel else followLabel,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

/**
 * Interactive frosted glass chip for categories, quick queries, and filters.
 */
@Composable
fun StudioInteractiveChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector? = null
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale = rememberPressScale(interactionSource)
  val shape = RoundedCornerShape(999.dp)
  val bgBrush =
    if (selected) {
      Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
      )
    } else {
      Brush.horizontalGradient(
        listOf(
          MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
          MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
      )
    }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .scale(scale)
        .clip(shape)
        .background(bgBrush)
        .border(
          width = 1.dp,
          color = if (selected) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
          shape = shape
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        )
        .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = if (selected) Color.White else MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false
      )
    }
  }
}

/**
 * Animated heart favorite toggle button.
 */
@Composable
fun StudioFavoriteIconButton(
  isFavorite: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val scale by animateFloatAsState(
    targetValue = if (isFavorite) 1.12f else 1f,
    animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
    label = "fav_heart_scale"
  )
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .size(42.dp)
        .scale(scale)
        .clip(CircleShape)
        .background(
          if (isFavorite) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
          else Color.Transparent
        )
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick
        )
  ) {
    Icon(
      imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
      contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
      tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(21.dp)
    )
  }
}

/**
 * Resolves compressed base64 data-URLs, HTTPS URLs, or local avatar paths with a luminous circular frame.
 */
@Composable
fun StudioAvatar(
  photoUrlOrPath: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  ringColor: Color = MaterialTheme.colorScheme.primary
) {
  val dataUrlBitmap =
    remember(photoUrlOrPath) {
      if (photoUrlOrPath.startsWith("data:image/")) {
        com.example.security.PasscodeSecurityHelper.decodeDataUrlBitmap(photoUrlOrPath)
      } else null
    }

  if (dataUrlBitmap != null) {
    androidx.compose.foundation.Image(
      bitmap = dataUrlBitmap,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier =
        modifier
          .size(size)
          .clip(CircleShape)
          .border(1.5.dp, ringColor.copy(alpha = 0.65f), CircleShape)
    )
    return
  }

  val modelData: Any? =
    remember(photoUrlOrPath) {
      when {
        photoUrlOrPath.isBlank() -> null
        photoUrlOrPath.startsWith("http://") ||
          photoUrlOrPath.startsWith("https://") ||
          photoUrlOrPath.startsWith("content://") ||
          photoUrlOrPath.startsWith("file://") -> photoUrlOrPath
        else -> {
          val file = File(photoUrlOrPath)
          if (file.exists()) file else null
        }
      }
    }

  AsyncImage(
    model = modelData,
    contentDescription = contentDescription,
    placeholder = painterResource(R.drawable.ic_vizyon_logo),
    error = painterResource(R.drawable.ic_vizyon_logo),
    contentScale = ContentScale.Crop,
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .border(1.5.dp, ringColor.copy(alpha = 0.65f), CircleShape)
  )
}

@Composable
fun StudioSectionHeader(
  title: String,
  subtitle: String? = null,
  badge: String? = null,
  modifier: Modifier = Modifier
) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier.fillMaxWidth()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      if (!badge.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = badge,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
          )
        }
      }
    }
    if (!subtitle.isNullOrBlank()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
