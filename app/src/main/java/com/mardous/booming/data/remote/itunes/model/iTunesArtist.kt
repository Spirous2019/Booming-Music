package com.mardous.booming.data.remote.itunes.model

import com.mardous.booming.data.remote.deezer.model.DeezerArtist
import com.mardous.booming.extensions.utilities.normalize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class iTunesArtist(
    @SerialName("resultCount")
    val resultCount: Int = 0,
    @SerialName("results")
    val results: List<Result> = emptyList()
) {

    @Serializable
    data class Result(
        @SerialName("artistName")
        val artistName: String = "",
        @SerialName("artworkUrl100")
        val artworkUrl100: String? = null,
        @SerialName("artworkUrl60")
        val artworkUrl60: String? = null,
        @SerialName("collectionName")
        val collectionName: String? = null
    ) {
        val highResArtworkUrl: String?
            get() {
                val base = artworkUrl100 ?: artworkUrl60 ?: return null
                return base.replace("100x100bb", "1000x1000bb")
                    .replace("60x60bb", "1000x1000bb")
            }
    }

    fun getFilteredCandidates(requestedName: String): List<Pair<String, String>> {
        if (results.isEmpty()) return emptyList()

        val resolvedAlias = DeezerArtist.ARTIST_ALIASES[requestedName.trim().lowercase()] ?: requestedName
        val normRequested = resolvedAlias.normalize().lowercase()
        val primaryName = resolvedAlias.split(Regex("(?i)\\s+(feat\\.|ft\\.|with|&|,|/)\\s+")).firstOrNull()?.trim() ?: resolvedAlias
        val normPrimary = primaryName.normalize().lowercase()

        val seenUrls = mutableSetOf<String>()
        val candidates = mutableListOf<Pair<String, String>>()

        for (item in results) {
            val normArtist = item.artistName.normalize().lowercase()
            if (normArtist.isBlank()) continue

            val isExactMatch = normArtist == normRequested || normArtist == normPrimary
            val isPrefixOrSubstring = normArtist.startsWith(normPrimary) || normPrimary.startsWith(normArtist) ||
                    (normRequested.length >= 3 && normArtist.contains(normRequested))
            val jwScore = DeezerArtist.JW_SIMILARITY.apply(normArtist, normRequested)

            val isNoiseKeyword = normArtist.contains("piano") || normArtist.contains("karaoke") ||
                    normArtist.contains("tribute") || normArtist.contains("cover") ||
                    normArtist.contains("beatzz") || normArtist.contains("ensemble") ||
                    normArtist.contains("instrumental") || normArtist.contains("string quartet") ||
                    normArtist.contains("orchestra")

            val isMatch = (isExactMatch || isPrefixOrSubstring || jwScore >= 0.80) && !isNoiseKeyword
            if (!isMatch) continue

            val url = item.highResArtworkUrl ?: continue
            if (seenUrls.add(url)) {
                val label = if (!item.collectionName.isNullOrBlank()) "${item.artistName} - ${item.collectionName}" else item.artistName
                candidates.add(label to url)
            }
        }

        return candidates
    }
}
