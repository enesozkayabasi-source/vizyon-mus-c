package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ItunesSearchResponse(
  val resultCount: Int = 0,
  val results: List<ItunesItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesItemDto(
  val wrapperType: String = "",
  val kind: String = "",
  val trackId: Long = 0L,
  val artistId: Long = 0L,
  val trackName: String = "",
  val artistName: String = "",
  val artistLinkUrl: String = "",
  val collectionName: String = "",
  val artworkUrl100: String = "",
  val artworkUrl60: String = "",
  val trackTimeMillis: Long = 0L,
  val primaryGenreName: String = "",
  val country: String = ""
) {
  fun highResArtworkUrl(): String {
    val raw = artworkUrl100.ifBlank { artworkUrl60 }
    if (raw.isBlank()) return ""
    return raw
      .replace("100x100bb", "600x600bb")
      .replace("60x60bb", "600x600bb")
  }
}

/**
 * Retrofit service interface for the free, quota-free iTunes Search API
 * (`https://itunes.apple.com/`).
 */
interface ItunesSearchApiService {
  @GET("search")
  suspend fun searchSongs(
    @Query("term") term: String,
    @Query("media") media: String = "music",
    @Query("entity") entity: String = "song",
    @Query("country") country: String = "TR",
    @Query("limit") limit: Int = 25,
    @Query("explicit") explicit: String = "Yes"
  ): ItunesSearchResponse

  @GET("search")
  suspend fun searchArtists(
    @Query("term") term: String,
    @Query("media") media: String = "music",
    @Query("entity") entity: String = "musicArtist",
    @Query("country") country: String = "TR",
    @Query("limit") limit: Int = 12
  ): ItunesSearchResponse

  @GET("lookup")
  suspend fun lookupArtistTopSongs(
    @Query("id") artistId: Long,
    @Query("entity") entity: String = "song",
    @Query("country") country: String = "TR",
    @Query("limit") limit: Int = 20
  ): ItunesSearchResponse
}
