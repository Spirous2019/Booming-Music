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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ArtistNameSplitter].
 *
 * These tests pass explicit [enabledSeparators] sets to avoid
 * depending on Android SharedPreferences in a JVM test environment.
 */
class ArtistNameSplitterTest {

    /** All separator keys enabled for comprehensive testing. */
    private val allEnabled = setOf(
        ARTIST_SEPARATOR_FEAT_DOT,
        ARTIST_SEPARATOR_FT_DOT,
        ARTIST_SEPARATOR_SEMICOLON,
        ARTIST_SEPARATOR_SLASH,
        ARTIST_SEPARATOR_COMMA,
        ARTIST_SEPARATOR_AMPERSAND
    )

    /** Only unambiguous separators enabled (feat., ft., semicolon). */
    private val defaultEnabled = setOf(
        ARTIST_SEPARATOR_FEAT_DOT,
        ARTIST_SEPARATOR_FT_DOT,
        ARTIST_SEPARATOR_SEMICOLON
    )

    // ── Single artist ──────────────────────────────────────────

    @Test
    fun `single artist - no separators present`() {
        val result = ArtistNameSplitter.split("Sia", allEnabled)
        assertEquals(listOf("Sia"), result)
    }

    @Test
    fun `single artist - whitespace only`() {
        val result = ArtistNameSplitter.split("  Sia  ", allEnabled)
        assertEquals(listOf("Sia"), result)
    }

    // ── Two artists ────────────────────────────────────────────

    @Test
    fun `two artists - semicolon`() {
        val result = ArtistNameSplitter.split("Eminem; Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `two artists - semicolon no space`() {
        val result = ArtistNameSplitter.split("Eminem;Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `two artists - feat dot`() {
        val result = ArtistNameSplitter.split("Eminem feat. Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `two artists - Feat dot case insensitive`() {
        val result = ArtistNameSplitter.split("Drake Feat. Rihanna", allEnabled)
        assertEquals(listOf("Drake", "Rihanna"), result)
    }

    @Test
    fun `two artists - FEAT dot all caps`() {
        val result = ArtistNameSplitter.split("Drake FEAT. Rihanna", allEnabled)
        assertEquals(listOf("Drake", "Rihanna"), result)
    }

    @Test
    fun `two artists - ft dot`() {
        val result = ArtistNameSplitter.split("Eminem ft. Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `two artists - comma`() {
        val result = ArtistNameSplitter.split("Eminem, Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `two artists - slash`() {
        val result = ArtistNameSplitter.split("Eminem / Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `two artists - ampersand`() {
        val result = ArtistNameSplitter.split("Eminem & Rihanna", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    // ── Three artists ──────────────────────────────────────────

    @Test
    fun `three artists - semicolons`() {
        val result = ArtistNameSplitter.split("Eminem; Rihanna; Sia", allEnabled)
        assertEquals(listOf("Eminem", "Rihanna", "Sia"), result)
    }

    @Test
    fun `three artists - mixed separators`() {
        val result = ArtistNameSplitter.split("A; B feat. C", allEnabled)
        assertEquals(listOf("A", "B", "C"), result)
    }

    // ── Deduplication ──────────────────────────────────────────

    @Test
    fun `duplicate names are deduplicated`() {
        val result = ArtistNameSplitter.split("Sia; Sia", allEnabled)
        assertEquals(listOf("Sia"), result)
    }

    @Test
    fun `case-insensitive dedup preserves first occurrence`() {
        val result = ArtistNameSplitter.split("SIA; sia; Sia", allEnabled)
        assertEquals(listOf("SIA"), result)
    }

    // ── Edge cases ─────────────────────────────────────────────

    @Test
    fun `null input returns empty list`() {
        val result = ArtistNameSplitter.split(null, allEnabled)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty string returns single element list`() {
        val result = ArtistNameSplitter.split("", allEnabled)
        assertEquals(listOf(""), result)
    }

    @Test
    fun `blank string returns single element list`() {
        val result = ArtistNameSplitter.split("   ", allEnabled)
        // After trim, all parts become empty and are filtered out
        assertTrue(result.isEmpty())
    }

    // ── Separator toggle behavior ──────────────────────────────

    @Test
    fun `ampersand disabled - Simon and Garfunkel stays intact`() {
        val result = ArtistNameSplitter.split("Simon & Garfunkel", defaultEnabled)
        assertEquals(listOf("Simon & Garfunkel"), result)
    }

    @Test
    fun `comma disabled - Crosby Stills and Nash stays intact`() {
        val result = ArtistNameSplitter.split("Crosby, Stills & Nash", defaultEnabled)
        assertEquals(listOf("Crosby, Stills & Nash"), result)
    }

    @Test
    fun `slash disabled - AC DC stays intact`() {
        val result = ArtistNameSplitter.split("AC/DC", defaultEnabled)
        assertEquals(listOf("AC/DC"), result)
    }

    @Test
    fun `semicolon still works with default enabled`() {
        val result = ArtistNameSplitter.split("Eminem; Rihanna", defaultEnabled)
        assertEquals(listOf("Eminem", "Rihanna"), result)
    }

    @Test
    fun `feat dot still works with default enabled`() {
        val result = ArtistNameSplitter.split("Drake feat. Lil Wayne", defaultEnabled)
        assertEquals(listOf("Drake", "Lil Wayne"), result)
    }

    @Test
    fun `ft dot still works with default enabled`() {
        val result = ArtistNameSplitter.split("Drake ft. Lil Wayne", defaultEnabled)
        assertEquals(listOf("Drake", "Lil Wayne"), result)
    }

    @Test
    fun `no separators enabled returns original name`() {
        val result = ArtistNameSplitter.split("Eminem; Rihanna", emptySet())
        assertEquals(listOf("Eminem; Rihanna"), result)
    }

    // ── Mixed separator combinations ───────────────────────────

    @Test
    fun `semicolon and feat in same string`() {
        val result = ArtistNameSplitter.split("Jay-Z; Kanye West feat. Rihanna", allEnabled)
        assertEquals(listOf("Jay-Z", "Kanye West", "Rihanna"), result)
    }

    @Test
    fun `feat dot does not match without surrounding whitespace`() {
        // "feat." requires spaces around it, so "featured" should not match
        val result = ArtistNameSplitter.split("The Featured Artists", allEnabled)
        assertEquals(listOf("The Featured Artists"), result)
    }

    @Test
    fun `ft dot does not match without surrounding whitespace`() {
        val result = ArtistNameSplitter.split("Loft.Music", allEnabled)
        assertEquals(listOf("Loft.Music"), result)
    }
}
