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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.audio.VoiceMessageAudioHelper
import com.example.model.DirectMessageType
import com.example.model.FollowRequest
import com.example.model.MessageRequest
import com.example.model.ReportReason
import com.example.model.UiState
import com.example.model.formatRelativeTimeTr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestsModalSheet(
  followRequestsState: UiState<List<FollowRequest>>,
  messageRequestsState: UiState<List<MessageRequest>>,
  initialTab: Int = 0,
  onDismiss: () -> Unit,
  onRespondFollowRequest: (FollowRequest, Boolean) -> Unit,
  onRespondMessageRequest: (MessageRequest, Boolean) -> Unit,
  onNotifyMessage: (String) -> Unit = {}
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val followList = (followRequestsState as? UiState.Success)?.data.orEmpty()
  val messageList = (messageRequestsState as? UiState.Success)?.data.orEmpty()
  var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
  var playingVoiceReqId by remember { mutableStateOf<String?>(null) }

  DisposableEffect(Unit) {
    onDispose {
      VoiceMessageAudioHelper.stopPlayback()
      playingVoiceReqId = null
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("requests_modal_sheet")
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 340.dp, max = 600.dp)
          .navigationBarsPadding()
          .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "TAKİP & MESAJ İSTEKLERİ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Gelen İstekler",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
        }
        StudioCircleIconButton(
          icon = Icons.Rounded.Close,
          contentDescription = "İstek panelini kapat",
          onClick = onDismiss,
          size = 48.dp,
          modifier = Modifier.testTag("close_requests_sheet_btn")
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        StudioInteractiveChip(
          label = "Takip (${followList.size})",
          selected = selectedTab == 0,
          leadingIcon = Icons.Rounded.PersonAdd,
          onClick = { selectedTab = 0 },
          modifier = Modifier.weight(1f).testTag("requests_tab_follow")
        )
        StudioInteractiveChip(
          label = "Mesaj (${messageList.size})",
          selected = selectedTab == 1,
          leadingIcon = Icons.Rounded.MarkChatUnread,
          onClick = { selectedTab = 1 },
          modifier = Modifier.weight(1f).testTag("requests_tab_message")
        )
      }

      if (selectedTab == 0) {
        when (followRequestsState) {
          is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
          is UiState.Error -> {
            Text(
              text = followRequestsState.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
          }
          is UiState.Success -> {
            if (followList.isEmpty()) {
              EmptySocialCard(
                icon = Icons.Rounded.PersonAdd,
                title = "Bekleyen takip isteği yok",
                subtitle = "Onaylı takip modu açıkken gelen yeni takip istekleri burada görünür."
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
              ) {
                items(followList, key = { "freq_${it.id}_${it.requesterId}" }) { req ->
                  GlassStudioCard(
                    highlighted = true,
                    cornerRadius = 18.dp,
                    modifier = Modifier.fillMaxWidth().testTag("follow_request_card_${req.id}")
                  ) {
                    Column(
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.padding(12.dp)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        StudioAvatar(
                          photoUrlOrPath = req.requesterAvatarUrl,
                          contentDescription = req.requesterName,
                          size = 44.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = req.requesterName.ifBlank { "Vizyon Dinleyicisi" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = "${req.requesterHandle.ifBlank { "@vizyon" }} • ${formatRelativeTimeTr(req.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                          )
                        }
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                          onClick = { onRespondFollowRequest(req, true) },
                          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            .testTag("accept_follow_req_${req.id}")
                        ) {
                          Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Kabul Et", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                          onClick = { onRespondFollowRequest(req, false) },
                          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            .testTag("decline_follow_req_${req.id}")
                        ) {
                          Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Reddet")
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        when (messageRequestsState) {
          is UiState.Loading -> {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
              CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
          }
          is UiState.Error -> {
            Text(
              text = messageRequestsState.message,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
          }
          is UiState.Success -> {
            if (messageList.isEmpty()) {
              EmptySocialCard(
                icon = Icons.Rounded.MarkChatUnread,
                title = "Bekleyen mesaj isteği yok",
                subtitle = "Seni takip etmeyen dinleyicilerden gelen mesaj ve sesli mesaj istekleri burada görünür."
              )
            } else {
              LazyColumn(
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
              ) {
                items(messageList, key = { "mreq_${it.id}_${it.requesterId}" }) { req ->
                  val isVoice =
                    DirectMessageType.fromRaw(req.messageType) == DirectMessageType.VOICE &&
                      req.audioBase64.isNotBlank()
                  val isPlayingThis = playingVoiceReqId == req.id
                  GlassStudioCard(
                    highlighted = true,
                    cornerRadius = 18.dp,
                    modifier = Modifier.fillMaxWidth().testTag("message_request_card_${req.id}")
                  ) {
                    Column(
                      verticalArrangement = Arrangement.spacedBy(10.dp),
                      modifier = Modifier.padding(12.dp)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        StudioAvatar(
                          photoUrlOrPath = req.requesterAvatarUrl,
                          contentDescription = req.requesterName,
                          size = 44.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = req.requesterName.ifBlank { "Vizyon Dinleyicisi" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = "${req.requesterHandle.ifBlank { "@vizyon" }} • ${formatRelativeTimeTr(req.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                          )
                        }
                      }

                      if (req.previewText.isNotBlank()) {
                        Surface(
                          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Text(
                            text = req.previewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                          )
                        }
                      }

                      if (isVoice) {
                        Surface(
                          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                          ) {
                            StudioCircleIconButton(
                              icon = if (isPlayingThis) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                              contentDescription = if (isPlayingThis) "Sesli mesajı durdur" else "Sesli mesajı dinle",
                              size = 48.dp,
                              highlighted = true,
                              onClick = {
                                if (isPlayingThis) {
                                  VoiceMessageAudioHelper.stopPlayback()
                                  playingVoiceReqId = null
                                } else {
                                  playingVoiceReqId = req.id
                                  VoiceMessageAudioHelper.playVoiceClip(
                                    context = context,
                                    key = "mreq_${req.id}",
                                    audioBase64 = req.audioBase64,
                                    onCompleted = {
                                      if (playingVoiceReqId == req.id) playingVoiceReqId = null
                                    },
                                    onError = { err ->
                                      if (playingVoiceReqId == req.id) playingVoiceReqId = null
                                      onNotifyMessage(err)
                                    }
                                  )
                                }
                              },
                              modifier = Modifier.testTag("play_msg_req_voice_${req.id}")
                            )
                            Icon(
                              Icons.Rounded.GraphicEq,
                              contentDescription = null,
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(18.dp)
                            )
                            Text(
                              text = "Sesli Mesaj İsteği (${VoiceMessageAudioHelper.formatDurationMmSs(req.audioDurationMs)})",
                              style = MaterialTheme.typography.labelMedium,
                              fontWeight = FontWeight.SemiBold,
                              modifier = Modifier.weight(1f)
                            )
                          }
                        }
                      }

                      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                          onClick = {
                            VoiceMessageAudioHelper.stopPlayback()
                            playingVoiceReqId = null
                            onRespondMessageRequest(req, true)
                          },
                          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            .testTag("accept_msg_req_${req.id}")
                        ) {
                          Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Kabul Et", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                          onClick = {
                            VoiceMessageAudioHelper.stopPlayback()
                            playingVoiceReqId = null
                            onRespondMessageRequest(req, false)
                          },
                          modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                            .testTag("decline_msg_req_${req.id}")
                        ) {
                          Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Reddet")
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

@Composable
fun ReportUserDialog(
  targetDisplayName: String,
  isAlreadyBlocked: Boolean = false,
  onDismiss: () -> Unit,
  onSubmitReport: (ReportReason, String) -> Unit,
  onBlockAfterReport: (() -> Unit)? = null
) {
  var selectedReason by remember { mutableStateOf(ReportReason.SPAM) }
  var details by remember { mutableStateOf("") }
  var reportSubmitted by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
      GlassStudioCard(
        highlighted = true,
        cornerRadius = 24.dp,
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 440.dp)
            .testTag("report_user_dialog")
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
          if (!reportSubmitted) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Text(
                text = "${targetDisplayName.ifBlank { "Kullanıcıyı" }} Şikayet Et",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = "Topluluk güvenliği için şikayet nedenini seç. Bildirimlerin gizli tutulur.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
              ReportReason.entries.chunked(2).forEach { rowReasons ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                  rowReasons.forEach { reason ->
                    val selected = selectedReason == reason
                    Surface(
                      color =
                        if (selected) MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                      shape = RoundedCornerShape(14.dp),
                      modifier =
                        Modifier.weight(1f)
                          .defaultMinSize(minHeight = 48.dp)
                          .clip(RoundedCornerShape(14.dp))
                          .border(
                            width = 1.dp,
                            color =
                              if (selected) MaterialTheme.colorScheme.error
                              else MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                            shape = RoundedCornerShape(14.dp)
                          )
                          .clickable { selectedReason = reason }
                          .testTag("report_reason_${reason.name}")
                    ) {
                      Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                      ) {
                        Text(
                          text = reason.label,
                          style = MaterialTheme.typography.labelMedium,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                          color = if (selected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }
                    }
                  }
                }
              }
            }
            OutlinedTextField(
              value = details,
              onValueChange = { if (it.length <= 240) details = it },
              label = { Text("Ek açıklama (isteğe bağlı)") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("report_details_input")
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
              TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("Vazgeç")
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  onSubmitReport(selectedReason, details.trim())
                  reportSubmitted = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("submit_report_btn")
              ) {
                Text("Şikayet Gönder", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          } else {
            val safeTargetName = targetDisplayName.ifBlank { "Bu kullanıcı" }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(
                text = "Şikayetin Alındı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text =
                if (!isAlreadyBlocked && onBlockAfterReport != null)
                  "$safeTargetName hakkındaki bildirimin incelemeye alındı. İstersen bu kullanıcıyı hemen engelleyerek sana mesaj veya bildirim göndermesini kapatabilirsin."
                else
                  "$safeTargetName hakkındaki bildirimin incelemeye alındı. Teşekkür ederiz.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth().testTag("report_post_actions_row")
            ) {
              OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("report_post_close_btn")
              ) {
                Text(
                  text = if (!isAlreadyBlocked && onBlockAfterReport != null) "Engellemeden Kapat" else "Kapat",
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              if (!isAlreadyBlocked && onBlockAfterReport != null) {
                Button(
                  onClick = {
                    onBlockAfterReport()
                    onDismiss()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp).testTag("report_post_block_btn")
                ) {
                  Icon(Icons.Rounded.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Kullanıcıyı Engelle", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun BlockUserConfirmDialog(
  targetDisplayName: String,
  onDismiss: () -> Unit,
  onConfirmBlock: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(
          text = "${targetDisplayName.ifBlank { "Kullanıcıyı" }} Engelle",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Text(
        text = "Bu kullanıcıyı engellediğinde gönderileri, hikayeleri, profili ve mesajları gizlenir. İstediğin zaman Hakkında & Gizlilik bölümünden engeli kaldırabilirsin.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirmBlock,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).testTag("confirm_block_user_btn")
      ) {
        Text("Engelle", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
        Text("Vazgeç")
      }
    },
    modifier = Modifier.testTag("block_user_confirm_dialog")
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSafetyModalSheet(onDismiss: () -> Unit) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("data_safety_modal_sheet")
  ) {
    LazyColumn(
      contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp).navigationBarsPadding()
    ) {
      item {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            ) {
              Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column {
              Text(
                text = "GOOGLE PLAY DATA SAFETY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Veri Güvenliği Beyanı",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
              )
            }
          }
          StudioCircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Veri Güvenliği ekranını kapat",
            onClick = onDismiss,
            size = 48.dp,
            modifier = Modifier.testTag("close_data_safety_sheet_btn")
          )
        }
      }

      item {
        DataSafetySectionCard(
          title = "1. İsteğe Bağlı Bulut & Kimlik Verileri",
          body = "Google ile giriş yapmayı seçersen ad, @handle, e-posta, profil fotoğrafı ve müzik tercihlerin yalnızca kendi hesabını ve sosyal özelliklerini senkronize etmek için Firebase Authentication & Firestore üzerinde HTTPS (TLS) ile şifreli aktarılır."
        )
      }
      item {
        DataSafetySectionCard(
          title = "2. Sesli Mesajlar (RECORD_AUDIO)",
          body = "Mikrofon izni yalnızca sohbette Sesli Mesaj kaydet butonuna bastığında istenir. Kayıt en fazla 30 saniyedir (.m4a AAC), önizleme onayı olmadan sunucuya gönderilmez."
        )
      }
      item {
        DataSafetySectionCard(
          title = "3. Sıfır Depolama İznli Fotoğraf Seçimi",
          body = "Profil fotoğrafı Android Fotoğraf Seçici (Photo Picker) aracılığıyla seçilir; geniş galeri veya dosya okuma izni (READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE) istenmez."
        )
      }
      item {
        DataSafetySectionCard(
          title = "4. Güvenlik Kasası & Şifre Koruması",
          body = "Stüdyo şifresi düz metin olarak saklanmaz; cihazda ve bulut yedeğinde SHA-256 + rastgele tuz ile özetlenir. Şifre değişikliği mevcut şifre doğrulaması gerektirir."
        )
      }
      item {
        DataSafetySectionCard(
          title = "5. Anlık Bildirimler (Firestore & FCM)",
          body = "Uygulama açıkken sosyal ve sohbet bildirimleri Firestore anlık dinleyicisi ile iletilir. Uygulama arka plandayken veya kapalıyken isteğe bağlı Anlık Push Bildirimleri (FCM) için cihaz bildirim anahtarı (fcmToken) şifreli olarak saklanır; çıkış yaptığında veya kapattığında temizlenir. Kalıcı arka plan veri senkronizasyon servisi kullanılmaz."
        )
      }
      item {
        DataSafetySectionCard(
          title = "6. Veri Silme, Engelleme & Şikayet Hakkı",
          body = "Diğer kullanıcıları engelleyebilir veya şikayet edebilirsin. Profil Ayarları altındaki Hesabı Kalıcı Sil duvarı üzerinden tüm verilerini ve bulut kaydını tek dokunuşla kalıcı olarak silebilirsin. Reklam SDK'sı veya üçüncü taraf izleyici yoktur."
        )
      }
      item {
        Spacer(modifier = Modifier.height(6.dp))
      }
    }
  }
}

@Composable
private fun DataSafetySectionCard(title: String, body: String) {
  GlassStudioCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
          Icons.Rounded.Shield,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(16.dp)
        )
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      }
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
