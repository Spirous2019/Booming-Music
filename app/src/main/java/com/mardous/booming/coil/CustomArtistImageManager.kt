package com.mardous.booming.coil

import android.content.Context
import android.net.Uri
import android.provider.MediaStore.Audio.Artists
import android.util.Log
import androidx.core.content.edit
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.toBitmap
import com.mardous.booming.coil.model.ArtistImage
import com.mardous.booming.data.model.Artist
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.extensions.utilities.sanitize
import com.mardous.booming.util.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CustomArtistImageManager(private val context: Context) {

    private val coroutineScope = MainScope()
    private val contentResolver get() = context.contentResolver
    private val imagesPreferences by lazy {
        context.getSharedPreferences("custom_artist_images", Context.MODE_PRIVATE)
    }
    private val signaturesPreferences by lazy {
        context.getSharedPreferences("artist_signatures", Context.MODE_PRIVATE)
    }

    private fun getCanonicalName(name: String): String =
        name.trim().lowercase(Locale.US)

    private fun getCanonicalFileName(name: String): String =
        "${name.trim().lowercase(Locale.US).sanitize()}.jpeg"

    fun hasCustomImage(name: String): Boolean {
        val canonical = getCanonicalName(name)
        if (imagesPreferences.getBoolean(canonical, false)) return true
        val dir = FileUtil.customArtistImagesDirectory() ?: return false
        val existing = findExistingFile(name, dir)
        if (existing != null && existing.isFile && existing.length() > 0) {
            imagesPreferences.edit(true) { putBoolean(canonical, true) }
            return true
        }
        return false
    }

    fun hasCustomImage(image: ArtistImage): Boolean = hasCustomImage(image.name)
    fun hasCustomImage(artist: Artist): Boolean = hasCustomImage(artist.name)

    fun isNoImage(name: String): Boolean =
        imagesPreferences.getBoolean("no_image_" + getCanonicalName(name), false)

    fun isNoImage(image: ArtistImage): Boolean = isNoImage(image.name)
    fun isNoImage(artist: Artist): Boolean = isNoImage(artist.name)

    fun setNoImage(name: String, noImage: Boolean) {
        val canonical = getCanonicalName(name)
        imagesPreferences.edit(true) {
            putBoolean("no_image_$canonical", noImage)
        }
        if (noImage) {
            val dir = FileUtil.customArtistImagesDirectory()
            if (dir != null) {
                deleteArtistFiles(name, dir)
            }
        }
        signaturesPreferences.edit(true) {
            putLong(canonical, System.currentTimeMillis())
            putLong(name, System.currentTimeMillis())
        }
        clearCoilMemoryCache()
        contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)
    }

    fun setNoImage(artist: Artist, noImage: Boolean) {
        setNoImage(artist.name, noImage)
    }

    fun getSignature(name: String): String {
        val canonical = getCanonicalName(name)
        val sig = signaturesPreferences.getLong(canonical, signaturesPreferences.getLong(name, 0))
        return sig.toString()
    }

    fun getSignature(image: ArtistImage): String = getSignature(image.name)
    fun getSignature(artist: Artist): String = getSignature(artist.name)

    fun getCustomImageFile(name: String): File? {
        val dir = FileUtil.customArtistImagesDirectory() ?: return null
        return findExistingFile(name, dir) ?: File(dir, getCanonicalFileName(name))
    }

    fun getCustomImageFile(image: ArtistImage): File? = getCustomImageFile(image.name)
    fun getCustomImageFile(artist: Artist): File? = getCustomImageFile(artist.name)

    suspend fun setCustomImage(artist: Artist, uri: Uri): Boolean {
        return try {
            suspendCancellableCoroutine { continuation ->
                SingletonImageLoader.get(context).enqueue(
                    ImageRequest.Builder(context)
                        .data(uri)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .size(2048)
                        .target(
                            onSuccess = { drawable ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    val dir = FileUtil.customArtistImagesDirectory()
                                    if (dir == null) {
                                        continuation.resume(false)
                                        return@launch
                                    }
                                    deleteArtistFiles(artist.name, dir)
                                    val imageFile = File(dir, getCanonicalFileName(artist.name))
                                    try {
                                        val imageCreated = imageFile.outputStream()
                                            .buffered()
                                            .use { stream ->
                                                drawable.toBitmap().toJPG(100, stream)
                                            }

                                        updateHasImage(artist.name, imageCreated)
                                        clearCoilMemoryCache()
                                        contentResolver.notifyChange(
                                            Artists.EXTERNAL_CONTENT_URI,
                                            null
                                        )

                                        if (!imageCreated) {
                                            imageFile.deleteQuietly()
                                        }

                                        continuation.resume(imageCreated)
                                    } catch (t: Throwable) {
                                        imageFile.deleteQuietly()
                                        continuation.resumeWithException(t)
                                    }
                                    continuation.invokeOnCancellation {
                                        imageFile.deleteQuietly()
                                    }
                                }
                            },
                            onError = {
                                continuation.resume(false)
                            }
                        )
                        .build()
                )
            }
        } catch (t: Throwable) {
            Log.e("CustomArtistImageManager", "Cannot set artist image", t)
            false
        }
    }

    suspend fun setCustomImageFromUrl(name: String, imageUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = FileUtil.customArtistImagesDirectory() ?: return@withContext false
            deleteArtistFiles(name, dir)
            val imageFile = File(dir, getCanonicalFileName(name))
            val connection = java.net.URL(imageUrl).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.getInputStream().use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            updateHasImage(name, true)
            clearCoilMemoryCache()
            contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)
            true
        } catch (e: Exception) {
            Log.e("CustomArtistImageManager", "Failed to save artist image from URL: $imageUrl", e)
            false
        }
    }

    suspend fun setCustomImageFromUrl(image: ArtistImage, imageUrl: String): Boolean =
        setCustomImageFromUrl(image.name, imageUrl)

    suspend fun setCustomImageFromUrl(artist: Artist, imageUrl: String): Boolean =
        setCustomImageFromUrl(artist.name, imageUrl)

    suspend fun removeCustomImage(artist: Artist): Boolean = withContext(Dispatchers.IO) {
        updateHasImage(artist.name, false)
        val dir = FileUtil.customArtistImagesDirectory()
        if (dir != null) {
            deleteArtistFiles(artist.name, dir)
        }
        clearCoilMemoryCache()
        contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)
        true
    }

    suspend fun resetAllArtistImages(): Boolean = withContext(Dispatchers.IO) {
        try {
            imagesPreferences.edit(true) { clear() }
            signaturesPreferences.edit(true) { clear() }

            FileUtil.customArtistImagesDirectory()?.listFiles()?.forEach { file ->
                file.deleteQuietly()
            }

            clearCoilMemoryCache(clearDisk = true)
            contentResolver.notifyChange(Artists.EXTERNAL_CONTENT_URI, null)
            true
        } catch (e: Exception) {
            Log.e("CustomArtistImageManager", "Failed to reset all artist images", e)
            false
        }
    }

    private fun updateHasImage(name: String, hasImage: Boolean) {
        val canonical = getCanonicalName(name)
        imagesPreferences.edit(true) {
            putBoolean(canonical, hasImage)
            if (hasImage) {
                remove("no_image_$canonical")
            }
        }
        signaturesPreferences.edit(true) {
            putLong(canonical, System.currentTimeMillis())
            putLong(name, System.currentTimeMillis())
        }
    }

    private fun clearCoilMemoryCache(clearDisk: Boolean = false) {
        try {
            val loader = SingletonImageLoader.get(context)
            loader.memoryCache?.clear()
            if (clearDisk) {
                loader.diskCache?.clear()
            }
        } catch (_: Exception) {}
    }

    private fun findExistingFile(name: String, dir: File): File? {
        val canonicalName = getCanonicalFileName(name)
        val canonicalFile = File(dir, canonicalName)
        if (canonicalFile.isFile && canonicalFile.length() > 0) {
            return canonicalFile
        }

        val sanitizedRawName = name.sanitize()
        val allFiles = dir.listFiles() ?: return null
        val matched = allFiles.firstOrNull { file ->
            val fileName = file.name
            fileName.equals(canonicalName, ignoreCase = true) ||
            fileName.equals("$sanitizedRawName.jpeg", ignoreCase = true) ||
            (fileName.startsWith("#") && fileName.endsWith("#$sanitizedRawName.jpeg", ignoreCase = true))
        }

        if (matched != null && matched.isFile && matched.length() > 0) {
            try {
                if (matched.absolutePath != canonicalFile.absolutePath) {
                    matched.copyTo(canonicalFile, overwrite = true)
                    matched.deleteQuietly()
                }
            } catch (_: Exception) {}
            return canonicalFile
        }

        return null
    }

    private fun deleteArtistFiles(name: String, dir: File) {
        val canonicalName = getCanonicalFileName(name)
        val sanitizedRawName = name.sanitize()
        val allFiles = dir.listFiles() ?: return
        for (file in allFiles) {
            val fileName = file.name
            if (fileName.equals(canonicalName, ignoreCase = true) ||
                fileName.equals("$sanitizedRawName.jpeg", ignoreCase = true) ||
                (fileName.startsWith("#") && fileName.endsWith("#$sanitizedRawName.jpeg", ignoreCase = true))) {
                file.deleteQuietly()
            }
        }
    }

    private fun File.deleteQuietly() = try {
        this.delete()
    } catch (e: IOException) {
        Log.e("CustomArtistImageManager", "Unable to delete file $this", e)
        false
    }
}