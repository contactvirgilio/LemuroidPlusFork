/*
 * Game.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.library.db.entity

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "games",
    indices = [
        Index("id", unique = true),
        Index("fileUri", unique = true),
        Index("title"),
        Index("systemId"),
        Index("lastIndexedAt"),
        Index("lastPlayedAt"),
        Index("isFavorite"),
    ],
)
data class Game(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fileName: String,
    val fileUri: String,
    val title: String,
    val systemId: String,
    val developer: String?,
    // Kept only for database compatibility: box art is no longer fetched or shown.
    val coverFrontUrl: String? = null,
    val lastIndexedAt: Long,
    val lastPlayedAt: Long? = null,
    val isFavorite: Boolean = false,
    val customName: String? = null,
    // Kept only for database compatibility: custom artwork is no longer supported.
    val customCoverUri: String? = null,
) : Serializable {
    /**
     * Name shown in the UI. A custom name is shown as-is; otherwise the title is shown without
     * tags in brackets such as "(USA)", "(World)", "(Rev 1)" or "[!]".
     */
    fun displayName(): String = customName ?: cleanTitle(title)

    companion object {
        private val BRACKET_TAGS = Regex("""\s*(\([^)]*\)|\[[^\]]*\])""")
        private val EXTRA_SPACES = Regex("\\s{2,}")

        fun cleanTitle(title: String): String {
            val cleaned =
                title.replace(BRACKET_TAGS, "")
                    .replace(EXTRA_SPACES, " ")
                    .trim()
                    .trimEnd('-', ',', ' ')
            return cleaned.ifBlank { title }
        }

        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<Game>() {
                override fun areItemsTheSame(
                    oldItem: Game,
                    newItem: Game,
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Game,
                    newItem: Game,
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}
