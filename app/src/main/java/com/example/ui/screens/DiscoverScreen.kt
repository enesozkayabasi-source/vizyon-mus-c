package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ArtistSummary
import com.example.model.MusicCategory
import com.example.model.MusicCategoryGroup
import com.example.model.MusicTrack
import com.example.model.UiState
import com.example.model.sanitizeFirestoreId
import com.example.ui.components.CategoryBadgePill
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.LiveSoundwaveEqualizer
import com.example.ui.components.StudioAvatar
import com.example.ui.components.StudioCircleIconButton
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioGradientCtaButton
import com.example.ui.components.StudioInteractiveChip
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.TrackRowItem
import com.example.ui.viewmodel.HomeCatalogData

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverScreen(
  catalogState: UiState<HomeCatalogData>,
  userName: String,
  userPhotoUrl: String,
  unreadNotificationCount: Int,
  preferredGenres: List<String>,
  currentTrackId: String?,
  isPlaying: Boolean,
  favoriteIds: Set<String>,
  lastCatalogRefreshMs: Long = 0L,
  isAutoRefreshing: Boolean = false,
  onRefresh: () -> Unit,
  onOpenNotifications: () -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
  onArtistClick: (ArtistSummary) -> Unit,
  onToggleFavorite: (MusicTrack) -> Unit,
  onAddToQueue: (MusicTrack) -> Unit,
  onOpenPlaylistPicker: (MusicTrack) -> Unit,
  onShareTrack: (MusicTrack) -> Unit,
  onOpenStudio: () -> Unit
) {
  var selectedCategoryGroup by remember { mutableStateOf<MusicCategoryGroup?>(null) }
  var selectedCategory by remember { mutableStateOf<MusicCategory?>(null) }

  LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.fillMaxSize().testTag("discover_screen")
  ) {
    // Editorial Studio Header with Custom Avatar & Notification Bell
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          StudioAvatar(
            photoUrlOrPath = userPhotoUrl,
            contentDescription = "Vizyon Music Profil",
            size = 46.dp,
            modifier =
              Modifier.clickable(onClick = onOpenStudio)
                .testTag("discover_profile_btn")
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "VİZYON STUDIO",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Merhaba, ${userName.substringBefore(" ").ifBlank { "Dinleyici" }}",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          StudioCircleIconButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "Bildirim Kutusu",
            onClick = onOpenNotifications,
            highlighted = unreadNotificationCount > 0,
            badgeCount = unreadNotificationCount,
            modifier = Modifier.testTag("notifications_bell_btn")
          )
          StudioCircleIconButton(
            icon = Icons.Rounded.Refresh,
            contentDescription = "Kataloğu yenile",
            onClick = onRefresh,
            modifier = Modifier.testTag("refresh_catalog_btn")
          )
        }
      }
    }

    // Redesigned Hero Studio Stage ("Günün Vitrini") with Live Waveform & Bespoke Buttons
    item {
      val heroTracks = (catalogState as? UiState.Success)?.data?.tracks.orEmpty()
      val personalizedQueue =
        if (preferredGenres.isEmpty()) heroTracks
        else {
          val matching = heroTracks.filter { it.categoryTag in preferredGenres }
          matching.ifEmpty { heroTracks }
        }
      val leadTrack = personalizedQueue.firstOrNull()
      val genreSubtitle =
        if (preferredGenres.isNotEmpty()) preferredGenres.take(3).joinToString(" • ")
        else "iTunes Seçkisi • Resmi Video"

      GlassStudioCard(
        highlighted = true,
        cornerRadius = 24.dp
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            painter = painterResource(R.drawable.img_hero_stage),
            contentDescription = "Günün Vitrini",
            contentScale = ContentScale.Crop,
            alpha = 0.34f,
            modifier = Modifier.matchParentSize()
          )
          Box(
            modifier =
              Modifier.matchParentSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color(0x4407132B), Color(0xCC060D1E), Color(0xF0050A14))
                  )
                )
          )
          Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(18.dp)
          ) {
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                shape = RoundedCornerShape(999.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                  Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "GÜNÜN VİTRİNİ • KİŞİSEL KARIŞIM",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
              Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.testTag("discover_refresh_status_pill")
              ) {
                val minsAgo =
                  if (lastCatalogRefreshMs > 0L) ((System.currentTimeMillis() - lastCatalogRefreshMs) / 60_000L).coerceAtLeast(0L)
                  else 0L
                val statusLabel =
                  when {
                    isAutoRefreshing -> "Yenileniyor..."
                    lastCatalogRefreshMs <= 0L -> "30 dk oto yenileme"
                    minsAgo == 0L -> "Az önce • 30 dk oto"
                    else -> "${minsAgo} dk önce • 30 dk oto"
                  }
                Text(
                  text = statusLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFD6E2F5),
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              AsyncImage(
                model = leadTrack?.artworkUrl?.ifBlank { null },
                contentDescription = leadTrack?.title ?: "Günün Vitrini",
                placeholder = painterResource(R.drawable.img_playlist_cover),
                error = painterResource(R.drawable.img_playlist_cover),
                contentScale = ContentScale.Crop,
                modifier =
                  Modifier.size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.2.dp, Color.White.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
              )
              Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = leadTrack?.title ?: "Kişisel Vitrinin Hazırlanıyor",
                  style = MaterialTheme.typography.titleLarge,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = leadTrack?.let { "${it.artistName} • $genreSubtitle" } ?: genreSubtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFD6E2F5),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            LiveSoundwaveEqualizer(
              barCount = 26,
              energyBoost = if (isPlaying) 1.05f else 0.72f,
              height = 24.dp
            )

            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              StudioGradientCtaButton(
                text = "Vitrini Başlat",
                icon = Icons.Rounded.PlayArrow,
                enabled = leadTrack != null,
                onClick = { if (leadTrack != null) onPlayTrack(leadTrack, personalizedQueue) },
                modifier = Modifier.weight(1f).testTag("hero_play_button")
              )
              StudioGlassButton(
                text = "Karışık Çal",
                icon = Icons.Rounded.Shuffle,
                enabled = personalizedQueue.isNotEmpty(),
                tintColor = Color.White,
                containerColor = Color.White.copy(alpha = 0.12f),
                borderColor = Color.White.copy(alpha = 0.34f),
                onClick = {
                  if (personalizedQueue.isNotEmpty()) {
                    val shuffled = personalizedQueue.shuffled()
                    onPlayTrack(shuffled.first(), shuffled)
                  }
                },
                modifier = Modifier.testTag("hero_shuffle_button")
              )
            }
          }
        }
      }
    }

    when (catalogState) {
      is UiState.Loading -> {
        item {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
          ) {
            CircularProgressIndicator()
          }
        }
      }
      is UiState.Error -> {
        item {
          GlassStudioCard(
            accentColor = MaterialTheme.colorScheme.error,
            cornerRadius = 18.dp
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(catalogState.message, color = MaterialTheme.colorScheme.error)
              StudioGradientCtaButton(text = "Tekrar Dene", onClick = onRefresh)
            }
          }
        }
      }
      is UiState.Success -> {
        val data = catalogState.data

        // Featured Artists Carousel
        if (data.artists.isNotEmpty()) {
          item {
            StudioSectionHeader(
              title = "Öne Çıkan Sanatçılar",
              badge = "${data.artists.size} Sahne"
            )
          }
          item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              items(data.artists, key = { it.artistId }) { artist ->
                GlassStudioCard(
                  cornerRadius = 20.dp,
                  onClick = { onArtistClick(artist) },
                  modifier =
                    Modifier.width(140.dp)
                      .testTag("artist_card_${artist.artistId}")
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    AsyncImage(
                      model = artist.imageUrl.ifBlank { null },
                      contentDescription = artist.name,
                      placeholder = painterResource(R.drawable.img_artist_spotlight),
                      error = painterResource(R.drawable.img_artist_spotlight),
                      contentScale = ContentScale.Crop,
                      modifier =
                        Modifier.size(84.dp)
                          .clip(CircleShape)
                          .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), CircleShape)
                    )
                    Text(
                      text = artist.name,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    CategoryBadgePill(label = artist.genres.firstOrNull() ?: "Sanatçı")
                  }
                }
              }
            }
          }
        }

        // Music Genre / Mood Filter Chips with Turkish Music categorization
        item {
          val visibleCategories =
            remember(selectedCategoryGroup) {
              if (selectedCategoryGroup == null) MusicCategory.entries
              else MusicCategory.entries.filter { it.group == selectedCategoryGroup }
            }
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudioSectionHeader(
              title = "Kategoriler & Türk Müziği Seçkisi",
              subtitle = "Türkçe Pop, Rap, Anadolu Rock, Arabesk, Halk & Sanat Müziği ve daha fazlası"
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                StudioInteractiveChip(
                  label = "Tümü (${MusicCategory.entries.size})",
                  selected = selectedCategoryGroup == null,
                  onClick = {
                    selectedCategoryGroup = null
                    selectedCategory = null
                  },
                  modifier = Modifier.testTag("category_group_ALL")
                )
              }
              items(listOf(MusicCategoryGroup.TURKISH, MusicCategoryGroup.GLOBAL)) { group ->
                StudioInteractiveChip(
                  label = group.label,
                  selected = selectedCategoryGroup == group,
                  onClick = {
                    selectedCategoryGroup = if (selectedCategoryGroup == group) null else group
                    if (selectedCategory != null && selectedCategoryGroup != null && selectedCategory?.group != selectedCategoryGroup) {
                      selectedCategory = null
                    }
                  },
                  modifier = Modifier.testTag("category_group_${group.name}")
                )
              }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              item {
                StudioInteractiveChip(
                  label = "Karışık",
                  selected = selectedCategory == null,
                  onClick = { selectedCategory = null },
                  modifier = Modifier.testTag("category_chip_ALL")
                )
              }
              items(visibleCategories, key = { it.name }) { category ->
                StudioInteractiveChip(
                  label = category.label,
                  selected = selectedCategory == category,
                  onClick = {
                    selectedCategory = if (selectedCategory == category) null else category
                  },
                  modifier = Modifier.testTag("category_chip_${category.name}")
                )
              }
            }
          }
        }

        val filteredTracks =
          if (selectedCategory != null) {
            val matching = data.tracks.filter { it.categoryTag == selectedCategory!!.label }
            matching.ifEmpty { data.tracks }
          } else if (selectedCategoryGroup != null) {
            val groupLabels = MusicCategory.entries.filter { it.group == selectedCategoryGroup }.map { it.label }.toSet()
            val matching = data.tracks.filter { it.categoryTag in groupLabels }
            matching.ifEmpty { data.tracks }
          } else {
            data.tracks
          }

        items(filteredTracks, key = { it.trackId }) { track ->
          val safeId = sanitizeFirestoreId(track.trackId)
          TrackRowItem(
            track = track,
            isCurrentTrack = currentTrackId == track.trackId,
            isPlaying = isPlaying && currentTrackId == track.trackId,
            isFavorite = safeId in favoriteIds,
            onPlayClick = { onPlayTrack(track, filteredTracks) },
            onArtistClick = {
              onArtistClick(
                ArtistSummary(
                  artistId = track.artistId,
                  name = track.artistName,
                  imageUrl = track.artworkUrl,
                  source = track.source
                )
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
