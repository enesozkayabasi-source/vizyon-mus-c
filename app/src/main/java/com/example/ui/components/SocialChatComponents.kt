package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.VoiceMessageAudioHelper
import com.example.model.ChatPrivacy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
  onDeleteMessage: (DirectChatMessage) -> Unit = {},
  hasPendingMessageRequest: Boolean = false,
  isBlockedByPeer: Boolean = false,
  onSendVoiceMessage: (String, Long) -> Unit = { _, _ -> },
  onSendMessageRequest: (String) -> Unit = {},
  onSendVoiceMessageRequest: (String, Long) -> Unit = { _, _ -> },
  onBlockUser: () -> Unit = {},
  onReportUser: () -> Unit = {},
  onNotifyMessage: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val selfUid = currentUserId ?: "local"
  val haptic = LocalHapticFeedback.current
  var messageInput by remember { mutableStateOf("") }
  var attachedTrack by remember { mutableStateOf<MusicTrack?>(null) }
  var showTrackPicker by remember { mutableStateOf(false) }
  var pendingDeleteThreadConfirm by remember { mutableStateOf(false) }
  var pendingDeleteMessage by remember { mutableStateOf<DirectChatMessage?>(null) }

  // Voice recording & playback state (max 30 seconds)
  var isRecordingVoice by remember { mutableStateOf(false) }
  var recordingElapsedMs by remember { mutableLongStateOf(0L) }
  var recordedPreviewClip by remember { mutableStateOf<VoiceMessageAudioHelper.RecordedVoiceClip?>(null) }
  var playingVoiceKey by remember { mutableStateOf<String?>(null) }

  fun beginVoiceRecording() {
    recordedPreviewClip?.let { VoiceMessageAudioHelper.discardActiveRecording(it.filePath) }
    recordedPreviewClip = null
    VoiceMessageAudioHelper.stopPlayback()
    playingVoiceKey = null
    val res = VoiceMessageAudioHelper.startRecording(context)
    if (res.isSuccess) {
      recordingElapsedMs = 0L
      isRecordingVoice = true
    } else {
      onNotifyMessage(res.exceptionOrNull()?.localizedMessage ?: "Ses kaydı başlatılamadı.")
    }
  }

  val recordAudioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        beginVoiceRecording()
      } else {
        onNotifyMessage("Sesli mesaj kaydetmek için mikrofon izni gereklidir.")
      }
    }

  LaunchedEffect(isRecordingVoice) {
    if (!isRecordingVoice) return@LaunchedEffect
    val startedAt = System.currentTimeMillis()
    while (isRecordingVoice) {
      val elapsed = System.currentTimeMillis() - startedAt
      recordingElapsedMs = elapsed.coerceAtMost(VoiceMessageAudioHelper.MAX_DURATION_MS)
      if (elapsed >= VoiceMessageAudioHelper.MAX_DURATION_MS) {
        isRecordingVoice = false
        VoiceMessageAudioHelper.stopRecordingAndEncode(elapsed)
          .onSuccess { clip -> recordedPreviewClip = clip }
          .onFailure { err -> onNotifyMessage(err.localizedMessage ?: "Ses kaydı tamamlanamadı.") }
        break
      }
      delay(200L)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      VoiceMessageAudioHelper.stopPlayback()
      VoiceMessageAudioHelper.discardActiveRecording(recordedPreviewClip?.filePath)
    }
  }

  val trackOptions = remember(currentTrack, candidateTracks) {
    (listOfNotNull(currentTrack) + candidateTracks).distinctBy { it.trackId }.take(8)
  }
  val privacy = ChatPrivacy.fromRaw(peer.chatPrivacy)
  val blockedByPrivacy = privacy == ChatPrivacy.FOLLOWERS_ONLY && !isFollowerOrFollowing

  val listState = rememberLazyListState()
  val messages = (messagesState as? UiState.Success)?.data.orEmpty()
  val seenBubbleIds = remember(peer.userId) { HashSet<String>() }
  var chatBaselineLoaded by remember(peer.userId) { mutableStateOf(false) }

  LaunchedEffect(peer.userId, messagesState) {
    if (messagesState is UiState.Success && !chatBaselineLoaded) {
      messages.forEach { seenBubbleIds.add(it.id) }
      chatBaselineLoaded = true
    }
  }

  LaunchedEffect(peer.userId, messages.size, messages.lastOrNull()?.id) {
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.Flag,
          contentDescription = "Kullanıcıyı şikayet et",
          onClick = onReportUser,
          size = 48.dp,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.testTag("chat_report_user_btn")
        )
        StudioCircleIconButton(
          icon = Icons.Rounded.Block,
          contentDescription = "Kullanıcıyı engelle",
          onClick = onBlockUser,
          size = 48.dp,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.testTag("chat_block_user_btn")
        )
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

      if (!isBlockedByPeer && blockedByPrivacy) {
        Surface(
          color =
            if (hasPendingMessageRequest) MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("direct_chat_request_banner")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(
              imageVector = if (hasPendingMessageRequest) Icons.Rounded.Schedule else Icons.Rounded.Lock,
              contentDescription = null,
              tint = if (hasPendingMessageRequest) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )
            Text(
              text =
                if (hasPendingMessageRequest)
                  "${peer.displayName} kullanıcısına mesaj isteği gönderildi. Onaylandığında sohbet açılacak."
                else
                  "${peer.displayName} yalnızca takipçilerinden doğrudan mesaj alıyor. Mesaj veya sesli mesaj isteği gönderebilirsin.",
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
                  text =
                    when {
                      isBlockedByPeer -> "${peer.displayName} tarafından engellendiğin için yeni mesaj gönderemezsin."
                      blockedByPrivacy -> "Henüz mesaj yok. Aşağıdan ${peer.displayName} için mesaj isteği gönderebilirsin."
                      else -> "İlk mesajı, sesli mesajı veya sevdiğin bir şarkıyı ${peer.displayName} ile paylaş!"
                    },
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
                  val shouldAnimateEntrance =
                    remember(msg.id) { chatBaselineLoaded && seenBubbleIds.add(msg.id) }
                  val revealProgress = remember(msg.id) { Animatable(if (shouldAnimateEntrance) 0f else 1f) }
                  LaunchedEffect(msg.id, shouldAnimateEntrance) {
                    if (shouldAnimateEntrance) {
                      revealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec =
                          spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                          )
                      )
                    }
                  }
                  Row(
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom,
                    modifier =
                      Modifier.fillMaxWidth()
                        .graphicsLayer {
                          val t = revealProgress.value
                          val offsetPx = 28.dp.toPx()
                          val risePx = 14.dp.toPx()
                          translationX = (1f - t) * (if (isMine) offsetPx else -offsetPx)
                          translationY = (1f - t) * risePx
                          val sc = 0.88f + 0.12f * t
                          scaleX = sc
                          scaleY = sc
                          transformOrigin = TransformOrigin(if (isMine) 1f else 0f, 1f)
                          alpha = t.coerceIn(0f, 1f)
                        }
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
                        if (msg.isVoiceMessage) {
                          val voiceKey = "msg_${msg.id}"
                          val isPlayingThis = playingVoiceKey == voiceKey
                          Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("voice_msg_bubble_${msg.id}")
                          ) {
                            Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.spacedBy(8.dp),
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                              StudioCircleIconButton(
                                icon = if (isPlayingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlayingThis) "Sesli mesajı durdur" else "Sesli mesajı çal",
                                size = 48.dp,
                                highlighted = true,
                                onClick = {
                                  if (isPlayingThis) {
                                    VoiceMessageAudioHelper.stopPlayback()
                                    playingVoiceKey = null
                                  } else {
                                    playingVoiceKey = voiceKey
                                    VoiceMessageAudioHelper.playVoiceClip(
                                      context = context,
                                      key = voiceKey,
                                      audioBase64 = msg.audioBase64,
                                      onCompleted = {
                                        if (playingVoiceKey == voiceKey) playingVoiceKey = null
                                      },
                                      onError = { err ->
                                        if (playingVoiceKey == voiceKey) playingVoiceKey = null
                                        onNotifyMessage(err)
                                      }
                                    )
                                  }
                                },
                                modifier = Modifier.testTag("play_voice_msg_${msg.id}")
                              )
                              Icon(
                                Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                              )
                              Column(modifier = Modifier.weight(1f)) {
                                Text(
                                  text = "Sesli Mesaj",
                                  style = MaterialTheme.typography.labelMedium,
                                  fontWeight = FontWeight.Bold
                                )
                                Text(
                                  text = VoiceMessageAudioHelper.formatDurationMmSs(msg.audioDurationMs),
                                  style = MaterialTheme.typography.labelSmall,
                                  color = MaterialTheme.colorScheme.primary
                                )
                              }
                            }
                          }
                        }
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
                        if (msg.text.isNotBlank() && !msg.isVoiceMessage) {
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

      AnimatedVisibility(visible = !isBlockedByPeer && showTrackPicker && trackOptions.isNotEmpty() && !blockedByPrivacy) {
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

      if (!isBlockedByPeer) {
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
      }

      if (isBlockedByPeer) {
        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.13f),
          shape = RoundedCornerShape(18.dp),
          modifier =
            Modifier.fillMaxWidth()
              .padding(bottom = 8.dp)
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.42f),
                shape = RoundedCornerShape(18.dp)
              )
              .testTag("chat_blocked_by_peer_panel")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
            ) {
              Icon(
                imageVector = Icons.Rounded.Block,
                contentDescription = "Engellendiniz",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
              )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = "Bu kişi tarafından engellendiniz",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Bu kullanıcı sizi engellediği için mesaj, sesli mesaj veya şarkı gönderemezsiniz.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      } else if (isRecordingVoice) {
        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("voice_recording_bar")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ses Kaydediliyor • ${VoiceMessageAudioHelper.formatDurationMmSs(recordingElapsedMs)} / 0:30",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
              )
              Text(
                text = "Bitirmek ve önizlemek için Durdur'a dokun",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            StudioCircleIconButton(
              icon = Icons.Rounded.Close,
              contentDescription = "Kaydı iptal et",
              onClick = {
                isRecordingVoice = false
                VoiceMessageAudioHelper.discardActiveRecording()
              },
              size = 48.dp,
              modifier = Modifier.testTag("voice_record_cancel_btn")
            )
            StudioCircleIconButton(
              icon = Icons.Rounded.Stop,
              contentDescription = "Kaydı durdur ve önizle",
              highlighted = true,
              tint = MaterialTheme.colorScheme.error,
              onClick = {
                val elapsed = recordingElapsedMs
                isRecordingVoice = false
                scope.launch {
                  VoiceMessageAudioHelper.stopRecordingAndEncode(elapsed)
                    .onSuccess { clip -> recordedPreviewClip = clip }
                    .onFailure { err -> onNotifyMessage(err.localizedMessage ?: "Ses kaydı alınamadı.") }
                }
              },
              size = 48.dp,
              modifier = Modifier.testTag("voice_record_stop_btn")
            )
          }
        }
      } else if (recordedPreviewClip != null) {
        val clip = recordedPreviewClip!!
        val previewKey = "preview_${clip.filePath}"
        val isPlayingPreview = playingVoiceKey == previewKey
        Surface(
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("voice_preview_bar")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            StudioCircleIconButton(
              icon = if (isPlayingPreview) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
              contentDescription = if (isPlayingPreview) "Önizlemeyi durdur" else "Ses kaydını dinle",
              highlighted = true,
              onClick = {
                if (isPlayingPreview) {
                  VoiceMessageAudioHelper.stopPlayback()
                  playingVoiceKey = null
                } else {
                  playingVoiceKey = previewKey
                  VoiceMessageAudioHelper.playVoiceClip(
                    context = context,
                    key = previewKey,
                    filePath = clip.filePath,
                    audioBase64 = clip.base64,
                    onCompleted = {
                      if (playingVoiceKey == previewKey) playingVoiceKey = null
                    },
                    onError = { err ->
                      if (playingVoiceKey == previewKey) playingVoiceKey = null
                      onNotifyMessage(err)
                    }
                  )
                }
              },
              size = 48.dp,
              modifier = Modifier.testTag("voice_preview_play_btn")
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Sesli Mesaj Önizleme",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = VoiceMessageAudioHelper.formatDurationMmSs(clip.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
            StudioCircleIconButton(
              icon = Icons.Rounded.DeleteOutline,
              contentDescription = "Ses kaydını sil",
              tint = MaterialTheme.colorScheme.error,
              onClick = {
                VoiceMessageAudioHelper.stopPlayback()
                playingVoiceKey = null
                VoiceMessageAudioHelper.discardActiveRecording(clip.filePath)
                recordedPreviewClip = null
              },
              size = 48.dp,
              modifier = Modifier.testTag("voice_preview_discard_btn")
            )
            StudioCircleIconButton(
              icon = Icons.AutoMirrored.Rounded.Send,
              contentDescription = if (blockedByPrivacy) "Sesli Mesaj İsteği Gönder" else "Sesli Mesajı Gönder",
              highlighted = !hasPendingMessageRequest,
              onClick = {
                if (hasPendingMessageRequest) return@StudioCircleIconButton
                VoiceMessageAudioHelper.stopPlayback()
                playingVoiceKey = null
                if (blockedByPrivacy) {
                  onSendVoiceMessageRequest(clip.base64, clip.durationMs)
                } else {
                  onSendVoiceMessage(clip.base64, clip.durationMs)
                }
                VoiceMessageAudioHelper.discardActiveRecording(clip.filePath)
                recordedPreviewClip = null
              },
              size = 48.dp,
              modifier = Modifier.testTag("voice_preview_send_btn")
            )
          }
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          if (!blockedByPrivacy) {
            StudioCircleIconButton(
              icon = Icons.Rounded.MusicNote,
              contentDescription = "Şarkı ekle",
              highlighted = attachedTrack != null || showTrackPicker,
              onClick = { showTrackPicker = !showTrackPicker },
              size = 48.dp,
              modifier = Modifier.testTag("direct_chat_attach_track_btn")
            )
          }
          StudioCircleIconButton(
            icon = Icons.Rounded.Mic,
            contentDescription = "Sesli mesaj kaydet (maks 30 sn)",
            highlighted = false,
            onClick = {
              if (blockedByPrivacy && hasPendingMessageRequest) {
                onNotifyMessage("Bu kullanıcıya zaten bekleyen bir mesaj isteğin var.")
                return@StudioCircleIconButton
              }
              val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                  PackageManager.PERMISSION_GRANTED
              if (granted) {
                beginVoiceRecording()
              } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            },
            size = 48.dp,
            modifier = Modifier.testTag("direct_chat_voice_record_btn")
          )
          OutlinedTextField(
            value = messageInput,
            onValueChange = { if (it.length <= 600) messageInput = it },
            enabled = !(blockedByPrivacy && hasPendingMessageRequest),
            placeholder = {
              Text(
                when {
                  blockedByPrivacy && hasPendingMessageRequest -> "Mesaj isteği bekleniyor..."
                  blockedByPrivacy -> "Mesaj isteği yaz..."
                  else -> "Mesaj yaz veya şarkı ekle..."
                }
              )
            },
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
            contentDescription = if (blockedByPrivacy) "Mesaj İsteği Gönder" else "Gönder",
            highlighted =
              !(blockedByPrivacy && hasPendingMessageRequest) &&
                (messageInput.isNotBlank() || (!blockedByPrivacy && attachedTrack != null)),
            onClick = {
              if (blockedByPrivacy) {
                if (!hasPendingMessageRequest && messageInput.isNotBlank()) {
                  onSendMessageRequest(messageInput.trim())
                  messageInput = ""
                }
              } else if (messageInput.isNotBlank() || attachedTrack != null) {
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
          when {
            msg.isVoiceMessage -> "Sesli mesaj (${VoiceMessageAudioHelper.formatDurationMmSs(msg.audioDurationMs)})"
            msg.text.isNotBlank() -> msg.text
            msg.trackTitle.isNotBlank() -> "♪ ${msg.trackTitle}"
            else -> "Bu mesaj"
          }
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
          text = title.ifBlank { "Topluluk Listesi" },
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
