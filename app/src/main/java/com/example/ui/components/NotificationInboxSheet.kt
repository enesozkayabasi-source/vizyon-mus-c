package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.NotificationType
import com.example.model.UiState
import com.example.model.UserNotification
import com.example.model.formatRelativeTimeTr
import com.example.notifications.VizyonNotificationHelper
import java.util.Calendar

enum class NotificationInboxFilter(val label: String, val icon: ImageVector) {
  ALL("Tümü", Icons.Rounded.NotificationsActive),
  UNREAD("Okunmamış", Icons.Rounded.MarkEmailUnread),
  STORY("Hikaye", Icons.Rounded.AutoAwesome),
  MESSAGE("Mesajlar", Icons.Rounded.ChatBubble),
  FOLLOW("Takip", Icons.Rounded.PersonAdd),
  LIKE("Beğeni", Icons.Rounded.Favorite),
  SOCIAL("Yorum & Paylaşım", Icons.Rounded.Repeat)
}

enum class NotificationTimeBucket(val label: String) {
  TODAY("Bugün"),
  YESTERDAY("Dün"),
  OLDER("Daha Eski")
}

private data class NotificationTimeSection(
  val bucket: NotificationTimeBucket,
  val items: List<UserNotification>
)

private fun groupNotificationsByTimeBucket(
  list: List<UserNotification>,
  nowMs: Long = System.currentTimeMillis()
): List<NotificationTimeSection> {
  if (list.isEmpty()) return emptyList()
  val cal =
    Calendar.getInstance().apply {
      timeInMillis = nowMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
  val todayStartMs = cal.timeInMillis
  cal.add(Calendar.DAY_OF_YEAR, -1)
  val yesterdayStartMs = cal.timeInMillis

  val todayItems = ArrayList<UserNotification>()
  val yesterdayItems = ArrayList<UserNotification>()
  val olderItems = ArrayList<UserNotification>()

  list.forEach { item ->
    val createdMs = item.createdAt?.toDate()?.time
    when {
      createdMs == null || createdMs >= todayStartMs -> todayItems.add(item)
      createdMs >= yesterdayStartMs -> yesterdayItems.add(item)
      else -> olderItems.add(item)
    }
  }

  return buildList {
    if (todayItems.isNotEmpty()) add(NotificationTimeSection(NotificationTimeBucket.TODAY, todayItems))
    if (yesterdayItems.isNotEmpty()) add(NotificationTimeSection(NotificationTimeBucket.YESTERDAY, yesterdayItems))
    if (olderItems.isNotEmpty()) add(NotificationTimeSection(NotificationTimeBucket.OLDER, olderItems))
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationInboxSheet(
  notificationsState: UiState<List<UserNotification>>,
  onDismiss: () -> Unit,
  onNotificationClick: (UserNotification) -> Unit,
  onMarkAllRead: () -> Unit,
  onDeleteNotification: (UserNotification) -> Unit = {},
  onClearAllNotifications: () -> Unit = {},
  onPermissionResultMessage: (String) -> Unit
) {
  val context = LocalContext.current
  var selectedFilter by remember { mutableStateOf(NotificationInboxFilter.ALL) }
  var showMoreMenu by remember { mutableStateOf(false) }
  var showClearAllConfirmDialog by remember { mutableStateOf(false) }
  var hasPermission by remember {
    mutableStateOf(VizyonNotificationHelper.hasNotificationPermission(context))
  }
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasPermission = granted
      onPermissionResultMessage(
        if (granted) "Cihaz bildirimleri açıldı!"
        else "Cihaz bildirimi kapalı; bildirimlerin uygulama içi kutuya gelmeye devam edecek."
      )
    }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val allList = (notificationsState as? UiState.Success)?.data.orEmpty()
  val unreadCount = allList.count { !it.read }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("notification_inbox_sheet")
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .height(560.dp)
          .padding(horizontal = 18.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Polished header with live unread status pill & top-right 3-dot overflow menu
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Bildirim Paneli",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold
            )
            Surface(
              color =
                if (unreadCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(999.dp)
            ) {
              Text(
                text = if (unreadCount > 0) "$unreadCount yeni" else "Güncel",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color =
                  if (unreadCount > 0) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }
          Text(
            text = "Tüm sosyal etkileşimler ve mesajlar tek bakışta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box {
            StudioCircleIconButton(
              icon = Icons.Rounded.MoreVert,
              contentDescription = "Bildirim seçenekleri",
              onClick = { showMoreMenu = true },
              size = 48.dp,
              modifier = Modifier.testTag("notif_more_menu_btn")
            )
            DropdownMenu(
              expanded = showMoreMenu,
              onDismissRequest = { showMoreMenu = false },
              modifier = Modifier.testTag("notif_more_dropdown_menu")
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    text = "Tümünü Oku",
                    fontWeight = FontWeight.SemiBold
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.DoneAll,
                    contentDescription = null
                  )
                },
                enabled = unreadCount > 0,
                onClick = {
                  showMoreMenu = false
                  onMarkAllRead()
                },
                modifier = Modifier.testTag("mark_all_notifications_read_btn")
              )
              HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                modifier = Modifier.padding(vertical = 2.dp)
              )
              DropdownMenuItem(
                text = {
                  Text(
                    text = "Tümünü Temizle",
                    fontWeight = FontWeight.SemiBold
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null
                  )
                },
                colors =
                  MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                  ),
                enabled = allList.isNotEmpty(),
                onClick = {
                  showMoreMenu = false
                  showClearAllConfirmDialog = true
                },
                modifier = Modifier.testTag("clear_all_notifications_btn")
              )
            }
          }

          StudioCircleIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Bildirim panelini kapat",
            onClick = onDismiss,
            size = 48.dp,
            modifier = Modifier.testTag("close_notification_inbox_btn")
          )
        }
      }

      if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        GlassStudioCard(
          highlighted = true,
          cornerRadius = 18.dp
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Cihaz Bildirimlerini Aç",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Mesaj, takip ve hikaye bildirimlerini arka planda da anlık al.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            StudioGradientCtaButton(
              text = "İzin Ver",
              onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            )
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        NotificationInboxFilter.entries.forEach { filter ->
          val count =
            when (filter) {
              NotificationInboxFilter.ALL -> allList.size
              NotificationInboxFilter.UNREAD -> unreadCount
              NotificationInboxFilter.MESSAGE ->
                allList.count {
                  val t = NotificationType.fromRaw(it.type)
                  t == NotificationType.MESSAGE ||
                    t == NotificationType.VOICE_MESSAGE ||
                    t == NotificationType.MESSAGE_REQUEST
                }
              NotificationInboxFilter.FOLLOW ->
                allList.count {
                  val t = NotificationType.fromRaw(it.type)
                  t == NotificationType.FOLLOW || t == NotificationType.FOLLOW_REQUEST
                }
              NotificationInboxFilter.LIKE ->
                allList.count { NotificationType.fromRaw(it.type) == NotificationType.LIKE }
              NotificationInboxFilter.SOCIAL ->
                allList.count {
                  val t = NotificationType.fromRaw(it.type)
                  t == NotificationType.COMMENT || t == NotificationType.REPOST
                }
              NotificationInboxFilter.STORY ->
                allList.count { NotificationType.fromRaw(it.type) == NotificationType.STORY }
            }
          val chipText = if (count > 0) "${filter.label} ($count)" else filter.label
          StudioInteractiveChip(
            label = chipText,
            selected = selectedFilter == filter,
            leadingIcon = filter.icon,
            onClick = { selectedFilter = filter },
            modifier = Modifier.testTag("notif_filter_${filter.name}")
          )
        }
      }

      when (notificationsState) {
        is UiState.Loading -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text("Bildirimler yükleniyor...", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        is UiState.Error -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
            Text(notificationsState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val filteredList =
            remember(allList, selectedFilter) {
              when (selectedFilter) {
                NotificationInboxFilter.ALL -> allList
                NotificationInboxFilter.UNREAD -> allList.filter { !it.read }
                NotificationInboxFilter.MESSAGE ->
                  allList.filter {
                    val t = NotificationType.fromRaw(it.type)
                    t == NotificationType.MESSAGE ||
                      t == NotificationType.VOICE_MESSAGE ||
                      t == NotificationType.MESSAGE_REQUEST
                  }
                NotificationInboxFilter.FOLLOW ->
                  allList.filter {
                    val t = NotificationType.fromRaw(it.type)
                    t == NotificationType.FOLLOW || t == NotificationType.FOLLOW_REQUEST
                  }
                NotificationInboxFilter.LIKE ->
                  allList.filter { NotificationType.fromRaw(it.type) == NotificationType.LIKE }
                NotificationInboxFilter.SOCIAL ->
                  allList.filter {
                    val t = NotificationType.fromRaw(it.type)
                    t == NotificationType.COMMENT || t == NotificationType.REPOST
                  }
                NotificationInboxFilter.STORY ->
                  allList.filter { NotificationType.fromRaw(it.type) == NotificationType.STORY }
              }
            }
          val groupedSections = remember(filteredList) { groupNotificationsByTimeBucket(filteredList) }

          if (filteredList.isEmpty()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().weight(1f)) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier =
                    Modifier.size(64.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                ) {
                  Icon(
                    imageVector = Icons.Rounded.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                  )
                }
                Text(
                  text =
                    if (allList.isEmpty()) "Henüz bildirim yok"
                    else "${selectedFilter.label} kategorisinde bildirim yok",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "Yeni mesaj, takipçi, beğeni veya hikaye geldiğinde burada görünür.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(bottom = 12.dp),
              modifier = Modifier.fillMaxWidth().weight(1f).testTag("notif_inbox_lazy_list")
            ) {
              groupedSections.forEach { section ->
                item(
                  key = "header_${section.bucket.name}",
                  contentType = "notification_group_header"
                ) {
                  NotificationTimeSectionHeader(
                    bucket = section.bucket,
                    count = section.items.size,
                    modifier = Modifier.animateItem()
                  )
                }

                items(
                  items = section.items,
                  key = { it.id },
                  contentType = { "notification_card" }
                ) { notif ->
                  SwipeableNotificationCard(
                    notif = notif,
                    onClick = { onNotificationClick(notif) },
                    onDelete = { onDeleteNotification(notif) },
                    modifier = Modifier.animateItem()
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showClearAllConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearAllConfirmDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Rounded.DeleteSweep,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error
        )
      },
      title = {
        Text(
          text = "Tüm Bildirimleri Temizle",
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = "Tüm bildirim geçmişin kalıcı olarak silinecek ve cihaz bildirim çubuğundaki bildirimler temizlenecek. Devam etmek istiyor musun?"
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showClearAllConfirmDialog = false
            onClearAllNotifications()
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            ),
          modifier = Modifier.testTag("confirm_clear_notifications_btn")
        ) {
          Text("Temizle", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showClearAllConfirmDialog = false },
          modifier = Modifier.testTag("cancel_clear_notifications_btn")
        ) {
          Text("Vazgeç")
        }
      },
      modifier = Modifier.testTag("confirm_clear_notifications_dialog")
    )
  }
}

@Composable
private fun NotificationTimeSectionHeader(
  bucket: NotificationTimeBucket,
  count: Int,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .padding(top = 4.dp, bottom = 2.dp)
        .testTag("notif_group_header_${bucket.name}")
  ) {
    Text(
      text = bucket.label,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color =
        if (bucket == NotificationTimeBucket.TODAY) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
      modifier = Modifier.weight(1f)
    )
    Surface(
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      shape = RoundedCornerShape(999.dp)
    ) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationCard(
  notif: UserNotification,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val currentOnDelete by rememberUpdatedState(onDelete)
  var hasTriggeredDelete by remember(notif.id) { mutableStateOf(false) }
  val dismissState =
    rememberSwipeToDismissBoxState(
      confirmValueChange = { value ->
        if (
          (value == SwipeToDismissBoxValue.StartToEnd ||
            value == SwipeToDismissBoxValue.EndToStart) && !hasTriggeredDelete
        ) {
          hasTriggeredDelete = true
          currentOnDelete()
          true
        } else {
          false
        }
      },
      positionalThreshold = { totalDistance -> totalDistance * 0.42f }
    )

  val nType = NotificationType.fromRaw(notif.type)
  val unread = !notif.read
  val accentTint =
    when (nType) {
      NotificationType.LIKE -> MaterialTheme.colorScheme.secondary
      NotificationType.REPOST, NotificationType.STORY -> MaterialTheme.colorScheme.tertiary
      NotificationType.MESSAGE,
      NotificationType.VOICE_MESSAGE,
      NotificationType.MESSAGE_REQUEST -> MaterialTheme.colorScheme.secondary
      NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST -> MaterialTheme.colorScheme.primary
      NotificationType.COMMENT -> MaterialTheme.colorScheme.primary
    }

  SwipeToDismissBox(
    state = dismissState,
    enableDismissFromStartToEnd = true,
    enableDismissFromEndToStart = true,
    modifier =
      modifier
        .fillMaxWidth()
        .testTag("notification_swipe_${notif.id}"),
    backgroundContent = {
      val direction = dismissState.dismissDirection
      val isSwiping = direction != SwipeToDismissBoxValue.Settled
      val bgColor by
        animateColorAsState(
          targetValue =
            if (isSwiping) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
          label = "notif_swipe_bg"
        )
      val contentAlignment =
        when (direction) {
          SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
          else -> Alignment.CenterEnd
        }
      val iconScale by
        animateFloatAsState(
          targetValue =
            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) 1.12f else 0.92f,
          label = "notif_swipe_scale"
        )

      Box(
        contentAlignment = contentAlignment,
        modifier =
          Modifier.fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .padding(horizontal = 20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.scale(iconScale)
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Bildirimi sil",
            tint =
              if (isSwiping) MaterialTheme.colorScheme.onError
              else MaterialTheme.colorScheme.error
          )
          Text(
            text = "Sil",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color =
              if (isSwiping) MaterialTheme.colorScheme.onError
              else MaterialTheme.colorScheme.error
          )
        }
      }
    }
  ) {
    GlassStudioCard(
      highlighted = unread,
      accentColor = accentTint,
      cornerRadius = 18.dp,
      onClick = onClick,
      modifier =
        Modifier.testTag("notification_item_${notif.id}")
          .semantics {
            customActions =
              listOf(
                CustomAccessibilityAction(label = "Bildirimi sil") {
                  if (!hasTriggeredDelete) {
                    hasTriggeredDelete = true
                    currentOnDelete()
                  }
                  true
                }
              )
          }
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
      ) {
        if (unread) {
          Box(
            modifier =
              Modifier.width(4.dp)
                .fillMaxHeight()
                .background(
                  Brush.verticalGradient(
                    listOf(accentTint, MaterialTheme.colorScheme.primary)
                  )
                )
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
          Box(contentAlignment = Alignment.BottomEnd) {
            StudioAvatar(
              photoUrlOrPath = notif.senderAvatarUrl,
              contentDescription = VizyonNotificationHelper.cleanDisplayNameOnly(notif.senderName),
              size = 46.dp,
              ringColor =
                if (unread) accentTint
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(19.dp)
                  .clip(CircleShape)
                  .background(accentTint)
                  .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
              Icon(
                imageVector =
                  when (nType) {
                    NotificationType.LIKE -> Icons.Rounded.Favorite
                    NotificationType.COMMENT -> Icons.Rounded.ChatBubble
                    NotificationType.REPOST -> Icons.Rounded.Repeat
                    NotificationType.FOLLOW, NotificationType.FOLLOW_REQUEST -> Icons.Rounded.PersonAdd
                    NotificationType.STORY -> Icons.Rounded.AutoAwesome
                    NotificationType.VOICE_MESSAGE -> Icons.Rounded.GraphicEq
                    NotificationType.MESSAGE, NotificationType.MESSAGE_REQUEST -> Icons.Rounded.ChatBubble
                  },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
              )
            }
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val displayNameOnly =
              VizyonNotificationHelper.cleanDisplayNameOnly(notif.senderName)
            val cleanBody = VizyonNotificationHelper.sanitizeNotificationMessage(notif)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(
                  text = displayNameOnly,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false)
                )
                Surface(
                  color = accentTint.copy(alpha = 0.16f),
                  shape = RoundedCornerShape(999.dp)
                ) {
                  Text(
                    text = nType.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentTint,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                  )
                }
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = formatRelativeTimeTr(notif.createdAt),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (unread) {
                  Box(
                    modifier =
                      Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(accentTint)
                  )
                }
              }
            }
            Text(
              text = cleanBody,
              style = MaterialTheme.typography.bodyMedium,
              color =
                if (unread) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
              maxLines = 3,
              overflow = TextOverflow.Ellipsis
            )
            if (notif.trackTitle.isNotBlank()) {
              Surface(
                color = accentTint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accentTint,
                    modifier = Modifier.size(13.dp)
                  )
                  Text(
                    text = notif.trackTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentTint,
                    fontWeight = FontWeight.SemiBold,
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
