package com.mardous.booming.data.remote.fanarttv

import com.mardous.booming.extensions.utilities.normalize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicBrainzSearchResponse(
    @SerialName("artists")
    val artists: List<MusicBrainzArtist> = emptyList()
)

@Serializable
data class MusicBrainzArtist(
    @SerialName("id")
    val id: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("score")
    val score: Int = 0
)

@Serializable
data class FanartTvMusicResponse(
    @SerialName("name")
    val name: String = "",
    @SerialName("mbid_id")
    val mbidId: String = "",
    @SerialName("artistthumb")
    val artistThumb: List<FanartTvImage>? = null,
    @SerialName("artistbackground")
    val artistBackground: List<FanartTvImage>? = null
)

@Serializable
data class FanartTvImage(
    @SerialName("id")
    val id: String = "",
    @SerialName("url")
    val url: String = "",
    @SerialName("likes")
    val likes: String = "0"
)

class FanartTvService(private val client: HttpClient) {

    private val userAgent = "BoomingMusic/1.0 (Android; Music Player; contact@booming.com)"
    private val fanartApiKey = "2793132e4d0d0f419d8544c9b31d4e68"

    /**
     * Searches MusicBrainz for artist MBID, then fetches all high-resolution artist thumbnails
     * and background portraits from Fanart.tv.
     */
    suspend fun getArtistPortraits(artistName: String): List<Pair<String, String>> {
        val candidates = mutableListOf<Pair<String, String>>()
        val seenUrls = mutableSetOf<String>()

        try {
            // 1. Query MusicBrainz API for artist MBID
            val mbResponse = client.get("https://musicbrainz.org/ws/2/artist/") {
                parameter("query", artistName)
                parameter("fmt", "json")
                parameter("limit", "5")
                header(HttpHeaders.UserAgent, userAgent)
            }.body<MusicBrainzSearchResponse>()

            val normRequested = artistName.trim().normalize().lowercase()
            val bestArtist = mbResponse.artists.firstOrNull {
                val normName = it.name.trim().normalize().lowercase()
                normName == normRequested
            } ?: mbResponse.artists.firstOrNull() ?: return emptyList()

            val mbid = bestArtist.id
            if (mbid.isBlank()) return emptyList()

            // 2. Query Fanart.tv API using MusicBrainz MBID
            try {
                val fanartResponse = client.get("https://webservice.fanart.tv/v3/music/$mbid") {
                    parameter("api_key", fanartApiKey)
                    header(HttpHeaders.UserAgent, userAgent)
                }.body<FanartTvMusicResponse>()

                // 3. Extract artist thumbs (square/portrait thumbnails)
                for (img in fanartResponse.artistThumb.orEmpty()) {
                    val url = img.url
                    if (url.isNotBlank() && url.startsWith("http") && seenUrls.add(url)) {
                        candidates.add(bestArtist.name to url)
                    }
                }

                // 4. Extract artist backgrounds (landscape/concert portraits)
                for (img in fanartResponse.artistBackground.orEmpty()) {
                    val url = img.url
                    if (url.isNotBlank() && url.startsWith("http") && seenUrls.add(url)) {
                        candidates.add(bestArtist.name to url)
                    }
                }
            } catch (_: Exception) {}

        } catch (_: Exception) {}

        return candidates
    }
}
