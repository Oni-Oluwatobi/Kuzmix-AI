package com.kuzmix.ai.data.secret

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider

/**
 * Creates the study-guide .docx and hands it to whatever Office viewer the
 * device has (Microsoft Word, WPS Office, Google Docs, …) via FileProvider.
 * Returns false when no viewer is installed.
 */
object StudyNotesOpener {

    const val DOCX_MIME =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    fun open(context: Context, classKey: String): Boolean {
        val json = context.assets.open("secret/$classKey.json")
            .bufferedReader().use { it.readText() }
        val file = GuideDocxBuilder.build(context, classKey, json)
        val uri = FileProvider.getUriForFile(
            context, context.packageName + ".fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, DOCX_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            false
        }
    }
}
