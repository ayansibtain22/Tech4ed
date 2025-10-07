package com.lms.ayan.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object UriUtils {

    /** For private app files; use when the audio lives in filesDir/cacheDir/getExternalFilesDir. */
    fun File.asContentUri(context: Context): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)

    /**
     * Publish into MediaStore (Music) and return the public content:// URI (API 29+).
     * Why: visible to music/voice apps and survives app uninstall.
     */
    fun addAudioToMediaStore(
        context: Context,
        source: File,
        displayName: String = source.nameWithoutExtension,
        mime: String = "audio/m4a",
        relativeDir: String = Environment.DIRECTORY_MUSIC + "/Recordings"
    ): Uri? {
        if (Build.VERSION.SDK_INT < 29) return null
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$displayName.${source.extension}")
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, relativeDir)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: return null
        try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            // Clean on failure
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
    }

    /** Share a file via chooser using FileProvider URI. */
    fun shareAudio(context: Context, file: File, mime: String = "audio/m4a") {
        val uri = file.asContentUri(context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share recording"))
    }
}