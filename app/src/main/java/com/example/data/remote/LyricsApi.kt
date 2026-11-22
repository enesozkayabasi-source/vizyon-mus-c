package com.example.data.remote

import com.example.model.LyricLine
import com.example.model.TrackLyrics
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class LrcLibTrackDto(
  val id: Long = 0L,
  val trackName: String = "",
  val artistName: String = "",
  val albumName: String = "",
  val duration: Double = 0.0,
  val instrumental: Boolean = false,
  val plainLyrics: String? = null,
  val syncedLyrics: String? = null
)

interface LrcLibApiService {
  @GET("api/get")
  suspend fun getLyrics(
    @Query("track_name") trackName: String,
    @Query("artist_name") artistName: String,
    @Header("User-Agent") userAgent: String = "VizyonMusic/1.0"
  ): LrcLibTrackDto

  @GET("api/search")
  suspend fun searchLyrics(
    @Query("q") query: String,
    @Header("User-Agent") userAgent: String = "VizyonMusic/1.0"
  ): List<LrcLibTrackDto>
}

fun createLrcLibApiService(client: OkHttpClient, moshi: Moshi): LrcLibApiService =
  Retrofit.Builder()
    .baseUrl("https://lrclib.net/")
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .build()
    .create(LrcLibApiService::class.java)

fun LrcLibTrackDto.toTrackLyrics(trackId: String): TrackLyrics {
  val synced = syncedLyrics?.let(::parseLrcLines).orEmpty()
  val plain =
    plainLyrics
      ?.lines()
      ?.map { it.trimEnd() }
      ?.dropWhile { it.isBlank() }
      ?.dropLastWhile { it.isBlank() }
      .orEmpty()
  return TrackLyrics(
    trackId = trackId,
    trackName = trackName,
    artistName = artistName,
    syncedLines = synced,
    plainLines = plain,
    isInstrumental = instrumental
  )
}

private val LRC_TAG_REGEX = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?]")

fun parseLrcLines(lrcText: String): List<LyricLine> {
  val lines = mutableListOf<LyricLine>()
  for (rawLine in lrcText.lineSequence()) {
    val matches = LRC_TAG_REGEX.findAll(rawLine).toList()
    if (matches.isEmpty()) continue
    val lyricText = rawLine.replace(LRC_TAG_REGEX, "").trim()
    if (lyricText.isBlank()) continue
    for (m in matches) {
      val mins = m.groupValues[1].toLongOrNull() ?: 0L
      val secs = m.groupValues[2].toLongOrNull() ?: 0L
      val fracRaw = m.groupValues[3]
      val ms =
        when (fracRaw.length) {
          0 -> 0L
          1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
          2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
          else -> fracRaw.take(3).toLongOrNull() ?: 0L
        }
      lines.add(LyricLine(timeMs = (mins * 60_000L) + (secs * 1000L) + ms, text = lyricText))
    }
  }
  return lines.sortedBy { it.timeMs }
}
