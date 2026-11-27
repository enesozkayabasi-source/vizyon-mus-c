package com.example.data.remote

import android.util.Log
import com.example.model.ApiBridgeStatus
import com.example.model.AppLanguage
import com.example.model.ArtistSummary
import com.example.model.MusicCategory
import com.example.model.MusicCategoryGroup
import com.example.model.MusicTrack
import com.example.model.TrackLyrics
import com.example.model.TrackSource
import com.example.model.sanitizeFirestoreId
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.log10

object KidsContentFilter {
  private val blockedGenreTokens =
    setOf(
      "children's music",
      "childrens music",
      "children",
      "kids",
      "lullabies",
      "lullaby",
      "sing-along",
      "cocuk",
      "kids & family"
    )

  private val blockedKeywords =
    listOf(
      "cocomelon",
      "niloya",
      "pepee",
      "kral sakir",
      "kukuli",
      "baby shark",
      "pinkfong",
      "super simple songs",
      "sevimli dostlar",
      "smart kids",
      "little baby bum",
      "dave and ava",
      "chu chu tv",
      "chuchutv",
      "trt cocuk",
      "minika",
      "disney junior",
      "okul oncesi",
      "cocuk sarkilari",
      "cocuk sarkisi",
      "bebek sarkilari",
      "bebek ninnisi",
      "uyku ninnisi",
      "masal saati",
      "tekerleme",
      "nursery rhyme",
      "nursery rhymes",
      "toddler",
      "kids song",
      "children's song",
      "childrens song",
      "lullaby for babies",
      "dandini dandini",
      "mini mini bir kus",
      "ali babanin ciftligi",
      "kirmizi balik",
      "arkadasim esek cocuk"
    )

  fun normalize(raw: String): String {
    val trMapped =
      raw
        .lowercase(Locale.ROOT)
        .replace('ı', 'i')
        .replace('İ', 'i')
        .replace('ş', 's')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ö', 'o')
        .replace('ç', 'c')
    val decomposed = Normalizer.normalize(trMapped, Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "").trim()
  }

  fun isChildContent(
    title: String,
    artistName: String = "",
    albumName: String = "",
    genre: String = ""
  ): Boolean {
    val normGenre = normalize(genre)
    if (blockedGenreTokens.any { normGenre.contains(it) }) return true

    val combined = normalize("$title $artistName $albumName $genre")
    if (blockedKeywords.any { combined.contains(it) }) return true

    val tokens = combined.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }.toSet()
    val standaloneKidsWords = setOf("ninni", "ninniler", "ninnisi", "tekerleme", "tekerlemeler", "masal")
    if (tokens.any { it in standaloneKidsWords }) return true
    if ("cocuk" in tokens && ("sarki" in tokens || "sarkilari" in tokens || "egitim" in tokens || "dans" in tokens)) {
      return true
    }
    return false
  }
}

sealed interface VideoResolutionResult {
  data class Success(val track: MusicTrack) : VideoResolutionResult
  data class MissingApiKey(val track: MusicTrack) : VideoResolutionResult
  data class NotFound(val track: MusicTrack, val reason: String) : VideoResolutionResult
}

class HybridMusicRepository {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  val okHttpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .followRedirects(true)
      .addInterceptor(
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
      )
      .build()

  private val itunesApi: ItunesSearchApiService =
    Retrofit.Builder()
      .baseUrl("https://itunes.apple.com/")
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(ItunesSearchApiService::class.java)

  val youtubeApi: YouTubeMusicApiService = createYouTubeMusicApiService(okHttpClient, moshi)
  private val lrcLibApi: LrcLibApiService = createLrcLibApiService(okHttpClient, moshi)

  private data class CachedVideoMatch(
    val videoId: String,
    val durationMs: Long,
    val cachedAtMs: Long
  )

  private val videoMatchCache = ConcurrentHashMap<String, CachedVideoMatch>()
  private val lyricsCache = ConcurrentHashMap<String, TrackLyrics>()

  fun isYouTubeConfigured(): Boolean = resolveYouTubeApiKey().isNotBlank()

  suspend fun loadHomeCatalog(
    preferredGenres: List<String> = emptyList(),
    preferredLanguage: AppLanguage = AppLanguage.TURKISH,
    showcaseSeed: Long = System.currentTimeMillis()
  ): Triple<List<MusicTrack>, List<ArtistSummary>, ApiBridgeStatus> = coroutineScope {
    val categoryTerms = buildCatalogQueries(preferredGenres, preferredLanguage, showcaseSeed)
    val countryCode = preferredLanguage.itunesCountry

    val deferredBatches =
      categoryTerms.map { (categoryLabel, query) ->
        async {
          runCatching {
            itunesApi.searchSongs(term = query, country = countryCode, limit = 10).results.mapNotNull {
              it.toMusicTrack(categoryLabel)
            }
          }.getOrElse {
            Log.w(TAG, "iTunes catalog query failed for '$query': ${it.message}")
            emptyList()
          }
        }
      }

    val seededRandom = kotlin.random.Random(showcaseSeed)
    val rawTracks =
      deferredBatches
        .flatMap { it.await() }
        .distinctBy { it.trackId }

    val preferredSet = preferredGenres.toSet()
    val preferredPool =
      if (preferredSet.isEmpty()) rawTracks.shuffled(seededRandom)
      else rawTracks.filter { it.categoryTag in preferredSet }.shuffled(seededRandom)
    val otherPool =
      if (preferredSet.isEmpty()) emptyList()
      else rawTracks.filterNot { it.categoryTag in preferredSet }.shuffled(seededRandom)
    val allTracks = (preferredPool + otherPool).distinctBy { it.trackId }

    val artists =
      allTracks
        .distinctBy { it.artistName.lowercase(Locale.ROOT) }
        .take(20)
        .map { tr ->
          ArtistSummary(
            artistId = tr.artistId,
            name = tr.artistName,
            imageUrl = tr.artworkUrl,
            followersCount = 125_000L,
            genres = listOf(tr.categoryTag, "iTunes Katalog"),
            source = TrackSource.ITUNES
          )
        }

    val ytConfigured = isYouTubeConfigured()
    val status =
      ApiBridgeStatus(
        itunesConnected = allTracks.isNotEmpty(),
        youtubeApiConfigured = ytConfigured,
        youtubeConnected = ytConfigured,
        lyricsReady = true,
        activeMessage =
          if (ytConfigured) {
            "iTunes Search API & Resmi YouTube IFrame eşleştirmesi hazır"
          } else {
            "iTunes Katalog aktif • Resmi video eşleştirme için YOUTUBE_API_KEY tanımlayın"
          }
      )

    Triple(allTracks, artists, status)
  }

