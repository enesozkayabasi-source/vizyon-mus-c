package com.example.ui.components

import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.core.view.WindowCompat
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.model.MusicStoryItem
import com.example.model.MusicTrack
import com.example.model.StoryCardTheme
import com.example.model.TrackSource
import com.example.model.formatRelativeTimeTr
import com.example.ui.stories.StoryBitmapRenderer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryStudioSheet(
  userName: String,
  userHandle: String,
  userPhotoUrl: String,
  initialTrack: MusicTrack?,
  candidateTracks: List<MusicTrack>,
  initialTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
  onDismiss: () -> Unit,
  onPublishStory: (MusicTrack, String, StoryCardTheme) -> Unit,
  onNotifyMessage: (String) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val fallbackTrack = remember(userName) {
    MusicTrack(
      trackId = "vizyon_studio_session",
      title = "Gece Stüdyo Seçkisi",
      artistId = "vizyon_artist",
      artistName = userName.ifBlank { "Vizyon Dinleyicisi" },
      source = TrackSource.HYBRID
    )
  }
  val trackOptions = remember(initialTrack, candidateTracks, fallbackTrack) {
    (listOfNotNull(initialTrack) + candidateTracks).distinctBy { it.trackId }.ifEmpty { listOf(fallbackTrack) }
  }
  var selectedTrack by remember(trackOptions) { mutableStateOf(initialTrack ?: trackOptions.first()) }
  var selectedTheme by remember(initialTheme) { mutableStateOf(initialTheme) }
  var noteText by remember { mutableStateOf("") }
  var isRenderingShare by remember { mutableStateOf(false) }

  val previewStory = remember(userName, userHandle, userPhotoUrl, selectedTrack, noteText, selectedTheme) {
    MusicStoryItem(
      id = "preview",
      userId = "self",
      userName = userName.ifBlank { "Vizyon Dinleyicisi" },
      userHandle = userHandle.ifBlank { "@vizyon" },
      userAvatarUrl = userPhotoUrl,
      trackId = selectedTrack.trackId,
      trackTitle = selectedTrack.title,
      artistId = selectedTrack.artistId,
      artistName = selectedTrack.artistName,
      albumArtUrl = selectedTrack.artworkUrl,
      youtubeVideoId = selectedTrack.youtubeVideoId,
      source = selectedTrack.source.name,
      note = noteText.trim(),
      cardTheme = selectedTheme.name
    )
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("story_studio_sheet")
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(max = 680.dp)
          .imePadding()
          .navigationBarsPadding()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "HİKAYE KARTI STÜDYOSU",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Hikaye Oluştur & Paylaş",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Hikaye stüdyosunu kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_story_studio_btn")
        )
      }

      Column(
        modifier =
          Modifier.weight(1f, fill = false)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // 1. Compact Live Preview of Story Card
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          StoryCardVisualPreview(
            story = previewStory,
            modifier =
              Modifier.width(176.dp)
                .aspectRatio(9f / 13f)
                .testTag("story_card_preview")
          )
        }

        // 2. Theme Selector
        Text(
          text = "Kart Teması",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(StoryCardTheme.entries) { theme ->
            val selected = selectedTheme == theme
            val accent = Color(theme.accentColorHex)
            Surface(
              color = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(999.dp),
              modifier =
                Modifier.clip(RoundedCornerShape(999.dp))
                  .border(
                    width = if (selected) 1.6.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(999.dp)
                  )
                  .clickable { selectedTheme = theme }
                  .testTag("story_theme_${theme.name}")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
              ) {
                Box(
                  modifier =
                    Modifier.size(14.dp)
                      .clip(CircleShape)
                      .background(Brush.linearGradient(listOf(Color(theme.startColorHex), accent)))
                )
                Text(
                  text = theme.label,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }
        }

        // 3. Track Picker
        if (trackOptions.size > 1) {
          Text(
            text = "Parça Seç",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trackOptions, key = { it.trackId }) { tr ->
              StudioInteractiveChip(
                label = "${tr.title} • ${tr.artistName}",
                selected = selectedTrack.trackId == tr.trackId,
                leadingIcon = Icons.Rounded.MusicNote,
                onClick = { selectedTrack = tr }
              )
            }
          }
        }

        // 4. Mood Note
        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it.take(140) },
          label = { Text("Hikaye Notu (isteğe bağlı, maks 140)") },
          placeholder = { Text("Örn: Bu gece ritim hiç durmuyor...") },
          maxLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().testTag("story_note_input")
        )
      }

      // 5. Always-visible Sticky Action Footer (In-App Story + External PNG Share)
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier =
          Modifier.fillMaxWidth()
            .border(
              width = 0.8.dp,
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
            )
            .testTag("story_studio_sticky_footer")
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
          StudioGradientCtaButton(
            text = "Hikayemde Paylaş (24 Saat)",
            icon = Icons.Rounded.AutoAwesome,
            onClick = {
              onPublishStory(selectedTrack, noteText, selectedTheme)
              onDismiss()
            },
            modifier = Modifier.fillMaxWidth().testTag("publish_story_btn")
          )
          StudioGlassButton(
            text = if (isRenderingShare) "PNG Kart Hazırlanıyor..." else "Diğer Uygulamalarda Paylaş (PNG)",
            icon = Icons.Rounded.Share,
            enabled = !isRenderingShare,
            onClick = {
              isRenderingShare = true
              scope.launch {
                val uri = StoryBitmapRenderer.renderAndCacheStoryPngUri(context, previewStory)
                isRenderingShare = false
                if (uri != null) {
                  StoryBitmapRenderer.launchExternalStoryShareChooser(context, uri, previewStory)
                } else {
                  onNotifyMessage("Hikaye kartı görseli oluşturulamadı.")
                }
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("share_story_png_btn")
          )
        }
      }
    }
  }
}

