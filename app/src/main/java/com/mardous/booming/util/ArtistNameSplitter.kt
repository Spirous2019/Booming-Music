/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.util

/**
 * Centralized utility for splitting compound artist name strings into
 * individual artist names.
 *
 * Supports configurable separators. Each separator can be independently
 * enabled or disabled via [Preferences].
 *
 * @author Christians M. A. (mardous)
 */
object ArtistNameSplitter {

    /**
     * Represents a separator that can split compound artist names.
     *
     * @param displayName Human-readable name shown in preferences UI
     * @param preferenceKey SharedPreferences key for the toggle
     * @param pattern Regex pattern used for splitting
     * @param defaultEnabled Whether this separator is enabled by default
     */
    data class Separator(
        val displayName: String,
        val preferenceKey: String,
        val pattern: Regex,
        val defaultEnabled: Boolean
    )

    /**
     * All supported separators, ordered by priority (longest/most specific first
     * to avoid substring conflicts).
     */
    val ALL_SEPARATORS: List<Separator> = listOf(
        Separator(
            displayName = "feat.",
            preferenceKey = ARTIST_SEPARATOR_FEAT_DOT,
            pattern = Regex("""\s+feat\.\s+""", RegexOption.IGNORE_CASE),
            defaultEnabled = true
        ),
        Separator(
            displayName = "ft.",
            preferenceKey = ARTIST_SEPARATOR_FT_DOT,
            pattern = Regex("""\s+ft\.\s+""", RegexOption.IGNORE_CASE),
            defaultEnabled = true
        ),
        Separator(
            displayName = "; (semicolon)",
            preferenceKey = ARTIST_SEPARATOR_SEMICOLON,
            pattern = Regex("""\s*;\s*"""),
            defaultEnabled = true
        ),
        Separator(
            displayName = "/ (slash)",
            preferenceKey = ARTIST_SEPARATOR_SLASH,
            pattern = Regex("""\s*/\s*"""),
            defaultEnabled = false
        ),
        Separator(
            displayName = ", (comma)",
            preferenceKey = ARTIST_SEPARATOR_COMMA,
            pattern = Regex("""\s*,\s*"""),
            defaultEnabled = false
        ),
        Separator(
            displayName = "& (ampersand)",
            preferenceKey = ARTIST_SEPARATOR_AMPERSAND,
            pattern = Regex("""\s*&\s*"""),
            defaultEnabled = false
        )
    )

    /**
     * Splits a raw artist name string into individual artist names using
     * all currently enabled separators.
     *
     * @param artistName The raw artist name string from metadata (e.g. "Eminem; Rihanna")
     * @param enabledSeparators The set of separator preference keys that are enabled.
     *                          If null, uses [Preferences] to determine which are enabled.
     * @return A list of unique, trimmed artist names. Never empty for non-null input;
     *         returns a single-element list if no separators match.
     *         Returns an empty list for null input.
     */
    @JvmStatic
    fun split(artistName: String?, enabledSeparators: Set<String>? = null): List<String> {
        if (artistName == null) return emptyList()
        if (artistName.isBlank()) return listOf(artistName)

        val enabled = enabledSeparators ?: getEnabledSeparatorKeys()
        val activeSeparators = ALL_SEPARATORS.filter { it.preferenceKey in enabled }

        if (activeSeparators.isEmpty()) return listOf(artistName)

        // Apply separators iteratively: split current fragments with each separator
        var fragments = listOf(artistName)
        for (separator in activeSeparators) {
            fragments = fragments.flatMap { fragment ->
                fragment.split(separator.pattern).map { it.trim() }
            }
        }

        // Remove empty strings and deduplicate (case-insensitive, preserving first occurrence)
        val seen = mutableSetOf<String>()
        return fragments.filter { it.isNotEmpty() && seen.add(it.lowercase()) }
    }

    /**
     * Reads enabled separator keys from [Preferences].
     */
    private fun getEnabledSeparatorKeys(): Set<String> {
        return Preferences.enabledArtistSeparators
    }
}
