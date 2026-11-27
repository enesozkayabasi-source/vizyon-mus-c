package com.example.ui.components

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.MusicTrack
import com.example.model.TrackLyrics
import com.example.model.UiState
import com.example.player.PlaybackUiState
import com.example.player.RepeatMode
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch

/**
 * Floating frosted-glass MiniPlayer dock whose album-cover slot hosts a LIVE compact 16:9
 * YouTube video viewport so playback keeps running while navigating or searching.
 */
@Composable
fun MiniPlayerBar(
  playbackState: PlaybackUiState,
  sharedWebView: WebView,
  onIFrameLoadFailure: (String) -> Unit,
  onRetryVideo: () -> Unit,
  onExpandClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onNextClick: () -> Unit,
  onDismissSwipe: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val track = playbackState.currentTrack ?: return
  val progress =
    if (playbackState.durationMs > 0L) {
      (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

  val scope = rememberCoroutineScope()
  val offsetX = remember { Animatable(0f) }
  var widthPx by remember { mutableFloatStateOf(900f) }

  LaunchedEffect(track.trackId) {
    offsetX.snapTo(0f)
  }

  val dragProgress = (abs(offsetX.value) / (widthPx * 0.42f)).coerceIn(0f, 1f)
  val cardAlpha = 1f - (dragProgress * 0.45f)

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      modifier
        .fillMaxWidth()
        .onSizeChanged { if (it.width > 0) widthPx = it.width.toFloat() }
  ) {
    if (abs(offsetX.value) > 10f) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
          if (offsetX.value >= 0f) Arrangement.Start else Arrangement.End,
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp)
            .alpha(dragProgress)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Oynatıcıyı kapat",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Kapatmak için kaydır",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.error,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    GlassStudioCard(
      highlighted = true,
      cornerRadius = 18.dp,
      onClick = onExpandClick,
      modifier =
        Modifier
          .offset { IntOffset(offsetX.value.roundToInt(), 0) }
          .alpha(cardAlpha)
          .draggable(
            orientation = Orientation.Horizontal,
            state =
              rememberDraggableState { delta ->
                scope.launch { offsetX.snapTo(offsetX.value + delta) }
              },
            onDragStopped = { velocity ->
              scope.launch {
                val threshold = maxOf(140f, widthPx * 0.34f)
                val shouldDismiss =
                  abs(offsetX.value) >= threshold ||
                    (abs(velocity) >= 1000f && abs(offsetX.value) >= 48f)
                if (shouldDismiss) {
                  val direction = if (offsetX.value != 0f) sign(offsetX.value) else sign(velocity)
                  offsetX.animateTo(
                    targetValue = direction * widthPx * 1.15f,
                    animationSpec = tween(durationMillis = 180)
                  )
                  onDismissSwipe()
                } else {
                  offsetX.animateTo(0f, animationSpec = spring())
                }
              }
            }
          )
          .padding(horizontal = 12.dp, vertical = 4.dp)
          .testTag("mini_player_bar")
    ) {
    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
      ) {
        // Live mini exact 16:9 video player (96dp x 54dp) directly in place of the album cover
        Box(
          modifier =
            Modifier.width(96.dp)
              .aspectRatio(16f / 9f)
              .clip(RoundedCornerShape(10.dp))
              .testTag("mini_player_live_video_slot")
        ) {
          YouTubeIframePlayerBox(
            sharedWebView = sharedWebView,
            videoId = playbackState.activeVideoId,
            isResolvingVideo = playbackState.isResolvingVideo,
            isPlayerReady = playbackState.isPlayerReady,
            errorMessage = playbackState.playbackMessage,
            onIFrameLoadFailure = onIFrameLoadFailure,
            onRetryVideo = onRetryVideo,
            compactMode = true,
            onCompactClick = onExpandClick,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Box(
              modifier =
                Modifier.size(7.dp)
                  .clip(CircleShape)
                  .background(
                    if (playbackState.isPlaying) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                  )
            )
            Text(
              text = track.title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          Text(
            text = "${track.artistName} • ${if (playbackState.isPlaying) "Canlı Mini Video" else "Büyütmek için dokun"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Bespoke glowing circular Play/Pause control
        val miniBtnGlow = MaterialTheme.colorScheme.primary
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(38.dp)
              .drawBehind {
                drawCircle(
                  color = miniBtnGlow.copy(alpha = 0.24f),
                  radius = size.minDimension / 2f + 2.dp.toPx(),
                  style = Stroke(width = 2.5.dp.toPx())
                )
              }
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
              )
              .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
              .clickable(onClick = onPlayPauseClick)
              .testTag("mini_player_play_pause")
        ) {
          Icon(
            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (playbackState.isPlaying) "Duraklat" else "Oynat",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        StudioCircleIconButton(
          icon = Icons.Rounded.SkipNext,
          contentDescription = "Sonraki parça",
          onClick = onNextClick,
          size = 36.dp,
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.testTag("mini_player_next_btn")
        )
      }

      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(2.5.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
      )
    }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingModal(
  playbackState: PlaybackUiState,
  lyricsState: UiState<TrackLyrics?>,
  sharedWebView: WebView,
  isFavorite: Boolean,
  onDismiss: () -> Unit,
  onIFrameLoadFailure: (String) -> Unit,
  onRetryVideo: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onSeekTo: (Long) -> Unit,
  onNextClick: () -> Unit,
  onPreviousClick: () -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggleFavorite: (MusicTrack) -> Unit,
  onOpenPlaylistPicker: (MusicTrack) -> Unit,
  onShareTrack: (MusicTrack) -> Unit,
  onRefreshLyrics: () -> Unit,
  onArtistClick: (MusicTrack) -> Unit,
  onSelectTrackFromQueue: (MusicTrack) -> Unit
) {
  BackHandler(onBack = onDismiss)
  val track = playbackState.currentTrack ?: return
  var showQueueSheet by remember { mutableStateOf(false) }
  var showLyricsSheet by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("now_playing_modal")
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedWaveformBackdrop(
        currentStep = if (playbackState.isPlaying) 3 else 1,
        selectedGenreCount = 3
      )
      Column(
        modifier =
          Modifier.fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          StudioCircleIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Küçült",
            onClick = onDismiss,
            tint = MaterialTheme.colorScheme.onSurface
          )
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "VİZYON SAHNE OYNATICISI",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Küçültünce Mini Video Modu Aktif",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          StudioCircleIconButton(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = "Sıra",
            onClick = { showQueueSheet = true },
            tint = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Shared YouTube IFrame Video Player
        YouTubeIframePlayerBox(
          sharedWebView = sharedWebView,
          videoId = playbackState.activeVideoId,
          isResolvingVideo = playbackState.isResolvingVideo,
          isPlayerReady = playbackState.isPlayerReady,
          errorMessage = playbackState.playbackMessage,
          onIFrameLoadFailure = onIFrameLoadFailure,
          onRetryVideo = onRetryVideo,
          compactMode = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Track Title, Artist & Favorite
        GlassStudioCard(cornerRadius = 22.dp) {
          Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = track.title,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = track.artistName,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.clickable { onArtistClick(track) }
                )
              }
              StudioFavoriteIconButton(
                isFavorite = isFavorite,
                onClick = { onToggleFavorite(track) }
              )
            }
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              CategoryBadgePill(label = track.categoryTag)
              CategoryBadgePill(label = "Resmi Video • Ön Plan")
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Seek Slider synced with IFrame Player
        Column(modifier = Modifier.fillMaxWidth()) {
          val maxDur = playbackState.durationMs.coerceAtLeast(1000L).toFloat()
          Slider(
            value = playbackState.positionMs.coerceIn(0L, maxDur.toLong()).toFloat(),
            onValueChange = { onSeekTo(it.toLong()) },
            valueRange = 0f..maxDur,
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary
            )
          )
          Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = formatDurationMs(playbackState.positionMs),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = formatDurationMs(playbackState.durationMs),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Main Playback Controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceEvenly,
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
          StudioCircleIconButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "Karıştır",
            onClick = onToggleShuffle,
            highlighted = playbackState.shuffleEnabled,
            tint =
              if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant
          )
          IconButton(onClick = onPreviousClick, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Önceki", modifier = Modifier.size(34.dp))
          }
          val fullBtnGlow = MaterialTheme.colorScheme.primary
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(74.dp)
                .drawBehind {
                  drawCircle(
                    color = fullBtnGlow.copy(alpha = 0.24f),
                    radius = size.minDimension / 2f + 3.dp.toPx(),
                    style = Stroke(width = 4.5.dp.toPx())
                  )
                }
                .clip(CircleShape)
                .background(
                  Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                  )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.42f), CircleShape)
                .clickable(onClick = onPlayPauseClick)
                .testTag("now_playing_play_pause")
          ) {
            if (playbackState.isResolvingVideo) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
              )
            } else {
              Icon(
                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playbackState.isPlaying) "Duraklat" else "Oynat",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
              )
            }
          }
          IconButton(onClick = onNextClick, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Sonraki", modifier = Modifier.size(34.dp))
          }
          StudioCircleIconButton(
            icon =
              if (playbackState.repeatMode == RepeatMode.ONE) Icons.Rounded.RepeatOne
              else Icons.Rounded.Repeat,
            contentDescription = "Tekrarla",
            onClick = onCycleRepeat,
            highlighted = playbackState.repeatMode != RepeatMode.NONE,
            tint =
              if (playbackState.repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Quick Bottom Actions + Bespoke Gradient Lyrics Button
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        ) {
          StudioCircleIconButton(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            contentDescription = "Listeye Ekle",
            onClick = { onOpenPlaylistPicker(track) },
            tint = MaterialTheme.colorScheme.onSurface
          )
          StudioGradientCtaButton(
            text = "Şarkı Sözleri",
            icon = Icons.Rounded.Lyrics,
            onClick = { showLyricsSheet = true },
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            modifier = Modifier.testTag("lyrics_button")
          )
          StudioCircleIconButton(
            icon = Icons.Rounded.Share,
            contentDescription = "Paylaş",
            onClick = { onShareTrack(track) },
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }

  if (showQueueSheet) {
    ModalBottomSheet(onDismissRequest = { showQueueSheet = false }) {
      Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Çalma Sırası (${playbackState.queue.size})", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          items(playbackState.queue, key = { it.trackId }) { item ->
            GlassStudioCard(
              highlighted = item.trackId == track.trackId,
              cornerRadius = 16.dp,
              onClick = {
                onSelectTrackFromQueue(item)
                showQueueSheet = false
              }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(12.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text(item.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CategoryBadgePill(label = item.categoryTag)
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  if (showLyricsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showLyricsSheet = false },
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      SyncedLyricsSheetContent(
        track = track,
        positionMs = playbackState.positionMs,
        lyricsState = lyricsState,
        onSeekTo = onSeekTo,
        onRefreshLyrics = onRefreshLyrics
      )
    }
  }
}