  fun instantSearchSuggestions(prefix: String): List<com.example.model.SearchAutocompleteSuggestion> {
    val normQuery = KidsContentFilter.normalize(prefix)
    if (normQuery.length < 2) return emptyList()
    val startsWithMatches = mutableListOf<com.example.model.SearchAutocompleteSuggestion>()
    val wordOrContainsMatches = mutableListOf<com.example.model.SearchAutocompleteSuggestion>()

    curatedSuggestionCatalog.forEach { (display, meta) ->
      val normDisplay = KidsContentFilter.normalize(display)
      val normMeta = KidsContentFilter.normalize(meta)
      when {
        normDisplay.startsWith(normQuery) ->
          startsWithMatches += com.example.model.SearchAutocompleteSuggestion(display, "$meta • Anlık Tahmin", isLiveApi = false)
        normDisplay.split(' ').any { it.startsWith(normQuery) } || normDisplay.contains(normQuery) || normMeta.contains(normQuery) ->
          wordOrContainsMatches += com.example.model.SearchAutocompleteSuggestion(display, "$meta • Anlık Tahmin", isLiveApi = false)
      }
    }
    return (startsWithMatches + wordOrContainsMatches)
      .distinctBy { it.text.lowercase(Locale.ROOT) }
      .take(6)
  }

  suspend fun fetchLiveSearchSuggestions(
    query: String,
    country: String = "TR"
  ): List<com.example.model.SearchAutocompleteSuggestion> = coroutineScope {
    val cleanQuery = query.trim()
    if (cleanQuery.length < 2 || KidsContentFilter.isChildContent(title = cleanQuery)) {
      return@coroutineScope emptyList()
    }
    val artistsDef = async {
      runCatching {
        itunesApi.searchArtists(term = cleanQuery, country = country, limit = 4).results.mapNotNull { dto ->
          val name = dto.artistName.trim()
          if (name.isBlank() || KidsContentFilter.isChildContent("", artistName = name, genre = dto.primaryGenreName)) {
            null
          } else {
            com.example.model.SearchAutocompleteSuggestion(
              text = name,
              subtitle = "${dto.primaryGenreName.ifBlank { "Sanatçı" }} • Canlı Öneri",
              isLiveApi = true
            )
          }
        }
      }.getOrDefault(emptyList())
    }
    val songsDef = async {
      runCatching {
        itunesApi.searchSongs(term = cleanQuery, country = country, limit = 5).results.mapNotNull { dto ->
          val title = dto.trackName.trim()
          val artist = dto.artistName.trim()
          if (title.isBlank() || artist.isBlank() || KidsContentFilter.isChildContent(title, artist, dto.collectionName, dto.primaryGenreName)) {
            null
          } else {
            com.example.model.SearchAutocompleteSuggestion(
              text = "$title $artist",
              subtitle = "$artist • Canlı Parça",
              isLiveApi = true
            )
          }
        }
      }.getOrDefault(emptyList())
    }
    (artistsDef.await() + songsDef.await())
      .distinctBy { it.text.lowercase(Locale.ROOT) }
      .take(5)
  }

  suspend fun searchUnified(
    query: String,
    country: String = "TR"
  ): Pair<List<MusicTrack>, List<ArtistSummary>> =
    coroutineScope {
      val cleanQuery = query.trim()
      if (cleanQuery.isBlank() || KidsContentFilter.isChildContent(title = cleanQuery)) {
        return@coroutineScope Pair(emptyList(), emptyList())
      }

      val inferredCategory = inferCategoryFromQuery(cleanQuery)

      val songsDef = async {
        runCatching {
          itunesApi.searchSongs(term = cleanQuery, country = country, limit = 30).results.mapNotNull {
            it.toMusicTrack(inferredCategory)
          }
        }.getOrElse {
          Log.w(TAG, "iTunes song search failed: ${it.message}")
          emptyList()
        }
      }

      val artistsDef = async {
        runCatching {
          itunesApi.searchArtists(term = cleanQuery, country = country, limit = 10).results.mapNotNull { dto ->
            val name = dto.artistName.trim()
            if (name.isBlank() || KidsContentFilter.isChildContent(title = "", artistName = name, genre = dto.primaryGenreName)) {
              null
            } else {
              ArtistSummary(
                artistId = sanitizeFirestoreId("itart_${dto.artistId.takeIf { it > 0L } ?: name.hashCode()}"),
                name = name,
                imageUrl = dto.highResArtworkUrl(),
                followersCount = 92_000L,
                genres = listOfNotNull(dto.primaryGenreName.takeIf { it.isNotBlank() }, "Sanatçı"),
                source = TrackSource.ITUNES
              )
            }
          }
        }.getOrDefault(emptyList())
      }

      val tracks = songsDef.await().distinctBy { it.trackId }
      val foundArtists = artistsDef.await()

      val derivedArtists =
        tracks
          .distinctBy { it.artistName.lowercase(Locale.ROOT) }
          .take(8)
          .map { tr ->
            ArtistSummary(
              artistId = tr.artistId,
              name = tr.artistName,
              imageUrl = tr.artworkUrl,
              followersCount = 85_000L,
              genres = listOf(tr.categoryTag),
              source = TrackSource.ITUNES
            )
          }

      val mergedArtists =
        (foundArtists.map { art ->
          val fallbackImg =
            art.imageUrl.ifBlank {
              tracks.firstOrNull { it.artistName.equals(art.name, ignoreCase = true) }?.artworkUrl.orEmpty()
            }
          art.copy(imageUrl = fallbackImg)
        } + derivedArtists)
          .distinctBy { it.name.lowercase(Locale.ROOT) }
          .distinctBy { it.artistId }

      Pair(tracks, mergedArtists)
    }

