package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ActivityActionType
import com.example.model.ActivityFeedItem
import com.example.model.ArtistSummary
import com.example.model.MusicStoryItem
import com.example.model.MusicTrack
import com.example.model.PublicProfile
import com.example.model.StoryCardTheme
import com.example.model.TrackSource
import com.example.model.formatRelativeTimeTr

enum class SocialFeedTab(val title: String) {
  ALL("Akış"),
  FOLLOWING("Takip"),
  LISTENERS("Kişiler"),
  CHATS("Sohbet")
}

@Composable
fun SocialSegmentedTabs(
  selectedTab: SocialFeedTab,
  followingCount: Int,
  listenersCount: Int,
  chatsCount: Int = 0,
  onSelectTab: (SocialFeedTab) -> Unit
) {
  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    shape = RoundedCornerShape(999.dp),
    shadowElevation = 10.dp,
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 1.dp,
          brush = Brush.horizontalGradient(
            listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
              Color.White.copy(alpha = 0.18f),
              MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
            )
          ),
          shape = RoundedCornerShape(999.dp)
        )
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.fillMaxWidth().padding(4.dp)
    ) {
      SocialFeedTab.entries.forEach { tab ->
        val selected = tab == selectedTab
        val textColor by animateColorAsState(
          targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
          animationSpec = androidx.compose.animation.core.tween(durationMillis = 130),
          label = "social_tab_text_color"
        )
        val label = tab.title
        val bgBrush =
          if (selected) {
            Brush.horizontalGradient(
              listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
          } else {
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
          }
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.weight(1f)
              .defaultMinSize(minHeight = 48.dp)
              .clip(RoundedCornerShape(999.dp))
              .background(bgBrush)
              .then(
                if (selected) {
                  Modifier.border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                } else {
                  Modifier
                }
              )
              .clickable(onClick = { onSelectTab(tab) })
              .padding(horizontal = 4.dp, vertical = 10.dp)
              .testTag("feed_tab_${tab.name}")
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

@Composable
fun SocialStoriesStrip(
  stories: List<MusicStoryItem> = emptyList(),
  onCreateStoryClick: () -> Unit = {},
  onStoryClick: (MusicStoryItem) -> Unit = {}
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(vertical = 2.dp),
    modifier = Modifier.fillMaxWidth().testTag("feed_stories_strip")
  ) {

    item(key = "story_create_card") {
      val interactionSource = remember { MutableInteractionSource() }
      val scale = rememberPressScale(interactionSource)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.width(76.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = onCreateStoryClick
            )
            .padding(vertical = 4.dp)
            .testTag("story_ring_create")
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(66.dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
                  )
                )
              )
              .border(
                width = 2.dp,
                brush = Brush.sweepGradient(
                  listOf(
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                  )
                ),
                shape = CircleShape
              )
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Hikaye Kartı Oluştur",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
          )
        }
        Text(
          text = "Hikayem",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Kart Oluştur",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center
        )
      }
    }

    items(stories, key = { "st_${it.id}" }) { story ->
      val theme = StoryCardTheme.fromRaw(story.cardTheme)
      val themeAccent = Color(theme.accentColorHex)
      val interactionSource = remember { MutableInteractionSource() }
      val scale = rememberPressScale(interactionSource)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
          Modifier.width(76.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
              interactionSource = interactionSource,
              indication = null,
              onClick = { onStoryClick(story) }
            )
            .padding(vertical = 4.dp)
            .testTag("story_item_${story.id}")
      ) {
        Box(contentAlignment = Alignment.BottomCenter) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(66.dp)
                .clip(CircleShape)
                .border(
                  width = 2.5.dp,
                  brush = Brush.sweepGradient(
                    listOf(themeAccent, Color(theme.endColorHex), themeAccent)
                  ),
                  shape = CircleShape
                )
                .padding(4.dp)
          ) {
            StudioAvatar(
              photoUrlOrPath = story.userAvatarUrl,
              contentDescription = story.userName,
              size = 54.dp
            )
          }
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.clip(RoundedCornerShape(999.dp))
                .background(themeAccent)
                .border(1.5.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(999.dp))
                .padding(horizontal = 6.dp, vertical = 1.dp)
          ) {
            Text(
              text = "HİKAYE",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Text(
          text = story.userName.ifBlank { "Dinleyici" },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = story.trackTitle,
          style = MaterialTheme.typography.labelSmall,
          color = themeAccent,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}

@Composable
fun QuickSocialComposerCard(
  userName: String,
  userPhotoUrl: String,
  currentTrack: MusicTrack?,
  candidateTracks: List<MusicTrack>,
  onPublishPost: (note: String, attachedTrack: MusicTrack?) -> Unit,
  onOpenStoryStudio: (MusicTrack?) -> Unit = {}
) {
  var noteText by remember { mutableStateOf("") }
  var showTrackDrawer by remember { mutableStateOf(false) }
  val trackOptions = remember(currentTrack, candidateTracks) {
    (listOfNotNull(currentTrack) + candidateTracks).distinctBy { it.trackId }.take(6)
  }
  var selectedTrack by remember(currentTrack?.trackId) { mutableStateOf(currentTrack) }

  GlassStudioCard(
    highlighted = noteText.isNotBlank() || selectedTrack != null,
    cornerRadius = 22.dp,
    modifier = Modifier.testTag("feed_quick_composer")
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        StudioAvatar(
          photoUrlOrPath = userPhotoUrl,
          contentDescription = userName,
          size = 36.dp
        )

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it.take(240) },
          placeholder = {
            Text(
              text = "Müzik veya durum paylaş...",
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              style = MaterialTheme.typography.bodySmall
            )
          },
          singleLine = noteText.length < 36,
          maxLines = 3,
          textStyle = MaterialTheme.typography.bodySmall,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
          ),
          shape = RoundedCornerShape(999.dp),
          modifier = Modifier.weight(1f).testTag("feed_composer_input")
        )

        // Attach Music Track Toggle Button (48dp touch target)
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(
                if (showTrackDrawer || selectedTrack != null)
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
              )
              .border(
                width = 1.dp,
                color =
                  if (selectedTrack != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                  else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                shape = CircleShape
              )
              .clickable { showTrackDrawer = !showTrackDrawer }
              .testTag("feed_attach_track_btn")
        ) {
          Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Şarkı Ekle",
            tint =
              if (selectedTrack != null || showTrackDrawer) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        // Open Story Card Studio Button (48dp touch target)
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
              .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f), CircleShape)
              .clickable { onOpenStoryStudio(selectedTrack ?: trackOptions.firstOrNull()) }
              .testTag("open_story_studio_btn")
        ) {
          Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = "Hikaye Kartı Oluştur",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
          )
        }

        // Compact Send Pill (48dp min height)
        val canSend = noteText.isNotBlank() || selectedTrack != null
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .background(
                if (canSend) {
                  Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                  )
                } else {
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    )
                  )
                }
              )
              .clickable(enabled = canSend) {
                onPublishPost(noteText, selectedTrack)
                noteText = ""
                showTrackDrawer = false
              }
              .testTag("feed_publish_post_btn")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Send,
            contentDescription = "Paylaş",
            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(19.dp)
          )
        }
      }

      AnimatedVisibility(
        visible = selectedTrack != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        selectedTrack?.let { attached ->
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
              AsyncImage(
                model = attached.artworkUrl.ifBlank { null },
                contentDescription = attached.title,
                placeholder = painterResource(R.drawable.img_playlist_cover),
                error = painterResource(R.drawable.img_playlist_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = attached.title,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = attached.artistName,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier.size(48.dp)
                    .clip(CircleShape)
                    .clickable { selectedTrack = null }
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = "Seçili parçayı kaldır",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(17.dp)
                )
              }
            }
          }
        }
      }

      AnimatedVisibility(
        visible = showTrackDrawer && trackOptions.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(trackOptions, key = { it.trackId }) { track ->
            val isSelected = selectedTrack?.trackId == track.trackId
            StudioInteractiveChip(
              label = "${track.title} • ${track.artistName}",
              selected = isSelected,
              leadingIcon = Icons.Rounded.MusicNote,
              onClick = {
                selectedTrack = if (isSelected) null else track
                showTrackDrawer = false
              }
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RichActivityPostCard(
  event: ActivityFeedItem,
  likedByMe: Boolean,
  onOpenAuthorProfile: () -> Unit,
  onPlayTrack: (MusicTrack) -> Unit,
  onArtistClick: (ArtistSummary) -> Unit,
  onLike: () -> Unit,
  onOpenComments: () -> Unit,
  onOpenRepost: () -> Unit,
  onOpenLikers: () -> Unit = {},
  onLongPress: () -> Unit = {}
) {
  val actionType = ActivityActionType.fromRaw(event.actionType)
  val source = TrackSource.fromRaw(event.source)
  val relativeTime = remember(event.createdAt) { formatRelativeTimeTr(event.createdAt) }
  val haptics = LocalHapticFeedback.current
  val triggerLongPress: () -> Unit = {
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    onLongPress()
  }

  GlassStudioCard(
    cornerRadius = 24.dp,
    modifier =
      Modifier.combinedClickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {},
          onLongClick = triggerLongPress
        )
        .testTag("activity_post_card_${event.id}")
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (actionType == ActivityActionType.REPOSTED_TRACK && event.repostedFromName.isNotBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Repeat,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "${event.repostedFromName} (${event.repostedFromHandle}) paylaşımını yeniden paylaştı",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
              onClick = onOpenAuthorProfile,
              onLongClick = triggerLongPress
            )
            .padding(vertical = 2.dp)
            .testTag("post_author_btn_${event.id}")
      ) {
        StudioAvatar(
          photoUrlOrPath = event.userAvatarUrl,
          contentDescription = event.userName,
          size = 44.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = event.userName.ifBlank { "Vizyon Dinleyicisi" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${event.userHandle.ifBlank { "@vizyon" }} • $relativeTime",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        CategoryBadgePill(label = actionType.label)
      }

      if (event.note.isNotBlank()) {
        Text(
          text = "“${event.note}”",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      if (event.trackId != "status_note") {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
          shape = RoundedCornerShape(18.dp),
          modifier =
            Modifier.fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
              .clip(RoundedCornerShape(18.dp))
              .combinedClickable(
                onClick = {
                  onPlayTrack(
                    MusicTrack(
                      trackId = event.trackId,
                      title = event.trackTitle,
                      artistId = event.artistId,
                      artistName = event.artistName,
                      artworkUrl = event.albumArtUrl,
                      youtubeVideoId = event.youtubeVideoId,
                      source = source,
                      durationMs = 180_000L
                    )
                  )
                },
                onLongClick = triggerLongPress
              )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.background(
                  Brush.horizontalGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                      Color.Transparent
                    )
                  )
                )
                .padding(10.dp)
          ) {
            AsyncImage(
              model = event.albumArtUrl.ifBlank { null },
              contentDescription = event.trackTitle,
              placeholder = painterResource(R.drawable.img_playlist_cover),
              error = painterResource(R.drawable.img_playlist_cover),
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = event.trackTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = event.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                  Modifier.defaultMinSize(minHeight = 24.dp)
                    .clickable {
                      onArtistClick(
                        ArtistSummary(
                          artistId = event.artistId,
                          name = event.artistName,
                          imageUrl = event.albumArtUrl,
                          source = source
                        )
                      )
                    }
              )
            }
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                    )
                  )
                  .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            ) {
              Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = "Dinle",
                tint = Color.White
              )
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        SocialActionPill(
          icon = if (likedByMe) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
          label = "${event.likesCount} Beğeni",
          highlighted = likedByMe,
          tint = MaterialTheme.colorScheme.primary,
          onClick = onLike,
          onLongClick = onOpenLikers,
          modifier = Modifier.weight(1f).testTag("like_activity_btn_${event.id}")
        )
        SocialActionPill(
          icon = Icons.Rounded.ChatBubbleOutline,
          label = "${event.commentsCount} Yorum",
          highlighted = event.commentsCount > 0,
          tint = MaterialTheme.colorScheme.secondary,
          onClick = onOpenComments,
          modifier = Modifier.weight(1f).testTag("comment_activity_btn_${event.id}")
        )
        SocialActionPill(
          icon = Icons.Rounded.Repeat,
          label = "${event.repostsCount} Paylaş",
          highlighted = event.repostsCount > 0,
          tint = MaterialTheme.colorScheme.tertiary,
          onClick = onOpenRepost,
          modifier = Modifier.weight(1f).testTag("repost_activity_btn_${event.id}")
        )
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SocialActionPill(
  icon: ImageVector,
  label: String,
  highlighted: Boolean,
  tint: Color,
  onClick: () -> Unit,
  onLongClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val interactionSource = remember { MutableInteractionSource() }
  val haptics = LocalHapticFeedback.current
  val scale = rememberPressScale(interactionSource)
  val shape = RoundedCornerShape(999.dp)
  val bgColor by animateColorAsState(
    targetValue =
      if (highlighted) tint.copy(alpha = 0.16f)
      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
    label = "social_pill_bg"
  )
  val borderColor by animateColorAsState(
    targetValue =
      if (highlighted) tint.copy(alpha = 0.48f)
      else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
    label = "social_pill_border"
  )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
    modifier =
      modifier
        .scale(scale)
        .defaultMinSize(minHeight = 48.dp)
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
        .combinedClickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
          onLongClick =
            onLongClick?.let { longAction ->
              {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                longAction()
              }
            }
        )
        .padding(horizontal = 8.dp, vertical = 10.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (highlighted) tint else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(17.dp)
    )
    Spacer(modifier = Modifier.width(5.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
      color = if (highlighted) tint else MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )
  }
}
