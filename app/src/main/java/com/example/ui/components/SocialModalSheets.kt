package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ActivityComment
import com.example.model.ActivityFeedItem
import com.example.model.ActivityLikeUser
import com.example.model.UiState
import com.example.model.formatRelativeTimeTr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCommentsSheet(
  activity: ActivityFeedItem,
  commentsState: UiState<List<ActivityComment>>,
  onDismiss: () -> Unit,
  onSubmitComment: (String) -> Unit
) {
  var commentText by remember { mutableStateOf("") }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("activity_comments_sheet")
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 380.dp, max = 560.dp)
          .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "Yorumlar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${activity.userName} • ♪ ${activity.trackTitle}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Yorumları kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_comments_sheet_btn")
        )
      }

      when (commentsState) {
        is UiState.Loading -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
          }
        }
        is UiState.Error -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(commentsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val comments = commentsState.data
          if (comments.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(20.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.ChatBubbleOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(40.dp)
                )
                Text(
                  text = "Henüz yorum yok",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "İlk yorumu bırakarak bu şarkı hakkında sohbet başlat.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
              items(comments, key = { it.id }) { comment ->
                GlassStudioCard(
                  cornerRadius = 16.dp,
                  modifier = Modifier.testTag("activity_comment_item_${comment.id}")
                ) {
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                  ) {
                    StudioAvatar(
                      photoUrlOrPath = comment.userAvatarUrl,
                      contentDescription = comment.userName,
                      size = 36.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                      Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Text(
                          text = "${comment.userName} (${comment.userHandle})",
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = FontWeight.Bold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.weight(1f)
                        )
                        Text(
                          text = formatRelativeTimeTr(comment.createdAt),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                      Text(
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
      ) {
        OutlinedTextField(
          value = commentText,
          onValueChange = { commentText = it.take(280) },
          placeholder = { Text("Yorumunu yaz...") },
          maxLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.weight(1f).testTag("comment_input")
        )
        StudioGradientCtaButton(
          text = "Gönder",
          icon = Icons.AutoMirrored.Rounded.Send,
          enabled = commentText.isNotBlank(),
          onClick = {
            onSubmitComment(commentText)
            commentText = ""
          },
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          modifier = Modifier.testTag("submit_comment_btn")
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepostQuoteSheet(
  original: ActivityFeedItem,
  onDismiss: () -> Unit,
  onConfirmRepost: (String) -> Unit
) {
  var quoteNote by remember { mutableStateOf("") }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("repost_quote_sheet")
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Rounded.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
          Text(
            text = "Yeniden Paylaş",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
          )
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Yeniden paylaşımı kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_repost_sheet_btn")
        )
      }

      GlassStudioCard(cornerRadius = 18.dp) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(12.dp)
        ) {
          AsyncImage(
            model = original.albumArtUrl.ifBlank { null },
            contentDescription = original.trackTitle,
            placeholder = painterResource(R.drawable.img_playlist_cover),
            error = painterResource(R.drawable.img_playlist_cover),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = original.trackTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${original.artistName} • Kaynak: ${original.userName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      OutlinedTextField(
        value = quoteNote,
        onValueChange = { quoteNote = it.take(240) },
        label = { Text("Alıntı Notu (İsteğe Bağlı)") },
        placeholder = { Text("Bu paylaşıma kendi yorumunu ekle...") },
        maxLines = 3,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().testTag("repost_note_input")
      )

      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
      ) {
        StudioGlassButton(
          text = "Vazgeç",
          onClick = onDismiss,
          modifier = Modifier.weight(1f)
        )
        StudioGradientCtaButton(
          text = "Yeniden Paylaş",
          icon = Icons.Rounded.Repeat,
          onClick = { onConfirmRepost(quoteNote) },
          modifier = Modifier.weight(1.4f).testTag("confirm_repost_btn")
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLikersModalSheet(
  activity: ActivityFeedItem,
  likersState: UiState<List<ActivityLikeUser>>,
  onDismiss: () -> Unit,
  onOpenListenerProfile: (ActivityLikeUser) -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("activity_likers_sheet")
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 320.dp, max = 540.dp)
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Beğenenler (${activity.likesCount})",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${activity.userName} • ♪ ${activity.trackTitle}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Beğenenler listesini kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_likers_sheet_btn")
        )
      }

      when (likersState) {
        is UiState.Loading -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
          }
        }
        is UiState.Error -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(likersState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val likers = likersState.data
          if (likers.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(20.dp)
              ) {
                Icon(
                  imageVector = Icons.Rounded.Favorite,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                  modifier = Modifier.size(40.dp)
                )
                Text(
                  text = "Henüz beğenen yok",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "İlk beğeniyi bırakarak bu paylaşımı destekleyebilirsin.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(bottom = 16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
              items(likers, key = { it.userId.ifBlank { it.id } }) { liker ->
                val uid = liker.userId.ifBlank { liker.id }
                GlassStudioCard(
                  cornerRadius = 16.dp,
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .clickable { onOpenListenerProfile(liker) }
                      .testTag("activity_liker_item_$uid")
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(12.dp)
                  ) {
                    StudioAvatar(
                      photoUrlOrPath = liker.photoUrl,
                      contentDescription = liker.displayName,
                      size = 42.dp
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                      Text(
                        text = liker.displayName.ifBlank { "Vizyon Dinleyicisi" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "${liker.handle.ifBlank { "@vizyon" }} • ${formatRelativeTimeTr(liker.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