  suspend fun fetchArtistDetail(
    artistId: String,
    fallbackName: String,
    fallbackImage: String
  ): Pair<ArtistSummary, List<MusicTrack>> {
    val cleanNumericId = artistId.removePrefix("itart_").toLongOrNull()
    val lookupTracks =
      if (cleanNumericId != null && cleanNumericId > 0L) {
        runCatching {
          itunesApi.lookupArtistTopSongs(artistId = cleanNumericId, country = "TR", limit = 20)
            .results
            .filter { it.wrapperType == "track" || it.trackName.isNotBlank() }
            .mapNotNull { it.toMusicTrack(MusicCategory.TRENDING.label) }
        }.getOrDefault(emptyList())
      } else emptyList()

    val tracks =
      lookupTracks.ifEmpty {
        searchUnified(fallbackName).first
      }

    val artist =
      ArtistSummary(
        artistId = artistId,
        name = fallbackName.ifBlank { tracks.firstOrNull()?.artistName ?: "Sanatçı" },
        imageUrl = fallbackImage.ifBlank { tracks.firstOrNull()?.artworkUrl.orEmpty() },
        followersCount = 140_000L,
        genres = tracks.map { it.categoryTag }.distinct().take(3).ifEmpty { listOf("Müzik") },
        source = TrackSource.ITUNES,
        bio = "$fallbackName — iTunes Katalog & Resmi YouTube video eşleşmeli sanatçı profili."
      )
    return Pair(artist, tracks)
  }

  suspend fun resolveYouTubeVideoForTrack(
    track: MusicTrack,
    forceRefresh: Boolean = false,
    excludedVideoIds: Set<String> = emptySet()
  ): VideoResolutionResult {
    val nowMs = System.currentTimeMillis()
    val effectiveExcluded = excludedVideoIds.toMutableSet()

    if (forceRefresh) {
      videoMatchCache.remove(track.trackId)
    } else if (track.youtubeVideoId.isNotBlank() && track.youtubeVideoId !in effectiveExcluded) {
      val cached = videoMatchCache[track.trackId]
      if (cached != null && cached.videoId == track.youtubeVideoId && nowMs - cached.cachedAtMs < VIDEO_CACHE_TTL_MS) {
        return VideoResolutionResult.Success(track)
      }
      if (!isYouTubeConfigured()) {
        return VideoResolutionResult.Success(track)
      }
      // Lightweight 1-unit verification: ensure pre-saved ID is still embeddable and matches the track title/artist
      val verificationResp =
        runCatching { youtubeApi.getVideoDetails(videoIds = track.youtubeVideoId) }.getOrNull()
      if (verificationResp == null) {
        // Network/API transiently unavailable; preserve pre-attached videoId instead of blocking playback
        return VideoResolutionResult.Success(track)
      }
      val existingDetail = verificationResp.items.firstOrNull { it.id == track.youtubeVideoId }
      if (existingDetail != null && existingDetail.status?.embeddable != false) {
        val asSearchItem =
          YouTubeSearchItem(
            id = YouTubeSearchId(kind = "youtube#video", videoId = existingDetail.id),
            snippet = existingDetail.snippet
          )
        val existingScore =
          scoreYouTubeCandidate(
            item = asSearchItem,
            targetTitle = cleanSearchTerm(track.title),
            targetArtist = cleanSearchTerm(track.artistName),
            detail = existingDetail,
            targetDurationMs = track.durationMs
          )
        if (existingScore >= MINIMUM_CONFIDENCE_MATCH_SCORE && isPlausibleVideoDuration(existingDetail, track.durationMs)) {
          val durMs =
            parseIsoDurationMsOrNull(existingDetail.contentDetails?.duration)
              ?: track.durationMs.takeIf { it > 0L }
              ?: 210_000L
          videoMatchCache[track.trackId] = CachedVideoMatch(existingDetail.id, durMs, nowMs)
          return VideoResolutionResult.Success(track.copy(durationMs = durMs))
        }
      }
      // Pre-attached video was deleted, unembeddable, or mismatched; exclude it and re-resolve cleanly
      effectiveExcluded.add(track.youtubeVideoId)
    }

    if (!forceRefresh) {
      videoMatchCache[track.trackId]?.let { cached ->
        if (
          nowMs - cached.cachedAtMs < VIDEO_CACHE_TTL_MS &&
            cached.videoId.isNotBlank() &&
            cached.videoId !in effectiveExcluded
        ) {
          return VideoResolutionResult.Success(
            track.copy(
              youtubeVideoId = cached.videoId,
              durationMs = cached.durationMs.takeIf { it > 0L } ?: track.durationMs
            )
          )
        }
      }
    }

    if (!isYouTubeConfigured()) {
      return VideoResolutionResult.MissingApiKey(track)
    }

    val cleanedTitle = cleanSearchTerm(track.title)
    val cleanedArtist = cleanSearchTerm(track.artistName)
    val primaryArtist = extractPrimaryArtist(cleanedArtist)
    val targetDurMs = track.durationMs

    val seenCandidateIds = effectiveExcluded.toMutableSet()
    val candidatePool = mutableListOf<YouTubeSearchItem>()
    val detailMap = mutableMapOf<String, YouTubeVideoItem>()

    suspend fun queryAndInspect(query: String, categoryId: String?, limit: Int) {
      val cleanQ = query.trim()
      if (cleanQ.isBlank()) return
      val resp =
        youtubeApi.searchMusicCategory(
          query = cleanQ,
          type = "video",
          videoCategoryId = categoryId,
          videoEmbeddable = "true",
          maxResults = limit
        )
      val fresh =
        filterSafeYouTubeCandidates(resp.items, seenCandidateIds)
          .onEach { seenCandidateIds.add(it.id.videoId) }
      if (fresh.isEmpty()) return
      candidatePool.addAll(fresh)

      val topToInspect =
        fresh
          .sortedByDescending { scoreYouTubeCandidate(it, cleanedTitle, cleanedArtist) }
          .take(8)
          .map { it.id.videoId }
          .filter { it !in detailMap }
      if (topToInspect.isNotEmpty()) {
        runCatching { youtubeApi.getVideoDetails(videoIds = topToInspect.joinToString(",")).items }
          .getOrDefault(emptyList())
          .forEach { item -> detailMap[item.id] = item }
      }
    }

    fun bestRankedEmbeddable(): Pair<YouTubeSearchItem, Int>? =
      candidatePool
        .asSequence()
        .filter { cand ->
          val vid = cand.id.videoId
          val detail = detailMap[vid]
          vid.isNotBlank() &&
            vid !in effectiveExcluded &&
            (detail == null || detail.status?.embeddable != false) &&
            isPlausibleVideoDuration(detail, targetDurMs)
        }
        .map { cand ->
          cand to
            scoreYouTubeCandidate(
              item = cand,
              targetTitle = cleanedTitle,
              targetArtist = cleanedArtist,
              detail = detailMap[cand.id.videoId],
              targetDurationMs = targetDurMs
            )
        }
        .maxByOrNull { it.second }

    return try {
      // Stage 1: Exact "Artist - Title" in YouTube Music category (captures Topic + Official Audio + Clips)
      queryAndInspect(
        query = listOf(cleanedArtist, cleanedTitle).filter { it.isNotBlank() }.joinToString(" - "),
        categoryId = "10",
        limit = 12
      )

      val afterStage1 = bestRankedEmbeddable()
      // Stage 2: Broaden across categories if no high-confidence match yet (some labels upload under Entertainment)
      if (afterStage1 == null || afterStage1.second < HIGH_CONFIDENCE_MATCH_SCORE) {
        queryAndInspect(
          query = "$cleanedArtist $cleanedTitle".trim(),
          categoryId = null,
          limit = 10
        )
      }

      val afterStage2 = bestRankedEmbeddable()
      // Stage 3: Fallback using primary solo artist when collaboration formatting obscures results
      if ((afterStage2 == null || afterStage2.second < MINIMUM_CONFIDENCE_MATCH_SCORE + 20) &&
        primaryArtist.isNotBlank() &&
        !primaryArtist.equals(cleanedArtist, ignoreCase = true)
      ) {
        queryAndInspect(
          query = "$primaryArtist - $cleanedTitle".trim(),
          categoryId = null,
          limit = 8
        )
      }

      if (candidatePool.isEmpty()) {
        return VideoResolutionResult.NotFound(
          track,
          "\"${track.title}\" için yerleştirilebilir resmi YouTube videosu bulunamadı."
        )
      }

      val bestMatch = bestRankedEmbeddable()
      if (bestMatch == null || bestMatch.second < MINIMUM_CONFIDENCE_MATCH_SCORE) {
        return VideoResolutionResult.NotFound(
          track,
          "\"${track.title}\" için şarkı adı ve sanatçıyla eşleşen doğrulanabilir bir video bulunamadı."
        )
      }

      val bestVideoId = bestMatch.first.id.videoId
      val detailDurMs =
        parseIsoDurationMsOrNull(detailMap[bestVideoId]?.contentDetails?.duration)
          ?: track.durationMs.takeIf { it > 0L }
          ?: 210_000L

      videoMatchCache[track.trackId] = CachedVideoMatch(bestVideoId, detailDurMs, nowMs)
      VideoResolutionResult.Success(
        track.copy(
          youtubeVideoId = bestVideoId,
          durationMs = detailDurMs
        )
      )
    } catch (e: HttpException) {
      val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
      Log.w(TAG, "YouTube Data API HTTP ${e.code()}: ${e.message()} $errorBody")
      val msg =
        when (e.code()) {
          403 ->
            if (errorBody.contains("quotaExceeded", ignoreCase = true)) {
              "YouTube Data API v3 günlük kotası doldu (HTTP 403)."
            } else {
              "YouTube API erişimi reddedildi veya anahtar kısıtlı (HTTP 403)."
            }
          400 -> "YouTube API anahtarı veya arama isteği geçersiz (HTTP 400)."
          404 -> "YouTube API uç noktası bulunamadı (HTTP 404)."
          else -> "YouTube video araması başarısız (HTTP ${e.code()})."
        }
      VideoResolutionResult.NotFound(track, msg)
    } catch (e: Exception) {
      Log.w(TAG, "YouTube video lookup error: ${e.message}")
      VideoResolutionResult.NotFound(
        track,
        "Ağ hatası nedeniyle video eşleştirilemedi: ${e.localizedMessage ?: "Bağlantı sorunu"}"
      )
    }
  }

