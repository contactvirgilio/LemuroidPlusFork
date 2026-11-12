package com.swordfish.lemuroid.metadata.libretrodb.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

class LibretroDBManager(private val context: Context) {
    companion object {
        private const val DB_NAME = "libretro-db"
        private const val ARCADE_MAP_ASSET = "arcade_map.txt"
        private const val NOCASE_INDEX_SQL =
            "CREATE INDEX IF NOT EXISTS lemuroidplus_games_romName_nocase ON games(romName COLLATE NOCASE)"
    }

    /**
     * Arcade romset file name (e.g. "sf2.zip") to title, from Cannoli's arcade_map.txt (GPL-3.0).
     * Titles starting with "." are BIOS/system sets, not games.
     */
    val arcadeTitles: Map<String, String> by lazy {
        runCatching {
            context.assets.open(ARCADE_MAP_ASSET).bufferedReader().useLines { lines ->
                lines.filter { '\t' in it }
                    .associate { line ->
                        val (fileName, title) = line.split('\t', limit = 2)
                        fileName.trim().lowercase() to title.trim()
                    }
            }
        }.getOrDefault(emptyMap())
    }

    val dbInstance: LibretroDatabase by lazy {
        Room.databaseBuilder(context, LibretroDatabase::class.java, DB_NAME)
            .createFromAsset("libretro-db.sqlite")
            .fallbackToDestructiveMigration()
            .addCallback(
                object : RoomDatabase.Callback() {
                    // Looking a file name up ignoring letter case can't use the normal index and
                    // would read all ~100,000 entries for every new file. This index is built
                    // once (it is kept with the database) and makes those lookups instant.
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        runCatching { db.execSQL(NOCASE_INDEX_SQL) }
                            .onFailure { Timber.e(it, "Cannot create the file name index") }
                    }
                },
            )
            .build()
    }
}
