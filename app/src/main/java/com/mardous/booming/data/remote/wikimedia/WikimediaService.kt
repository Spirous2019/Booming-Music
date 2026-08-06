package com.mardous.booming.data.remote.wikimedia

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WikiSearchResponse(
    @SerialName("query")
    val query: WikiQuery? = null
)

@Serializable
data class WikiQuery(
    @SerialName("search")
    val search: List<WikiSearchResult> = emptyList(),
    @SerialName("pages")
    val pages: Map<String, WikiPage> = emptyMap()
)

@Serializable
data class WikiSearchResult(
    @SerialName("title")
    val title: String = ""
)

@Serializable
data class WikiPageSummary(
    @SerialName("title")
    val title: String = "",
    @SerialName("originalimage")
    val originalImage: WikiImage? = null,
    @SerialName("thumbnail")
    val thumbnail: WikiImage? = null
)

@Serializable
data class WikiImage(
    @SerialName("source")
    val source: String = ""
)

@Serializable
data class WikiPage(
    @SerialName("title")
    val title: String = "",
    @SerialName("thumbnail")
    val thumbnail: WikiImage? = null,
    @SerialName("imageinfo")
    val imageInfo: List<WikiImageInfo> = emptyList()
)

@Serializable
data class WikiImageInfo(
    @SerialName("url")
    val url: String = "",
    @SerialName("thumburl")
    val thumbUrl: String? = null,
    @SerialName("mime")
    val mime: String = ""
)

class WikimediaService(private val client: HttpClient) {

    private val userAgent = "BoomingMusic/1.0 (Android; Music Player; contact@booming.com)"

    /**
     * Searches Wikipedia for the artist's article and extracts genuine portrait/press/event photos.
     */
    suspend fun getArtistPortraits(artistName: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val seenUrls = mutableSetOf<String>()

        try {
            // 1. Search Wikipedia for artist title
            val searchResponse = client.get("https://en.wikipedia.org/w/api.php") {
                parameter("action", "query")
                parameter("list", "search")
                parameter("srsearch", artistName)
                parameter("format", "json")
                header(HttpHeaders.UserAgent, userAgent)
            }.body<WikiSearchResponse>()

            val searchResults = searchResponse.query?.search ?: emptyList()
            if (searchResults.isEmpty()) return emptyList()

            // Find best matching Wikipedia article title
            val normRequested = artistName.trim().lowercase()
            val bestTitle = searchResults.firstOrNull {
                it.title.trim().lowercase() == normRequested ||
                        it.title.trim().lowercase().startsWith(normRequested)
            }?.title ?: searchResults.first().title

            // 2. Get main page summary photo (highest priority)
            try {
                val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/${java.net.URLEncoder.encode(bestTitle, "UTF-8")}"
                val summary = client.get(summaryUrl) {
                    header(HttpHeaders.UserAgent, userAgent)
                }.body<WikiPageSummary>()

                val rawMainUrl = summary.originalImage?.source ?: summary.thumbnail?.source
                if (!rawMainUrl.isNullOrBlank()) {
                    val cdnUrl = toWikiCdnUrl(rawMainUrl)
                    if (seenUrls.add(cdnUrl)) {
                        results.add(bestTitle to cdnUrl)
                    }
                }
            } catch (_: Exception) {}

            // 3. Query Wikipedia page images (gallery images in the article)
            try {
                val galleryResponse = client.get("https://en.wikipedia.org/w/api.php") {
                    parameter("action", "query")
                    parameter("titles", bestTitle)
                    parameter("generator", "images")
                    parameter("gimlimit", 30)
                    parameter("prop", "pageimages|imageinfo")
                    parameter("pithumbsize", 1000)
                    parameter("iiprop", "url|mime")
                    parameter("format", "json")
                    header(HttpHeaders.UserAgent, userAgent)
                }.body<WikiSearchResponse>()

                val pages = galleryResponse.query?.pages ?: emptyMap()
                for ((_, page) in pages) {
                    val info = page.imageInfo.firstOrNull()
                    val rawUrl = page.thumbnail?.source ?: info?.thumbUrl ?: info?.url ?: continue
                    val mime = info?.mime?.lowercase() ?: ""

                    // Only accept JPEG / PNG images
                    if (mime.isNotBlank() && !mime.contains("jpeg") && !mime.contains("jpg") && !mime.contains("png")) continue

                    // Ignore logos, icons, flags, diagrams, signatures, album covers
                    val titleLower = page.title.lowercase()
                    if (isIgnoredWikiFile(titleLower)) continue
                    if (!isImageFilenameRelevantToArtist(titleLower, artistName)) continue

                    val cdnUrl = toWikiCdnUrl(rawUrl)
                    if (seenUrls.add(cdnUrl)) {
                        val cleanLabel = page.title.removePrefix("File:").removeSuffix(".jpg").removeSuffix(".jpeg").removeSuffix(".png")
                        results.add(cleanLabel to cdnUrl)
                    }
                }
            } catch (_: Exception) {}

        } catch (_: Exception) {}

        return results
    }

    private fun isImageFilenameRelevantToArtist(fileName: String, artistName: String): Boolean {
        val normFile = fileName.lowercase()
        val normArtist = artistName.trim().lowercase()

        // 1. Direct full name match
        if (normFile.contains(normArtist)) return true

        // 2. Token match: at least one primary token of the artist's name must be in the filename
        val nameTokens = normArtist.split(Regex("[\\s._\\-]+")).filter { it.length >= 3 }
        if (nameTokens.isEmpty()) return true

        return nameTokens.any { token -> normFile.contains(token) }
    }

    /**
     * Converts a Wikipedia Commons URL to Wikipedia's public thumbnail CDN URL.
     * This avoids 403 Forbidden blocking when loaded in Coil on Android.
     */
    private fun toWikiCdnUrl(url: String): String {
        if (url.contains("/wikipedia/commons/thumb/")) return url
        val commonsPrefix = "https://upload.wikimedia.org/wikipedia/commons/"
        if (url.startsWith(commonsPrefix)) {
            val relativePath = url.substring(commonsPrefix.length)
            val fileName = relativePath.substringAfterLast("/")
            return "https://upload.wikimedia.org/wikipedia/commons/thumb/$relativePath/1000px-$fileName"
        }
        val enWikiPrefix = "https://upload.wikimedia.org/wikipedia/en/"
        if (url.startsWith(enWikiPrefix)) {
            val relativePath = url.substring(enWikiPrefix.length)
            val fileName = relativePath.substringAfterLast("/")
            return "https://upload.wikimedia.org/wikipedia/en/thumb/$relativePath/1000px-$fileName"
        }
        return url
    }

    private fun isIgnoredWikiFile(fileName: String): Boolean {
        val ignoredKeywords = listOf(
            "logo", "icon", "flag", "map", "signature", "autograph", "stub",
            "commons", "diagram", "symbol", "chart", "star", "coat_of_arms",
            "album", "cover", "single", "discography", "sound", "audio", "speaker",
            "wiki", "edit", "padlock", "question", "folder", "play"
        )
        return ignoredKeywords.any { fileName.contains(it) }
    }
}