  private fun isPlausibleVideoDuration(
    detail: YouTubeVideoItem?,
    targetDurationMs: Long
  ): Boolean {
    val videoDurMs = parseIsoDurationMsOrNull(detail?.contentDetails?.duration) ?: return true
    if (targetDurationMs <= 30_000L) return true
    if (videoDurMs < 45_000L && targetDurationMs > 60_000L) return false
    val maxAllowedMs = maxOf((targetDurationMs * 2.5).toLong(), 14L * 60_000L)
    if (videoDurMs > maxAllowedMs) return false
    return true
  }

  private fun filterSafeYouTubeCandidates(
    items: List<YouTubeSearchItem>,
    excludedVideoIds: Set<String>
  ): List<YouTubeSearchItem> =
    items.filter { item ->
      val vid = item.id.videoId
      val decodedTitle = decodeHtmlEntities(item.snippet.title)
      val decodedChannel = decodeHtmlEntities(item.snippet.channelTitle)
      val decodedDesc = decodeHtmlEntities(item.snippet.description)
      vid.isNotBlank() &&
        vid !in excludedVideoIds &&
        !KidsContentFilter.isChildContent(
          title = decodedTitle,
          artistName = decodedChannel,
          albumName = decodedDesc
        )
    }

  suspend fun fetchLyrics(track: MusicTrack): TrackLyrics? {
    lyricsCache[track.trackId]?.let { return it }
    val cleanTitle = cleanSearchTerm(track.title)
    val cleanArtist = cleanSearchTerm(track.artistName)

    val exact =
      runCatching {
        lrcLibApi.getLyrics(trackName = cleanTitle, artistName = cleanArtist)
      }.getOrNull()

    if (exact != null && (!exact.syncedLyrics.isNullOrBlank() || !exact.plainLyrics.isNullOrBlank() || exact.instrumental)) {
      return exact.toTrackLyrics(track.trackId).also { lyricsCache[track.trackId] = it }
    }

    val searchQueries =
      listOf("$cleanArtist $cleanTitle".trim(), cleanTitle)
        .filter { it.isNotBlank() }
        .distinct()

    for (q in searchQueries) {
      val candidates = runCatching { lrcLibApi.searchLyrics(query = q) }.getOrDefault(emptyList())
      val best =
        candidates.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
          ?: candidates.firstOrNull { !it.plainLyrics.isNullOrBlank() }
          ?: candidates.firstOrNull { it.instrumental }
      if (best != null) {
        return best.toTrackLyrics(track.trackId).also { lyricsCache[track.trackId] = it }
      }
    }
    return null
  }

