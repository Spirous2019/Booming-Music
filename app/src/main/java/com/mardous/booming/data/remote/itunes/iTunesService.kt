package com.mardous.booming.data.remote.itunes

import com.mardous.booming.data.remote.itunes.model.iTunesArtist
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class iTunesService(private val client: HttpClient) {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    suspend fun searchArtist(artistName: String, limit: Int = 25): iTunesArtist {
        val rawText: String = client.get("https://itunes.apple.com/search") {
            parameter("media", "music")
            parameter("entity", "album")
            parameter("limit", limit)
            parameter("term", artistName)
            header(HttpHeaders.UserAgent, userAgent)
        }.body()

        return json.decodeFromString<iTunesArtist>(rawText)
    }

    suspend fun searchArtistMusic(artistName: String, limit: Int = 25): iTunesArtist {
        return searchArtist(artistName, limit)
    }
}