@Composable
fun StoryViewerOverlay(
  story: MusicStoryItem,
  isAuthor: Boolean,
  onDismiss: () -> Unit,
  onPlayTrack: (MusicTrack) -> Unit,
  onDeleteStory: (String) -> Unit,
  onNotifyMessage: (String) -> Unit,
  onOpenAuthorProfile: (() -> Unit)? = null,
  onMessageAuthor: (() -> Unit)? = null
) {
  BackHandler(enabled = true) { onDismiss() }
  Box(
    modifier =
      Modifier.fillMaxSize()
        .pointerInput(Unit) {
          detectTapGestures { }
        }
  ) {
    StoryViewerContent(
      story = story,
      isAuthor = isAuthor,
      onDismiss = onDismiss,
      onPlayTrack = onPlayTrack,
      onDeleteStory = onDeleteStory,
      onNotifyMessage = onNotifyMessage,
      onOpenAuthorProfile = onOpenAuthorProfile,
      onMessageAuthor = onMessageAuthor
    )
  }
}

@Composable
fun StoryViewerDialog(
  story: MusicStoryItem,
  isAuthor: Boolean,
  onDismiss: () -> Unit,
  onPlayTrack: (MusicTrack) -> Unit,
  onDeleteStory: (String) -> Unit,
  onNotifyMessage: (String) -> Unit,
  onOpenAuthorProfile: (() -> Unit)? = null,
  onMessageAuthor: (() -> Unit)? = null
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
      )
  ) {
    val hostView = LocalView.current
    val dialogWindow = (hostView.parent as? DialogWindowProvider)?.window
    SideEffect {
      dialogWindow?.let { w ->
        WindowCompat.setDecorFitsSystemWindows(w, false)
        w.setLayout(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        @Suppress("DEPRECATION")
        w.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        w.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(w, hostView).apply {
          isAppearanceLightStatusBars = false
          isAppearanceLightNavigationBars = false
        }
        w.setBackgroundDrawable(ColorDrawable(0xF4040710.toInt()))
      }
    }

    StoryViewerContent(
      story = story,
      isAuthor = isAuthor,
      onDismiss = onDismiss,
      onPlayTrack = onPlayTrack,
      onDeleteStory = onDeleteStory,
      onNotifyMessage = onNotifyMessage,
      onOpenAuthorProfile = onOpenAuthorProfile,
      onMessageAuthor = onMessageAuthor
    )
  }
}

