package com.example

import com.example.data.remote.HybridMusicRepository
import com.example.data.remote.ItunesItemDto
import com.example.data.remote.KidsContentFilter
import com.example.data.remote.YouTubeMusicApiService
import com.example.data.remote.YouTubeSearchId
import com.example.data.remote.YouTubeSearchItem
import com.example.data.remote.YouTubeSnippet
import com.example.data.remote.parseLrcLines
import com.example.model.MusicTrack
import com.example.player.YouTubeForegroundPlayerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.Query

class ExampleUnitTest {
  @Test
  fun parseLrcLines_extractsTimestampsInOrder() {
    val rawLrc =
      """
      [00:12.35]First lyric line
      [01:05.50]Second lyric line
      [00:04.10]Intro line
      """.trimIndent()
    val parsed = parseLrcLines(rawLrc)
    assertEquals(3, parsed.size)
    assertEquals(4_100L, parsed[0].timeMs)
    assertEquals("Intro line", parsed[0].text)
    assertEquals(12_350L, parsed[1].timeMs)
    assertEquals("First lyric line", parsed[1].text)
    assertEquals(65_500L, parsed[2].timeMs)
    assertEquals("Second lyric line", parsed[2].text)
  }

  @Test
  fun kidsContentFilter_blocksKidsSongsAndAllowsRegularMusic() {
    assertTrue(KidsContentFilter.isChildContent(title = "Baby Shark Dance", artistName = "Pinkfong"))
    assertTrue(KidsContentFilter.isChildContent(title = "Kırmızı Balık", artistName = "Çocuk Şarkıları"))
    assertTrue(KidsContentFilter.isChildContent(title = "Dandini Dandini Dastana", genre = "Ninni"))
    assertTrue(KidsContentFilter.isChildContent(title = "Happy Tune", genre = "Children's Music"))
    assertTrue(KidsContentFilter.isChildContent(title = "Pepee Şarkısı", artistName = "Pepee"))

    assertFalse(KidsContentFilter.isChildContent(title = "Dudu", artistName = "Tarkan", genre = "Turkish Pop"))
    assertFalse(KidsContentFilter.isChildContent(title = "Canın Sağ Olsun", artistName = "Semicenk", genre = "Pop"))
    assertFalse(KidsContentFilter.isChildContent(title = "Blinding Lights", artistName = "The Weeknd", genre = "Synthpop"))
  }

  @Test
  fun itunesArtworkUrl_upgradesToHighResolution() {
    val item = ItunesItemDto(artworkUrl100 = "https://is1-ssl.mzstatic.com/image/thumb/Music/v4/100x100bb.jpg")
    assertEquals("https://is1-ssl.mzstatic.com/image/thumb/Music/v4/600x600bb.jpg", item.highResArtworkUrl())
  }

