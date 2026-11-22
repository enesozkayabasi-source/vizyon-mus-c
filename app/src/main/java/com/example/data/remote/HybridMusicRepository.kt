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
        } + derivedArtists).distinctBy { it.name.lowercase(Locale.ROOT) }

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
    if (!forceRefresh && track.youtubeVideoId.isNotBlank() && track.youtubeVideoId !in excludedVideoIds) {
      return VideoResolutionResult.Success(track)
    }

    val nowMs = System.currentTimeMillis()
    if (!forceRefresh) {
      videoMatchCache[track.trackId]?.let { cached ->
        if (
          nowMs - cached.cachedAtMs < VIDEO_CACHE_TTL_MS &&
            cached.videoId.isNotBlank() &&
            cached.videoId !in excludedVideoIds
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
    val query = "$cleanedArtist $cleanedTitle official".trim()

    return try {
      val primaryResp =
        youtubeApi.searchMusicCategory(
          query = query,
          type = "video",
          videoCategoryId = "10",
          videoEmbeddable = "true",
          maxResults = 10
        )
      var candidates = filterSafeYouTubeCandidates(primaryResp.items, excludedVideoIds)
      if (candidates.isEmpty()) {
        val fallbackResp =
          youtubeApi.searchMusicCategory(
            query = "$cleanedArtist $cleanedTitle".trim(),
            type = "video",
            videoCategoryId = null,
            videoEmbeddable = "true",
            maxResults = 10
          )
        candidates = filterSafeYouTubeCandidates(fallbackResp.items, excludedVideoIds)
      }

      if (candidates.isEmpty()) {
        return VideoResolutionResult.NotFound(
          track,
          "\"${track.title}\" için yerleştirilebilir resmi YouTube videosu bulunamadı."
        )
      }

      val orderedCandidates =
        candidates
          .sortedByDescending { scoreYouTubeCandidate(it, cleanedTitle, cleanedArtist) }
          .take(5)

      val candidateIds = orderedCandidates.map { it.id.videoId }.joinToString(",")
      val detailItems =
        runCatching { youtubeApi.getVideoDetails(videoIds = candidateIds).items }
          .getOrDefault(emptyList())
          .associateBy { it.id }

      val embeddableCandidate =
        orderedCandidates.firstOrNull { cand ->
          val detail = detailItems[cand.id.videoId]
          detail == null || detail.status?.embeddable != false
        }
          ?: return VideoResolutionResult.NotFound(
            track,
            "\"${track.title}\" için eşleşen videoların gömülü oynatma (embed) izni kapalı."
          )

      val bestVideoId = embeddableCandidate.id.videoId
      val detailDurMs =
        detailItems[bestVideoId]?.contentDetails?.duration
          ?.let(::parseIsoDurationMs)
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

  private fun filterSafeYouTubeCandidates(
    items: List<YouTubeSearchItem>,
    excludedVideoIds: Set<String>
  ): List<YouTubeSearchItem> =
    items.filter { item ->
      val vid = item.id.videoId
      vid.isNotBlank() &&
        vid !in excludedVideoIds &&
        !KidsContentFilter.isChildContent(
          title = item.snippet.title,
          artistName = item.snippet.channelTitle,
          albumName = item.snippet.description
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

  private fun cleanSearchTerm(raw: String): String =
    raw
      .replace(Regex("\\((official|feat\\.|ft\\.|remaster|live|acoustic|remix).*?\\)", RegexOption.IGNORE_CASE), "")
      .replace(Regex("\\[.*?]"), "")
      .replace(Regex("\\s{2,}"), " ")
      .trim()
      .ifBlank { raw.trim() }

  internal fun pickBestYouTubeCandidate(
    items: List<YouTubeSearchItem>,
    targetTitle: String,
    targetArtist: String
  ): YouTubeSearchItem? =
    items.maxByOrNull { scoreYouTubeCandidate(it, targetTitle, targetArtist) }

  internal fun scoreYouTubeCandidate(
    item: YouTubeSearchItem,
    targetTitle: String,
    targetArtist: String
  ): Int {
    val normTitle = KidsContentFilter.normalize(targetTitle)
    val normArtist = KidsContentFilter.normalize(targetArtist)
    val titleTokens = normTitle.split(Regex("\\s+")).filter { it.length > 1 }
    val artistTokens = normArtist.split(Regex("\\s+")).filter { it.length > 1 }
    val candTitle = KidsContentFilter.normalize(item.snippet.title)
    val candChannel = KidsContentFilter.normalize(item.snippet.channelTitle)
    var score = 0
    if (candTitle.contains(normTitle)) score += 60
    if (candTitle.contains(normArtist) || candChannel.contains(normArtist)) score += 45
    titleTokens.forEach { tok -> if (candTitle.contains(tok)) score += 10 }
    artistTokens.forEach { tok -> if (candTitle.contains(tok) || candChannel.contains(tok)) score += 8 }
    if (candTitle.contains("official") || candChannel.contains("vevo") || candChannel.contains("topic")) {
      score += 18
    }
    if (candTitle.contains("slowed") || candTitle.contains("reverb") || candTitle.contains("nightcore") || candTitle.contains("8d")) {
      score -= 40
    }
    if (candTitle.contains("karaoke") || candTitle.contains("cover") || candTitle.contains("reaction")) {
      score -= 35
    }
    return score
  }

  private companion object {
    const val TAG = "HybridMusicRepo"
    const val VIDEO_CACHE_TTL_MS = 2 * 60 * 60 * 1000L

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