@Composable
private fun StoryViewerContent(
  story: MusicStoryItem,
  isAuthor: Boolean,
  onDismiss: () -> Unit,
  onPlayTrack: (MusicTrack) -> Unit,
  onDeleteStory: (String) -> Unit,
  onNotifyMessage: (String) -> Unit,
  onOpenAuthorProfile: (() -> Unit)? = null,
  onMessageAuthor: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isSharing by remember { mutableStateOf(false) }

  BoxWithConstraints(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxSize()
        .background(Color(0xF2040710))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp, vertical = 14.dp)
        .testTag("story_viewer_dialog")
  ) {
    val fixedChromeHeight = 168.dp
    val availableCardH =
      minOf(maxHeight - fixedChromeHeight, maxHeight * 0.56f).coerceAtLeast(140.dp)
    val cardMaxW = (maxWidth - 16.dp).coerceAtMost(276.dp)
    val targetHeight = minOf(availableCardH, cardMaxW * (14f / 9f))
    val targetWidth = (targetHeight * (9f / 14f)).coerceAtMost(cardMaxW)

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
      modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth().wrapContentHeight()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable(enabled = !isAuthor && onOpenAuthorProfile != null) {
                onDismiss()
                onOpenAuthorProfile?.invoke()
              }
              .padding(vertical = 4.dp)
        ) {
          StudioAvatar(
            photoUrlOrPath = story.userAvatarUrl,
            contentDescription = story.userName,
            size = 42.dp
          )
          Column {
            Text(
              text = story.userName.ifBlank { "Vizyon Dinleyicisi" },
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = formatRelativeTimeTr(story.createdAt),
              style = MaterialTheme.typography.labelSmall,
              color = Color(0xFFBBD0F5),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          if (isAuthor) {
            StudioCircleIconButton(
              icon = Icons.Rounded.DeleteOutline,
              contentDescription = "Hikayeyi sil",
              onClick = {
                onDeleteStory(story.id)
                onDismiss()
              },
              tint = MaterialTheme.colorScheme.error,
              size = 48.dp,
              modifier = Modifier.testTag("story_viewer_delete_btn")
            )
          }
          StudioCircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Hikayeyi kapat",
            onClick = onDismiss,
            tint = Color.White,
            size = 48.dp,
            modifier = Modifier.testTag("close_story_viewer_btn")
          )
        }
      }

      StoryCardVisualPreview(
        story = story,
        modifier =
          Modifier.width(targetWidth)
            .height(targetHeight)
            .testTag("story_viewer_card")
      )

      Surface(
        color = Color(0xE60B1328),
        shape = RoundedCornerShape(22.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(22.dp))
            .testTag("story_viewer_bottom_bar")
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(10.dp)
        ) {
          StudioGradientCtaButton(
            text = "Parçayı Çal",
            icon = Icons.Rounded.PlayArrow,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
            onClick = {
              onPlayTrack(story.toMusicTrack())
              onDismiss()
            },
            modifier = Modifier.weight(1f).testTag("story_viewer_play_btn")
          )
          if (!isAuthor && onMessageAuthor != null) {
            StudioCircleIconButton(
              icon = Icons.Rounded.ChatBubbleOutline,
              contentDescription = "Hikaye sahibine mesaj gönder",
              tint = Color.White,
              highlighted = true,
              size = 48.dp,
              onClick = {
                onDismiss()
                onMessageAuthor()
              },
              modifier = Modifier.testTag("story_viewer_dm_btn")
            )
          }
          StudioGlassButton(
            text = if (isSharing) "Hazır..." else "Kartı Paylaş",
            icon = Icons.Rounded.Share,
            enabled = !isSharing,
            tintColor = Color.White,
            containerColor = Color.White.copy(alpha = 0.14f),
            borderColor = Color.White.copy(alpha = 0.34f),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
            onClick = {
              isSharing = true
              scope.launch {
                val uri = StoryBitmapRenderer.renderAndCacheStoryPngUri(context, story)
                isSharing = false
                if (uri != null) {
                  StoryBitmapRenderer.launchExternalStoryShareChooser(context, uri, story)
                } else {
                  onNotifyMessage("Hikaye kartı görseli oluşturulamadı.")
                }
              }
            },
            modifier = Modifier.weight(1f).testTag("story_viewer_share_png_btn")
          )
        }
      }
    }
  }
}

@Composable
fun StoryCardVisualPreview(
  story: MusicStoryItem,
  modifier: Modifier = Modifier
) {
  val theme = StoryCardTheme.fromRaw(story.cardTheme)
  val startColor = Color(theme.startColorHex)
  val endColor = Color(theme.endColorHex)
  val accentColor = Color(theme.accentColorHex)

  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(26.dp))
        .background(
          Brush.linearGradient(
            colors = listOf(startColor, endColor, Color(0xFF060912))
          )
        )
        .border(1.4.dp, accentColor.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
        .padding(14.dp)
  ) {
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        StudioAvatar(
          photoUrlOrPath = story.userAvatarUrl,
          contentDescription = story.userName,
          size = 28.dp,
          ringColor = accentColor
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = story.userName.ifBlank { "Vizyon Dinleyicisi" },
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = story.userHandle.ifBlank { "@vizyon" },
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Surface(
          color = accentColor.copy(alpha = 0.22f),
          shape = RoundedCornerShape(999.dp)
        ) {
          Text(
            text = theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Surface(
        color = Color(0x990A1224),
        shape = RoundedCornerShape(20.dp),
        modifier =
          Modifier.weight(1f, fill = false)
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(12.dp)
        ) {
          AsyncImage(
            model = story.albumArtUrl.ifBlank { null },
            contentDescription = story.trackTitle,
            placeholder = painterResource(R.drawable.img_playlist_cover),
            error = painterResource(R.drawable.img_playlist_cover),
            contentScale = ContentScale.Crop,
            modifier =
              Modifier.fillMaxWidth(0.62f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
          )
          Text(
            text = story.trackTitle.ifBlank { "Vizyon Parçası" },
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = story.artistName.ifBlank { "Vizyon Sanatçısı" },
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          LiveSoundwaveEqualizer(barCount = 16, energyBoost = 0.9f, height = 16.dp)
          if (story.note.isNotBlank()) {
            Text(
              text = "“${story.note}”",
              style = MaterialTheme.typography.bodySmall,
              fontStyle = FontStyle.Italic,
              color = Color(0xFFE2E8F0),
              textAlign = TextAlign.Center,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Text(
        text = "VİZYON MUSIC STUDIO",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.85f),
        fontWeight = FontWeight.Bold
      )
    }
  }
}
