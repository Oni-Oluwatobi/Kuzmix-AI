package com.kuzmix.ai.domain.ai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * Copies an imported model file (.task / .gguf) from a SAF Uri into the
 * app's private models directory so MediaPipe can load it by path.
 * Everything stays on-device; no network is involved.
 */
class ModelFileManager(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun importFromUri(uri: Uri): File {
        val name = queryDisplayName(uri) ?: "imported_model.task"
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(modelsDir, safe)

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open the selected file")
        input.use { src ->
            target.outputStream().use { dst -> src.copyTo(dst, BUFFER) }
        }
        if (target.length() < MIN_BYTES) {
            target.delete()
            throw IOException("File is too small to be a model")
        }
        return target
    }

    fun delete(path: String): Boolean = try {
        val f = File(path)
        f.exists() && f.delete()
    } catch (t: Throwable) {
        Log.w(TAG, "model delete failed", t)
        false
    }

    fun exists(path: String): Boolean = File(path).exists()

    fun describe(path: String): String? {
        val f = File(path)
        if (!f.exists()) return null
        val mb = f.length() / (1024.0 * 1024.0)
        return "%s (%.1f MB)".format(f.name, mb)
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME),
            null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (t: Throwable) {
        Log.w(TAG, "display name query failed", t)
        null
    }

    companion object {
        private const val TAG = "ModelFileManager"
        private const val BUFFER = 1024 * 1024
        private const val MIN_BYTES = 50L * 1024 * 1024
    }
}
