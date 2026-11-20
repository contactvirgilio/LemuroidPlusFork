package com.swordfish.lemuroid.app.shared.labels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.util.LruCache
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.swordfish.lemuroid.app.shared.game.NeoGeoSets
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Cartridge labels from the LaunchBox Games Database (TEST, 2026-11-20).
 *
 * The user points the app at LaunchBox's Metadata.zip (no account needed). It is read once, as a
 * stream, and only a small index is kept: for each game of a supported system, the file name of
 * its "Cart - Front" (or "Disc") picture. Pictures are downloaded the first time a game is shown
 * and kept in their own folder (files/labels), apart from screenshots and artwork.
 */
object CartLabels {
    private const val IMAGE_HOST = "https://images.launchbox-app.com/"

    /** LaunchBox platform name -> our key (system id, or "neogeo" for Neo Geo arcade sets). */
    private val PLATFORMS =
        mapOf(
            "Nintendo Entertainment System" to "nes",
            "Super Nintendo Entertainment System" to "snes",
            "Sega Genesis" to "md",
            "Sega Mega Drive" to "md",
            "Nintendo Game Boy" to "gb",
            "Nintendo Game Boy Color" to "gbc",
            "Nintendo Game Boy Advance" to "gba",
            "Nintendo 64" to "n64",
            "Sega Master System" to "sms",
            "Sony PSP" to "psp",
            "Nintendo DS" to "nds",
            "Sega Game Gear" to "gg",
            "Atari 2600" to "atari2600",
            "Sony Playstation" to "psx",
            "NEC TurboGrafx-16" to "pce",
            "NEC PC Engine" to "pce",
            "Atari Lynx" to "lynx",
            "Atari 7800" to "atari7800",
            "Sega CD" to "scd",
            "SNK Neo Geo Pocket" to "ngp",
            "SNK Neo Geo Pocket Color" to "ngc",
            "WonderSwan" to "ws",
            "WonderSwan Color" to "wsc",
            "SNK Neo Geo AES" to "neogeo",
            "Nintendo 3DS" to "3ds",
        )

    /** Systems whose media is a disc: the whole disc picture is used instead of a label. */
    private val DISC_SYSTEMS = setOf("psx", "scd", "psp")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloads = Semaphore(3)
    private val artCache = LruCache<String, ImageBitmap>(48)
    private val misses = HashSet<String>()

    /** Short status for Settings, e.g. "Reading… 1200 labels" or "1834 labels ready". */
    var status by mutableStateOf<String?>(null)
        private set

    /** Bumped when a new index is ready, so tiles look again. */
    var generation by mutableStateOf(0)
        private set

    @Volatile private var index: Map<String, String>? = null

    private fun folder(context: Context) = File(context.filesDir, "labels").apply { mkdirs() }

    private fun indexFile(context: Context) = File(folder(context), "index.tsv")

    fun isDisc(game: Game): Boolean = keyOf(game) in DISC_SYSTEMS

    private fun keyOf(game: Game): String {
        val arcade = game.systemId == "fbneo" || game.systemId == "mame2003plus"
        return if (arcade && NeoGeoSets.isNeoGeo(game.fileName)) "neogeo" else game.systemId
    }

