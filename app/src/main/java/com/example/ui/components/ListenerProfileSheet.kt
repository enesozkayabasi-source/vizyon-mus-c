package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ActivityFeedItem
import com.example.model.MusicTrack
import com.example.model.PublicProfile
import com.example.model.RecentTrackSummary
import com.example.model.TrackSource
import com.example.model.formatRelativeTimeTr

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListenerProfileSheet(
  person: PublicProfile,
  isSelf: Boolean,
  isFollowing: Boolean,
  recentPosts: List<ActivityFeedItem>,
  selfRecentTracks: List<RecentTrackSummary> = emptyList(),
  onDismiss: () -> Unit,
  onToggleFollow: () -> Unit,
  onPlayActivityTrack: (MusicTrack) -> Unit,
  onCreateStoryFromTrack: (MusicTrack) -> Unit = {},
  onOpenChat: () -> Unit = {},
  onOpenFollowLists: () -> Unit = {},
  isFollowRequestPending: Boolean = false,
  onBlockUser: () -> Unit = {},
  onReportUser: () -> Unit = {}
) {
  val recentList =
    if (isSelf && selfRecentTracks.isNotEmpty()) selfRecentTracks.take(5)
    else person.resolvedRecentTracks().take(5)
  val liveTrackTitle = person.currentTrackTitle.ifBlank { person.lastTrackTitle }
  val liveArtistName = person.currentArtistName.ifBlank { person.lastArtistName }
  val hasRecentActivity = recentList.isNotEmpty() || liveTrackTitle.isNotBlank()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("listener_profile_sheet")
  ) {
    LazyColumn(
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
    ) {
      item {
        GlassStudioCard(highlighted = true, cornerRadius = 24.dp) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Box(contentAlignment = Alignment.BottomEnd) {
                StudioAvatar(
                  photoUrlOrPath = person.photoUrl,
                  contentDescription = person.displayName,
                  size = 68.dp,
                  ringColor =
                    if (hasRecentActivity) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                )
                if (hasRecentActivity) {
                  Box(
                    modifier =
                      Modifier.size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                  )
                }
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = person.displayName.ifBlank { "Vizyon Dinleyicisi" },
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = person.handle.ifBlank { "@vizyon" },
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenFollowLists)
                        .testTag("profile_followers_pill")
                  ) {
                    CategoryBadgePill(label = "${person.followerCount} Takipçi")
                  }
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onOpenFollowLists)
                        .testTag("profile_following_pill")
                  ) {
                    CategoryBadgePill(label = "${person.followingCount} Takip")
                  }
                  CategoryBadgePill(label = "${recentPosts.size} Paylaşım")
                }
              }
              StudioCircleIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Profil penceresini kapat",
                onClick = onDismiss,
                size = 48.dp,
                modifier = Modifier.testTag("close_listener_profile_sheet_btn")
              )
            }

            if (person.bio.isNotBlank()) {
              Text(
                text = person.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            if (liveTrackTitle.isNotBlank()) {
              Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .testTag("profile_live_listening_banner")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text =
                      if (liveArtistName.isNotBlank()) "Şu an profilinde dinliyor: $liveTrackTitle — $liveArtistName"
                      else "Şu an profilinde dinliyor: $liveTrackTitle",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }

            if (!isSelf) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                StudioGlassButton(
                  text = "Mesaj Gönder",
                  icon = Icons.Rounded.ChatBubbleOutline,
                  onClick = onOpenChat,
                  modifier = Modifier.weight(1f).testTag("profile_sheet_message_btn")
                )
                StudioFollowToggleButton(
                  isFollowing = isFollowing,
                  isPending = isFollowRequestPending,
                  onClick = onToggleFollow,
                  modifier = Modifier.weight(1f).testTag("profile_sheet_follow_btn")
                )
              }
              Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                StudioGlassButton(
                  text = "Şikayet Et",
                  icon = Icons.Rounded.Flag,
                  onClick = onReportUser,
                  tintColor = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.weight(1f).testTag("profile_report_user_btn")
                )
                StudioGlassButton(
                  text = "Engelle",
                  icon = Icons.Rounded.Block,
                  onClick = onBlockUser,
                  tintColor = MaterialTheme.colorScheme.error,
                  modifier = Modifier.weight(1f).testTag("profile_block_user_btn")
                )
              }
            }
          }
        }
      }

      item {
        Column(
          modifier = Modifier.testTag("profile_recent_tracks_section"),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          StudioSectionHeader(
            title = "Son 5 Dinlenen Şarkı",
            subtitle = "En son dinlediği parçalar ve hızlı hikaye kartı",
            badge = "${recentList.size}/5"
          )
        }
      }

      if (recentList.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("profile_recent_tracks_section_empty")
          ) {
            Text(
              text = "Henüz son dinlenen şarkı geçmişi oluşmamış.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(14.dp)
            )
          }
        }
      } else {
        itemsIndexed(recentList, key = { idx, item -> "${idx}_${item.trackId}_${item.title}" }) { idx, summary ->
          val musicTrack = summary.toMusicTrack()
          GlassStudioCard(
            highlighted = idx == 0,
            cornerRadius = 18.dp,
            onClick = { onPlayActivityTrack(musicTrack) },
            modifier = Modifier.testTag("recent_track_row_$idx")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(12.dp)
            ) {
              Text(
                text = "${idx + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = if (idx == 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              AsyncImage(
                model = summary.artworkUrl.ifBlank { null },
                contentDescription = summary.title,
                placeholder = painterResource(R.drawable.img_playlist_cover),
                error = painterResource(R.drawable.img_playlist_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
              )
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = summary.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = summary.artistName.ifBlank { "Sanatçı" },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              StudioCircleIconButton(
                icon = Icons.Rounded.AutoAwesome,
                contentDescription = "Hikaye Kartı Oluştur",
                onClick = { onCreateStoryFromTrack(musicTrack) },
                size = 48.dp,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("recent_track_story_btn_$idx")
              )
              StudioCircleIconButton(
                icon = Icons.Rounded.PlayArrow,
                contentDescription = "Şarkıyı Çal",
                onClick = { onPlayActivityTrack(musicTrack) },
                size = 48.dp,
                highlighted = idx == 0,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("recent_track_play_btn_$idx")
              )
            }
          }
        }
      }

      item {
        StudioSectionHeader(
          title = "Son Paylaşımları",
          badge = "${recentPosts.size}"
        )
      }

      if (recentPosts.isEmpty()) {
        item {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Bu dinleyicinin akışta henüz paylaşımı bulunmuyor.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(14.dp)
            )
          }
        }
      } else {
        items(recentPosts.take(6), key = { it.id }) { post ->
          val isStatusOnly = post.trackId == "status_note"
          GlassStudioCard(
            cornerRadius = 18.dp,
            onClick =
              if (isStatusOnly) null
              else {
                {
                  onPlayActivityTrack(
                    MusicTrack(
                      trackId = post.trackId,
                      title = post.trackTitle,
                      artistId = post.artistId,
                      artistName = post.artistName,
                      artworkUrl = post.albumArtUrl,
                      youtubeVideoId = post.youtubeVideoId,
                      source = TrackSource.fromRaw(post.source),
                      durationMs = 180_000L
                    )
                  )
                }
              }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(12.dp)
            ) {
              AsyncImage(
                model = post.albumArtUrl.ifBlank { null },
                contentDescription = post.trackTitle,
                placeholder = painterResource(R.drawable.img_playlist_cover),
                error = painterResource(R.drawable.img_playlist_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = post.trackTitle,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = if (post.note.isNotBlank()) "“${post.note}”" else post.artistName,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Text(
                text = formatRelativeTimeTr(post.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(14.dp)) }
    }
  }
}