  private fun ItunesItemDto.toMusicTrack(defaultCategory: String): MusicTrack? {
    val cleanTitle = trackName.trim()
    val cleanArtist = artistName.trim()
    if (trackId <= 0L || cleanTitle.isBlank() || cleanArtist.isBlank()) return null
    if (KidsContentFilter.isChildContent(cleanTitle, cleanArtist, collectionName, primaryGenreName)) {
      return null
    }
    val normTrackMeta = KidsContentFilter.normalize("$cleanTitle $cleanArtist $collectionName")
    if (
      normTrackMeta.containsAny(
        "made famous by",
        "tribute to",
        "karaoke box",
        "prosound karaoke",
        "ringtone",
        "8-bit arcade"
      )
    ) {
      return null
    }
    val mappedCategory =
      classifyTrackCategory(
        rawGenre = primaryGenreName,
        title = cleanTitle,
        artistName = cleanArtist,
        fallback = defaultCategory
      )
    return MusicTrack(
      trackId = sanitizeFirestoreId("it_$trackId"),
      title = cleanTitle,
      artistId = sanitizeFirestoreId("itart_${artistId.takeIf { it > 0L } ?: cleanArtist.hashCode()}"),
      artistName = cleanArtist,
      albumName = collectionName.ifBlank { "Tekli" },
      artworkUrl = highResArtworkUrl(),
      youtubeVideoId = "",
      durationMs = trackTimeMillis.takeIf { it > 0L } ?: 200_000L,
      source = TrackSource.ITUNES,
      viewCountLabel = primaryGenreName,
      categoryTag = mappedCategory
    )
  }

  private fun buildCatalogQueries(
    preferredGenres: List<String>,
    preferredLanguage: AppLanguage,
    showcaseSeed: Long = 0L
  ): List<Pair<String, String>> {
    val useGlobalQuery = preferredLanguage == AppLanguage.ENGLISH
    val allCategories = MusicCategory.entries
    val allPairs =
      allCategories.map { cat ->
        cat.label to (if (useGlobalQuery) cat.catalogQueryGlobal else cat.catalogQueryTr)
      }
    val ordered =
      if (preferredGenres.isEmpty()) {
        when (preferredLanguage) {
          AppLanguage.ENGLISH ->
            allCategories
              .filter { it.group != MusicCategoryGroup.TURKISH || it == MusicCategory.TURKISH_POP || it == MusicCategory.ANATOLIAN_ROCK }
              .map { it.label to it.catalogQueryGlobal }
          else -> allPairs
        }
      } else {
        val preferredSet = preferredGenres.toSet()
        val prioritized = allPairs.filter { it.first in preferredSet }
        val rest = allPairs.filterNot { it.first in preferredSet }
        (prioritized + rest).take(15)
      }
    return if (showcaseSeed != 0L && ordered.size > 2) {
      val rng = kotlin.random.Random(showcaseSeed)
      val headCount = ordered.size.coerceAtMost(4)
      ordered.take(headCount).shuffled(rng) + ordered.drop(headCount)
    } else {
      ordered
    }
  }

  internal fun classifyTrackCategory(
    rawGenre: String,
    title: String = "",
    artistName: String = "",
    fallback: String = MusicCategory.TRENDING.label
  ): String {
    val g = KidsContentFilter.normalize(rawGenre)
    val meta = KidsContentFilter.normalize("$title $artistName $rawGenre")
    val fallbackIsTurkish =
      MusicCategory.entries.any { it.label == fallback && it.group == MusicCategoryGroup.TURKISH }

    if (fallbackIsTurkish) {
      return when {
        g.contains("hip-hop") || g.contains("rap") -> MusicCategory.TURKISH_RAP.label
        meta.containsAny("arabesk", "fantezi", "muslum", "tatlises", "gencebay", "ferdi tayfur", "ebru gundes", "bergen") ->
          MusicCategory.ARABESK_FANTASY.label
        meta.containsAny("turku", "baglama", "halk", "neset", "selda bagcan", "musa eroglu", "arif sag", "asik veysel") ->
          MusicCategory.TURKISH_FOLK.label
        meta.containsAny("sanat muzigi", "zeki muren", "muzeyyen senar", "bulent ersoy", "fasil", "emelsayin") ->
          MusicCategory.TURKISH_CLASSICAL.label
        meta.containsAny("anadolu rock", "baris manco", "cem karaca", "erkIn koray", "mogollar", "duman", "mor ve otesi", "sebnem ferah", "teoman") ->
          MusicCategory.ANATOLIAN_ROCK.label
        meta.containsAny("alternatif", "madrigal", "yuzyuzeyken", "dolu kadehi", "adamlar", "kalben", "buyuk ev ablukada") ->
          MusicCategory.TURKISH_ALTERNATIVE.label
        else -> fallback
      }
    }

    return when {
      meta.containsAny("arabesk", "fantezi", "muslum gurses", "ibrahim tatlises", "orhan gencebay", "ferdi tayfur") ->
        MusicCategory.ARABESK_FANTASY.label
      meta.containsAny("turk halk", "turku", "baglama", "neset ertas", "selda bagcan", "musa eroglu") ->
        MusicCategory.TURKISH_FOLK.label
      meta.containsAny("turk sanat", "zeki muren", "muzeyyen senar", "bulent ersoy", "fasil") ->
        MusicCategory.TURKISH_CLASSICAL.label
      meta.containsAny("anadolu rock", "baris manco", "cem karaca", "mor ve otesi", "duman") ->
        MusicCategory.ANATOLIAN_ROCK.label
      meta.containsAny("ezhel", "ceza", "sagopa", "turkce rap", "semicenk", "motive", "lvbel") ->
        MusicCategory.TURKISH_RAP.label
      meta.containsAny("madrigal", "yuzyuzeyken", "dolu kadehi", "adamlar", "kalben", "turkce alternatif") ->
        MusicCategory.TURKISH_ALTERNATIVE.label
      g.contains("turk") || meta.containsAny("tarkan", "sezen aksu", "mabel matiz", "gulsen", "kenan dogulu", "yalIn") ->
        MusicCategory.TURKISH_POP.label
      g.contains("electronic") || g.contains("dance") || g.contains("house") || g.contains("techno") ->
        MusicCategory.ELECTRONIC.label
      g.contains("jazz") || g.contains("lofi") || g.contains("lo-fi") ->
        MusicCategory.JAZZ_LOFI.label
      g.contains("r&b") || g.contains("soul") ->
        MusicCategory.RNB_SOUL.label
      g.contains("hip-hop") || g.contains("rap") -> MusicCategory.HIPHOP.label
      g.contains("rock") || g.contains("alternative") || g.contains("indie") ->
        MusicCategory.ROCK.label
      g.contains("acoustic") || g.contains("singer") || g.contains("folk") ->
        MusicCategory.ACOUSTIC.label
      g.contains("pop") -> MusicCategory.POP.label
      else -> fallback
    }
  }

