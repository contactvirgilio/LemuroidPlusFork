package com.swordfish.lemuroid.app.shared.settings

import android.content.Context
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Keeps a copy of the app's settings in Lemuroid/settings/settings.json, so they survive
 * uninstalling the app, and restores it on the first launch after a reinstall.
 *
 * The live settings stay in the app's normal storage; this file is only a copy. It is written
 * when leaving the library, and read once, as soon as the app has "All files access" again.
 */
object SettingsSnapshot {
    private const val FILE_NAME = "settings.json"

    // Set once the restore check has run on this install. Before that the copy is never written,
    // so a fresh install can't overwrite the saved settings with its defaults.
    private const val RESTORE_CHECKED_KEY = "pref_key_settings_snapshot_checked"

    // The ROM folder needs Android's permission, which a reinstall loses: never restore it.
    private val EXCLUDED_KEYS = setOf("external_folder", "external_folder_2", "legacy_external_folder", RESTORE_CHECKED_KEY)

    fun settingsFolder(): File = File(DirectoriesManager.getPublicRootDirectory(), "settings")

    private fun snapshotFile() = File(settingsFolder(), FILE_NAME)

    /**
     * Restores the saved settings if this install hasn't checked yet. Returns true when settings
     * were restored (the caller should reload its screen).
     */
    fun restoreIfNeeded(context: Context): Boolean {
        if (!DirectoriesManager.hasPublicStorageAccess(context)) return false
        val preferences = SharedPreferencesHelper.getSharedPreferences(context)
        if (preferences.getBoolean(RESTORE_CHECKED_KEY, false)) return false

        val restored =
            runCatching {
                val file = snapshotFile()
                if (!file.isFile) return@runCatching false
                val json = JSONObject(file.readText())
                val editor = preferences.edit()
                json.keys().forEach { key ->
                    if (key in EXCLUDED_KEYS) return@forEach
                    val entry = json.optJSONObject(key) ?: return@forEach
                    when (entry.optString("type")) {
                        "boolean" -> editor.putBoolean(key, entry.getBoolean("value"))
                        "int" -> editor.putInt(key, entry.getInt("value"))
                        "long" -> editor.putLong(key, entry.getLong("value"))
                        "float" -> editor.putFloat(key, entry.getDouble("value").toFloat())
                        "string" -> editor.putString(key, entry.getString("value"))
                        "set" -> {
                            val array = entry.getJSONArray("value")
                            editor.putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                        }
                    }
                }
                editor.commit()
                true
            }.onFailure { Timber.e(it, "Cannot restore settings") }.getOrDefault(false)

        preferences.edit().putBoolean(RESTORE_CHECKED_KEY, true).commit()
        return restored
    }

    /** Writes the current settings to Lemuroid/settings/settings.json. */
    fun save(context: Context) {
        if (!DirectoriesManager.hasPublicStorageAccess(context)) return
        val preferences = SharedPreferencesHelper.getSharedPreferences(context)
        if (!preferences.getBoolean(RESTORE_CHECKED_KEY, false)) return

        runCatching {
            val json = JSONObject()
            preferences.all.forEach { (key, value) ->
                if (key in EXCLUDED_KEYS || value == null) return@forEach
                val entry =
                    when (value) {
                        is Boolean -> JSONObject().put("type", "boolean").put("value", value)
                        is Int -> JSONObject().put("type", "int").put("value", value)
                        is Long -> JSONObject().put("type", "long").put("value", value)
                        is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
                        is String -> JSONObject().put("type", "string").put("value", value)
                        is Set<*> -> {
                            val array = JSONArray(value.map { it.toString() })
                            JSONObject().put("type", "set").put("value", array)
                        }
                        else -> null
                    } ?: return@forEach
                json.put(key, entry)
            }
            val folder = settingsFolder().apply { mkdirs() }
            val temp = File(folder, "$FILE_NAME.tmp")
            temp.writeText(json.toString(2))
            if (!temp.renameTo(snapshotFile())) {
                snapshotFile().delete()
                temp.renameTo(snapshotFile())
            }
        }.onFailure { Timber.e(it, "Cannot save settings") }
    }
}
