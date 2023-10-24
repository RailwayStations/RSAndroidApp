package de.bahnhoefe.deutschlands.bahnhofsfotos.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Locale

object FileUtils {
    private val TAG = FileUtils::class.java.simpleName

    /**
     * Get the base directory for storing fotos
     *
     * @return the File denoting the base directory or null, if cannot write to it
     */
    private fun getLocalFotoDir(context: Context): File? {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?.let { mkdirs(it) }
    }

    private fun mkdirs(dir: File): File? {
        try {
            Files.createDirectories(dir.toPath())
            return dir
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create directory structure " + dir.absolutePath, e)
        }
        return null
    }

    /**
     * Get the file path for storing this stations foto
     *
     * @return the File
     */
    fun getStoredMediaFile(context: Context, uploadId: Long?): File? {
        val mediaStorageDir = getLocalFotoDir(context)
            ?: return null
        val storeMediaFile =
            File(mediaStorageDir, String.format(Locale.ENGLISH, "%d.jpg", uploadId))
        Log.d(TAG, "StoredMediaFile: $storeMediaFile")
        return storeMediaFile
    }

    fun getImageCacheFile(applicationContext: Context, imageId: String): File {
        val imagePath = File(applicationContext.cacheDir, "images")
        mkdirs(imagePath)
        return File(imagePath, "$imageId.jpg")
    }

    fun deleteQuietly(file: File?) {
        try {
            Files.delete(file!!.toPath())
        } catch (exception: IOException) {
            Log.w(TAG, "unable to delete file $file", exception)
        }
    }
}