package com.example.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActivityComment
import com.example.model.ActivityFeedItem
import com.example.model.ArtistSummary
import com.example.model.DirectChatThread
import com.example.model.FollowConnection
import com.example.model.MusicStoryItem
import com.example.model.MusicTrack
import com.example.model.PublicProfile
import com.example.model.RecentTrackSummary
import com.example.model.StoryCardTheme
import com.example.model.UiState
import com.example.model.sanitizeFirestoreId
import com.example.ui.components.ActivityCommentsSheet
import com.example.ui.components.CloudSignInPromptCard
import com.example.ui.components.DirectChatsListSection
import com.example.ui.components.EmptySocialCard
import com.example.ui.components.GlassStudioCard
import com.example.ui.components.ListenerDirectoryCard
import com.example.ui.components.ListenerProfileSheet
import com.example.ui.components.QuickSocialComposerCard
import com.example.ui.components.RepostQuoteSheet
import com.example.ui.components.RichActivityPostCard
import com.example.ui.components.SocialFeedTab
import com.example.ui.components.SocialSegmentedTabs
import com.example.ui.components.SocialStoriesStrip
import com.example.ui.components.StoryStudioSheet
import com.example.ui.components.StoryViewerDialog
import com.example.ui.components.StudioAvatar
import com.example.ui.components.StudioCircleIconButton
import com.example.ui.components.StudioGlassButton
import com.example.ui.components.StudioSectionHeader
import com.example.ui.viewmodel.NotificationDeepLinkTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
  currentUserId: String?,
  userName: String,
  userHandle: String,
  userBio: String,
  userPhotoUrl: String,
  currentTrack: MusicTrack?,
  candidateTracks: List<MusicTrack>,
  selfRecentTracks: List<RecentTrackSummary> = emptyList(),
  defaultStoryTheme: StoryCardTheme = StoryCardTheme.NEON_WAVE,
  storiesState: UiState<List<MusicStoryItem>> = UiState.Success(emptyList()),
  activityFeedState: UiState<List<ActivityFeedItem>>,
  activityCommentsState: UiState<List<ActivityComment>>,
  publicListenersState: UiState<List<PublicProfile>>,
  followingState: UiState<List<FollowConnection>>,
  followersCount: Int = 0,
  directChatsState: UiState<List<DirectChatThread>> = UiState.Success(emptyList()),
  likedActivityIds: Set<String>,
  unreadNotificationCount: Int,
  pendingDeepLink: NotificationDeepLinkTarget? = null,
  onConsumeDeepLink: () -> Unit = {},
  onOpenNotifications: () -> Unit,
  onPublishQuickPost: (note: String, attachedTrack: MusicTrack?) -> Unit,
  onPublishStory: (MusicTrack, String, StoryCardTheme) -> Unit = { _, _, _ -> },
  onDeleteStory: (String) -> Unit = {},
  onPlayActivityTrack: (MusicTrack) -> Unit,
  onLikeActivity: (ActivityFeedItem) -> Unit,
  onOpenComments: (ActivityFeedItem) -> Unit,
  onCloseComments: () -> Unit,
  onSubmitComment: (ActivityFeedItem, String) -> Unit,
  onRepostActivity: (ActivityFeedItem, String) -> Unit,
  onDeleteActivityPost: (ActivityFeedItem) -> Unit = {},
  onToggleFollowListener: (PublicProfile) -> Unit,
  onOpenDirectChat: (PublicProfile) -> Unit = {},
  onOpenDirectChatById: (String) -> Unit = {},
  onDeleteDirectChat: (String) -> Unit = {},
  onInspectFollowLists: (String) -> Unit = {},
  onArtistClick: (ArtistSummary) -> Unit,
  onOpenStudioForCloudSync: () -> Unit,
  onOpenStoryViewer: ((MusicStoryItem) -> Unit)? = null,
  onNotifyMessage: (String) -> Unit = {},
  pendingRequestsCount: Int = 0,
  outgoingRequestKeys: Set<String> = emptySet(),
  onOpenRequests: () -> Unit = {},
  onBlockUser: (userId: String, displayName: String, handle: String, photoUrl: String) -> Unit = { _, _, _, _ -> },
  onReportUser: (targetUserId: String, targetType: String, targetId: String, displayName: String) -> Unit = { _, _, _, _ -> }
) {
  var selectedTab by remember { mutableStateOf(SocialFeedTab.ALL) }
  var userSearchQuery by remember { mutableStateOf("") }
  var showFeedSearch by remember { mutableStateOf(false) }
  var selectedListenerForSheet by remember { mutableStateOf<PublicProfile?>(null) }
  var activeCommentsTarget by remember { mutableStateOf<ActivityFeedItem?>(null) }
  var activeRepostTarget by remember { mutableStateOf<ActivityFeedItem?>(null) }
  var activeLongPressPost by remember { mutableStateOf<ActivityFeedItem?>(null) }
  var storyStudioInitialTrack by remember { mutableStateOf<MusicTrack?>(null) }
  var showStoryStudio by remember { mutableStateOf(false) }
  var activeStoryForViewer by remember { mutableStateOf<MusicStoryItem?>(null) }

  val followingIds =
    ((followingState as? UiState.Success)?.data ?: emptyList())
      .map { it.targetUserId.ifBlank { it.id } }
      .toSet()

  val allListeners =
    ((publicListenersState as? UiState.Success)?.data ?: emptyList())
      .filter { it.userId != currentUserId }

  val allFeedItems = (activityFeedState as? UiState.Success)?.data ?: emptyList()
  val activeStories = (storiesState as? UiState.Success)?.data ?: emptyList()
  val chatsList = (directChatsState as? UiState.Success)?.data.orEmpty()

  LaunchedEffect(pendingDeepLink, allFeedItems, allListeners, activeStories) {
    val target = pendingDeepLink ?: return@LaunchedEffect
    when (target) {
      is NotificationDeepLinkTarget.OpenFeed -> {
        selectedTab = SocialFeedTab.ALL
        onConsumeDeepLink()
      }
      is NotificationDeepLinkTarget.OpenRequests -> {
        onOpenRequests()
        onConsumeDeepLink()
      }
      is NotificationDeepLinkTarget.OpenPostComments -> {
        val post = allFeedItems.firstOrNull { it.id == target.activityId }
        if (post != null) {
          selectedTab = SocialFeedTab.ALL
          onOpenComments(post)
          activeCommentsTarget = post
          onConsumeDeepLink()
        } else if (activityFeedState is UiState.Success) {
          selectedTab = SocialFeedTab.ALL
          onConsumeDeepLink()
        }
      }
      is NotificationDeepLinkTarget.OpenListenerProfile -> {
        val person = allListeners.firstOrNull { it.userId == target.userId }
        if (person != null) {
          selectedListenerForSheet = person
          onConsumeDeepLink()
        } else if (publicListenersState is UiState.Success) {
          selectedTab = SocialFeedTab.LISTENERS
          onConsumeDeepLink()
        }
      }
      is NotificationDeepLinkTarget.OpenStory -> {
        val story = activeStories.firstOrNull { it.id == target.storyId }
        if (story != null) {
          if (onOpenStoryViewer != null) {
            onOpenStoryViewer(story)
          } else {
            activeStoryForViewer = story
          }
          onConsumeDeepLink()
        } else if (storiesState is UiState.Success) {
          onConsumeDeepLink()
        }
      }
      is NotificationDeepLinkTarget.OpenChat -> {
        selectedTab = SocialFeedTab.CHATS
        onOpenDirectChatById(target.userId)
        onConsumeDeepLink()
      }
    }
  }

  val cleanQuery = userSearchQuery.trim()
  val filteredListeners =
    if (cleanQuery.isBlank()) allListeners
    else {
      allListeners.filter { person ->
        person.displayName.contains(cleanQuery, ignoreCase = true) ||
          person.handle.contains(cleanQuery, ignoreCase = true) ||
          person.bio.contains(cleanQuery, ignoreCase = true)
      }
    }

  val selfPublicProfile =
    remember(currentUserId, userName, userHandle, userBio, userPhotoUrl, currentTrack, selfRecentTracks, followingIds.size, followersCount) {
      PublicProfile(
        id = currentUserId ?: "local",
        userId = currentUserId ?: "local",
        displayName = userName.ifBlank { "Sen" },
        handle = userHandle.ifBlank { "@vizyon" },
        bio = userBio,
        photoUrl = userPhotoUrl,
        currentTrackTitle = currentTrack?.title.orEmpty(),
        currentArtistName = currentTrack?.artistName.orEmpty(),
        lastTrackId = currentTrack?.trackId.orEmpty(),
        lastTrackTitle = currentTrack?.title.orEmpty(),
        lastArtistId = currentTrack?.artistId.orEmpty(),
        lastArtistName = currentTrack?.artistName.orEmpty(),
        lastTrackArtworkUrl = currentTrack?.artworkUrl.orEmpty(),
        lastTrackYoutubeId = currentTrack?.youtubeVideoId.orEmpty(),
        recentTracks = selfRecentTracks,
        followerCount = followersCount.toLong(),
        followingCount = followingIds.size.toLong()
      )
    }

  val smoothEase = remember { CubicBezierEasing(0.22f, 1f, 0.36f, 1f) }
  val itemFadeIn = remember(smoothEase) { tween<Float>(durationMillis = 150, easing = smoothEase) }
  val itemFadeOut = remember(smoothEase) { tween<Float>(durationMillis = 100, easing = smoothEase) }

  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize().testTag("social_feed_screen")
    ) {
      // Slim Instagram-style Top Header with Search Toggle, Messages & Notification Bell + Requests Pill
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "VİZYON SOSYAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Text("Akış", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier.size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CircleShape)
                    .clickable { selectedListenerForSheet = selfPublicProfile }
                    .testTag("feed_profile_btn")
              ) {
                StudioAvatar(
                  photoUrlOrPath = userPhotoUrl,
                  contentDescription = "Profilimi Görüntüle",
                  size = 36.dp,
                  ringColor = MaterialTheme.colorScheme.primary
                )
              }
              StudioCircleIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = "Akışta Ara",
                onClick = { showFeedSearch = !showFeedSearch },
                size = 48.dp,
                highlighted = showFeedSearch || userSearchQuery.isNotBlank(),
                modifier = Modifier.testTag("feed_search_toggle_btn")
              )
              StudioCircleIconButton(
                icon = Icons.Rounded.ChatBubbleOutline,
                contentDescription = "Sohbetler",
                onClick = { selectedTab = SocialFeedTab.CHATS },
                size = 48.dp,
                highlighted = selectedTab == SocialFeedTab.CHATS,
                badgeCount = chatsList.size,
                modifier = Modifier.testTag("feed_messages_btn")
              )
              StudioCircleIconButton(
                icon = Icons.Rounded.Notifications,
                contentDescription = "Bildirim Kutusu",
                onClick = onOpenNotifications,
                size = 48.dp,
                highlighted = unreadNotificationCount > 0,
                badgeCount = unreadNotificationCount,
                modifier = Modifier.testTag("feed_notifications_bell_btn")
              )
            }
          }

          StudioGlassButton(
            text =
              if (pendingRequestsCount > 0) "Takip & Mesaj İstekleri ($pendingRequestsCount)"
              else "Takip & Mesaj İstekleri",
            icon = Icons.Rounded.MarkChatUnread,
            tintColor =
              if (pendingRequestsCount > 0) MaterialTheme.colorScheme.secondary
              else MaterialTheme.colorScheme.primary,
            onClick = onOpenRequests,
            modifier = Modifier.fillMaxWidth().testTag("feed_requests_btn")
          )
        }
      }

      // Smart Search Bar (always shown on Kişiler/Sohbet, toggleable on Akış/Takip)
      val searchVisible =
        showFeedSearch ||
          userSearchQuery.isNotBlank() ||
          selectedTab == SocialFeedTab.LISTENERS ||
          selectedTab == SocialFeedTab.CHATS
      if (searchVisible) {
        item {
          GlassStudioCard(cornerRadius = 18.dp) {
            OutlinedTextField(
              value = userSearchQuery,
              onValueChange = { userSearchQuery = it },
              placeholder = { Text("Kullanıcı, @handle, gönderi veya şarkı ara...") },
              leadingIcon = {
                Icon(
                  Icons.Rounded.PersonSearch,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              trailingIcon = {
                if (userSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { userSearchQuery = "" }) {
                    Icon(Icons.Rounded.Clear, contentDescription = "Aramayı temizle")
                  }
                }
              },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
              ),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().testTag("feed_user_search_input")
            )
          }
        }
      }

    if (selectedTab == SocialFeedTab.CHATS) {
      item(key = "chats_section", contentType = "chats_section") {
        Box(
          modifier =
            Modifier.animateItem(
              fadeInSpec = itemFadeIn,
              fadeOutSpec = itemFadeOut,
              placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
            )
        ) {
          DirectChatsListSection(
            chatsState = directChatsState,
            currentUserId = currentUserId,
            publicListeners = filteredListeners,
            onOpenChatThread = { peer -> onOpenDirectChat(peer) },
            onOpenListenersTab = { selectedTab = SocialFeedTab.LISTENERS },
            onDeleteChatThread = onDeleteDirectChat
          )
        }
      }
    } else if (selectedTab == SocialFeedTab.LISTENERS) {
      item {
        StudioSectionHeader(
          title = "Dinleyici Dizini",
          subtitle = "Kullanıcıları keşfet, takip et veya doğrudan mesaj gönder",
          badge = "${filteredListeners.size}"
        )
      }

      if (currentUserId.isNullOrBlank()) {
        item {
          CloudSignInPromptCard(onOpenStudioForCloudSync = onOpenStudioForCloudSync)
        }
      } else {
        when (publicListenersState) {
          is UiState.Loading -> {
            item {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
              }
            }
          }
          is UiState.Error -> {
            item {
              Text(publicListenersState.message, color = MaterialTheme.colorScheme.error)
            }
          }
          is UiState.Success -> {
            if (filteredListeners.isEmpty()) {
              item {
                EmptySocialCard(
                  icon = Icons.Rounded.Groups,
                  title = if (cleanQuery.isNotBlank()) "Eşleşen dinleyici yok" else "Henüz keşfedilebilir dinleyici yok",
                  subtitle =
                    if (cleanQuery.isNotBlank()) "“$cleanQuery” sorgusuyla eşleşen bir profil bulunamadı."
                    else "Yeni dinleyiciler katıldıkça profilleri ve hikayeleri burada listelenecek."
                )
              }
            } else {
              items(
                items = filteredListeners,
                key = { "listener_${it.userId.ifBlank { it.id }}" },
                contentType = { "listener_card" }
              ) { person ->
                val rawPeerId = person.userId.ifBlank { person.id }
                val safePeerId = if (rawPeerId.isNotBlank()) sanitizeFirestoreId(rawPeerId) else ""
                val isFollowPending = safePeerId.isNotBlank() && "follow_${safePeerId}::PENDING" in outgoingRequestKeys
                Box(
                  modifier =
                    Modifier.animateItem(
                      fadeInSpec = itemFadeIn,
                      fadeOutSpec = itemFadeOut,
                      placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                    )
                ) {
                  ListenerDirectoryCard(
                    person = person,
                    isFollowing = person.userId in followingIds || safePeerId in followingIds,
                    isFollowRequestPending = isFollowPending,
                    onOpenProfile = { selectedListenerForSheet = person },
                    onToggleFollow = { onToggleFollowListener(person) },
                    onOpenChat = { onOpenDirectChat(person) }
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // Pure Instagram-style Stories Strip right at the top (only + Hikayem & active stories)
      item {
        SocialStoriesStrip(
          stories = activeStories,
          onCreateStoryClick = {
            storyStudioInitialTrack = currentTrack ?: candidateTracks.firstOrNull()
            showStoryStudio = true
          },
          onStoryClick = { story ->
            if (onOpenStoryViewer != null) {
              onOpenStoryViewer(story)
            } else {
              activeStoryForViewer = story
            }
          }
        )
      }

      // Inline Quick Music & Status Composer
      item {
        QuickSocialComposerCard(
          userName = userName,
          userPhotoUrl = userPhotoUrl,
          currentTrack = currentTrack,
          candidateTracks = candidateTracks,
          onPublishPost = onPublishQuickPost,
          onOpenStoryStudio = { preferred ->
            storyStudioInitialTrack = preferred ?: currentTrack ?: candidateTracks.firstOrNull()
            showStoryStudio = true
          }
        )
      }

      if (currentUserId.isNullOrBlank() && selectedTab == SocialFeedTab.FOLLOWING) {
        item {
          CloudSignInPromptCard(onOpenStudioForCloudSync = onOpenStudioForCloudSync)
        }
      }

      // Rich Activity Posts Stream (user-shared posts & reposts only)
      when (activityFeedState) {
        is UiState.Loading -> {
          item {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(activityFeedState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val scopedItems =
            if (selectedTab == SocialFeedTab.FOLLOWING) {
              activityFeedState.data.filter { it.userId in followingIds || it.userId == currentUserId }
            } else {
              activityFeedState.data
            }
          val items =
            if (cleanQuery.isBlank()) scopedItems
            else {
              scopedItems.filter { ev ->
                ev.userName.contains(cleanQuery, ignoreCase = true) ||
                  ev.userHandle.contains(cleanQuery, ignoreCase = true) ||
                  ev.trackTitle.contains(cleanQuery, ignoreCase = true) ||
                  ev.artistName.contains(cleanQuery, ignoreCase = true) ||
                  ev.note.contains(cleanQuery, ignoreCase = true)
              }
            }

          if (items.isEmpty()) {
            item {
              EmptySocialCard(
                icon = Icons.Rounded.Headphones,
                title =
                  if (selectedTab == SocialFeedTab.FOLLOWING) "Takip akışı henüz boş"
                  else "Akışta henüz gönderi yok",
                subtitle =
                  if (selectedTab == SocialFeedTab.FOLLOWING)
                    "Kişiler sekmesinden müzikseverleri takip edebilir veya yukarıdan ilk paylaşımını yapabilirsin."
                  else "Yukarıdaki hızlı kutudan bir parça ve durum notu paylaşarak akışı başlat.",
                actionLabel = if (selectedTab == SocialFeedTab.FOLLOWING) "Kişileri Gör" else null,
                onAction =
                  if (selectedTab == SocialFeedTab.FOLLOWING) {
                    { selectedTab = SocialFeedTab.LISTENERS }
                  } else null
              )
            }
          } else {
            items(
              items = items,
              key = { "${selectedTab.name}_${it.id}" },
              contentType = { "activity_post" }
            ) { event ->
              Box(
                modifier =
                  Modifier.animateItem(
                    fadeInSpec = itemFadeIn,
                    fadeOutSpec = itemFadeOut,
                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                  )
              ) {
                RichActivityPostCard(
                  event = event,
                  likedByMe = event.id in likedActivityIds,
                  onOpenAuthorProfile = {
                    val matched =
                      allListeners.firstOrNull { it.userId == event.userId }
                        ?: if (event.userId == currentUserId || event.userId == "local") selfPublicProfile
                        else PublicProfile(
                          id = event.userId,
                          userId = event.userId,
                          displayName = event.userName,
                          handle = event.userHandle,
                          photoUrl = event.userAvatarUrl,
                          currentTrackTitle = event.trackTitle,
                          currentArtistName = event.artistName,
                          lastTrackId = event.trackId,
                          lastTrackTitle = event.trackTitle,
                          lastArtistId = event.artistId,
                          lastArtistName = event.artistName,
                          lastTrackArtworkUrl = event.albumArtUrl,
                          lastTrackYoutubeId = event.youtubeVideoId
                        )
                    selectedListenerForSheet = matched
                  },
                  onPlayTrack = onPlayActivityTrack,
                  onArtistClick = onArtistClick,
                  onLike = { onLikeActivity(event) },
                  onOpenComments = {
                    onOpenComments(event)
                    activeCommentsTarget = event
                  },
                  onOpenRepost = { activeRepostTarget = event },
                  onLongPress = { activeLongPressPost = event }
                )
              }
            }
          }
        }
      }
    }
    }

    // Bottom floating Instagram-style capsule tabs: Akış | Takip | Kişiler | Sohbet
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      SocialSegmentedTabs(
        selectedTab = selectedTab,
        followingCount = followingIds.size,
        listenersCount = allListeners.size,
        chatsCount = chatsList.size,
        onSelectTab = { selectedTab = it }
      )
    }
  }

  // Modal Sheet 1: Comments Thread + Composer
  activeCommentsTarget?.let { target ->
    val liveTarget = allFeedItems.firstOrNull { it.id == target.id } ?: target
    ActivityCommentsSheet(
      activity = liveTarget,
      commentsState = activityCommentsState,
      onDismiss = {
        activeCommentsTarget = null
        onCloseComments()
      },
      onSubmitComment = { text -> onSubmitComment(liveTarget, text) }
    )
  }

  // Modal Sheet 2: Repost / Quote Sheet
  activeRepostTarget?.let { target ->
    RepostQuoteSheet(
      original = target,
      onDismiss = { activeRepostTarget = null },
      onConfirmRepost = { quote ->
        onRepostActivity(target, quote)
        activeRepostTarget = null
      }
    )
  }

  // Modal Sheet 3: Detailed Listener Profile Window (includes live status & Son 5 Dinlenen Şarkı)
  selectedListenerForSheet?.let { person ->
    val rawPersonId = person.userId.ifBlank { person.id }
    val safePersonId = if (rawPersonId.isNotBlank()) sanitizeFirestoreId(rawPersonId) else ""
    val isSelf = rawPersonId == "local" || (!currentUserId.isNullOrBlank() && rawPersonId == currentUserId)
    val isFollowPending = !isSelf && safePersonId.isNotBlank() && "follow_${safePersonId}::PENDING" in outgoingRequestKeys
    val recentPosts =
      allFeedItems.filter {
        it.userId == person.userId ||
          (isSelf && it.userHandle.equals(person.handle, ignoreCase = true))
      }
    ListenerProfileSheet(
      person = person,
      isSelf = isSelf,
      isFollowing = person.userId in followingIds || safePersonId in followingIds,
      isFollowRequestPending = isFollowPending,
      recentPosts = recentPosts,
      selfRecentTracks = selfRecentTracks,
      onDismiss = { selectedListenerForSheet = null },
      onToggleFollow = { onToggleFollowListener(person) },
      onPlayActivityTrack = onPlayActivityTrack,
      onCreateStoryFromTrack = { tr ->
        selectedListenerForSheet = null
        storyStudioInitialTrack = tr
        showStoryStudio = true
      },
      onOpenChat = {
        selectedListenerForSheet = null
        onOpenDirectChat(person)
      },
      onOpenFollowLists = {
        val targetUid = if (isSelf) (currentUserId ?: "local") else rawPersonId
        onInspectFollowLists(targetUid)
      },
      onBlockUser = {
        selectedListenerForSheet = null
        onBlockUser(rawPersonId, person.displayName, person.handle, person.photoUrl)
      },
      onReportUser = {
        selectedListenerForSheet = null
        onReportUser(rawPersonId, "USER", rawPersonId, person.displayName)
      }
    )
  }

  // Modal Sheet 4: Story Studio Sheet (In-app 24h Story + External PNG Share)
  if (showStoryStudio) {
    StoryStudioSheet(
      userName = userName,
      userHandle = userHandle,
      userPhotoUrl = userPhotoUrl,
      initialTrack = storyStudioInitialTrack,
      candidateTracks = candidateTracks,
      initialTheme = defaultStoryTheme,
      onDismiss = { showStoryStudio = false },
      onPublishStory = onPublishStory,
      onNotifyMessage = onNotifyMessage
    )
  }

  // Dialog 5: Fullscreen Story Viewer
  activeStoryForViewer?.let { story ->
    val isAuthor = story.userId == "local" || (!currentUserId.isNullOrBlank() && story.userId == currentUserId)
    val authorProfile =
      allListeners.firstOrNull { it.userId == story.userId }
        ?: PublicProfile(
          id = story.userId,
          userId = story.userId,
          displayName = story.userName.ifBlank { "Vizyon Dinleyicisi" },
          handle = story.userHandle.ifBlank { "@vizyon" },
          photoUrl = story.userAvatarUrl,
          currentTrackTitle = story.trackTitle,
          currentArtistName = story.artistName
        )
    StoryViewerDialog(
      story = story,
      isAuthor = isAuthor,
      onDismiss = { activeStoryForViewer = null },
      onPlayTrack = onPlayActivityTrack,
      onDeleteStory = onDeleteStory,
      onNotifyMessage = onNotifyMessage,
      onOpenAuthorProfile = { selectedListenerForSheet = authorProfile },
      onMessageAuthor = { onOpenDirectChat(authorProfile) },
      onBlockAuthor =
        if (!isAuthor) {
          { onBlockUser(story.userId, story.userName, story.userHandle, story.userAvatarUrl) }
        } else null,
      onReportAuthor =
        if (!isAuthor) {
          { onReportUser(story.userId, "STORY", story.id, story.userName) }
        } else null
    )
  }

  // Modal Sheet 6: Long-Press Post Action Sheet (Delete owned post / Comments / Repost)
  activeLongPressPost?.let { post ->
    val isAuthor = post.userId == "local" || (!currentUserId.isNullOrBlank() && post.userId == currentUserId)
    ModalBottomSheet(
      onDismissRequest = { activeLongPressPost = null },
      containerColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier.testTag("post_actions_bottom_sheet")
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          StudioAvatar(
            photoUrlOrPath = post.userAvatarUrl,
            contentDescription = post.userName,
            size = 40.dp
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = post.userName.ifBlank { "Vizyon Dinleyicisi" },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = post.note.ifBlank { "${post.trackTitle} • ${post.artistName}" },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        if (isAuthor) {
          Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            shape = RoundedCornerShape(16.dp),
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                  onDeleteActivityPost(post)
                  activeLongPressPost = null
                }
                .testTag("post_action_delete_btn")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Gönderiyi Sil",
                tint = MaterialTheme.colorScheme.error
              )
              Column {
                Text(
                  text = "Gönderiyi Sil",
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.error,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Akıştan ve profil geçmişinden kalıcı olarak kaldırılır.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StudioGlassButton(
              text = "Şikayet Et",
              icon = Icons.Rounded.Flag,
              tintColor = MaterialTheme.colorScheme.secondary,
              onClick = {
                val target = post
                activeLongPressPost = null
                onReportUser(target.userId, "POST", target.id, target.userName)
              },
              modifier = Modifier.weight(1f).testTag("post_action_report_btn")
            )
            StudioGlassButton(
              text = "Engelle",
              icon = Icons.Rounded.Block,
              tintColor = MaterialTheme.colorScheme.error,
              onClick = {
                val target = post
                activeLongPressPost = null
                onBlockUser(target.userId, target.userName, target.userHandle, target.userAvatarUrl)
              },
              modifier = Modifier.weight(1f).testTag("post_action_block_btn")
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
          StudioGlassButton(
            text = "Yorumlara Git",
            icon = Icons.Rounded.ChatBubbleOutline,
            onClick = {
              val target = post
              activeLongPressPost = null
              onOpenComments(target)
              activeCommentsTarget = target
            },
            modifier = Modifier.weight(1f).testTag("post_action_comments_btn")
          )
          StudioGlassButton(
            text = "Yeniden Paylaş",
            icon = Icons.Rounded.Repeat,
            onClick = {
              val target = post
              activeLongPressPost = null
              activeRepostTarget = target
            },
            modifier = Modifier.weight(1f).testTag("post_action_repost_btn")
          )
        }
      }
    }
  }
}