    /** Lower case letters and digits only, without tags in brackets and a leading or trailing "the". */
    fun normalize(name: String): String {
        val plain =
            name.lowercase()
                .replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), " ")
                .replace("&", " and ")
                .replace(Regex(""",\s*the\b"""), " ")
                .replace(Regex("""^\s*the\s+"""), " ")
        return plain.filter { it.isLetterOrDigit() }
    }

    fun loadStatus(context: Context) {
        if (status != null) return
        val count = loadIndex(context)?.size ?: 0
        status = if (count > 0) "$count labels ready" else null
    }

    private fun loadIndex(context: Context): Map<String, String>? {
        index?.let { return it }
        val file = indexFile(context)
        if (!file.exists()) return null
        val map = HashMap<String, String>()
        file.forEachLine { line ->
            val parts = line.split('\t')
            if (parts.size == 2) map[parts[0]] = parts[1]
        }
        index = map
        return map
    }

    /** Reads LaunchBox's Metadata.zip in the background and keeps the small index. */
    fun import(
        context: Context,
        uri: Uri,
    ) {
        val app = context.applicationContext
        scope.launch {
            status = "Reading…"
            val result = runCatching { readZip(app, uri) }
            status =
                result.fold(
                    onSuccess = { "$it labels ready" },
                    onFailure = { "Couldn't read that file (${it.javaClass.simpleName})" },
                )
            synchronized(misses) { misses.clear() }
            artCache.evictAll()
            generation++
        }
    }

    private fun readZip(
        context: Context,
        uri: Uri,
    ): Int {
        // DatabaseID -> "key|name" for games of our systems.
        val games = HashMap<String, String>()
        // "key|name" -> (rank, file name): the best picture found so far.
        val best = HashMap<String, Pair<Int, String>>()

        context.contentResolver.openInputStream(uri)!!.use { raw ->
            val zip = ZipInputStream(raw.buffered())
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("Metadata.xml", ignoreCase = true)) {
                    parse(zip, games, best)
                    break
                }
                entry = zip.nextEntry
            }
        }

        val file = indexFile(context)
        file.bufferedWriter().use { out ->
            best.forEach { (key, value) -> out.write("$key\t${value.second}\n") }
        }
        index = best.mapValues { it.value.second }
        return best.size
    }

    private fun parse(
        input: java.io.InputStream,
        games: HashMap<String, String>,
        best: HashMap<String, Pair<Int, String>>,
    ) {
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")
        val fields = HashMap<String, String>()
        var record: String? = null
        var field: String? = null
        var seen = 0

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (record == null && (name == "Game" || name == "GameImage")) {
                        record = name
                        fields.clear()
                    } else if (record != null) {
                        field = name
                    }
                }
                XmlPullParser.TEXT -> {
                    val f = field
                    if (record != null && f != null) fields[f] = (fields[f] ?: "") + parser.text
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name == record) {
                        if (record == "Game") {
                            val key = PLATFORMS[fields["Platform"]?.trim()]
                            val id = fields["DatabaseID"]?.trim()
                            val title = fields["Name"]
                            if (key != null && id != null && title != null) {
                                games[id] = key + "|" + normalize(title)
                            }
                        } else {
                            addImage(fields, games, best)
                            if (++seen % 5000 == 0) status = "Reading… ${best.size} labels"
                        }
                        record = null
                    }
                    field = null
                }
            }
            event = parser.next()
        }
    }

    private fun addImage(
        fields: Map<String, String>,
        games: Map<String, String>,
        best: HashMap<String, Pair<Int, String>>,
    ) {
        val target = games[fields["DatabaseID"]?.trim()] ?: return
        val type = fields["Type"]?.trim() ?: return
        val disc = target.substringBefore('|') in DISC_SYSTEMS
        val wanted = if (disc) type == "Disc" else type == "Cart - Front"
        if (!wanted) return
        val fileName = fields["FileName"]?.trim() ?: return
        val rank =
            when (fields["Region"]?.trim()) {
                "North America", "United States" -> 0
                null, "", "World" -> 1
                "Europe" -> 2
                else -> 3
            }
        val current = best[target]
        if (current == null || rank < current.first) best[target] = rank to fileName
    }

    /**
     * The picture to draw for [game], or null: for cartridges the photo cropped and masked into
     * the label area ([labelRes], same size as the cartridge picture); for discs the disc photo.
     */
    suspend fun art(
        context: Context,
        game: Game,
        @DrawableRes labelRes: Int?,
    ): ImageBitmap? {
        val key = keyOf(game) + "|" + normalize(game.title)
        artCache.get(key)?.let { return it }
        if (synchronized(misses) { key in misses }) return null
        val disc = isDisc(game)
        if (!disc && labelRes == null) return null

        val photo =
            withContext(Dispatchers.IO) {
                val fileName = loadIndex(context)?.get(key) ?: return@withContext null
                val local = File(folder(context), key.replace(Regex("[^a-z0-9|]"), "").replace('|', '_') + "." + fileName.substringAfterLast('.', "jpg"))
                if (!local.exists()) downloads.withPermit { download(IMAGE_HOST + fileName, local) }
                if (local.exists()) BitmapFactory.decodeFile(local.path) else null
            }
        if (photo == null) {
            synchronized(misses) { misses.add(key) }
            return null
        }

        val art =
            withContext(Dispatchers.Default) {
                if (disc) photo.asImageBitmap() else masked(context, photo, labelRes!!)?.asImageBitmap()
            } ?: return null
        artCache.put(key, art)
        return art
    }

    private fun download(
        url: String,
        target: File,
    ) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            try {
                if (connection.responseCode != 200) return
                val temp = File(target.path + ".part")
                connection.inputStream.use { input -> temp.outputStream().use { input.copyTo(it) } }
                temp.renameTo(target)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** The photo's middle, cropped to the label's shape and kept only where the label is. */
    private fun masked(
        context: Context,
        photo: Bitmap,
        @DrawableRes labelRes: Int,
    ): Bitmap? {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val mask = BitmapFactory.decodeResource(context.resources, labelRes, options) ?: return null
        val w = mask.width
        val h = mask.height
        val pixels = IntArray(w * h)
        mask.getPixels(pixels, 0, w, 0, 0, w, h)
        var left = w
        var top = h
        var right = -1
        var bottom = -1
        for (y in 0 until h) {
            for (x in 0 until w) {
                if ((pixels[y * w + x] ushr 24) > 127) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < left || bottom < top) return null
        val box = Rect(left, top, right + 1, bottom + 1)

        // Cart photos show the whole shell: skip its edges, then crop the rest to the label's shape.
        val inner = Rect(photo.width * 12 / 100, photo.height * 8 / 100, photo.width * 88 / 100, photo.height * 70 / 100)
        val boxRatio = box.width().toFloat() / box.height()
        val innerRatio = inner.width().toFloat() / inner.height()
        val src =
            if (innerRatio > boxRatio) {
                val cw = (inner.height() * boxRatio).toInt()
                val cx = inner.centerX() - cw / 2
                Rect(cx, inner.top, cx + cw, inner.bottom)
            } else {
                val ch = (inner.width() / boxRatio).toInt()
                val cy = inner.centerY() - ch / 2
                Rect(inner.left, cy, inner.right, cy + ch)
            }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(mask, 0f, 0f, null)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }
        canvas.drawBitmap(photo, src, box, paint)
        return out
    }
}
