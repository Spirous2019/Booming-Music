package com.mardous.booming.data.remote.deezer.model

import com.mardous.booming.extensions.utilities.normalize
import com.mardous.booming.util.ImageSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.apache.commons.text.similarity.JaroWinklerSimilarity

@Serializable
class DeezerArtist(
    @SerialName("data")
    val result: List<Result> = emptyList(),
    val total: Int = 0
) {

    fun getBestImage(requestedName: String, requestedImageSize: String): Pair<Boolean, String?> {
        if (result.isEmpty()) return false to null

        val resolvedAlias = ARTIST_ALIASES[requestedName.trim().lowercase()] ?: requestedName
        val normRequested = resolvedAlias.normalize().lowercase()
        val primaryName = resolvedAlias.split(Regex("(?i)\\s+(feat\\.|ft\\.|with|&|,|/)\\s+")).firstOrNull()?.trim() ?: resolvedAlias
        val normPrimary = primaryName.normalize().lowercase()

        val firstArtist = result[0]
        val normFirst = firstArtist.artistName.normalize().lowercase()

        // 1. Direct name match (exact, prefix)
        val isExactOrPrefix = normFirst == normRequested || normFirst == normPrimary ||
                normFirst.startsWith(normPrimary) || normPrimary.startsWith(normFirst)

        // 2. Substring match only for names >= 5 chars to avoid false positives
        val isSubstringMatch = (normPrimary.length >= 5 && normFirst.contains(normPrimary)) ||
                (normFirst.length >= 5 && normRequested.contains(normFirst))

        val firstScore = JW_SIMILARITY.apply(normFirst, normRequested)

        // 3. High-confidence candidate selection
        val matchedArtist: Result? = when {
            isExactOrPrefix -> firstArtist
            isSubstringMatch && firstScore >= 0.65 -> firstArtist
            firstScore >= 0.75 -> firstArtist
            // Deezer single alias match (e.g. "Biggie Smalls" -> "The Notorious B.I.G.")
            result.size == 1 && firstScore >= 0.45 -> firstArtist
            else -> {
                // Find the candidate with the highest similarity score >= 0.70
                val bestCandidate = result.map { artist ->
                    val normArtist = artist.artistName.normalize().lowercase()
                    val score = JW_SIMILARITY.apply(normArtist, normRequested)
                    artist to score
                }.maxByOrNull { it.second }

                if (bestCandidate != null && bestCandidate.second >= 0.70) {
                    bestCandidate.first
                } else null
            }
        }

        if (matchedArtist == null) {
            return false to null
        }

        val tentativeImage = matchedArtist.xlImage ?: matchedArtist.largeImage ?: matchedArtist.mediumImage ?: matchedArtist.image

        val finalImage = tentativeImage?.takeIf {
            it.isNotBlank() && !it.contains("/images/artist//")
        }
        return (finalImage != null) to finalImage
    }

    /**
     * Returns filtered candidate images for the manual picker grid.
     * Each entry is a Pair of (artistName, bestImageUrl).
     * Only artists whose names match the query are included.
     * Only the highest-quality image (xl) is returned per artist.
     */
    fun getFilteredCandidates(requestedName: String): List<Pair<String, String>> {
        if (result.isEmpty()) return emptyList()

        val resolvedAlias = ARTIST_ALIASES[requestedName.trim().lowercase()] ?: requestedName
        val normRequested = resolvedAlias.normalize().lowercase()
        val primaryName = resolvedAlias.split(Regex("(?i)\\s+(feat\\.|ft\\.|with|&|,|/)\\s+")).firstOrNull()?.trim() ?: resolvedAlias
        val normPrimary = primaryName.normalize().lowercase()

        val seenImageHashes = mutableSetOf<String>()
        val candidates = mutableListOf<Pair<String, String>>()

        for (artist in result) {
            val normArtist = artist.artistName.normalize().lowercase()

            // Check if this artist matches the query
            val isExactOrPrefix = normArtist == normRequested || normArtist == normPrimary ||
                    normArtist.startsWith(normPrimary) || normPrimary.startsWith(normArtist)
            val isSubstringMatch = (normPrimary.length >= 5 && normArtist.contains(normPrimary)) ||
                    (normArtist.length >= 5 && normRequested.contains(normArtist))
            val jwScore = JW_SIMILARITY.apply(normArtist, normRequested)

            val isMatch = isExactOrPrefix || (isSubstringMatch && jwScore >= 0.60) || jwScore >= 0.70

            if (!isMatch) continue

            // Take only the highest quality image
            val bestUrl = artist.xlImage ?: artist.largeImage ?: artist.mediumImage ?: artist.image
            if (bestUrl.isNullOrBlank() || bestUrl.contains("/images/artist//")) continue

            // Deduplicate by image hash (the path segment before resolution)
            val imageHash = extractImageHash(bestUrl)
            if (imageHash != null && !seenImageHashes.add(imageHash)) continue

            candidates.add(artist.artistName to bestUrl)
        }

        return candidates
    }

    @Serializable
    class Result(
        @SerialName("name")
        val artistName: String,
        @SerialName("picture")
        val image: String? = null,
        @SerialName("picture_small")
        val smallImage: String? = null,
        @SerialName("picture_medium")
        val mediumImage: String? = null,
        @SerialName("picture_big")
        val largeImage: String? = null,
        @SerialName("picture_xl")
        val xlImage: String? = null
    )

    companion object {
        val JW_SIMILARITY = JaroWinklerSimilarity()

        /**
         * Extracts the unique image hash from a Deezer image URL.
         * Deezer URLs follow the pattern: .../images/artist/{hash}/{resolution}-...
         * We extract {hash} to deduplicate different resolution variants of the same photo.
         */
        private fun extractImageHash(url: String): String? {
            val regex = Regex("/images/artist/([^/]+)/")
            return regex.find(url)?.groupValues?.getOrNull(1)
        }

        val ARTIST_ALIASES = mapOf(
            "biggie smalls" to "the notorious b.i.g.",
            "biggie" to "the notorious b.i.g.",
            "notorious big" to "the notorious b.i.g.",
            "cold play" to "coldplay",
            "snoop dog" to "snoop dogg",
            "snoop doggy dogg" to "snoop dogg",
            "2 pac" to "2pac",
            "tupac shakur" to "2pac",
            "dr dre" to "dr. dre",
            "fifty cent" to "50 cent"
        )
    }
}