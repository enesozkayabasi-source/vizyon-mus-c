package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class YouTubeVideosResponse(
  val nextPageToken: String? = null,
  val items: List<YouTubeVideoItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItem(
  val id: String = "",
  val snippet: YouTubeSnippet = YouTubeSnippet(),
  val contentDetails: YouTubeContentDetails? = null,
  val statistics: YouTubeStatistics? = null,
  val topicDetails: YouTubeTopicDetails? = null,
  val status: YouTubeVideoStatus? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoStatus(
  val embeddable: Boolean = true,
  val privacyStatus: String = "public"
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
  val nextPageToken: String? = null,
  val items: List<YouTubeSearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItem(
  val id: YouTubeSearchId = YouTubeSearchId(),
  val snippet: YouTubeSnippet = YouTubeSnippet()
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchId(
  val kind: String = "",
  val videoId: String = "",
  val channelId: String = "",
  val playlistId: String = ""
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippet(
  val publishedAt: String = "",
  val title: String = "",
  val description: String = "",
  val channelId: String = "",
  val channelTitle: String = "",
  val categoryId: String = "",
  val tags: List<String> = emptyList(),
  val thumbnails: YouTubeThumbnails = YouTubeThumbnails(),
  val resourceId: YouTubeResourceId? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeResourceId(
  val kind: String = "",
  val videoId: String = ""
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnails(
  val maxres: YouTubeThumbnail? = null,
  val high: YouTubeThumbnail? = null,
  val medium: YouTubeThumbnail? = null,
  val default: YouTubeThumbnail? = null
) {
  fun bestUrl(): String =
    maxres?.url?.takeIf { it.isNotBlank() }
      ?: high?.url?.takeIf { it.isNotBlank() }
      ?: medium?.url?.takeIf { it.isNotBlank() }
      ?: default?.url.orEmpty()
}

@JsonClass(generateAdapter = true)
data class YouTubeThumbnail(
  val url: String = "",
  val width: Int? = null,
  val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentDetails(
  val duration: String = "",
  val definition: String = "",
  val licensedContent: Boolean = false
)

@JsonClass(generateAdapter = true)
data class YouTubeStatistics(
  val viewCount: String = "0",
  val likeCount: String = "0",
  val subscriberCount: String = "0"
)

@JsonClass(generateAdapter = true)
data class YouTubeTopicDetails(
  val topicCategories: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelsResponse(val items: List<YouTubeChannelItem> = emptyList())

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
  val id: String = "",
  val snippet: YouTubeSnippet = YouTubeSnippet(),
  val statistics: YouTubeStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItemsResponse(
  val nextPageToken: String? = null,
  val items: List<YouTubePlaylistItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistItem(
  val id: String = "",
  val snippet: YouTubeSnippet = YouTubeSnippet(),
  val contentDetails: YouTubePlaylistContentDetails? = null
)

@JsonClass(generateAdapter = true)
data class YouTubePlaylistContentDetails(
  val videoId: String = "",
  val videoPublishedAt: String = ""
)

/**
 * Retrofit service interface for YouTube Data API v3 (`https://www.googleapis.com/youtube/v3/`).
 * API key authentication (`BuildConfig.YOUTUBE_API_KEY`) is injected automatically via
 * [YouTubeApiKeyInterceptor].
 */
interface YouTubeMusicApiService {
  @GET("videos")
  suspend fun getPopularMusicVideos(
    @Query("part") part: String = "snippet,contentDetails,statistics",
    @Query("chart") chart: String = "mostPopular",
    @Query("videoCategoryId") videoCategoryId: String = "10",
    @Query("regionCode") regionCode: String = "TR",
    @Query("maxResults") maxResults: Int = 25,
    @Query("pageToken") pageToken: String? = null
  ): YouTubeVideosResponse

  @GET("videos")
  suspend fun getVideoDetails(
    @Query("part") part: String = "snippet,contentDetails,statistics,topicDetails,status",
    @Query("id") videoIds: String
  ): YouTubeVideosResponse

  @GET("search")
  suspend fun searchMusicCategory(
    @Query("part") part: String = "snippet",
    @Query("q") query: String,
    @Query("type") type: String = "video",
    @Query("videoCategoryId") videoCategoryId: String? = "10",
    @Query("videoEmbeddable") videoEmbeddable: String = "true",
    @Query("safeSearch") safeSearch: String = "moderate",
    @Query("maxResults") maxResults: Int = 10,
    @Query("pageToken") pageToken: String? = null
  ): YouTubeSearchResponse

  @GET("search")
  suspend fun searchChannels(
    @Query("part") part: String = "snippet",
    @Query("q") query: String,
    @Query("type") type: String = "channel",
    @Query("maxResults") maxResults: Int = 10
  ): YouTubeSearchResponse

  @GET("channels")
  suspend fun getChannelDetails(
    @Query("part") part: String = "snippet,statistics",
    @Query("id") channelId: String
  ): YouTubeChannelsResponse

  @GET("playlistItems")
  suspend fun getPlaylistItems(
    @Query("part") part: String = "snippet,contentDetails",
    @Query("playlistId") playlistId: String,
    @Query("maxResults") maxResults: Int = 25,
    @Query("pageToken") pageToken: String? = null
  ): YouTubePlaylistItemsResponse
}

/**
 * OkHttp interceptor that attaches `BuildConfig.YOUTUBE_API_KEY` as the `key` query parameter
 * on every YouTube Data API v3 call when configured in AI Studio Secrets.
 */
class YouTubeApiKeyInterceptor(
  private val apiKeyProvider: () -> String = { resolveYouTubeApiKey() }
) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val original = chain.request()
    val apiKey = apiKeyProvider()
    val updatedRequest =
      if (apiKey.isNotBlank() && original.url.queryParameter("key").isNullOrBlank()) {
        val updatedUrl = original.url.newBuilder().addQueryParameter("key", apiKey).build()
        original.newBuilder().url(updatedUrl).build()
      } else {
        original
      }
    return chain.proceed(updatedRequest)
  }
}

fun resolveYouTubeApiKey(): String =
  BuildConfig.YOUTUBE_API_KEY.takeIf {
    it.isNotBlank() && it != "MY_YOUTUBE_API_KEY"
  }.orEmpty()

fun createYouTubeMusicApiService(
  baseClient: OkHttpClient,
  moshi: Moshi
): YouTubeMusicApiService {
  val youtubeClient =
    baseClient.newBuilder()
      .addInterceptor(YouTubeApiKeyInterceptor())
      .build()
  return Retrofit.Builder()
    .baseUrl("https://www.googleapis.com/youtube/v3/")
    .client(youtubeClient)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
    .create(YouTubeMusicApiService::class.java)
}

fun parseIsoDurationMs(iso: String?): Long {
  if (iso.isNullOrBlank()) return 180_000L
  val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
  val match = regex.matchEntire(iso) ?: return 180_000L
  val hours = match.groupValues[1].toLongOrNull() ?: 0L
  val minutes = match.groupValues[2].toLongOrNull() ?: 0L
  val seconds = match.groupValues[3].toLongOrNull() ?: 0L
  val total = (hours * 3600 + minutes * 60 + seconds) * 1000L
  return if (total > 0L) total else 180_000L
}
