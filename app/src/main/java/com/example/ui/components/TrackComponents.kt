package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import kotlinx.coroutines.launch
import com.example.model.MusicTrack
import com.example.model.Playlist
import java.net.URLEncoder
import java.util.Locale

@Composable
fun CategoryBadgePill(label: String, modifier: Modifier = Modifier) {
  val pillColor = MaterialTheme.colorScheme.primary
  Surface(
    color = pillColor.copy(alpha = 0.14f),
    shape = RoundedCornerShape(999.dp),
    modifier = modifier
  ) {
    Text(
      text = label.ifBlank { "Trendler" },
      style = MaterialTheme.typography.labelSmall,
      color = pillColor,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}

@Composable
fun TrackRowItem(
  track: MusicTrack,
  isCurrentTrack: Boolean,
  isPlaying: Boolean,
  isFavorite: Boolean,
  onPlayClick: () -> Unit,
  onArtistClick: () -> Unit,
  onFavoriteToggle: () -> Unit,
  onAddToQueue: () -> Unit,
  onOpenPlaylistPicker: () -> Unit,
  onShareClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var menuExpanded by remember { mutableStateOf(false) }

  GlassStudioCard(
    highlighted = isCurrentTrack,
    cornerRadius = 18.dp,
    onClick = onPlayClick,
    modifier = modifier.testTag("track_row_${track.trackId}")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        AsyncImage(
          model = track.artworkUrl.ifBlank { null },
          contentDescription = "${track.title} kapak görseli",
          placeholder = painterResource(R.drawable.img_playlist_cover),
          error = painterResource(R.drawable.img_playlist_cover),
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
        )
        if (isCurrentTrack) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.56f))
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(26.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (isCurrentTrack) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = track.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(onClick = onArtistClick).weight(1f, fill = false)
          )
          Text(
            text = "• ${formatDurationMs(track.durationMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          CategoryBadgePill(label = track.categoryTag)
          Icon(
            imageVector = Icons.Rounded.SmartDisplay,
            contentDescription = "YouTube video eşleşmeli",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      StudioFavoriteIconButton(
        isFavorite = isFavorite,
        onClick = onFavoriteToggle,
        modifier = Modifier.testTag("favorite_btn_${track.trackId}")
      )

      Box {
        IconButton(onClick = { menuExpanded = true }) {
          Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = "Diğer seçenekler",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
          DropdownMenuItem(
            text = { Text("Sıraya Ekle") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
            onClick = {
              menuExpanded = false
              onAddToQueue()
            }
          )
          DropdownMenuItem(
            text = { Text("Çalma Listesine Ekle") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
            onClick = {
              menuExpanded = false
              onOpenPlaylistPicker()
            }
          )
          DropdownMenuItem(
            text = { Text("Sanatçı Profilini Aç") },
            leadingIcon = { Icon(Icons.Rounded.GraphicEq, null) },
            onClick = {
              menuExpanded = false
              onArtistClick()
            }
          )
          DropdownMenuItem(
            text = { Text("Sosyal Akışta Paylaş") },
            leadingIcon = { Icon(Icons.Rounded.Share, null) },
            onClick = {
              menuExpanded = false
              onShareClick()
            }
          )
        }
      }
    }
  }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ShareTrackDialog(
  track: MusicTrack,
  onDismiss: () -> Unit,
  onShareToFeed: (String) -> Unit,
  listeners: List<com.example.model.PublicProfile> = emptyList(),
  followingIds: Set<String> = emptySet(),
  currentUserName: String = "Vizyon Dinleyicisi",
  currentUserHandle: String = "@vizyon",
  currentUserPhotoUrl: String = "",
  defaultStoryTheme: com.example.model.StoryCardTheme = com.example.model.StoryCardTheme.NEON_WAVE,
  onShareToStory: (String, com.example.model.StoryCardTheme) -> Unit = { _, _ -> },
  onSendTrackDirectMessage: (com.example.model.PublicProfile, String) -> Unit = { _, _ -> },
  onNotifyMessage: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var activeTabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
  var feedNote by remember { mutableStateOf("") }
  var storyNote by remember { mutableStateOf("") }
  var selectedStoryTheme by remember(defaultStoryTheme) { mutableStateOf(defaultStoryTheme) }
  var isRenderingStoryPng by remember { mutableStateOf(false) }
  var dmNote by remember { mutableStateOf("") }
  var dmSearchQuery by remember { mutableStateOf("") }

  val tabLabels = remember { listOf("Akış", "Hikaye", "Mesaj") }
  val tabTags = remember { listOf("share_studio_tab_feed", "share_studio_tab_story", "share_studio_tab_dm") }

  val sortedListeners =
    remember(listeners, followingIds, dmSearchQuery) {
      val q = dmSearchQuery.trim().lowercase(Locale.ROOT)
      listeners
        .filter { peer ->
          if (q.isBlank()) true
          else peer.displayName.lowercase(Locale.ROOT).contains(q) || peer.handle.lowercase(Locale.ROOT).contains(q)
        }
        .sortedWith(
          compareByDescending<com.example.model.PublicProfile> {
            val pid = com.example.model.sanitizeFirestoreId(it.userId.ifBlank { it.id })
            pid in followingIds
          }.thenBy { it.displayName.lowercase(Locale.ROOT) }
        )
    }

  val previewStory =
    remember(currentUserName, currentUserHandle, currentUserPhotoUrl, track, storyNote, selectedStoryTheme) {
      com.example.model.MusicStoryItem(
        id = "share_preview",
        userId = "self",
        userName = currentUserName.ifBlank { "Vizyon Dinleyicisi" },
        userHandle = currentUserHandle.ifBlank { "@vizyon" },
        userAvatarUrl = currentUserPhotoUrl,
        trackId = track.trackId,
        trackTitle = track.title,
        artistId = track.artistId,
        artistName = track.artistName,
        albumArtUrl = track.artworkUrl,
        youtubeVideoId = track.youtubeVideoId,
        source = track.source.name,
        note = storyNote.trim(),
        cardTheme = selectedStoryTheme.name
      )
    }

  androidx.compose.material3.ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("share_track_studio_sheet")
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 18.dp)
          .padding(bottom = 22.dp)
    ) {
      // Compact Track Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        AsyncImage(
          model = track.artworkUrl.ifBlank { null },
          contentDescription = track.title,
          placeholder = painterResource(R.drawable.img_playlist_cover),
          error = painterResource(R.drawable.img_playlist_cover),
          contentScale = ContentScale.Crop,
          modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "PARÇA PAYLAŞIM STÜDYOSU",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = track.artistName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // 3-Panel Segmented Pill Selector (Akış | Hikaye | Mesaj)
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().padding(4.dp)
        ) {
          tabLabels.forEachIndexed { idx, label ->
            val selected = activeTabIndex == idx
            Surface(
              color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
              shape = RoundedCornerShape(12.dp),
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { activeTabIndex = idx }
                  .testTag(tabTags[idx])
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 10.dp)
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }

      when (activeTabIndex) {
        0 -> {
          // Panel 1: Akış (Feed)
          Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf("🔥 Harika", "🌙 Gece Modu", "🔁 Döngüde", "🎧 Stüdyo Seçkisi").forEach { tag ->
                StudioInteractiveChip(
                  label = tag,
                  selected = feedNote.contains(tag),
                  onClick = {
                    feedNote =
                      if (feedNote.isBlank()) tag
                      else if (feedNote.contains(tag)) feedNote
                      else "$feedNote • $tag".take(220)
                  }
                )
              }
            }
            OutlinedTextField(
              value = feedNote,
              onValueChange = { if (it.length <= 220) feedNote = it },
              label = { Text("Akış paylaşımına bir not ekle") },
              placeholder = { Text("Örn: Şarkı sözleri ve sahne atmosferi harika!") },
              maxLines = 3,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().testTag("share_studio_feed_note_input")
            )
            StudioGradientCtaButton(
              text = "Akışta Paylaş",
              icon = Icons.Rounded.Share,
              onClick = {
                onShareToFeed(feedNote)
                onDismiss()
              },
              modifier = Modifier.fillMaxWidth().testTag("confirm_share_feed_button")
            )
            StudioGlassButton(
              text = "Diğer Uygulamalarda Paylaş",
              icon = Icons.Rounded.Share,
              onClick = {
                shareExternally(context, track)
                onDismiss()
              },
              modifier = Modifier.fillMaxWidth().testTag("share_studio_external_btn")
            )
          }
        }

        1 -> {
          // Panel 2: Hikaye (Story Card Studio)
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
          ) {
            StoryCardVisualPreview(
              story = previewStory,
              modifier =
                Modifier.width(154.dp)
                  .height(216.dp)
                  .testTag("share_studio_story_preview")
            )
            androidx.compose.foundation.lazy.LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(com.example.model.StoryCardTheme.entries) { theme ->
                val selected = selectedStoryTheme == theme
                StudioInteractiveChip(
                  label = theme.label,
                  selected = selected,
                  onClick = { selectedStoryTheme = theme },
                  modifier = Modifier.testTag("share_studio_story_theme_${theme.name}")
                )
              }
            }
            OutlinedTextField(
              value = storyNote,
              onValueChange = { storyNote = it.take(140) },
              label = { Text("Hikaye Notu (isteğe bağlı, maks 140)") },
              placeholder = { Text("Bu gece bu parça eşlik ediyor...") },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().testTag("share_studio_story_note_input")
            )
            StudioGradientCtaButton(
              text = "Hikayemde Paylaş (24 Saat)",
              icon = Icons.Rounded.GraphicEq,
              onClick = {
                onShareToStory(storyNote, selectedStoryTheme)
                onDismiss()
              },
              modifier = Modifier.fillMaxWidth().testTag("share_studio_publish_story_btn")
            )
            StudioGlassButton(
              text = if (isRenderingStoryPng) "PNG Kart Hazırlanıyor..." else "Hikaye Kartını PNG Paylaş",
              icon = Icons.Rounded.Share,
              enabled = !isRenderingStoryPng,
              onClick = {
                isRenderingStoryPng = true
                runLaunchShareStoryPng(
                  scope = scope,
                  context = context,
                  previewStory = previewStory,
                  onDone = { ok ->
                    isRenderingStoryPng = false
                    if (!ok) onNotifyMessage("Hikaye kartı görseli oluşturulamadı.")
                  }
                )
              },
              modifier = Modifier.fillMaxWidth().testTag("share_studio_story_png_btn")
            )
          }
        }

        else -> {
          // Panel 3: Mesaj (Direct Message / Message Request)
          Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = dmNote,
              onValueChange = { if (it.length <= 160) dmNote = it },
              label = { Text("Mesaj notu (isteğe bağlı)") },
              placeholder = { Text("Örn: Tam senin tarzın, dinlemeni istedim!") },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().testTag("share_studio_dm_note_input")
            )
            OutlinedTextField(
              value = dmSearchQuery,
              onValueChange = { if (it.length <= 40) dmSearchQuery = it },
              label = { Text("Dinleyici veya @kullanıcı adı ara") },
              singleLine = true,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth().testTag("share_studio_dm_search_input")
            )
            if (sortedListeners.isEmpty()) {
              GlassStudioCard(cornerRadius = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Column(
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.padding(14.dp)
                ) {
                  Text(
                    text = "Eşleşen dinleyici bulunamadı",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Diğer uygulamalar üzerinden arkadaşlarına parça bağlantısını gönderebilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  StudioGlassButton(
                    text = "Diğer Uygulamalarda Paylaş",
                    icon = Icons.Rounded.Share,
                    onClick = {
                      shareExternally(context, track)
                      onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            } else {
              LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(230.dp)
              ) {
                items(
                  items = sortedListeners,
                  key = { it.userId.ifBlank { it.id } }
                ) { peer ->
                  val peerSafeId = com.example.model.sanitizeFirestoreId(peer.userId.ifBlank { peer.id })
                  val isFollowingPeer = peerSafeId in followingIds
                  Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                      StudioAvatar(
                        photoUrlOrPath = peer.photoUrl,
                        contentDescription = peer.displayName,
                        size = 42.dp
                      )
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = peer.displayName.ifBlank { "Vizyon Dinleyicisi" },
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text =
                            "${peer.handle.ifBlank { "@vizyon" }} • ${
                              if (isFollowingPeer) "Takip Ediliyor" else "Mesaj / İstek"
                            }",
                          style = MaterialTheme.typography.labelSmall,
                          color = if (isFollowingPeer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                      Button(
                        onClick = {
                          onSendTrackDirectMessage(peer, dmNote)
                          onDismiss()
                        },
                        modifier = Modifier.testTag("share_studio_send_dm_$peerSafeId")
                      ) {
                        Text("Gönder")
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

internal fun runLaunchShareStoryPng(
  scope: kotlinx.coroutines.CoroutineScope,
  context: Context,
  previewStory: com.example.model.MusicStoryItem,
  onDone: (Boolean) -> Unit
) {
  scope.launch {
    val uri = com.example.ui.stories.StoryBitmapRenderer.renderAndCacheStoryPngUri(context, previewStory)
    if (uri != null) {
      com.example.ui.stories.StoryBitmapRenderer.launchExternalStoryShareChooser(context, uri, previewStory)
      onDone(true)
    } else {
      onDone(false)
    }
  }
}

@Composable
fun AddToPlaylistDialog(
  track: MusicTrack,
  playlists: List<Playlist>,
  onDismiss: () -> Unit,
  onSelectPlaylist: (String) -> Unit,
  onCreateNewQuick: (String) -> Unit
) {
  var quickName by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Çalma Listesine Ekle", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = track.title,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (playlists.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(playlists, key = { it.id }) { pl ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier =
                  Modifier.fillMaxWidth().clickable {
                    onSelectPlaylist(pl.id)
                    onDismiss()
                  }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                  Box(
                    modifier =
                      Modifier.size(12.dp)
                        .clip(CircleShape)
                        .background(parseColorSafe(pl.accentColorHex, MaterialTheme.colorScheme.primary))
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = pl.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                  )
                  Text(
                    text = "${pl.trackCount} parça",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
        OutlinedTextField(
          value = quickName,
          onValueChange = { if (it.length <= 60) quickName = it },
          label = { Text("Veya hızlıca yeni liste oluştur") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      if (quickName.isNotBlank()) {
        Button(
          onClick = {
            onCreateNewQuick(quickName.trim())
            onDismiss()
          }
        ) {
          Text("Oluştur")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Kapat") }
    }
  )
}

fun shareExternally(context: Context, track: MusicTrack) {
  val link =
    if (track.youtubeVideoId.isNotBlank()) {
      "https://www.youtube.com/watch?v=${track.youtubeVideoId}"
    } else {
      val encoded = runCatching { URLEncoder.encode("${track.artistName} ${track.title}", "UTF-8") }.getOrDefault("")
      "https://music.apple.com/tr/search?term=$encoded"
    }
  val sendIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(
        Intent.EXTRA_TEXT,
        "Vizyon Music'te dinliyorum: ${track.title} — ${track.artistName}\n$link"
      )
    }
  context.startActivity(Intent.createChooser(sendIntent, "Parçayı Paylaş"))
}

fun formatDurationMs(ms: Long): String {
  val totalSec = (ms / 1000L).coerceAtLeast(0L)
  val mins = totalSec / 60L
  val secs = totalSec % 60L
  return String.format(Locale.US, "%d:%02d", mins, secs)
}

fun parseColorSafe(hex: String, fallback: Color): Color =
  try {
    Color(android.graphics.Color.parseColor(hex))
  } catch (_: Exception) {
    fallback
  }