  private fun inferCategoryFromQuery(query: String): String {
    val q = KidsContentFilter.normalize(query)
    return when {
      q.containsAny("arabesk", "fantezi", "muslum", "tatlises", "gencebay") ->
        MusicCategory.ARABESK_FANTASY.label
      q.containsAny("turku", "halk muzigi", "baglama", "neset", "selda") ->
        MusicCategory.TURKISH_FOLK.label
      q.containsAny("sanat muzigi", "fasil", "zeki muren", "muzeyyen") ->
        MusicCategory.TURKISH_CLASSICAL.label
      q.containsAny("anadolu rock", "baris manco", "cem karaca", "duman", "mor ve otesi") ->
        MusicCategory.ANATOLIAN_ROCK.label
      q.containsAny("turkce rap", "ezhel", "ceza", "sagopa", "trap") ->
        MusicCategory.TURKISH_RAP.label
      q.containsAny("turkce alternatif", "madrigal", "yuzyuzeyken", "dolu kadehi", "kalben") ->
        MusicCategory.TURKISH_ALTERNATIVE.label
      q.contains("turk") -> MusicCategory.TURKISH_POP.label
      q.containsAny("jazz", "caz", "lofi", "lo-fi") -> MusicCategory.JAZZ_LOFI.label
      q.containsAny("r&b", "rnb", "soul") -> MusicCategory.RNB_SOUL.label
      q.containsAny("synth", "elektr", "dance", "house") ->
        MusicCategory.ELECTRONIC.label
      q.containsAny("rap", "hip") -> MusicCategory.HIPHOP.label
      q.containsAny("rock", "alternatif") -> MusicCategory.ROCK.label
      q.containsAny("akust", "acoust", "chill") ->
        MusicCategory.ACOUSTIC.label
      q.contains("pop") -> MusicCategory.POP.label
      else -> MusicCategory.TRENDING.label
    }
  }

  private fun String.containsAny(vararg needles: String): Boolean =
    needles.any { this.contains(it) }

  internal fun cleanSearchTerm(raw: String): String =
    decodeHtmlEntities(raw)
      .replace(
        Regex(
          "\\((official|feat\\.|ft\\.|with |remaster|deluxe|bonus|original|from |single|ep|version|radio edit|mono|stereo).*?\\)",
          RegexOption.IGNORE_CASE
        ),
        ""
      )
      .replace(Regex("\\[.*?]"), "")
      .replace(Regex("\\s+-\\s+(single|ep|remaster(?:ed)?(?:\\s+\\d{4})?|deluxe.*|album version|radio edit)$", RegexOption.IGNORE_CASE), "")
      .replace(Regex("\\s+(feat\\.|ft\\.)\\s+[^-()\\[\\]]+$", RegexOption.IGNORE_CASE), "")
      .replace(Regex("\\s{2,}"), " ")
      .trim()
      .ifBlank { raw.trim() }

  internal fun extractPrimaryArtist(rawArtist: String): String {
    val clean = cleanSearchTerm(rawArtist)
    return clean
      .split(Regex("\\s*(?:,|&|\\bx\\b|\\bfeat\\.?\\b|\\bft\\.?\\b|\\bwith\\b)\\s*", RegexOption.IGNORE_CASE))
      .firstOrNull { it.isNotBlank() }
      ?.trim()
      ?.ifBlank { clean }
      ?: clean
  }

  internal fun decodeHtmlEntities(raw: String): String {
    if (raw.isBlank() || !raw.contains('&')) return raw
    var decoded =
      raw
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
    decoded =
      Regex("&#(\\d+);").replace(decoded) { match ->
        match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
      }
    decoded =
      Regex("&#x([0-9a-fA-F]+);").replace(decoded) { match ->
        match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
      }
    return decoded
  }

  private fun tokenizeWords(normalized: String): List<String> =
    normalized.split(Regex("[^a-z0-9]+")).filter { it.isNotBlank() }

  private fun containsWholePhrase(haystackNorm: String, phraseNorm: String): Boolean {
    if (phraseNorm.isBlank() || haystackNorm.isBlank()) return false
    val pattern = Regex("(^|[^a-z0-9])${Regex.escape(phraseNorm)}([^a-z0-9]|$)")
    return pattern.containsMatchIn(haystackNorm)
  }

  internal fun pickBestYouTubeCandidate(
    items: List<YouTubeSearchItem>,
    targetTitle: String,
    targetArtist: String,
    detailsById: Map<String, YouTubeVideoItem> = emptyMap(),
    targetDurationMs: Long = 0L
  ): YouTubeSearchItem? =
    items.maxByOrNull {
      scoreYouTubeCandidate(
        item = it,
        targetTitle = targetTitle,
        targetArtist = targetArtist,
        detail = detailsById[it.id.videoId],
        targetDurationMs = targetDurationMs
      )
    }

