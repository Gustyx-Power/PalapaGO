package id.gustyxganteng.agent.domain.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import java.io.File

object WorkspacePathResolver {

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun getAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun normalizePath(rawPath: String): String {
        val trimmed = rawPath.trim()
        val normalized = when {
            trimmed == "/sdcard" || trimmed == "/mnt/sdcard" -> "/storage/emulated/0"
            trimmed.startsWith("/sdcard/") -> "/storage/emulated/0/" + trimmed.removePrefix("/sdcard/")
            trimmed.startsWith("/mnt/sdcard/") -> "/storage/emulated/0/" + trimmed.removePrefix("/mnt/sdcard/")
            else -> trimmed
        }
        return normalized.trimEnd('/')
    }

    fun resolveTreeUriToPath(context: Context, treeUri: Uri): String? {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: Exception) {}

        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            if (docId.startsWith("primary:")) {
                val relPath = docId.removePrefix("primary:")
                normalizePath("/storage/emulated/0/$relPath")
            } else if (docId.contains(":")) {
                val parts = docId.split(":")
                val volume = parts[0]
                val subPath = if (parts.size > 1) parts[1] else ""
                normalizePath("/storage/$volume/$subPath")
            } else {
                normalizePath("/storage/emulated/0/$docId")
            }
        } catch (e: Exception) {
            normalizePath(treeUri.path ?: "/storage/emulated/0")
        }
    }

    fun createDefaultProjectFolder(folderName: String): File {
        val baseDir = File("/storage/emulated/0/Download/GGAgent")
        baseDir.mkdirs()
        val sanitized = folderName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val projectDir = File(baseDir, sanitized)
        projectDir.mkdirs()
        return projectDir
    }
}
