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

@Composable
fun ShareTrackDialog(
  track: MusicTrack,
  onDismiss: () -> Unit,
  onShareToFeed: (String) -> Unit
) {
  var note by remember { mutableStateOf("") }
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Parçayı Paylaş", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "${track.title} — ${track.artistName}",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(
          value = note,
          onValueChange = { if (it.length <= 220) note = it },
          label = { Text("Arkadaşlarına bir not ekle") },
          placeholder = { Text("Örn: Şarkı sözleri ve klibi harika!") },
          singleLine = false,
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
          onClick = {
            shareExternally(context, track)
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Rounded.Share, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Diğer Uygulamalarda Paylaş")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onShareToFeed(note)
          onDismiss()
        },
        modifier = Modifier.testTag("confirm_share_feed_button")
      ) {
        Text("Akışta Paylaş")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Vazgeç") }
    }
  )
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
