package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ArtistSummary
import com.example.model.MusicTrack
import com.example.model.SearchAutocompleteSuggestion
import com.example.model.UiState
import com.example.model.sanitizeFirestoreId
import com.example.ui.components.CategoryBadgePill
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.StudioInteractiveChip
import com.example.ui.components.StudioSectionHeader
import com.example.ui.components.TrackRowItem
import com.example.ui.viewmodel.SearchResultData

@Composable
fun SearchScreen(
  searchState: UiState<SearchResultData>,
  suggestions: List<SearchAutocompleteSuggestion> = emptyList(),
  currentTrackId: String?,
  isPlaying: Boolean,
  favoriteIds: Set<String>,
  onQueryChange: (String) -> Unit = {},
  onSearchSubmit: (String) -> Unit,
  onPlayTrack: (MusicTrack, List<MusicTrack>) -> Unit,
  onArtistClick: (ArtistSummary) -> Unit,
  onToggleFavorite: (MusicTrack) -> Unit,
  onAddToQueue: (MusicTrack) -> Unit,
  onOpenPlaylistPicker: (MusicTrack) -> Unit,
  onShareTrack: (MusicTrack) -> Unit
) {
  var queryText by remember { mutableStateOf("") }
  var showSuggestions by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val quickTags = remember {
    listOf(
      "Türkçe Pop",
      "Anadolu Rock",
      "Türkçe Rap & Trap",
      "Arabesk & Fantezi",
      "Türk Halk Müziği",
      "Türk Sanat Müziği",
      "Türkçe Alternatif",
      "Caz & Lo-Fi",
      "Elektronik & Dans"
    )
  }

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Text("Müzik Ara", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(
      text = "Yazarken anlık sanatçı ve şarkı tahminleri görünür; mini oynatıcı kesintisiz devam eder",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))

    GlassStudioCard(cornerRadius = 18.dp) {
      OutlinedTextField(
        value = queryText,
        onValueChange = { next ->
          queryText = next
          showSuggestions = next.trim().length >= 2
          onQueryChange(next)
        },
        placeholder = { Text("Şarkı, sanatçı veya tarz ara (örn: müs)...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
          if (queryText.isNotEmpty()) {
            IconButton(onClick = {
              queryText = ""
              showSuggestions = false
              onQueryChange("")
              onSearchSubmit("")
            }) {
              Icon(Icons.Rounded.Clear, contentDescription = "Temizle")
            }
          }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
          showSuggestions = false
          focusManager.clearFocus()
          onSearchSubmit(queryText)
        }),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().testTag("search_input")
      )
    }

    AnimatedVisibility(visible = showSuggestions && queryText.trim().length >= 2 && suggestions.isNotEmpty()) {
      GlassStudioCard(
        highlighted = true,
        cornerRadius = 18.dp,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("search_autocomplete_card")
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          suggestions.forEachIndexed { idx, item ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    queryText = item.text
                    showSuggestions = false
                    focusManager.clearFocus()
                    onSearchSubmit(item.text)
                  }
                  .padding(horizontal = 14.dp, vertical = 11.dp)
                  .testTag("search_suggestion_$idx")
            ) {
              Icon(
                imageVector = if (item.isLiveApi) Icons.Rounded.MusicNote else Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = if (item.isLiveApi) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.text,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = item.subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              Icon(
                imageVector = Icons.Rounded.NorthWest,
                contentDescription = "Seç",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
            if (idx < suggestions.lastIndex) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(quickTags) { tag ->
        StudioInteractiveChip(
          label = tag,
          selected = queryText.equals(tag, ignoreCase = true),
          onClick = {
            queryText = tag
            showSuggestions = false
            onSearchSubmit(tag)
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    when (searchState) {
      is UiState.Loading -> {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Text(
          text = searchState.message,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium
        )
      }
      is UiState.Success -> {
        val data = searchState.data
        if (data.query.isBlank()) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            GlassStudioCard(cornerRadius = 24.dp, modifier = Modifier.padding(horizontal = 8.dp)) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(24.dp)
              ) {
                Icon(
                  Icons.Rounded.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(48.dp)
                )
                Text(
                  text = "Yukarıdan bir sanatçı veya parça ara",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "Çalan parçayı durdurmadan yeni şarkılar keşfet, mini video penceresinden dilediğin an tam ekrana geç.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        } else {
          val uniqueArtists = remember(data.artists) { data.artists.distinctBy { it.artistId } }
          val uniqueTracks = remember(data.tracks) { data.tracks.distinctBy { it.trackId } }
          LazyColumn(
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            if (uniqueArtists.isNotEmpty()) {
              item {
                StudioSectionHeader(title = "Sanatçılar", badge = "${uniqueArtists.size}")
              }
              item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  items(uniqueArtists, key = { it.artistId }) { artist ->
                    GlassStudioCard(
                      cornerRadius = 18.dp,
                      onClick = { onArtistClick(artist) },
                      modifier = Modifier.width(160.dp)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                      ) {
                        AsyncImage(
                          model = artist.imageUrl.ifBlank { null },
                          contentDescription = artist.name,
                          placeholder = painterResource(R.drawable.img_artist_spotlight),
                          error = painterResource(R.drawable.img_artist_spotlight),
                          contentScale = ContentScale.Crop,
                          modifier = Modifier.size(42.dp).clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(
                            text = artist.name,
                            style = MaterialTheme.typography.labelLarge,
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
            }

            item {
              StudioSectionHeader(
                title = "Şarkılar",
                badge = "${uniqueTracks.size} sonuç"
              )
            }
            if (uniqueTracks.isEmpty()) {
              item {
                Text(
                  text = "Eşleşen sonuç bulunamadı. Farklı bir sanatçı veya parça adı deneyin.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              items(uniqueTracks, key = { it.trackId }) { track ->
                val safeId = sanitizeFirestoreId(track.trackId)
                TrackRowItem(
                  track = track,
                  isCurrentTrack = currentTrackId == track.trackId,
                  isPlaying = isPlaying && currentTrackId == track.trackId,
                  isFavorite = safeId in favoriteIds,
                  onPlayClick = { onPlayTrack(track, uniqueTracks) },
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
    }
  }
}
