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

package com.mardous.booming.data.local.repository

import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import androidx.annotation.RequiresApi
import com.mardous.booming.core.sort.AlbumSortMode
import com.mardous.booming.core.sort.ArtistSortMode
import com.mardous.booming.data.local.MediaQueryDispatcher
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.utilities.collapseSpaces
import com.mardous.booming.util.ArtistNameSplitter
import com.mardous.booming.util.Preferences

interface ArtistRepository {
    fun artists(): List<Artist>
    fun artists(query: String): List<Artist>
    fun artist(artistId: Long, nameOverride: String? = null): Artist
    fun albumArtists(): List<Artist>
    fun albumArtist(artistName: String): Artist
    fun albumArtists(query: String): List<Artist>
    fun similarAlbumArtists(artist: Artist): List<Artist>
}

class RealArtistRepository(
    private val songRepository: RealSongRepository,
    private val albumRepository: RealAlbumRepository
) : ArtistRepository {

    private val filterSingles: Boolean
        get() = Preferences.ignoreSingles

    override fun artists(): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(null, null, DEFAULT_SORT_ORDER)
        )
        val minimumSongCount = Preferences.minimumSongCountForArtist
        val artists = splitIntoArtists(albumRepository.splitIntoAlbums(songs)).filter {
            it.songCount >= minimumSongCount
        }
        return sortArtists(artists)
    }

    override fun artist(artistId: Long, nameOverride: String?): Artist {
        if (artistId == Artist.VARIOUS_ARTISTS_ID) {
            // Get Various Artists
            val songs = songRepository.songs(
                songRepository.makeSongCursor(null, null, DEFAULT_SORT_ORDER)
            )
            val albums = with(AlbumSortMode.ArtistAlbums) {
                albumRepository.splitIntoAlbums(songs)
                    .filter { Artist.VARIOUS_ARTISTS_DISPLAY_NAME.equals(it.albumArtistName, ignoreCase = true) }
                    .sorted()
            }
            return Artist(Artist.VARIOUS_ARTISTS_ID, albums, filterSingles)
        }

        // Determine the individual artist name to use
        val artistName = nameOverride ?: run {
            // Fallback: try to determine from the first song
            val directSongs = songRepository.songs(
                songRepository.makeSongCursor(
                    AudioColumns.ARTIST_ID + "=?",
                    arrayOf(artistId.toString()),
                    DEFAULT_SORT_ORDER
                )
            )
            if (directSongs.isNotEmpty()) {
                val splitNames = ArtistNameSplitter.split(directSongs.first().artistName)
                if (splitNames.size == 1) splitNames.first() else null
            } else null
        }

        // Collect all songs that contain this artist name
        val allSongs = if (artistName != null) {
            // Search for songs where this artist name appears
            val matchedSongs = songRepository.songs(
                songRepository.makeSongCursor(
                    AudioColumns.ARTIST + " LIKE ?",
                    arrayOf("%$artistName%"),
                    DEFAULT_SORT_ORDER
                )
            ).filter { song ->
                // Only include songs where splitting reveals this exact artist name
                val parsed = ArtistNameSplitter.split(song.artistName)
                parsed.any { it.equals(artistName, ignoreCase = true) }
            }
            // Also include direct matches by artistId (for songs that weren't caught by LIKE)
            val directById = songRepository.songs(
                songRepository.makeSongCursor(
                    AudioColumns.ARTIST_ID + "=?",
                    arrayOf(artistId.toString()),
                    DEFAULT_SORT_ORDER
                )
            ).filter { song ->
                val parsed = ArtistNameSplitter.split(song.artistName)
                parsed.any { it.equals(artistName, ignoreCase = true) }
            }
            (matchedSongs + directById).distinctBy { it.id }
        } else {
            // No split name known: just load by ID (original behavior)
            songRepository.songs(
                songRepository.makeSongCursor(
                    AudioColumns.ARTIST_ID + "=?",
                    arrayOf(artistId.toString()),
                    DEFAULT_SORT_ORDER
                )
            )
        }

        val rawAlbums = albumRepository.splitIntoAlbums(
            songs = allSongs,
            sortMode = AlbumSortMode.ArtistAlbums
        )
        val mergedAlbums = if (artistName != null) mergeAlbumsByName(rawAlbums) else rawAlbums

        return Artist(
            id = artistId,
            albums = mergedAlbums,
            filterSingles = filterSingles,
            nameOverride = artistName
        )
    }

    override fun artists(query: String): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(AudioColumns.ARTIST + " LIKE ?", arrayOf("%$query%"), DEFAULT_SORT_ORDER)
        )
        val artists = splitIntoArtists(albumRepository.splitIntoAlbums(songs))
        // Filter to only include artists whose name actually matches the query
        val filtered = artists.filter { it.name.contains(query, ignoreCase = true) }
        return sortArtists(filtered)
    }

    override fun albumArtists(): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(null, null, "lower(${AudioColumns.ALBUM_ARTIST})")
        )
        val minimumSongCount = Preferences.minimumSongCountForArtist
        val albumArtists = splitIntoAlbumArtists(albumRepository.splitIntoAlbums(songs)).filter {
            it.songCount >= minimumSongCount
        }
        return sortArtists(albumArtists)
    }

    override fun albumArtist(artistName: String): Artist {
        if (Artist.VARIOUS_ARTISTS_DISPLAY_NAME.equals(artistName, ignoreCase = true)) {
            // Get Various Artists
            val songs = songRepository.songs(
                songRepository.makeSongCursor(null, null, DEFAULT_SORT_ORDER)
            )
            val albums = with(AlbumSortMode.ArtistAlbums) {
                albumRepository.splitIntoAlbums(songs)
                    .filter { Artist.VARIOUS_ARTISTS_DISPLAY_NAME.equals(it.albumArtistName, ignoreCase = true) }
                    .sorted()
            }
            return Artist(Artist.VARIOUS_ARTISTS_ID, albums, filterSingles, isAlbumArtist = true)
        }

        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                "lower(${AudioColumns.ALBUM_ARTIST})=?",
                arrayOf(artistName.lowercase()),
                DEFAULT_SORT_ORDER
            )
        )
        return Artist(
            artistName = artistName,
            albums = albumRepository.splitIntoAlbums(
                songs = songs,
                sortMode = AlbumSortMode.ArtistAlbums
            ),
            filterSingles = filterSingles
        )
    }

    override fun albumArtists(query: String): List<Artist> {
        val songs = songRepository.songs(
            songRepository.makeSongCursor(
                "${AudioColumns.ALBUM_ARTIST} LIKE ?",
                arrayOf("%$query%"),
                DEFAULT_SORT_ORDER
            )
        )
        val artists = splitIntoAlbumArtists(albumRepository.splitIntoAlbums(songs))
        // Filter to only include artists whose name actually matches the query
        val filtered = artists.filter { it.name.contains(query, ignoreCase = true) }
        return sortArtists(filtered)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun similarAlbumArtists(artist: Artist): List<Artist> {
        val genreNames = artist.songs.mapNotNull { it.genreName }.distinct()
        if (genreNames.isEmpty()) {
            return arrayListOf()
        }
        val selectionBuilder = StringBuilder("${AudioColumns.GENRE} IN(?")
        for (i in 1 until genreNames.size) {
            selectionBuilder.append(",?")
        }
        selectionBuilder.append(")")
        val songs = songRepository.makeSongCursor(
            MediaQueryDispatcher()
                .setProjection(RealSongRepository.getBaseProjection())
                .setSelection(selectionBuilder.toString())
                .setSelectionArguments(genreNames.toTypedArray())
                .addSelection("(${AudioColumns.ALBUM_ARTIST} NOT NULL AND ${AudioColumns.ALBUM_ARTIST} != ?)")
                .addArguments(artist.name)
        ).let {
            songRepository.songs(it)
        }
        return splitIntoAlbumArtists(albumRepository.splitIntoAlbums(songs, sorted = false)).take(MAX_SIMILAR_ARTISTS)
    }

    private fun splitIntoArtists(albums: List<Album>): List<Artist> {
        val filterSingles = this.filterSingles
        // Collect all songs from all albums
        val allSongs = albums.flatMap { it.songs }

        // Group songs by each individual parsed artist name
        val artistSongsMap = mutableMapOf<String, MutableList<Song>>() // lowercase key -> songs
        val artistDisplayNames = mutableMapOf<String, String>() // lowercase key -> first-seen display name
        val artistIds = mutableMapOf<String, Long>() // lowercase key -> artistId from first matching song

        for (song in allSongs) {
            val parsedNames = ArtistNameSplitter.split(song.artistName)
            for (parsedName in parsedNames) {
                val key = parsedName.lowercase()
                artistSongsMap.getOrPut(key) { mutableListOf() }.add(song)
                if (key !in artistDisplayNames) {
                    artistDisplayNames[key] = parsedName
                    artistIds[key] = song.artistId
                }
            }
        }

        // Build Artist objects from the grouped songs
        return artistSongsMap.map { (key, songs) ->
            val displayName = artistDisplayNames[key]!!
            val artistId = artistIds[key]!!
            val artistAlbums = albumRepository.splitIntoAlbums(songs)
            val mergedAlbums = mergeAlbumsByName(artistAlbums)
            val sortedAlbums = with(AlbumSortMode.ArtistAlbums) { mergedAlbums.sorted() }
            Artist(
                id = artistId,
                albums = sortedAlbums,
                filterSingles = filterSingles,
                nameOverride = displayName
            )
        }
    }

    fun splitIntoAlbumArtists(albums: List<Album>): List<Artist> {
        val filterSingles = this.filterSingles
        // Collect all songs from all albums
        val allSongs = albums.flatMap { it.songs }

        // Group songs by each individual parsed album artist name
        val artistSongsMap = mutableMapOf<String, MutableList<Song>>() // lowercase key -> songs
        val artistDisplayNames = mutableMapOf<String, String>() // lowercase key -> first-seen display name
        val artistIds = mutableMapOf<String, Long>() // lowercase key -> artistId from first matching song

        for (song in allSongs) {
            val albumArtistName = song.albumArtistName?.collapseSpaces()
            if (albumArtistName.isNullOrEmpty()) continue

            val parsedNames = ArtistNameSplitter.split(albumArtistName)
            for (parsedName in parsedNames) {
                val key = parsedName.lowercase()
                artistSongsMap.getOrPut(key) { mutableListOf() }.add(song)
                if (key !in artistDisplayNames) {
                    artistDisplayNames[key] = parsedName
                    artistIds[key] = song.artistId
                }
            }
        }

        // Build Artist objects from the grouped songs
        return artistSongsMap.map { (key, songs) ->
            val displayName = artistDisplayNames[key]!!
            val artistId = artistIds[key]!!
            val artistAlbums = albumRepository.splitIntoAlbums(songs)
            val sortedAlbums = with(AlbumSortMode.ArtistAlbums) { artistAlbums.sorted() }
            if (Artist.VARIOUS_ARTISTS_DISPLAY_NAME.equals(displayName, ignoreCase = true)) {
                Artist(Artist.VARIOUS_ARTISTS_ID, sortedAlbums, filterSingles, isAlbumArtist = true, nameOverride = displayName)
            } else {
                Artist(
                    id = artistId,
                    albums = sortedAlbums,
                    filterSingles = filterSingles,
                    isAlbumArtist = true,
                    nameOverride = displayName
                )
            }
        }
    }

    private fun sortArtists(artists: List<Artist>): List<Artist> {
        return with(ArtistSortMode.AllArtists) { artists.sorted() }
    }

    /**
     * Merges albums that share the same name into a single album entry.
     * This prevents duplicate album cards when songs from different MediaStore
     * album IDs have the same album name (common with multi-artist splits).
     */
    private fun mergeAlbumsByName(albums: List<Album>): List<Album> {
        return albums.groupBy { it.name.lowercase() }.map { (_, albumsWithSameName) ->
            if (albumsWithSameName.size == 1) {
                albumsWithSameName.first()
            } else {
                val primary = albumsWithSameName.first()
                val allSongs = albumsWithSameName.flatMap { it.songs }.distinctBy { it.id }
                Album(
                    id = primary.id,
                    artistName = primary.artistName,
                    albumArtistName = primary.albumArtistName,
                    year = primary.year,
                    songs = allSongs
                )
            }
        }
    }

    companion object {
        private const val MAX_SIMILAR_ARTISTS = 10
        const val DEFAULT_SORT_ORDER =
            MediaStore.Audio.Artists.ARTIST + ", " + MediaStore.Audio.Albums.ALBUM + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER
    }
}