  internal fun scoreYouTubeCandidate(
    item: YouTubeSearchItem,
    targetTitle: String,
    targetArtist: String,
    detail: YouTubeVideoItem? = null,
    targetDurationMs: Long = 0L
  ): Int {
    val cleanTargetTitle = cleanSearchTerm(targetTitle)
    val cleanTargetArtist = cleanSearchTerm(targetArtist)
    val normTitle = KidsContentFilter.normalize(cleanTargetTitle)
    val normArtist = KidsContentFilter.normalize(cleanTargetArtist)
    val normPrimaryArtist = KidsContentFilter.normalize(extractPrimaryArtist(cleanTargetArtist))

    val rawTitleTokens = tokenizeWords(normTitle)
    val meaningfulTitleTokens =
      rawTitleTokens.filter { it !in MATCH_STOPWORDS }.ifEmpty { rawTitleTokens }
    val rawArtistTokens = tokenizeWords(normArtist)
    val meaningfulArtistTokens =
      rawArtistTokens.filter { it !in MATCH_STOPWORDS && it.length > 1 }.ifEmpty { rawArtistTokens }

    val decodedTitle = decodeHtmlEntities(item.snippet.title)
    val decodedChannel = decodeHtmlEntities(item.snippet.channelTitle)
    val decodedDesc = decodeHtmlEntities(detail?.snippet?.description ?: item.snippet.description)

    val candTitle = KidsContentFilter.normalize(decodedTitle)
    val candChannel = KidsContentFilter.normalize(decodedChannel)
    val candDesc = KidsContentFilter.normalize(decodedDesc)

    val candTitleTokens = tokenizeWords(candTitle).toSet()
    val candChannelTokens = tokenizeWords(candChannel).toSet()
    val candDescTokens = tokenizeWords(candDesc).toSet()

    var score = 0

    // 1. Word-bounded song title matching & core segment exactness
    val exactTitlePhrase = containsWholePhrase(candTitle, normTitle)
    if (exactTitlePhrase) score += 65

    val candSongSegmentRaw =
      if (decodedTitle.contains(" - ")) decodedTitle.substringAfter(" - ") else decodedTitle
    val candSongSegmentClean = KidsContentFilter.normalize(cleanSearchTerm(candSongSegmentRaw))
    val candSongSegmentTokens =
      tokenizeWords(candSongSegmentClean)
        .filter { it !in MATCH_STOPWORDS && it !in METADATA_NOISE_WORDS }
        .toSet()

    if (candSongSegmentClean == normTitle) {
      score += 40
    } else if (normTitle.isNotBlank() && candSongSegmentTokens.isNotEmpty()) {
      val extraSongWords = candSongSegmentTokens - meaningfulTitleTokens.toSet() - meaningfulArtistTokens.toSet()
      if (extraSongWords.isNotEmpty() && meaningfulTitleTokens.size <= 2) {
        score -= (extraSongWords.size * 22).coerceAtMost(60)
      }
    }

    val matchedTitleCount = meaningfulTitleTokens.count { it in candTitleTokens }
    val titleCoverage =
      if (meaningfulTitleTokens.isEmpty()) 0f
      else matchedTitleCount.toFloat() / meaningfulTitleTokens.size.toFloat()
    score += (titleCoverage * 50f).toInt()
    if (meaningfulTitleTokens.isNotEmpty() && matchedTitleCount == 0 && !exactTitlePhrase) {
      score -= 180
    }

    // 2. Artist verification across video title, channel name, and description
    val exactArtistPhraseInTitle =
      containsWholePhrase(candTitle, normArtist) ||
        (normPrimaryArtist.isNotBlank() && containsWholePhrase(candTitle, normPrimaryArtist))
    val exactArtistPhraseInChannel =
      containsWholePhrase(candChannel, normArtist) ||
        (normPrimaryArtist.isNotBlank() && containsWholePhrase(candChannel, normPrimaryArtist))
    if (exactArtistPhraseInTitle || exactArtistPhraseInChannel) {
      score += 55
    }
    val combinedArtistHaystack = candTitleTokens + candChannelTokens
    val matchedArtistCount = meaningfulArtistTokens.count { it in combinedArtistHaystack }
    val artistInDescCount = meaningfulArtistTokens.count { it in candDescTokens }
    val artistMatched = exactArtistPhraseInTitle || exactArtistPhraseInChannel || matchedArtistCount > 0 || artistInDescCount > 0

    if (meaningfulArtistTokens.isNotEmpty()) {
      val artistCoverage = matchedArtistCount.toFloat() / meaningfulArtistTokens.size.toFloat()
      score += (artistCoverage * 30f).toInt()
      if (!artistMatched) {
        score -= 115
      }
    }

    // 3. Official / YouTube Topic / Turkish & Global Label signals
    val isTopicChannel =
      (candChannel.endsWith("topic") && artistMatched) ||
        candDesc.contains("provided to youtube by")
    if (isTopicChannel) score += 45

    val isOfficialLabel =
      candTitle.containsAny("official music video", "official video", "official audio", "resmi video", "official lyric") ||
        candChannel.containsAny("vevo", "netd muzik", "dmc", "poll production", "avrupa muzik", "sony music", "universal music", "warner music", "dokuz sekiz")
    if (isOfficialLabel) {
      score += 28
    } else if (candTitle.contains("official") || candChannel.contains("official")) {
      score += 14
    }

    // 4. Reject edits, covers, karaoke, shorts, mixes & unrequested live/remix variants
    val targetNormRaw = KidsContentFilter.normalize(targetTitle)
    val wantsLive = targetNormRaw.containsAny("live", "canli", "konser", "unplugged")
    val wantsRemix = targetNormRaw.containsAny("remix", "mix", "mashup")
    val wantsAcoustic = targetNormRaw.containsAny("akustik", "acoustic")
    val wantsInstrumental = targetNormRaw.containsAny("enstrumantal", "instrumental")

    if (!wantsLive && candTitle.containsAny("live", "canli", "konser", "concert", "festival", "performans")) {
      score -= 50
    }
    if (!wantsRemix && candTitle.containsAny("remix", "mashup", "megamix", "bootleg", "dj set")) {
      score -= 55
    }
    if (!wantsAcoustic && candTitle.containsAny("akustik", "acoustic", "unplugged")) {
      score -= 30
    }
    if (!wantsInstrumental && candTitle.containsAny("enstrumantal", "instrumental", "karaoke", "backing track")) {
      score -= 80
    }

    if (candTitle.containsAny("slowed", "reverb", "nightcore", "8d", "sped up", "speed up", "hizlandirilmis", "bass boosted")) {
      score -= 75
    }
    if (candTitle.containsAny("cover", "reaction", "tepki", "inceleme", "nasil calinir", "tutorial", "akor", "parody", "parodi", "ai cover", "yapay zeka")) {
      score -= 80
    }
    if (candTitle.containsAny("#shorts", "shorts", "teaser", "fragman", "tanitim", "playlist", "full album", "tum sarkilari", "best of", "top 10", "medley")) {
      score -= 95
    }

    // 5. Duration & view-count verification when video details are available
    val videoDurMs = parseIsoDurationMsOrNull(detail?.contentDetails?.duration)
    if (videoDurMs != null && targetDurationMs > 30_000L) {
      val diffMs = abs(videoDurMs - targetDurationMs)
      score +=
        when {
          videoDurMs < 45_000L && targetDurationMs > 60_000L -> -160
          videoDurMs > maxOf((targetDurationMs * 2.5).toLong(), 14L * 60_000L) -> -160
          diffMs <= 8_000L -> 48
          diffMs <= 20_000L -> 30
          diffMs <= 45_000L -> 12
          diffMs <= 90_000L -> 0
          diffMs <= 160_000L -> -35
          else -> -65
        }
    }

    val viewCount = detail?.statistics?.viewCount?.toLongOrNull() ?: 0L
    if (viewCount > 100L && matchedTitleCount > 0 && artistMatched) {
      score += log10(viewCount.toDouble()).toInt().coerceIn(0, 12)
    }

    return score
  }

