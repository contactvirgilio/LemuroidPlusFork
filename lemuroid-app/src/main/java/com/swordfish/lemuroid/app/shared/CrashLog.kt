package com.swordfish.lemuroid.app.shared

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lemuroid Plus crash log (2026-11-22): when something crashes, a text file with what happened is
 * written to a "LemuroidPlus logs" folder inside the games folder (or the app's own files if no games
 * folder is set). Nothing runs until a crash happens, so it costs nothing while playing.
 */
object CrashLog {
    private const val FOLDER = "LemuroidPlus logs"
    private const val KEEP = 10

    @Volatile
    var currentGame: String? = null

    /** Catches crashes in any process the app runs, then lets Android handle them as before. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            write(app, error, "Crash in ${thread.name}")
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(
        context: Context,
        error: Throwable,
        where: String,
    ) {
        runCatching {
            val now = Date()
            val text = report(context, error, where, now)
            val name = "crash-" + SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(now) + ".txt"
            if (!writeToGamesFolder(context, name, text)) writeToAppFiles(context, name, text)
        }
    }

    private fun report(
        context: Context,
        error: Throwable,
        where: String,
        now: Date,
    ): String {
        val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
        val version =
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
        return buildString {
            appendLine("Lemuroid Plus crash log")
            appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(now)}")
            appendLine("App: $version")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Where: $where")
            currentGame?.let { appendLine("Game: $it") }
            appendLine()
            append(trace)
        }
    }

    private fun writeToGamesFolder(
        context: Context,
        name: String,
        text: String,
    ): Boolean {
        val prefs = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val treeUri =
            prefs.getString(context.getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder), null)
                ?.takeIf { it.isNotBlank() } ?: return false
        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return false
        val folder = root.findFile(FOLDER) ?: root.createDirectory(FOLDER) ?: return false
        val file = folder.createFile("text/plain", name) ?: return false
        context.contentResolver.openOutputStream(file.uri)?.use { it.write(text.toByteArray()) } ?: return false
        // Keep only the newest logs.
        folder.listFiles().filter { it.name?.startsWith("crash-") == true }
            .sortedByDescending { it.name }
            .drop(KEEP)
            .forEach { it.delete() }
        return true
    }

    private fun writeToAppFiles(
        context: Context,
        name: String,
        text: String,
    ) {
        val folder = File(context.getExternalFilesDir(null) ?: context.filesDir, FOLDER).apply { mkdirs() }
        File(folder, name).writeText(text)
        folder.listFiles()?.sortedByDescending { it.name }?.drop(KEEP)?.forEach { it.delete() }
    }
}
