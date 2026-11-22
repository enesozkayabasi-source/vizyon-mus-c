package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ArtistFanPost
import com.example.model.ArtistSummary
import com.example.model.FanPostTag
import com.example.model.MusicTrack
import com.example.model.UiState
import com.example.model.sanitizeFirestoreId
import com.example.ui.components.AnimatedWaveformBackdrop
import com.example.ui.components.CategoryBadgePill
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.StudioCircleIconButton
import com.example.ui.components.StudioFollowToggleButton
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioGradientCtaButton
import com.example.ui.components.StudioInteractiveChip
import com.example.ui.components.TrackRowItem
import com.example.ui.viewmodel.ArtistDetailData

@Composable
fun ArtistProfileScreen(
  artistDetailState: UiState<ArtistDetailData>,
  fanPostsState: UiState<List<ArtistFanPost>>,
  isFollowingArtist: Boolean,
  currentTrackId: String?,
  isPlaying: Boolean,
  favoriteIds: Set<String>,
  onBack: () -> Unit,
  onToggleFollow: (ArtistSummary) -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
  onToggleFavorite: (MusicTrack) -> Unit,
  onAddToQueue: (MusicTrack) -> Unit,
  onOpenPlaylistPicker: (MusicTrack) -> Unit,
  onShareTrack: (MusicTrack) -> Unit,
  onPostFanMessage: (String, String, FanPostTag) -> Unit,
  onCheerFanPost: (String, ArtistFanPost) -> Unit
) {
  BackHandler(onBack = onBack)

  var fanMessage by remember { mutableStateOf("") }
  var selectedTag by remember { mutableStateOf(FanPostTag.APPLAUSE) }

  Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier.fillMaxSize()) {
      AnimatedWaveformBackdrop(currentStep = 2, selectedGenreCount = 3)
      when (artistDetailState) {
        is UiState.Loading -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(24.dp)
          ) {
            Text(artistDetailState.message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
            StudioGlassButton(text = "Geri Dön", onClick = onBack)
          }
        }
        is UiState.Success -> {
          val artist = artistDetailState.data.artist
          val topTracks = artistDetailState.data.topTracks

          LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().navigationBarsPadding()
          ) {
            item {
              Box(modifier = Modifier.fillMaxWidth().height(290.dp)) {
                Image(
                  painter = painterResource(R.drawable.img_artist_spotlight),
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.fillMaxSize()
                )
                if (artist.imageUrl.isNotBlank()) {
                  AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    alpha = 0.72f,
                    modifier = Modifier.fillMaxSize()
                  )
                }
                Box(
                  modifier =
                    Modifier.fillMaxSize()
                      .background(
                        Brush.verticalGradient(
                          colors = listOf(Color(0x66070910), Color(0xCC070910), MaterialTheme.colorScheme.background)
                        )
                      )
                )

                StudioCircleIconButton(
                  icon = Icons.AutoMirrored.Rounded.ArrowBack,
                  contentDescription = "Geri",
                  tint = Color.White,
                  onClick = onBack,
                  modifier = Modifier.statusBarsPadding().padding(14.dp)
                )

                Column(
                  modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryBadgePill(label = artist.genres.firstOrNull() ?: "Onaylı Sanatçı")
                    Icon(
                      Icons.Rounded.Verified,
                      contentDescription = "Onaylı Sanatçı",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                  Text(text = artist.name, style = MaterialTheme.typography.displayLarge, color = Color.White)
                  Text(
                    text = "${formatFollowers(artist.followersCount)} takipçi • ${artist.genres.joinToString(" • ").ifBlank { "Stüdyo Sanatçısı" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                  )
                }
              }
            }

            item {
              Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                StudioFollowToggleButton(
                  isFollowing = isFollowingArtist,
                  followLabel = "Sanatçıyı Takip Et",
                  followingLabel = "Takip Ediliyor",
                  onClick = { onToggleFollow(artist) },
                  modifier = Modifier.weight(1f).testTag("artist_follow_btn")
                )
                StudioGradientCtaButton(
                  text = "Tümünü Çal",
                  icon = Icons.Rounded.PlayArrow,
                  enabled = topTracks.isNotEmpty(),
                  onClick = { if (topTracks.isNotEmpty()) onPlayTrack(topTracks.first(), topTracks) },
                  modifier = Modifier.weight(1f)
                )
              }
            }

            if (artist.bio.isNotBlank()) {
              item {
                Text(
                  text = artist.bio,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 16.dp)
                )
              }
            }

            item {
              Text(
                text = "Popüler Parçalar (${topTracks.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
              )
            }

            items(topTracks, key = { it.trackId }) { track ->
              val safeId = sanitizeFirestoreId(track.trackId)
              Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                TrackRowItem(
                  track = track,
                  isCurrentTrack = currentTrackId == track.trackId,
                  isPlaying = isPlaying && currentTrackId == track.trackId,
                  isFavorite = safeId in favoriteIds,
                  onPlayClick = { onPlayTrack(track, topTracks) },
                  onArtistClick = {},
                  onFavoriteToggle = { onToggleFavorite(track) },
                  onAddToQueue = { onAddToQueue(track) },
                  onOpenPlaylistPicker = { onOpenPlaylistPicker(track) },
                  onShareClick = { onShareTrack(track) }
                )
              }
            }

            item {
              GlassStudioCard(
                highlighted = true,
                cornerRadius = 20.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
              ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sanatçı Etkileşim Duvarı", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                  }
                  Text(
                    text = "İstek parça gönder, konser sor veya dinleyicilerle etkileşime geç.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )

                  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FanPostTag.entries) { tag ->
                      StudioInteractiveChip(
                        label = tag.label,
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag }
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    OutlinedTextField(
                      value = fanMessage,
                      onValueChange = { if (it.length <= 260) fanMessage = it },
                      placeholder = { Text("${artist.name} için mesajını yaz...") },
                      maxLines = 2,
                      modifier = Modifier.weight(1f).testTag("fan_wall_input")
                    )
                    StudioCircleIconButton(
                      icon = Icons.AutoMirrored.Rounded.Send,
                      contentDescription = "Gönder",
                      highlighted = fanMessage.isNotBlank(),
                      onClick = {
                        if (fanMessage.isNotBlank()) {
                          onPostFanMessage(artist.artistId, fanMessage, selectedTag)
                          fanMessage = ""
                        }
                      },
                      modifier = Modifier.testTag("fan_wall_send_btn")
                    )
                  }
                }
              }
            }

            when (fanPostsState) {
              is UiState.Loading -> {
                item {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                  }
                }
              }
              is UiState.Error -> {
                item {
                  Text(
                    fanPostsState.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                  )
                }
              }
              is UiState.Success -> {
                if (fanPostsState.data.isEmpty()) {
                  item {
                    Text(
                      text = "İlk hayran yorumunu sen bırak!",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.padding(horizontal = 16.dp)
                    )
                  }
                } else {
                  items(fanPostsState.data, key = { it.id }) { post ->
                    val tag = FanPostTag.fromRaw(post.tag)
                    GlassStudioCard(
                      highlighted = false,
                      cornerRadius = 18.dp,
                      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.SpaceBetween,
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text(
                            text = "${post.authorName} (${post.authorHandle})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                          )
                          Surface(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(999.dp)
                          ) {
                            Text(
                              text = tag.label,
                              style = MaterialTheme.typography.labelSmall,
                              color = MaterialTheme.colorScheme.secondary,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                          }
                        }
                        Text(post.message, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onCheerFanPost(artist.artistId, post) }) {
                          Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Alkışla (${post.cheersCount})")
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
}

private fun formatFollowers(count: Long): String =
  when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}B"
    else -> count.toString()
  }
