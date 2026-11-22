package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.MusicTrack
import com.example.model.TrackLyrics
import com.example.model.UiState

@Composable
fun SyncedLyricsSheetContent(
  track: MusicTrack,
  positionMs: Long,
  lyricsState: UiState<TrackLyrics?>,
  onSeekTo: (Long) -> Unit,
  onRefreshLyrics: () -> Unit
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .height(520.dp)
        .padding(horizontal = 20.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      AsyncImage(
        model = track.artworkUrl.ifBlank { null },
        contentDescription = track.title,
        placeholder = painterResource(R.drawable.img_playlist_cover),
        error = painterResource(R.drawable.img_playlist_cover),
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
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
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      StudioCircleIconButton(
        icon = Icons.Rounded.Refresh,
        contentDescription = "Sözleri Yenile",
        onClick = onRefreshLyrics
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    when (lyricsState) {
      UiState.Loading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Şarkı sözleri aranıyor…",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      is UiState.Error -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Rounded.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = lyricsState.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            StudioGlassButton(text = "Tekrar Dene", onClick = onRefreshLyrics)
          }
        }
      }
      is UiState.Success -> {
        val lyrics = lyricsState.data
        when {
          lyrics == null || (!lyrics.hasAnyLyrics && !lyrics.isInstrumental) -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Rounded.Lyrics,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "Bu parça için şarkı sözü henüz bulunamadı.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                StudioGlassButton(text = "Tekrar Ara", onClick = onRefreshLyrics)
              }
            }
          }
          lyrics.isInstrumental && !lyrics.hasAnyLyrics -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                  imageVector = Icons.Rounded.MusicNote,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = "Bu parça enstrümantal görünüyor.",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
          lyrics.isSynced -> {
            val listState = rememberLazyListState()
            val activeIndex =
              lyrics.syncedLines.indexOfLast { it.timeMs <= positionMs + 250L }
            LaunchedEffect(activeIndex) {
              if (activeIndex >= 0) {
                listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
              }
            }
            LazyColumn(
              state = listState,
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              itemsIndexed(lyrics.syncedLines) { index, line ->
                val isActive = index == activeIndex
                val isPast = activeIndex >= 0 && index < activeIndex
                Surface(
                  color =
                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else Color.Transparent,
                  shape = RoundedCornerShape(12.dp),
                  modifier =
                    Modifier.fillMaxWidth().clickable {
                      onSeekTo(line.timeMs)
                    }
                ) {
                  Text(
                    text = line.text,
                    fontSize = if (isActive) 20.sp else 16.sp,
                    lineHeight = if (isActive) 28.sp else 24.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color =
                      when {
                        isActive -> MaterialTheme.colorScheme.primary
                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                      },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                  )
                }
              }
            }
          }
          else -> {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              itemsIndexed(lyrics.plainLines) { _, line ->
                Text(
                  text = line,
                  fontSize = 17.sp,
                  lineHeight = 25.sp,
                  fontWeight = FontWeight.Medium,
                  color =
                    if (line.isBlank()) Color.Transparent
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
              }
            }
          }
        }
      }
    }
  }
}
