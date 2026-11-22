package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AccentPalette
import com.example.model.ArtistSummary
import com.example.model.FollowedArtist
import com.example.model.MusicTrack
import com.example.model.Playlist
import com.example.model.PlaylistCoverStyle
import com.example.model.SavedTrack
import com.example.model.TrackSource
import com.example.model.UiState
import com.example.model.sanitizeFirestoreId
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.StudioCircleIconButton
import com.example.ui.components.StudioGradientCtaButton
import com.example.ui.components.StudioInteractiveChip
import com.example.ui.components.TrackRowItem
import com.example.ui.components.parseColorSafe

@Composable
fun LibraryScreen(
  favoritesState: UiState<List<SavedTrack>>,
  playlistsState: UiState<List<Playlist>>,
  selectedPlaylistId: String?,
  selectedPlaylistTracksState: UiState<List<SavedTrack>>,
  followedArtistsState: UiState<List<FollowedArtist>>,
  currentTrackId: String?,
  isPlaying: Boolean,
  favoriteIds: Set<String>,
  onSelectPlaylist: (String?) -> Unit,
  onCreatePlaylist: (String, String, String, PlaylistCoverStyle, Boolean) -> Unit,
  onDeletePlaylist: (String) -> Unit,
  onRemoveTrackFromPlaylist: (String, String) -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
  onArtistClick: (ArtistSummary) -> Unit,
  onToggleFavorite: (MusicTrack) -> Unit,
  onAddToQueue: (MusicTrack) -> Unit,
  onOpenPlaylistPicker: (MusicTrack) -> Unit,
  onShareTrack: (MusicTrack) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var showCreatePlaylistDialog by remember { mutableStateOf(false) }

  val playlists = (playlistsState as? UiState.Success)?.data.orEmpty()
  val activePlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

  if (activePlaylist != null) {
    BackHandler { onSelectPlaylist(null) }
    PlaylistDetailView(
      playlist = activePlaylist,
      tracksState = selectedPlaylistTracksState,
      currentTrackId = currentTrackId,
      isPlaying = isPlaying,
      favoriteIds = favoriteIds,
      onBack = { onSelectPlaylist(null) },
      onPlayTrack = onPlayTrack,
      onRemoveFromPlaylist = { trackId -> onRemoveTrackFromPlaylist(activePlaylist.id, trackId) },
      onArtistClick = onArtistClick,
      onToggleFavorite = onToggleFavorite,
      onAddToQueue = onAddToQueue,
      onOpenPlaylistPicker = onOpenPlaylistPicker,
      onShareTrack = onShareTrack
    )
    return
  }

  Scaffold(
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onBackground,
    floatingActionButton = {
      if (selectedTab == 1) {
        val fabGlow = MaterialTheme.colorScheme.primary
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(58.dp)
              .drawBehind {
                drawCircle(
                  color = fabGlow.copy(alpha = 0.24f),
                  radius = size.minDimension / 2f + 3.dp.toPx(),
                  style = Stroke(width = 4.dp.toPx())
                )
              }
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
              )
              .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
              .clickable { showCreatePlaylistDialog = true }
              .testTag("create_playlist_fab")
        ) {
          Icon(
            Icons.Rounded.Add,
            contentDescription = "Yeni çalma listesi oluştur",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(top = 14.dp)
    ) {
      Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
          text = "KİŞİSEL ARŞİV",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        Text("Kitaplığım", style = MaterialTheme.typography.headlineLarge)
        Text(
          text = "Favorilerin, özel çalma listelerin ve takip ettiğin sanatçılar",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Spacer(modifier = Modifier.height(12.dp))

      val tabs = listOf("Favoriler", "Çalma Listeleri", "Sanatçılar")
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
      ) {
        tabs.forEachIndexed { idx, label ->
          StudioInteractiveChip(
            label = label,
            selected = selectedTab == idx,
            onClick = { selectedTab = idx }
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))

      when (selectedTab) {
        0 -> {
          when (favoritesState) {
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(favoritesState.message)
            is UiState.Success -> {
              val favTracks = favoritesState.data.map { it.toMusicTrack() }
              if (favTracks.isEmpty()) {
                EmptyLibraryMessage("Henüz favori parçan yok. Beğendiğin parçaların kalp ikonuna dokun.")
              } else {
                LazyColumn(
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  items(favTracks, key = { it.trackId }) { track ->
                    TrackRowItem(
                      track = track,
                      isCurrentTrack = currentTrackId == track.trackId,
                      isPlaying = isPlaying && currentTrackId == track.trackId,
                      isFavorite = true,
                      onPlayClick = { onPlayTrack(track, favTracks) },
                      onArtistClick = {
                        onArtistClick(
                          ArtistSummary(track.artistId, track.artistName, track.artworkUrl, source = track.source)
                        )
                      },
                      onFavoriteToggle = { onToggleFavorite(track) },
                      onAddToQueue = { onAddToQueue(track) },
                      onOpenPlaylistPicker = { onOpenPlaylistPicker(track) },
                      onShareClick = { onShareTrack(track) }
                    )
                  }
                }
              }
            }
          }
        }
        1 -> {
          when (playlistsState) {
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(playlistsState.message)
            is UiState.Success -> {
              if (playlists.isEmpty()) {
                EmptyLibraryMessage("Özel çalma listeni oluşturmak için sağ alttaki + butonuna dokun.")
              } else {
                LazyColumn(
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  items(playlists, key = { it.id }) { pl ->
                    val accent = parseColorSafe(pl.accentColorHex, MaterialTheme.colorScheme.primary)
                    GlassStudioCard(
                      highlighted = false,
                      cornerRadius = 18.dp,
                      onClick = { onSelectPlaylist(pl.id) },
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                      ) {
                        Box(
                          contentAlignment = Alignment.Center,
                          modifier =
                            Modifier.size(54.dp)
                              .clip(RoundedCornerShape(14.dp))
                              .background(accent.copy(alpha = 0.22f))
                              .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        ) {
                          Icon(Icons.Rounded.LibraryMusic, null, tint = accent)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(pl.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                          if (pl.description.isNotBlank()) {
                            Text(
                              pl.description,
                              style = MaterialTheme.typography.bodySmall,
                              color = MaterialTheme.colorScheme.onSurfaceVariant,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis
                            )
                          }
                          Text(
                            "${pl.trackCount} parça • ${PlaylistCoverStyle.fromRaw(pl.coverStyle).label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                          )
                        }
                        IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                          Icon(
                            Icons.Rounded.DeleteOutline,
                            contentDescription = "Sil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        2 -> {
          when (followedArtistsState) {
            is UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(followedArtistsState.message)
            is UiState.Success -> {
              val artists = followedArtistsState.data
              if (artists.isEmpty()) {
                EmptyLibraryMessage("Takip ettiğin sanatçılar burada listelenir.")
              } else {
                LazyColumn(
                  contentPadding = PaddingValues(16.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  items(artists, key = { it.artistId }) { art ->
                    GlassStudioCard(
                      highlighted = false,
                      cornerRadius = 18.dp,
                      onClick = {
                        onArtistClick(
                          ArtistSummary(
                            artistId = art.artistId,
                            name = art.name,
                            imageUrl = art.imageUrl,
                            genres = art.genres,
                            source = TrackSource.fromRaw(art.source)
                          )
                        )
                      },
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                      ) {
                        AsyncImage(
                          model = art.imageUrl.ifBlank { null },
                          contentDescription = art.name,
                          placeholder = painterResource(R.drawable.img_artist_spotlight),
                          error = painterResource(R.drawable.img_artist_spotlight),
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(art.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                          Text(
                            art.genres.joinToString(" • ").ifBlank { "Vizyon Sanatçısı" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
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

  if (showCreatePlaylistDialog) {
    CreatePlaylistDialog(
      onDismiss = { showCreatePlaylistDialog = false },
      onCreate = { title, desc, accentHex, coverStyle, shared ->
        onCreatePlaylist(title, desc, accentHex, coverStyle, shared)
        showCreatePlaylistDialog = false
      }
    )
  }
}

@Composable
private fun PlaylistDetailView(
  playlist: Playlist,
  tracksState: UiState<List<SavedTrack>>,
  currentTrackId: String?,
  isPlaying: Boolean,
  favoriteIds: Set<String>,
  onBack: () -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
  onRemoveFromPlaylist: (String) -> Unit,
  onArtistClick: (ArtistSummary) -> Unit,
  onToggleFavorite: (MusicTrack) -> Unit,
  onAddToQueue: (MusicTrack) -> Unit,
  onOpenPlaylistPicker: (MusicTrack) -> Unit,
  onShareTrack: (MusicTrack) -> Unit
) {
  val accent = parseColorSafe(playlist.accentColorHex, MaterialTheme.colorScheme.primary)
  val tracks = (tracksState as? UiState.Success)?.data?.map { it.toMusicTrack() }.orEmpty()

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize()
  ) {
    item {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioCircleIconButton(
          icon = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Geri",
          onClick = onBack
        )
        Text(playlist.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
      }
    }

    item {
      GlassStudioCard(
        highlighted = true,
        cornerRadius = 20.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(16.dp)
        ) {
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(playlist.title, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            if (playlist.description.isNotBlank()) {
              Text(playlist.description, style = MaterialTheme.typography.bodySmall)
            }
            Text("${tracks.size} parça", style = MaterialTheme.typography.labelMedium)
          }
          StudioGradientCtaButton(
            text = "Tümünü Çal",
            icon = Icons.Rounded.PlayArrow,
            enabled = tracks.isNotEmpty(),
            onClick = { if (tracks.isNotEmpty()) onPlayTrack(tracks.first(), tracks) }
          )
        }
      }
    }

    if (tracks.isEmpty()) {
      item {
        EmptyLibraryMessage("Bu listede henüz parça yok. Keşfet veya Arama ekranından parça ekleyebilirsin.")
      }
    } else {
      items(tracks, key = { it.trackId }) { track ->
        val safeId = sanitizeFirestoreId(track.trackId)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          TrackRowItem(
            track = track,
            isCurrentTrack = currentTrackId == track.trackId,
            isPlaying = isPlaying && currentTrackId == track.trackId,
            isFavorite = safeId in favoriteIds,
            onPlayClick = { onPlayTrack(track, tracks) },
            onArtistClick = {
              onArtistClick(ArtistSummary(track.artistId, track.artistName, track.artworkUrl, source = track.source))
            },
            onFavoriteToggle = { onToggleFavorite(track) },
            onAddToQueue = { onAddToQueue(track) },
            onOpenPlaylistPicker = { onOpenPlaylistPicker(track) },
            onShareClick = { onShareTrack(track) }
          )
          TextButton(
            onClick = { onRemoveFromPlaylist(track.trackId) },
            modifier = Modifier.align(Alignment.End)
          ) {
            Text("Listeden Çıkar", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

@Composable
private fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String, PlaylistCoverStyle, Boolean) -> Unit
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedAccent by remember { mutableStateOf(AccentPalette.VIZYON_BLUE) }
  var selectedCover by remember { mutableStateOf(PlaylistCoverStyle.CHROME) }
  var sharedPublic by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Yeni Çalma Listesi", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { if (it.length <= 70) title = it },
          label = { Text("Liste Adı") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("playlist_title_input")
        )
        OutlinedTextField(
          value = description,
          onValueChange = { if (it.length <= 200) description = it },
          label = { Text("Açıklama (isteğe bağlı)") },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth()
        )
        Text("Tema Rengi", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          AccentPalette.entries.forEach { pal ->
            val col = Color(pal.primaryHex)
            Box(
              modifier =
                Modifier.size(30.dp)
                  .clip(CircleShape)
                  .background(col)
                  .border(
                    width = if (selectedAccent == pal) 3.dp else 0.dp,
                    color = Color.White,
                    shape = CircleShape
                  )
                  .clickable { selectedAccent = pal }
            )
          }
        }
        Text("Kapak Stili", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PlaylistCoverStyle.entries.take(3).forEach { style ->
            StudioInteractiveChip(
              label = style.label,
              selected = selectedCover == style,
              onClick = { selectedCover = style }
            )
          }
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Herkese açık liste", style = MaterialTheme.typography.bodyMedium)
          Switch(checked = sharedPublic, onCheckedChange = { sharedPublic = it })
        }
      }
    },
    confirmButton = {
      StudioGradientCtaButton(
        text = "Oluştur",
        onClick = {
          onCreate(
            title.ifBlank { "Özel Listem" },
            description,
            selectedAccent.hexString,
            selectedCover,
            sharedPublic
          )
        },
        modifier = Modifier.testTag("save_playlist_btn")
      )
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Vazgeç") }
    }
  )
}

@Composable
private fun LoadingBox() {
  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
    CircularProgressIndicator()
  }
}

@Composable
private fun ErrorBox(msg: String) {
  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
    Text(msg, color = MaterialTheme.colorScheme.error)
  }
}

@Composable
private fun EmptyLibraryMessage(msg: String) {
  Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(28.dp)) {
    Text(
      text = msg,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
