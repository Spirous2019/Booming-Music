package com.mardous.booming.coil.fetcher

import android.content.ContentResolver
import android.content.SharedPreferences
import android.webkit.MimeTypeMap
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.mardous.booming.coil.CustomArtistImageManager
import com.mardous.booming.coil.model.ArtistImage
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.util.ImageSize
import com.mardous.booming.util.PREFERRED_IMAGE_SIZE
import com.mardous.booming.util.Preferences.requireString
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import kotlin.math.min

class ArtistImageFetcher(
    private val loader: ImageLoader,
    private val options: Options,
    private val customImageManager: CustomArtistImageManager,
    private val repository: Repository,
    private val image: ArtistImage,
    private val imageSize: String
) : Fetcher {

    companion object {
        // Maximum 4 queries per artist
        private const val MAX_RESULT_PER_PAGE = 5
        private const val MAX_RESULT_COUNT = 20
    }

    private val contentResolver: ContentResolver
        get() = options.context.contentResolver

    override suspend fun fetch(): FetchResult? {
        if (customImageManager.isNoImage(image)) {
            return null
        }

        if (customImageManager.hasCustomImage(image)) {
            val imageFile = customImageManager.getCustomImageFile(image)
            if (imageFile?.isFile == true) {
                return SourceFetchResult(
                    source = ImageSource(imageFile.toOkioPath(), options.fileSystem),
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(imageFile.extension),
                    dataSource = DataSource.DISK
                )
            }
        }

        if (!image.isNameUnknown && NetworkFeature.Images.Artists.isAvailable(options.context)) {
            val resolvedName = com.mardous.booming.data.remote.deezer.model.DeezerArtist.ARTIST_ALIASES[image.name.trim().lowercase()] ?: image.name
            val cleanName = resolvedName.split(Regex("(?i)\\s+(feat\\.|ft\\.|with|&|,|/)\\s+")).firstOrNull()?.trim() ?: resolvedName

            // 1. Try Wikipedia Gallery Photos (Verified authentic press/concert photo)
            val wikiPortraits = repository.wikimediaArtistPortraits(cleanName).ifEmpty { repository.wikimediaArtistPortraits(resolvedName) }
            val wikiUrl = wikiPortraits.firstOrNull()?.second
            if (wikiUrl != null && wikiUrl.startsWith("http")) {
                val saved = customImageManager.setCustomImageFromUrl(image, wikiUrl)
                if (saved) {
                    val imageFile = customImageManager.getCustomImageFile(image)
                    if (imageFile?.isFile == true) {
                        return SourceFetchResult(
                            source = ImageSource(imageFile.toOkioPath(), options.fileSystem),
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(imageFile.extension),
                            dataSource = DataSource.DISK
                        )
                    }
                }
            }

            // 2. Try DuckDuckGo Web Portrait Search
            val duckPortraits = repository.duckDuckGoArtistPortraits(cleanName).ifEmpty { repository.duckDuckGoArtistPortraits(resolvedName) }
            val duckUrl = duckPortraits.firstOrNull()?.second
            if (duckUrl != null && duckUrl.startsWith("http")) {
                val saved = customImageManager.setCustomImageFromUrl(image, duckUrl)
                if (saved) {
                    val imageFile = customImageManager.getCustomImageFile(image)
                    if (imageFile?.isFile == true) {
                        return SourceFetchResult(
                            source = ImageSource(imageFile.toOkioPath(), options.fileSystem),
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(imageFile.extension),
                            dataSource = DataSource.DISK
                        )
                    }
                }
            }

            // 3. Fallback to Deezer Avatar
            var pageIndex = 0
            var revisedResults = 0
            var deezerArtist = repository.deezerArtist(cleanName, MAX_RESULT_PER_PAGE, pageIndex)
            if (deezerArtist == null || deezerArtist.result.isEmpty()) {
                deezerArtist = repository.deezerArtist(resolvedName, MAX_RESULT_PER_PAGE, pageIndex)
            }
            val total = min(deezerArtist?.total ?: 0, MAX_RESULT_COUNT)
            while (deezerArtist != null && revisedResults < total) {
                val (matched, imageUrl) = deezerArtist.getBestImage(resolvedName, imageSize)
                if (matched && imageUrl != null && imageUrl.startsWith("http") && !imageUrl.contains("/images/artist//")) {
                    val saved = customImageManager.setCustomImageFromUrl(image, imageUrl)
                    if (saved) {
                        val imageFile = customImageManager.getCustomImageFile(image)
                        if (imageFile?.isFile == true) {
                            return SourceFetchResult(
                                source = ImageSource(imageFile.toOkioPath(), options.fileSystem),
                                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(imageFile.extension),
                                dataSource = DataSource.DISK
                            )
                        }
                    }
                }
                revisedResults += deezerArtist.result.size
                if (revisedResults < total) {
                    deezerArtist = repository.deezerArtist(cleanName, min((total - revisedResults), MAX_RESULT_PER_PAGE), ++pageIndex)
                } else {
                    break
                }
            }
        }

        // Namida approach: Never fallback to album art for artist portraits!
        // Return null to display clean default circular artist placeholder icon
        return null
    }

    class Factory(
        private val preferences: SharedPreferences,
        private val customImageManager: CustomArtistImageManager,
        private val repository: Repository
    ) : Fetcher.Factory<ArtistImage> {
        override fun create(
            data: ArtistImage,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return ArtistImageFetcher(
                loader = imageLoader,
                options = options,
                customImageManager = customImageManager,
                repository = repository,
                image = data,
                imageSize = preferences.requireString(PREFERRED_IMAGE_SIZE, ImageSize.MEDIUM)
            )
        }
    }
}