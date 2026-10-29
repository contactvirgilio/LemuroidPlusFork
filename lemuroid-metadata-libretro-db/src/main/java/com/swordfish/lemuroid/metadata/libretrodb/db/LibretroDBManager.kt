package com.swordfish.lemuroid.metadata.libretrodb.db

import android.content.Context
import androidx.room.Room

class LibretroDBManager(private val context: Context) {
    companion object {
        private const val DB_NAME = "libretro-db"
        private const val ARCADE_MAP_ASSET = "arcade_map.txt"
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
            .build()
    }
}