  private companion object {
    const val TAG = "HybridMusicRepo"
    const val VIDEO_CACHE_TTL_MS = 2 * 60 * 60 * 1000L
    const val HIGH_CONFIDENCE_MATCH_SCORE = 95
    const val MINIMUM_CONFIDENCE_MATCH_SCORE = 25

    val MATCH_STOPWORDS =
      setOf(
        "the", "a", "an", "and", "of", "in", "on", "at", "to", "for", "with", "feat", "ft",
        "ve", "ile", "bir", "bu", "da", "de", "mi", "mu", "official", "video", "audio",
        "music", "mv", "hd", "4k", "lyrics", "sarki", "sozleri"
      )

    val METADATA_NOISE_WORDS =
      setOf(
        "official", "video", "audio", "music", "clip", "klip", "resmi", "lyric", "lyrics",
        "sozleri", "remastered", "remaster", "hd", "4k", "hq", "topic", "vevo", "netd", "muzik"
      )

    val curatedSuggestionCatalog: List<Pair<String, String>> = listOf(
      "Müslüm Gürses" to "Arabesk & Fantezi",
      "Mustafa Sandal" to "Türkçe Pop",
      "Semicenk" to "Türkçe Pop",
      "Tarkan" to "Türkçe Pop",
      "Sezen Aksu" to "Türkçe Pop",
      "Ahmet Kaya" to "Özgün & Halk Müziği",
      "Neşet Ertaş" to "Türk Halk Müziği",
      "Zeki Müren" to "Türk Sanat Müziği",
      "Müzeyyen Senar" to "Türk Sanat Müziği",
      "Bülent Ersoy" to "Türk Sanat Müziği",
      "İbrahim Tatlıses" to "Arabesk & Fantezi",
      "Ferdi Tayfur" to "Arabesk & Fantezi",
      "Orhan Gencebay" to "Arabesk & Fantezi",
      "Bergen" to "Arabesk & Fantezi",
      "Ebru Gündeş" to "Arabesk & Fantezi",
      "Barış Manço" to "Anadolu Rock",
      "Cem Karaca" to "Anadolu Rock",
      "Erkin Koray" to "Anadolu Rock",
      "Selda Bağcan" to "Türk Halk Müziği",
      "Duman" to "Anadolu Rock",
      "Mor ve Ötesi" to "Anadolu Rock",
      "Teoman" to "Rock",
      "Şebnem Ferah" to "Rock",
      "Madrigal" to "Türkçe Alternatif",
      "Yüzyüzeyken Konuşuruz" to "Türkçe Alternatif",
      "Dolu Kadehi Ters Tut" to "Türkçe Alternatif",
      "Adamlar" to "Türkçe Alternatif",
      "Kalben" to "Türkçe Alternatif",
      "Ceza" to "Türkçe Rap & Trap",
      "Sagopa Kajmer" to "Türkçe Rap & Trap",
      "Ezhel" to "Türkçe Rap & Trap",
      "Motive" to "Türkçe Rap & Trap",
      "Lvbel C5" to "Türkçe Rap & Trap",
      "Mabel Matiz" to "Türkçe Pop",
      "Kenan Doğulu" to "Türkçe Pop",
      "Yalın" to "Türkçe Pop",
      "Gülşen" to "Türkçe Pop",
      "Hande Yener" to "Türkçe Pop",
      "Dedublüman" to "Türkçe Alternatif",
      "Mert Demir" to "Türkçe Pop",
      "The Weeknd" to "Global Pop & R&B",
      "Daft Punk" to "Elektronik & Dans",
      "Dua Lipa" to "Pop",
      "Coldplay" to "Alternatif Rock",
      "Arctic Monkeys" to "Rock",
      "Billie Eilish" to "Pop",
      "Imagine Dragons" to "Rock & Pop",
      "Taylor Swift" to "Pop",
      "Drake" to "Hip-Hop & Rap",
      "Kendrick Lamar" to "Hip-Hop & Rap",
      "Miles Davis" to "Caz & Lo-Fi",
      "Lana Del Rey" to "Alternatif Pop",
      "Türkçe Pop" to "Müzik Türü",
      "Anadolu Rock" to "Müzik Türü",
      "Arabesk & Fantezi" to "Müzik Türü",
      "Türk Halk Müziği" to "Müzik Türü",
      "Türk Sanat Müziği" to "Müzik Türü",
      "Türkçe Rap & Trap" to "Müzik Türü",
      "Türkçe Alternatif" to "Müzik Türü"
    )
  }
}
