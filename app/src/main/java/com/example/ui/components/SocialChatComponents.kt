package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.ChatPrivacy
import com.example.model.DirectChatMessage
import com.example.model.DirectChatThread
import com.example.model.FollowConnection
import com.example.model.MusicTrack
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.formatRelativeTimeTr

@Composable
fun DirectChatsListSection(
  chatsState: UiState<List<DirectChatThread>>,
  currentUserId: String?,
  publicListeners: List<PublicProfile>,
  onOpenChatThread: (PublicProfile) -> Unit,
  onOpenListenersTab: () -> Unit,
  onDeleteChatThread: (String) -> Unit = {}
) {
  val selfUid = currentUserId ?: "local"
  val haptic = LocalHapticFeedback.current
  var pendingDeleteChat by remember { mutableStateOf<Pair<String, String>?>(null) }

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxWidth().testTag("direct_chats_list_section")
  ) {
    if (publicListeners.isNotEmpty()) {
      StudioSectionHeader(
        title = "Hızlı Sohbet Başlat",
        subtitle = "Müzik paylaşmak veya sohbet etmek için bir dinleyiciye dokun"
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(publicListeners.take(12), key = { "quick_chat_${it.userId}" }) { person ->
          GlassStudioCard(
            cornerRadius = 18.dp,
            onClick = { onOpenChatThread(person) },
            modifier = Modifier.width(116.dp).testTag("quick_chat_peer_${person.userId}")
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
              StudioAvatar(
                photoUrlOrPath = person.photoUrl,
                contentDescription = person.displayName,
                size = 44.dp
              )
              Text(
                text = person.displayName.ifBlank { "Dinleyici" },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = person.handle.ifBlank { "@vizyon" },
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

    when (chatsState) {
      is UiState.Loading -> {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
          CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
      }
      is UiState.Error -> {
        Text(
          text = chatsState.message,
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall
        )
      }
      is UiState.Success -> {
        val threads = chatsState.data
        StudioSectionHeader(
          title = "Sohbet Kutusu",
          subtitle = "Doğrudan mesajlar ve paylaşılan şarkılar • Silmek için basılı tut",
          badge = "${threads.size}"
        )
        if (threads.isEmpty()) {
          EmptySocialCard(
            icon = Icons.Rounded.ChatBubbleOutline,
            title = "Henüz sohbet başlamamış",
            subtitle = "Yukarıdan veya Dinleyiciler sekmesinden bir kullanıcı seçip anında mesaj ve şarkı gönderebilirsin.",
            actionLabel = "Dinleyicileri Keşfet",
            onAction = onOpenListenersTab
          )
        } else {
          threads.forEach { thread ->
            val peerId = thread.peerIdFor(selfUid)
            val peerName = thread.peerName(selfUid)
            val peerHandle = thread.peerHandle(selfUid)
            val peerAvatar = thread.peerAvatar(selfUid)
            val peerProfile =
              publicListeners.firstOrNull { it.userId == peerId }
                ?: PublicProfile(
                  id = peerId,
                  userId = peerId,
                  displayName = peerName,
                  handle = peerHandle,
                  photoUrl = peerAvatar
                )
            val resolvedName = peerProfile.displayName.ifBlank { peerName }.ifBlank { "Vizyon Dinleyicisi" }

            GlassStudioCard(
              highlighted = thread.lastSenderId.isNotBlank() && thread.lastSenderId != selfUid,
              cornerRadius = 20.dp,
              onClick = { onOpenChatThread(peerProfile) },
              onLongClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                pendingDeleteChat = peerId to resolvedName
              },
              modifier = Modifier.fillMaxWidth().testTag("chat_thread_card_$peerId")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)
              ) {
                StudioAvatar(
                  photoUrlOrPath = peerProfile.photoUrl.ifBlank { peerAvatar },
                  contentDescription = resolvedName,
                  size = 48.dp
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = resolvedName,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f)
                    )
                    Text(
                      text = formatRelativeTimeTr(thread.updatedAt),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }
                  Text(
                    text =
                      if (thread.lastTrackTitle.isNotBlank()) "♪ ${thread.lastTrackTitle} • ${thread.lastMessageText}"
                      else thread.lastMessageText.ifBlank { "Sohbeti aç" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
                IconButton(
                  onClick = { pendingDeleteChat = peerId to resolvedName },
                  modifier = Modifier.size(48.dp).testTag("delete_chat_btn_$peerId")
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Sohbeti sil",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  pendingDeleteChat?.let { (peerId, peerDisplayName) ->
    ConfirmDeleteChatDialog(
      peerName = peerDisplayName,
      onDismiss = { pendingDeleteChat = null },
      onConfirm = {
        pendingDeleteChat = null
        onDeleteChatThread(peerId)
      }
    )
  }
}

@Composable
private fun ConfirmDeleteChatDialog(
  peerName: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Sohbeti Sil", fontWeight = FontWeight.Bold) },
    text = {
      Text("$peerName ile olan tüm sohbet geçmişi ve mesajlar silinecek. Bu işlem geri alınamaz.")
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_delete_chat_btn")
      ) {
        Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Vazgeç")
      }
    },
    modifier = Modifier.testTag("confirm_delete_chat_dialog")
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DirectChatThreadSheet(
  peer: PublicProfile,
  currentUserId: String?,
  isFollowerOrFollowing: Boolean,
  messagesState: UiState<List<DirectChatMessage>>,
  currentTrack: MusicTrack?,
  candidateTracks: List<MusicTrack>,
  onDismiss: () -> Unit,
  onSendMessage: (String, MusicTrack?) -> Unit,
  onPlayTrack: (MusicTrack) -> Unit,
  onDeleteChatThread: () -> Unit = {},
  onDeleteMessage: (DirectChatMessage) -> Unit = {}
) {
  val selfUid = currentUserId ?: "local"
  val haptic = LocalHapticFeedback.current
  var messageInput by remember { mutableStateOf("") }
  var attachedTrack by remember { mutableStateOf<MusicTrack?>(null) }
  var showTrackPicker by remember { mutableStateOf(false) }
  var pendingDeleteThreadConfirm by remember { mutableStateOf(false) }
  var pendingDeleteMessage by remember { mutableStateOf<DirectChatMessage?>(null) }

  val trackOptions = remember(currentTrack, candidateTracks) {
    (listOfNotNull(currentTrack) + candidateTracks).distinctBy { it.trackId }.take(8)
  }
  val privacy = ChatPrivacy.fromRaw(peer.chatPrivacy)
  val blockedByPrivacy = privacy == ChatPrivacy.FOLLOWERS_ONLY && !isFollowerOrFollowing

  val listState = rememberLazyListState()
  val messages = (messagesState as? UiState.Success)?.data.orEmpty()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.lastIndex)
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("direct_chat_sheet")
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 460.dp, max = 640.dp)
          .imePadding()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        StudioAvatar(
          photoUrlOrPath = peer.photoUrl,
          contentDescription = peer.displayName,
          size = 44.dp
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = peer.displayName.ifBlank { "Vizyon Dinleyicisi" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "${peer.handle.ifBlank { "@vizyon" }} • ${privacy.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
          )
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.DeleteOutline,
          contentDescription = "Sohbeti sil",
          onClick = { pendingDeleteThreadConfirm = true },
          size = 48.dp,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.testTag("delete_active_chat_btn")
        )
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Sohbeti kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_direct_chat_btn")
        )
      }

      if (blockedByPrivacy) {
        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Text(
              text = "${peer.displayName} yalnızca takipçilerinden mesaj kabul ediyor. Mesaj göndermek için profilini takip et.",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when (messagesState) {
          is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
          is UiState.Error -> {
            Text(
              text = messagesState.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
          }
          is UiState.Success -> {
            if (messages.isEmpty()) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                Text(
                  text = "İlk mesajı veya sevdiğin bir şarkıyı ${peer.displayName} ile paylaş!",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                items(
                  items = messages,
                  key = { it.id },
                  contentType = { "chat_message" }
                ) { msg ->
                  val isMine = msg.senderId == selfUid
                  val track = msg.toAttachedTrack()
                  val bubbleShape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp
                  )
                  Row(
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    if (!isMine) {
                      StudioAvatar(
                        photoUrlOrPath = msg.senderAvatarUrl.ifBlank { peer.photoUrl },
                        contentDescription = msg.senderName,
                        size = 28.dp
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                    } else {
                      IconButton(
                        onClick = { pendingDeleteMessage = msg },
                        modifier = Modifier.size(48.dp).testTag("delete_chat_msg_btn_${msg.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.DeleteOutline,
                          contentDescription = "Mesajı sil",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                    Surface(
                      color =
                        if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                      shape = bubbleShape,
                      modifier =
                        Modifier.widthIn(max = 268.dp)
                          .clip(bubbleShape)
                          .border(
                            1.dp,
                            if (isMine) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            bubbleShape
                          )
                          .combinedClickable(
                            onClick = {},
                            onLongClick = {
                              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                              pendingDeleteMessage = msg
                            }
                          )
                          .testTag("chat_msg_bubble_${msg.id}")
                    ) {
                      Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                      ) {
                        if (track != null) {
                          Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(12.dp),
                            modifier =
                              Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPlayTrack(track) }
                          ) {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(8.dp),
                              modifier = Modifier.padding(8.dp)
                            ) {
                              AsyncImage(
                                model = track.artworkUrl.ifBlank { null },
                                contentDescription = track.title,
                                placeholder = painterResource(R.drawable.img_playlist_cover),
                                error = painterResource(R.drawable.img_playlist_cover),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp))
                              )
                              Column(modifier = Modifier.weight(1f)) {
                                Text(
                                  text = track.title,
                                  style = MaterialTheme.typography.labelMedium,
                                  fontWeight = FontWeight.Bold,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                  text = track.artistName,
                                  style = MaterialTheme.typography.labelSmall,
                                  color = MaterialTheme.colorScheme.primary,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis
                                )
                              }
                              Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                  Modifier.size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                              ) {
                                Icon(
                                  Icons.Rounded.PlayArrow,
                                  contentDescription = "Dinle",
                                  tint = Color.White,
                                  modifier = Modifier.size(18.dp)
                                )
                              }
                            }
                          }
                        }
                        if (msg.text.isNotBlank()) {
                          Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                          )
                        }
                        Text(
                          text = formatRelativeTimeTr(msg.createdAt),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                    if (!isMine) {
                      IconButton(
                        onClick = { pendingDeleteMessage = msg },
                        modifier = Modifier.size(48.dp).testTag("delete_chat_msg_btn_${msg.id}")
                      ) {
                        Icon(
                          imageVector = Icons.Rounded.DeleteOutline,
                          contentDescription = "Mesajı sil",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                          modifier = Modifier.size(18.dp)
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

      AnimatedVisibility(visible = showTrackPicker && trackOptions.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(trackOptions, key = { "dm_track_${it.trackId}" }) { tr ->
            StudioInteractiveChip(
              label = "${tr.title} • ${tr.artistName}",
              selected = attachedTrack?.trackId == tr.trackId,
              leadingIcon = Icons.Rounded.MusicNote,
              onClick = {
                attachedTrack = if (attachedTrack?.trackId == tr.trackId) null else tr
                showTrackPicker = false
              }
            )
          }
        }
      }

      attachedTrack?.let { tr ->
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "♪ ${tr.title} — ${tr.artistName}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
            StudioCircleIconButton(
              icon = Icons.Rounded.Close,
              contentDescription = "Parçayı kaldır",
              onClick = { attachedTrack = null },
              size = 36.dp
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        StudioCircleIconButton(
          icon = Icons.Rounded.MusicNote,
          contentDescription = "Şarkı ekle",
          highlighted = attachedTrack != null || showTrackPicker,
          onClick = { showTrackPicker = !showTrackPicker },
          size = 48.dp,
          modifier = Modifier.testTag("direct_chat_attach_track_btn")
        )
        OutlinedTextField(
          value = messageInput,
          onValueChange = { if (it.length <= 600) messageInput = it },
          placeholder = { Text("Mesaj yaz veya şarkı ekle...") },
          maxLines = 3,
          shape = RoundedCornerShape(18.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
          ),
          modifier = Modifier.weight(1f).testTag("direct_chat_input")
        )
        StudioCircleIconButton(
          icon = Icons.AutoMirrored.Rounded.Send,
          contentDescription = "Gönder",
          highlighted = !blockedByPrivacy && (messageInput.isNotBlank() || attachedTrack != null),
          onClick = {
            if (!blockedByPrivacy && (messageInput.isNotBlank() || attachedTrack != null)) {
              onSendMessage(messageInput, attachedTrack)
              messageInput = ""
              attachedTrack = null
              showTrackPicker = false
            }
          },
          size = 48.dp,
          modifier = Modifier.testTag("direct_chat_send_btn")
        )
      }
    }
  }

  if (pendingDeleteThreadConfirm) {
    ConfirmDeleteChatDialog(
      peerName = peer.displayName.ifBlank { "Vizyon Dinleyicisi" },
      onDismiss = { pendingDeleteThreadConfirm = false },
      onConfirm = {
        pendingDeleteThreadConfirm = false
        onDeleteChatThread()
      }
    )
  }

  pendingDeleteMessage?.let { msg ->
    AlertDialog(
      onDismissRequest = { pendingDeleteMessage = null },
      title = { Text("Mesajı Sil", fontWeight = FontWeight.Bold) },
      text = {
        val preview =
          msg.text.ifBlank { if (msg.trackTitle.isNotBlank()) "♪ ${msg.trackTitle}" else "Bu mesaj" }
        Text("“$preview” mesajını kalıcı olarak silmek istiyor musun?")
      },
      confirmButton = {
        Button(
          onClick = {
            val target = msg
            pendingDeleteMessage = null
            onDeleteMessage(target)
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_message_btn")
        ) {
          Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteMessage = null }) {
          Text("Vazgeç")
        }
      },
      modifier = Modifier.testTag("confirm_delete_message_dialog")
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowConnectionsModalSheet(
  title: String,
  followersState: UiState<List<FollowConnection>>,
  followingState: UiState<List<FollowConnection>>,
  currentUserId: String?,
  myFollowingIds: Set<String>,
  onDismiss: () -> Unit,
  onOpenProfileById: (String) -> Unit,
  onOpenChatById: (String) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val followersList = (followersState as? UiState.Success)?.data.orEmpty()
  val followingList = (followingState as? UiState.Success)?.data.orEmpty()
  val activeState = if (selectedTab == 0) followersState else followingState
  val activeList = if (selectedTab == 0) followersList else followingList

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("follow_connections_sheet")
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
        Text(
          text = title.ifBlank { "Takip Bağlantıları" },
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "Kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_follow_connections_btn")
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
          0 to "Takipçiler (${followersList.size})",
          1 to "Takip Edilenler (${followingList.size})"
        ).forEach { (idx, label) ->
          val selected = selectedTab == idx
          Surface(
            color =
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(999.dp),
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable { selectedTab = idx }
                .testTag("follow_modal_tab_$idx")
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }

      when (activeState) {
        is UiState.Loading -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
          }
        }
        is UiState.Error -> {
          Text(activeState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
          if (activeList.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
              Text(
                text = if (selectedTab == 0) "Henüz takipçi yok." else "Henüz takip edilen kullanıcı yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(activeList, key = { "${selectedTab}_${it.id}_${it.targetUserId}" }) { conn ->
                val targetUid = conn.targetUserId.ifBlank { conn.id }
                val isSelf = targetUid == currentUserId
                GlassStudioCard(
                  cornerRadius = 18.dp,
                  onClick = { onOpenProfileById(targetUid) },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(12.dp)
                  ) {
                    StudioAvatar(
                      photoUrlOrPath = conn.photoUrl,
                      contentDescription = conn.displayName,
                      size = 44.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = conn.displayName.ifBlank { "Vizyon Dinleyicisi" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text =
                          if (targetUid in myFollowingIds) "${conn.handle.ifBlank { "@vizyon" }} • Takip ediliyor"
                          else conn.handle.ifBlank { "@vizyon" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }
                    if (!isSelf) {
                      StudioCircleIconButton(
                        icon = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Mesaj gönder",
                        onClick = { onOpenChatById(targetUid) },
                        size = 48.dp,
                        tint = MaterialTheme.colorScheme.secondary
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
