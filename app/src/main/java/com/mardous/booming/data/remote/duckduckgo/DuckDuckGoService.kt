package com.mardous.booming.data.remote.duckduckgo

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DuckDuckGoImageResponse(
    @SerialName("results")
    val results: List<DuckDuckGoImageResult> = emptyList()
)

@Serializable
data class DuckDuckGoImageResult(
    @SerialName("title")
    val title: String = "",
    @SerialName("image")
    val image: String = "",
    @SerialName("thumbnail")
    val thumbnail: String = ""
)

class DuckDuckGoService(private val client: HttpClient) {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    /**
     * Searches DuckDuckGo Image API directly for high-resolution web portrait photos of the artist.
     */
    suspend fun getArtistPortraits(artistName: String): List<Pair<String, String>> {
        val candidates = mutableListOf<Pair<String, String>>()
        val seenUrls = mutableSetOf<String>()

        val cleanArtistName = artistName.trim().removeSurrounding("\"")
        val queries = listOf(
            "$cleanArtistName singer",
            "$cleanArtistName musician"
        )

        for (query in queries) {
            if (candidates.size >= 25) break

            try {
                // 1. Obtain DuckDuckGo vqd token for query
                val html: String = client.get("https://duckduckgo.com/") {
                    parameter("q", query)
                    parameter("iax", "images")
                    parameter("ia", "images")
                    header(HttpHeaders.UserAgent, userAgent)
                    header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
                }.body()

                val vqdRegex = Regex("""vqd=["']?([\d-]+)["']?""")
                val vqdAltRegex = Regex("""vqd=([\d-]+)""")

                val vqd = vqdRegex.find(html)?.groupValues?.getOrNull(1)
                    ?: vqdAltRegex.find(html)?.groupValues?.getOrNull(1)

                if (vqd.isNullOrBlank()) continue

                // 2. Fetch image results directly from DuckDuckGo's i.js endpoint
                val jsonResponse = client.get("https://duckduckgo.com/i.js") {
                    parameter("l", "wt-wt")
                    parameter("o", "json")
                    parameter("q", query)
                    parameter("vqd", vqd)
                    parameter("f", ",,,")
                    parameter("p", "1")
                    header(HttpHeaders.UserAgent, userAgent)
                    header(HttpHeaders.Referrer, "https://duckduckgo.com/")
                    header(HttpHeaders.Accept, "application/json")
                }.body<DuckDuckGoImageResponse>()

                for (item in jsonResponse.results) {
                    val url = item.image.ifEmpty { item.thumbnail }
                    if (url.isBlank() || !url.startsWith("http")) continue

                    val label = item.title.ifBlank { cleanArtistName }
                    if (isIgnoredWebImage(url) || isIgnoredWebImage(label)) continue
                    if (!isTitleRelevantToArtist(label, url, cleanArtistName)) continue

                    if (seenUrls.add(url)) {
                        candidates.add(label to url)
                    }

                    if (candidates.size >= 25) break
                }
            } catch (_: Exception) {}
        }

        return candidates
    }

    /**
     * Generic artist title relevance verification.
     */
    private fun isTitleRelevantToArtist(title: String, url: String, artistName: String): Boolean {
        if (title.isBlank()) return true

        val text = "$title $url".lowercase()
        val cleanName = artistName.trim().lowercase()

        // Direct full name match
        if (text.contains(cleanName)) return true

        // Token-based matching: every primary word in the artist's name must be present
        val nameTokens = cleanName.split(Regex("[\\s._\\-]+")).filter { it.length >= 2 }
        if (nameTokens.isEmpty()) return true

        return nameTokens.all { token -> text.contains(token) }
    }

    /**
     * Filters out non-photo web assets (vectors, icons, logos, gifs, svgs).
     */
    private fun isIgnoredWebImage(text: String): Boolean {
        val lower = text.lowercase()
        val assetExclusions = listOf(".svg", ".gif", "logo", "icon", "vector", "avatar_default", "clipart")
        return assetExclusions.any { lower.contains(it) }
    }
}