  @Test
  fun youtubeCandidateMatcher_prefersOfficialOriginalOverSlowedOrKaraoke() {
    val repo = HybridMusicRepository()
    val slowedRemix =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_slow"),
        snippet = YouTubeSnippet(title = "Tarkan - Şımarık (Slowed + Reverb)", channelTitle = "NightVibes")
      )
    val karaokeVersion =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_karaoke"),
        snippet = YouTubeSnippet(title = "Tarkan - Şımarık Karaoke", channelTitle = "Karaoke Box")
      )
    val officialVideo =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_official"),
        snippet = YouTubeSnippet(title = "Tarkan - Şımarık (Official Video)", channelTitle = "Tarkan")
      )

    val picked =
      repo.pickBestYouTubeCandidate(
        items = listOf(slowedRemix, karaokeVersion, officialVideo),
        targetTitle = "Şımarık",
        targetArtist = "Tarkan"
      )
    assertNotNull(picked)
    assertEquals("vid_official", picked?.id?.videoId)
  }

  @Test
  fun youtubePlayerManager_surfacesIFrameErrorsAndLoadFailures() {
    val manager = YouTubeForegroundPlayerManager()
    val track =
      MusicTrack(
        trackId = "t1",
        title = "Test",
        artistId = "art1",
        artistName = "Artist",
        youtubeVideoId = "dQw4w9WgXcQ"
      )
    manager.prepareLoading(track)
    manager.onIFrameReady("dQw4w9WgXcQ")
    assertTrue(manager.state.value.isPlayerReady)

    manager.onIFrameError(150)
    val afterError = manager.state.value
    assertFalse(afterError.isPlayerReady)
    assertFalse(afterError.isResolvingVideo)
    assertFalse(afterError.isPlaying)
    assertTrue(afterError.playbackMessage?.contains("150") == true)

    manager.onIFrameLoadFailure("YouTube oynatıcı HTTP hatası (HTTP 403).")
    val afterLoadFailure = manager.state.value
    assertFalse(afterLoadFailure.isPlayerReady)
    assertFalse(afterLoadFailure.isResolvingVideo)
    assertEquals("YouTube oynatıcı HTTP hatası (HTTP 403).", afterLoadFailure.playbackMessage)
  }

  @Test
  fun youtubeApiSearchMethod_requiresVideoTypeAndEmbeddable() {
    val searchMethod =
      YouTubeMusicApiService::class.java.methods.firstOrNull { it.name == "searchMusicCategory" }
    assertNotNull(searchMethod)
    val queryNames =
      searchMethod!!.parameterAnnotations.flatten().filterIsInstance<Query>().map { it.value }
    assertTrue(queryNames.contains("type"))
    assertTrue(queryNames.contains("videoEmbeddable"))
  }

  @Test
  fun publicProfile_mapsLastListenedTrackWhenPresentAndNullWhenEmpty() {
    val emptyProfile = com.example.model.PublicProfile(userId = "u1", displayName = "Deniz")
    org.junit.Assert.assertNull(emptyProfile.toLastListenedTrack())

    val activeProfile =
      com.example.model.PublicProfile(
        userId = "u2",
        displayName = "Ece",
        lastTrackId = "tr_99",
        lastTrackTitle = "Kuzu Kuzu",
        lastArtistId = "art_1",
        lastArtistName = "Tarkan",
        lastTrackArtworkUrl = "https://example.com/art.jpg",
        lastTrackYoutubeId = "abc123XYZ99",
        lastPlayedAtMillis = 123456L
      )
    val track = activeProfile.toLastListenedTrack()
    assertNotNull(track)
    assertEquals("tr_99", track?.trackId)
    assertEquals("Kuzu Kuzu", track?.title)
    assertEquals("Tarkan", track?.artistName)
    assertEquals("abc123XYZ99", track?.youtubeVideoId)
  }

  @Test
  fun notificationType_parsesFollowAndLikeSafely() {
    assertEquals(com.example.model.NotificationType.FOLLOW, com.example.model.NotificationType.fromRaw("FOLLOW"))
    assertEquals(com.example.model.NotificationType.LIKE, com.example.model.NotificationType.fromRaw("LIKE"))
    assertEquals(com.example.model.NotificationType.FOLLOW, com.example.model.NotificationType.fromRaw("UNKNOWN"))
  }

  @Test
  fun youtubePlayerManager_keepsPlayingWhenMinimizedToCompactMiniVideo() {
    val manager = YouTubeForegroundPlayerManager()
    val track =
      MusicTrack(
        trackId = "t2",
        title = "Mini Test",
        artistId = "art2",
        artistName = "Artist",
        youtubeVideoId = "dQw4w9WgXcQ"
      )
    manager.prepareLoading(track)
    manager.onIFrameReady("dQw4w9WgXcQ")
    manager.onIFrameStateChanged(1) // PLAYING
    assertTrue(manager.state.value.isPlaying)

    manager.pauseWhenMinimized()
    assertTrue(manager.state.value.isPlaying)

    manager.pauseForBackground()
    assertFalse(manager.state.value.isPlaying)
  }

  @Test
  fun passcodeSecurityHelper_hashesAndVerifiesPasscodesSafely() {
    val digest = com.example.security.PasscodeSecurityHelper.createPasscodeDigest("Vizyon!2026")
    assertTrue(digest.hashHex.isNotBlank())
    assertTrue(digest.saltBase64.isNotBlank())
    assertTrue(
      com.example.security.PasscodeSecurityHelper.verifyPasscode(
        input = "Vizyon!2026",
        expectedHash = digest.hashHex,
        salt = digest.saltBase64
      )
    )
    assertFalse(
      com.example.security.PasscodeSecurityHelper.verifyPasscode(
        input = "WrongPass",
        expectedHash = digest.hashHex,
        salt = digest.saltBase64
      )
    )
    val (_, strongScore) = com.example.security.PasscodeSecurityHelper.passcodeStrengthLabel("Vizyon!2026")
    val (_, shortScore) = com.example.security.PasscodeSecurityHelper.passcodeStrengthLabel("123")
    assertTrue(strongScore > shortScore)
  }

  @Test
  fun turkishMusicCategories_areGroupedAndComplete() {
    val turkishGenres =
      com.example.model.MusicCategory.entries.filter {
        it.group == com.example.model.MusicCategoryGroup.TURKISH
      }
    assertTrue(turkishGenres.size >= 7)
    assertTrue(com.example.model.MusicCategory.ARABESK_FANTASY in turkishGenres)
    assertTrue(com.example.model.MusicCategory.ANATOLIAN_ROCK in turkishGenres)
    assertTrue(com.example.model.MusicCategory.TURKISH_FOLK in turkishGenres)
    assertTrue(com.example.model.MusicCategory.TURKISH_CLASSICAL in turkishGenres)
    assertEquals(
      com.example.model.buildDirectChatId("userB", "userA"),
      com.example.model.buildDirectChatId("userA", "userB")
    )
  }

  @Test
  fun instantSearchSuggestions_matchesTurkishNormalizedPrefix() {
    val repo = HybridMusicRepository()
    val diacriticMatch = repo.instantSearchSuggestions("müs")
    val asciiMatch = repo.instantSearchSuggestions("mus")
    assertTrue(diacriticMatch.any { it.text == "Müslüm Gürses" })
    assertEquals("Müslüm Gürses", diacriticMatch.first().text)
    assertTrue(asciiMatch.any { it.text == "Müslüm Gürses" })

    val sezenMatch = repo.instantSearchSuggestions("sezen")
    assertTrue(sezenMatch.any { it.text == "Sezen Aksu" })

    val globalMatch = repo.instantSearchSuggestions("the w")
    assertTrue(globalMatch.any { it.text == "The Weeknd" })
  }

  @Test
  fun youtubePlayerManager_stopAndClear_resetsTrackAndPlaybackState() {
    val manager = YouTubeForegroundPlayerManager()
    val track =
      MusicTrack(
        trackId = "t_dismiss",
        title = "Dismiss Test",
        artistId = "art_d",
        artistName = "Artist",
        youtubeVideoId = "dQw4w9WgXcQ"
      )
    manager.prepareLoading(track)
    manager.onIFrameReady("dQw4w9WgXcQ")
    manager.onIFrameStateChanged(1)
    assertTrue(manager.state.value.isPlaying)

    manager.stopAndClear()
    val cleared = manager.state.value
    org.junit.Assert.assertNull(cleared.currentTrack)
    assertFalse(cleared.isPlaying)
    assertEquals("", cleared.activeVideoId)
  }

  @Test
  fun studioSoundEngine_synthesizesAppleStyleFastSwooshPop() {
    val pcm = com.example.audio.StudioSoundEngine.synthesizeSoftSendPit()
    assertTrue(pcm.size in 3_600..4_200)
    val maxSample = pcm.maxOf { kotlin.math.abs(it.toInt()) }
    assertTrue(maxSample in 12_000..17_000)
    assertTrue(kotlin.math.abs(pcm.last().toInt()) < 150)
  }

  @Test
  fun studioSoundEngine_synthesizesAppleMessagesIncomingReceivePop() {
    val pcm = com.example.audio.StudioSoundEngine.synthesizeSoftWaterDropReceive()
    assertTrue(pcm.size in 4_800..5_500)
    val maxSample = pcm.maxOf { kotlin.math.abs(it.toInt()) }
    assertTrue(maxSample in 11_500..16_500)
    assertTrue(kotlin.math.abs(pcm.last().toInt()) < 150)
  }

  @Test
  fun studioSoundEngine_synthesizesSoftDoubleDropletNotification() {
    val pcm = com.example.audio.StudioSoundEngine.synthesizeSoftDoubleDropletNotification()
    assertTrue(pcm.size in 15_500..16_200)
    val maxSample = pcm.maxOf { kotlin.math.abs(it.toInt()) }
    assertTrue(maxSample in 12_500..17_500)
    assertTrue(kotlin.math.abs(pcm.last().toInt()) < 150)
  }

  @Test
  fun notificationSanitizer_stripsHandleAndShowsDisplayNameOnly() {
    val legacyNotif =
      com.example.model.UserNotification(
        id = "n_1",
        senderId = "u_99",
        senderName = "Merve Aydın (@merve)",
        senderHandle = "@merve",
        type = com.example.model.NotificationType.MESSAGE.name,
        message = "Merve Aydın (@merve): Bu şarkıyı mutlaka dinle!"
      )
    assertEquals("Merve Aydın", com.example.notifications.VizyonNotificationHelper.cleanDisplayNameOnly(legacyNotif.senderName))
    assertEquals("Bu şarkıyı mutlaka dinle!", com.example.notifications.VizyonNotificationHelper.sanitizeNotificationMessage(legacyNotif))
  }

  @Test
  fun youtubeCandidateMatcher_rejectsWrongSongBySameArtistAndPrefersTopicAudio() {
    val repo = HybridMusicRepository()
    val wrongSongSameArtist =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_wrong_song"),
        snippet = YouTubeSnippet(
          title = "Tarkan - Dudu (Official Video)",
          channelTitle = "Tarkan"
        )
      )
    val topicExactSong =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_topic_exact"),
        snippet = YouTubeSnippet(
          title = "Şımarık",
          channelTitle = "Tarkan - Topic",
          description = "Provided to YouTube by Universal Music Turkey"
        )
      )
    val details =
      mapOf(
        "vid_wrong_song" to com.example.data.remote.YouTubeVideoItem(
          id = "vid_wrong_song",
          snippet = wrongSongSameArtist.snippet,
          contentDetails = com.example.data.remote.YouTubeContentDetails(duration = "PT4M20S")
        ),
        "vid_topic_exact" to com.example.data.remote.YouTubeVideoItem(
          id = "vid_topic_exact",
          snippet = topicExactSong.snippet,
          contentDetails = com.example.data.remote.YouTubeContentDetails(duration = "PT3M54S")
        )
      )

    val picked =
      repo.pickBestYouTubeCandidate(
        items = listOf(wrongSongSameArtist, topicExactSong),
        targetTitle = "Şımarık (Remastered 2020)",
        targetArtist = "Tarkan",
        detailsById = details,
        targetDurationMs = 234_000L
      )
    assertEquals("vid_topic_exact", picked?.id?.videoId)
  }

  @Test
  fun youtubeCandidateMatcher_rejectsShortsConcertsAndPartialWordFalsePositives() {
    val repo = HybridMusicRepository()
    val shortClip =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_short"),
        snippet = YouTubeSnippet(title = "Sertab Erener - Aşk #shorts", channelTitle = "Shorts Clips")
      )
    val fullConcert =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_concert"),
        snippet = YouTubeSnippet(title = "Sertab Erener - Aşk (Full Konser)", channelTitle = "Konser TV")
      )
    val wrongTitleSubstring =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_substring"),
        snippet = YouTubeSnippet(title = "Sertab Erener - Aşk Ölmez", channelTitle = "Sertab Erener")
      )
    val exactStudio =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_exact_ask"),
        snippet = YouTubeSnippet(title = "Sertab Erener - Aşk (Official Video)", channelTitle = "SertabErenerVEVO")
      )
    val details =
      mapOf(
        "vid_short" to com.example.data.remote.YouTubeVideoItem(
          id = "vid_short",
          snippet = shortClip.snippet,
          contentDetails = com.example.data.remote.YouTubeContentDetails(duration = "PT25S")
        ),
        "vid_concert" to com.example.data.remote.YouTubeVideoItem(
          id = "vid_concert",
          snippet = fullConcert.snippet,
          contentDetails = com.example.data.remote.YouTubeContentDetails(duration = "PT52M10S")
        ),
        "vid_substring" to com.example.data.remote.YouTubeVideoItem(
          id = "vid_substring",
          snippet = wrongTitleSubstring.snippet,
          contentDetails = com.example.data.remote.YouTubeContentDetails(duration = "PT4M15S")
        ),
        "vid_exact_ask" to com.example.data.remote.YouTubeVideoItem(
          id = "vid_exact_ask",
          snippet = exactStudio.snippet,
          contentDetails = com.example.data.remote.YouTubeContentDetails(duration = "PT3M32S")
        )
      )

    val picked =
      repo.pickBestYouTubeCandidate(
        items = listOf(shortClip, fullConcert, wrongTitleSubstring, exactStudio),
        targetTitle = "Aşk",
        targetArtist = "Sertab Erener",
        detailsById = details,
        targetDurationMs = 212_000L
      )
    assertEquals("vid_exact_ask", picked?.id?.videoId)
  }

  @Test
  fun youtubeCandidateMatcher_decodesHtmlEntitiesAndHandlesCollaborations() {
    val repo = HybridMusicRepository()
    assertEquals("Pişman Değilim", repo.cleanSearchTerm("Pişman Değilim (Remastered 2024)"))
    assertEquals("Semicenk", repo.extractPrimaryArtist("Semicenk & Doğu Swag"))

    val escapedCandidate =
      YouTubeSearchItem(
        id = YouTubeSearchId(videoId = "vid_collab_ok"),
        snippet = YouTubeSnippet(
          title = "Semicenk &amp; Doğu Swag - Pişman Değilim (Official Video)",
          channelTitle = "Semicenk"
        )
      )
    val score =
      repo.scoreYouTubeCandidate(
        item = escapedCandidate,
        targetTitle = "Pişman Değilim",
        targetArtist = "Semicenk & Doğu Swag",
        targetDurationMs = 190_000L
      )
    assertTrue("Expected high confidence match score, got $score", score >= 120)
  }

  @Test
  fun notificationHelper_routesAllTypesToV6ChannelsAndSanitizesFallbacks() {
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_FOLLOWS,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.FOLLOW)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_FOLLOWS,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.FOLLOW_REQUEST)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_REACTIONS,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.LIKE)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_REACTIONS,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.COMMENT)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_REACTIONS,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.REPOST)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_STORIES,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.STORY)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_MESSAGES,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.MESSAGE)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_MESSAGES,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.VOICE_MESSAGE)
    )
    assertEquals(
      com.example.notifications.VizyonNotificationHelper.CHANNEL_MESSAGES,
      com.example.notifications.VizyonNotificationHelper.channelForType(com.example.model.NotificationType.MESSAGE_REQUEST)
    )

    assertTrue(com.example.notifications.VizyonNotificationHelper.notificationSystemId("a") >= 100)
    assertTrue(com.example.notifications.VizyonNotificationHelper.notificationSystemId("") >= 100)

    val voiceFallback =
      com.example.model.UserNotification(
        id = "n_voice",
        senderName = "Ali (@ali)",
        senderHandle = "@ali",
        type = com.example.model.NotificationType.VOICE_MESSAGE.name,
        message = ""
      )
    assertEquals(
      "Sana bir sesli mesaj gönderdi",
      com.example.notifications.VizyonNotificationHelper.sanitizeNotificationMessage(voiceFallback)
    )

    val followReqFallback =
      com.example.model.UserNotification(
        id = "n_req",
        senderName = "Zeynep",
        type = com.example.model.NotificationType.FOLLOW_REQUEST.name,
        message = "Zeynep: "
      )
    assertEquals(
      "Sana takip isteği gönderdi",
      com.example.notifications.VizyonNotificationHelper.sanitizeNotificationMessage(followReqFallback)
    )
  }

  @Test
  fun userNotification_defaultsAndModelSanitizersAreConsistent() {
    val defaultNotif = com.example.model.UserNotification()
    assertFalse(defaultNotif.read)
    assertFalse(defaultNotif.sent)

    com.example.model.NotificationType.entries.forEach { entry ->
      assertEquals(entry, com.example.model.NotificationType.fromRaw(entry.name.lowercase()))
    }
    assertEquals(
      com.example.model.ChatPrivacy.FOLLOWERS_ONLY,
      com.example.model.ChatPrivacy.fromRaw("followers_only")
    )
    assertEquals(
      com.example.model.ChatPrivacy.PUBLIC,
      com.example.model.ChatPrivacy.fromRaw("invalid_mode")
    )
    assertEquals(
      com.example.model.ReportReason.SPAM,
      com.example.model.ReportReason.fromRaw("spam")
    )
    assertEquals(
      com.example.model.ReportReason.OTHER,
      com.example.model.ReportReason.fromRaw("unknown")
    )

    val longSanitized = com.example.model.sanitizeFirestoreId("user/with spaces & chars!" + "a".repeat(200))
    assertTrue(longSanitized.length <= 120)
    assertFalse(longSanitized.contains("/"))
    assertFalse(longSanitized.contains(" "))
    assertTrue(com.example.model.sanitizeFirestoreId("___").startsWith("item_"))
  }

  @Test
  fun youtubePlayerManager_repeatCyclingQueueDeduplicationAndFailureRecovery() {
    val manager = YouTubeForegroundPlayerManager()
    val trackA =
      MusicTrack(
        trackId = "t_a",
        title = "A",
        artistId = "art_a",
        artistName = "Artist A",
        youtubeVideoId = "vidA1234567"
      )
    val trackB =
      MusicTrack(
        trackId = "t_b",
        title = "B",
        artistId = "art_b",
        artistName = "Artist B",
        youtubeVideoId = "vidB1234567"
      )

    assertEquals(com.example.player.RepeatMode.NONE, manager.state.value.repeatMode)
    manager.cycleRepeatMode()
    assertEquals(com.example.player.RepeatMode.ALL, manager.state.value.repeatMode)
    manager.cycleRepeatMode()
    assertEquals(com.example.player.RepeatMode.ONE, manager.state.value.repeatMode)
    manager.cycleRepeatMode()
    assertEquals(com.example.player.RepeatMode.NONE, manager.state.value.repeatMode)

    manager.prepareLoading(trackA, listOf(trackA))
    manager.addToQueue(trackA)
    manager.addToQueue(trackB)
    assertEquals(2, manager.state.value.queue.size)

    manager.onVideoResolutionFailed("Video çözümlenemedi")
    assertFalse(manager.state.value.isResolvingVideo)
    assertFalse(manager.state.value.isPlaying)
    assertEquals("Video çözümlenemedi", manager.state.value.playbackMessage)
  }
}
