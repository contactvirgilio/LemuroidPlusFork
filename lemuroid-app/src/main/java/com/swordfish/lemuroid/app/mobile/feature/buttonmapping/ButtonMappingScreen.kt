package com.swordfish.lemuroid.app.mobile.feature.buttonmapping

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.focusHighlight
import com.swordfish.lemuroid.app.shared.input.ButtonMapping
import kotlinx.coroutines.launch
import com.swordfish.touchinput.controller.R as TouchR

private val LINE = Color(0xFF9A9A96)
private val TEAL = Color(0xFF5DCAA5)
private val TEAL_DARK = Color(0xFF04342C)
private val GOLD = Color(0xFFF2C94C)
private val TILE = Color(0xFF1D2126)
private val TILE_BORDER = Color(0xFF2C3137)
private val MUTED = Color(0xFF8A8F95)

/** Whether a gamepad (or the device's own buttons, like the RPC's) is connected; follows plugging in and out. */
@Composable
fun rememberHasGamepad(): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(android.content.Context.INPUT_SERVICE) as android.hardware.input.InputManager }
    fun check(): Boolean =
        manager.inputDeviceIds.any { id ->
            val device = manager.getInputDevice(id) ?: return@any false
            !device.isVirtual &&
                (
                    (device.sources and android.view.InputDevice.SOURCE_GAMEPAD) == android.view.InputDevice.SOURCE_GAMEPAD ||
                        (device.sources and android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK
                )
        }
    var has by remember { mutableStateOf(check()) }
    androidx.compose.runtime.DisposableEffect(manager) {
        val listener =
            object : android.hardware.input.InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    has = check()
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    has = check()
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    has = check()
                }
            }
        manager.registerInputDeviceListener(listener, null)
        onDispose { manager.unregisterInputDeviceListener(listener) }
    }
    return has
}

/** Settings > Controls > Button mapping: a tile per system, then that system's controller. */
@Composable
fun ButtonMappingSettingsScreen(
    modifier: Modifier = Modifier,
    translate: suspend (KeyEvent) -> Int,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<ControllerSpec?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val current = selected

    if (current != null) {
        BackHandler { selected = null }
        ButtonMappingEditor(
            modifier = modifier,
            spec = current,
            systemIds = current.systemIds,
            fileName = null,
            translate = translate,
            onDone = {
                selected = null
                refresh++
            },
        )
        return
    }

    LazyVerticalGrid(
        modifier = modifier.fillMaxSize().padding(12.dp),
        columns = GridCells.Adaptive(minSize = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ControllerSpecs.all, key = { it.name }) { spec ->
            val edited = remember(refresh) { spec.systemIds.any { ButtonMapping.has(context, ButtonMapping.systemKey(it)) } }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .background(TILE)
                        .border(if (edited) 2.dp else 1.dp, if (edited) TEAL else TILE_BORDER)
                        .focusHighlight(shape = RectangleShape, zoom = 1f)
                        .clickable { selected = spec }
                        .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MiniPad(spec, Modifier.size(width = 70.dp, height = 44.dp))
                Text(text = spec.name, fontSize = 13.sp, color = Color(0xFFE8E8E8), modifier = Modifier.padding(top = 6.dp))
                if (edited) Text(text = "edited", fontSize = 11.sp, color = TEAL)
            }
        }
    }
}

/**
 * One system's controller: tap (or select) a button, then press the button you want for it.
 * With [fileName] (opened from a game), it can also save for just that game.
 */
@Composable
fun ButtonMappingEditor(
    modifier: Modifier = Modifier,
    spec: ControllerSpec,
    // The game's system first; a system-wide change is saved for all of them (e.g. GB and GBC).
    systemIds: List<String>,
    fileName: String?,
    translate: suspend (KeyEvent) -> Int,
    onDone: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameKey = fileName?.let { ButtonMapping.gameKey(systemIds.first(), it) }
    val systemKey = ButtonMapping.systemKey(systemIds.first())
    var forGame by remember { mutableStateOf(gameKey != null && ButtonMapping.has(context, gameKey)) }

    fun save(newMap: Map<Int, Int>) {
        if (forGame && gameKey != null) {
            ButtonMapping.set(context, gameKey, newMap)
        } else {
            systemIds.forEach { ButtonMapping.set(context, ButtonMapping.systemKey(it), newMap) }
        }
    }

    val key = if (forGame && gameKey != null) gameKey else systemKey
    var map by remember(key) { mutableStateOf(ButtonMapping.get(context, key)) }
    var listening by remember { mutableStateOf<ButtonSpec?>(null) }
    val root = remember { FocusRequester() }
    val buttonKeys = spec.buttons.map { it.key }

    fun sourceOf(target: Int): Int = (buttonKeys + map.keys).distinct().firstOrNull { (map[it] ?: it) == target } ?: target

    LaunchedEffect(listening) { if (listening != null) runCatching { root.requestFocus() } }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .focusRequester(root)
                .onPreviewKeyEvent { event ->
                    val target = listening ?: return@onPreviewKeyEvent false
                    val native = event.nativeKeyEvent
                    if (native.keyCode == KeyEvent.KEYCODE_BACK) {
                        if (event.type == KeyEventType.KeyDown) listening = null
                        return@onPreviewKeyEvent true
                    }
                    if (event.type != KeyEventType.KeyDown || native.repeatCount > 0) return@onPreviewKeyEvent true
                    scope.launch {
                        val pressed = translate(native)
                        if (pressed in ButtonMapping.MAPPABLE) {
                            map = ButtonMapping.assign(map, buttonKeys, target.key, pressed)
                            save(map)
                            listening = null
                        }
                    }
                    true
                }
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = spec.name, fontSize = 18.sp, color = Color(0xFFE8E8E8), modifier = Modifier.fillMaxWidth())

        if (gameKey != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                listOf("This system" to false, "Just this game" to true).forEach { (label, game) ->
                    val on = forGame == game
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(TILE)
                                .border(if (on) 2.dp else 1.dp, if (on) TEAL else TILE_BORDER)
                                .focusHighlight(shape = RectangleShape, zoom = 1f)
                                .clickable {
                                    // Starting a game's own mapping copies the system's, to change from there.
                                    if (game && !ButtonMapping.has(context, gameKey)) {
                                        ButtonMapping.set(context, gameKey, ButtonMapping.get(context, systemKey))
                                    }
                                    forGame = game
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = label, fontSize = 13.sp, color = if (on) TEAL else MUTED)
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = 16.dp).aspectRatio(2f)) {
            val w = maxWidth
            val h = maxHeight
            Canvas(modifier = Modifier.fillMaxSize()) { drawPadBody(spec) }
            spec.buttons.forEach { button ->
                val source = sourceOf(button.key)
                PadButton(
                    button = button,
                    changedFrom = if (source != button.key) ButtonMapping.keyName(source) else null,
                    listening = listening == button,
                    unit = w,
                    x = w * button.x,
                    y = h * button.y,
                    onClick = { listening = button },
                )
            }
        }

        Text(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            text =
                listening?.let { "${it.label}: press the button you want…" }
                    ?: "Pick a button, then press the one you want for it. Teal = changed.",
            fontSize = 13.sp,
            color = if (listening != null) GOLD else MUTED,
            textAlign = TextAlign.Center,
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionBox("Reset to default", Modifier.weight(1f)) {
                map = emptyMap()
                save(map)
                listening = null
            }
            if (onDone != null) ActionBox("Done", Modifier.weight(1f)) { onDone() }
        }
    }
}

@Composable
private fun ActionBox(
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(42.dp)
                .background(TILE)
                .border(1.dp, TILE_BORDER)
                .focusHighlight(shape = RectangleShape, zoom = 1f)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFFC9C8D2))
    }
}

@Composable
private fun PadButton(
    button: ButtonSpec,
    changedFrom: String?,
    listening: Boolean,
    unit: Dp,
    x: Dp,
    y: Dp,
    onClick: () -> Unit,
) {
    val (bw, bh) =
        when (button.kind) {
            Kind.ROUND -> unit * 0.095f to unit * 0.095f
            Kind.SHOULDER -> unit * 0.18f to unit * 0.05f
            Kind.PILL -> if (button.icon != null) unit * 0.1f to unit * 0.055f else unit * 0.09f to unit * 0.04f
        }
    val shape = if (button.kind == Kind.ROUND) CircleShape else RoundedCornerShape(50)
    val changed = changedFrom != null
    val border = if (listening) GOLD else if (changed) TEAL else LINE
    Box(
        modifier =
            Modifier
                .offset(x = x - bw / 2, y = y - bh / 2)
                .size(bw, bh)
                .background(if (changed) TEAL else Color.Transparent, shape)
                .border(2.dp, border, shape)
                .focusHighlight(shape = shape, zoom = 1f)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (button.icon != null) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(button.icon),
                contentDescription = button.label,
                tint = if (changed) TEAL_DARK else Color(0xFFC9C8D2),
                modifier = Modifier.fillMaxSize().padding(if (button.kind == Kind.ROUND) 7.dp else 3.dp),
            )
        } else if (button.kind == Kind.ROUND) {
            Text(
                text = button.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (changed) TEAL_DARK else Color(0xFFC9C8D2),
                maxLines = 1,
            )
        }
    }
    // Small names under pills and shoulders, and the pressed button for changed ones.
    val caption =
        when {
            changed && button.kind == Kind.ROUND -> "← $changedFrom"
            changed -> "${button.label} ← $changedFrom"
            button.kind != Kind.ROUND && button.icon == null -> button.label
            else -> null
        }
    if (caption != null) {
        Text(
            modifier = Modifier.offset(x = x - 40.dp, y = y + bh / 2 + 1.dp).size(width = 80.dp, height = 14.dp),
            text = caption,
            fontSize = 9.sp,
            color = if (changed) TEAL else MUTED,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPadBody(spec: ControllerSpec) {
    val stroke = Stroke(width = 2.dp.toPx())
    val w = size.width
    val h = size.height
    // Like the Outline touch skin: no controller body, just the D-pad (or stick) and the buttons.
    // D-pad (or a stick for arcade).
    val cx = w * 0.2f
    val cy = h * 0.52f
    if (spec.stick) {
        drawCircle(LINE, radius = h * 0.2f, center = Offset(cx, cy), style = stroke)
        drawCircle(LINE, radius = h * 0.07f, center = Offset(cx, cy))
    } else {
        val a = h * 0.07f
        val b = h * 0.2f
        val plus =
            Path().apply {
                moveTo(cx - a, cy - b)
                lineTo(cx + a, cy - b)
                lineTo(cx + a, cy - a)
                lineTo(cx + b, cy - a)
                lineTo(cx + b, cy + a)
                lineTo(cx + a, cy + a)
                lineTo(cx + a, cy + b)
                lineTo(cx - a, cy + b)
                lineTo(cx - a, cy + a)
                lineTo(cx - b, cy + a)
                lineTo(cx - b, cy - a)
                lineTo(cx - a, cy - a)
                close()
            }
        drawPath(plus, LINE, style = stroke)
    }
}

/** The tile picture: the system's face buttons, drawn like the touch controls. */
@Composable
private fun MiniPad(
    spec: ControllerSpec,
    modifier: Modifier,
) {
    val round = spec.buttons.filter { it.kind == Kind.ROUND }
    val minX = round.minOf { it.x }
    val maxX = round.maxOf { it.x }
    val minY = round.minOf { it.y }
    val maxY = round.maxOf { it.y }
    BoxWithConstraints(modifier = modifier) {
        val d = minOf(maxWidth, maxHeight) * 0.34f
        round.forEach { button ->
            val fx = if (maxX > minX) (button.x - minX) / (maxX - minX) else 0.5f
            val fy = if (maxY > minY) (button.y - minY) / (maxY - minY) else 0.5f
            Box(
                modifier =
                    Modifier
                        .offset(x = (maxWidth - d) * fx, y = (maxHeight - d) * fy)
                        .size(d)
                        .border(1.5.dp, LINE, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (button.icon != null) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(button.icon),
                        contentDescription = null,
                        tint = Color(0xFFC9C8D2),
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                    )
                } else {
                    Text(text = button.label, fontSize = 8.sp, color = Color(0xFFC9C8D2), maxLines = 1)
                }
            }
        }
    }
}

enum class Kind { ROUND, SHOULDER, PILL }

/** A button of a system's controller: its name, the RetroPad key the core reads, and where it sits. */
data class ButtonSpec(
    val label: String,
    val key: Int,
    val x: Float,
    val y: Float,
    val kind: Kind = Kind.ROUND,
    // The touch controls' own icon for this button (PlayStation symbols, Start, Select, Coin).
    val icon: Int? = null,
)

data class ControllerSpec(
    val name: String,
    val systemIds: List<String>,
    val buttons: List<ButtonSpec>,
    val stick: Boolean = false,
)

/** Every system's buttons; the RetroPad keys come from the touch layouts, so they match the cores. */
object ControllerSpecs {
    private const val A = KeyEvent.KEYCODE_BUTTON_A
    private const val B = KeyEvent.KEYCODE_BUTTON_B
    private const val X = KeyEvent.KEYCODE_BUTTON_X
    private const val Y = KeyEvent.KEYCODE_BUTTON_Y
    private const val L1 = KeyEvent.KEYCODE_BUTTON_L1
    private const val R1 = KeyEvent.KEYCODE_BUTTON_R1
    private const val L2 = KeyEvent.KEYCODE_BUTTON_L2
    private const val R2 = KeyEvent.KEYCODE_BUTTON_R2
    private const val L3 = KeyEvent.KEYCODE_BUTTON_THUMBL
    private const val R3 = KeyEvent.KEYCODE_BUTTON_THUMBR
    private const val START = KeyEvent.KEYCODE_BUTTON_START
    private const val SELECT = KeyEvent.KEYCODE_BUTTON_SELECT

    private fun startSelect(
        start: String = "Start",
        select: String? = "Select",
    ) = listOfNotNull(
        select?.let { ButtonSpec(it, SELECT, 0.42f, 0.56f, Kind.PILL, icon = if (it == "Select") TouchR.drawable.button_select else null) },
        ButtonSpec(start, START, 0.56f, 0.56f, Kind.PILL, icon = if (start == "Start") TouchR.drawable.button_start else null),
    )

    private fun shoulders(
        l: String = "L",
        r: String = "R",
    ) = listOf(ButtonSpec(l, L1, 0.22f, 0.08f, Kind.SHOULDER), ButtonSpec(r, R1, 0.78f, 0.08f, Kind.SHOULDER))

    private val diamond =
        listOf(
            ButtonSpec("X", X, 0.78f, 0.32f),
            ButtonSpec("Y", Y, 0.70f, 0.52f),
            ButtonSpec("A", A, 0.86f, 0.52f),
            ButtonSpec("B", B, 0.78f, 0.72f),
        )

    private fun twoButtons(
        left: Pair<String, Int>,
        right: Pair<String, Int>,
    ) = listOf(ButtonSpec(left.first, left.second, 0.72f, 0.60f), ButtonSpec(right.first, right.second, 0.85f, 0.48f))

    val all: List<ControllerSpec> =
        listOf(
            ControllerSpec("NES", listOf("nes"), twoButtons("B" to B, "A" to A) + startSelect()),
            ControllerSpec("SNES", listOf("snes"), diamond + shoulders() + startSelect()),
            ControllerSpec(
                "Genesis",
                listOf("md", "scd"),
                listOf(
                    ButtonSpec("X", L1, 0.64f, 0.38f),
                    ButtonSpec("Y", X, 0.75f, 0.34f),
                    ButtonSpec("Z", R1, 0.86f, 0.30f),
                    ButtonSpec("A", Y, 0.64f, 0.62f),
                    ButtonSpec("B", B, 0.75f, 0.58f),
                    ButtonSpec("C", A, 0.86f, 0.54f),
                    ButtonSpec("Mode", SELECT, 0.45f, 0.40f, Kind.PILL),
                    ButtonSpec("Start", START, 0.45f, 0.60f, Kind.PILL),
                ),
            ),
            ControllerSpec("Game Boy", listOf("gb", "gbc"), twoButtons("B" to B, "A" to A) + startSelect()),
            ControllerSpec("GBA", listOf("gba"), twoButtons("B" to B, "A" to A) + shoulders() + startSelect()),
            ControllerSpec(
                "N64",
                listOf("n64"),
                listOf(
                    ButtonSpec("A", B, 0.74f, 0.64f),
                    ButtonSpec("B", Y, 0.65f, 0.50f),
                    ButtonSpec("Z", L2, 0.50f, 0.80f, Kind.SHOULDER),
                    ButtonSpec("Start", START, 0.50f, 0.42f, Kind.PILL),
                ) + shoulders(),
            ),
            ControllerSpec(
                "PlayStation",
                listOf("psx"),
                listOf(
                    ButtonSpec("△", X, 0.78f, 0.32f, icon = TouchR.drawable.psx_triangle),
                    ButtonSpec("□", Y, 0.70f, 0.52f, icon = TouchR.drawable.psx_square),
                    ButtonSpec("○", A, 0.86f, 0.52f, icon = TouchR.drawable.psx_circle),
                    ButtonSpec("✕", B, 0.78f, 0.72f, icon = TouchR.drawable.psx_cross),
                    ButtonSpec("L1", L1, 0.22f, 0.10f, Kind.SHOULDER),
                    ButtonSpec("R1", R1, 0.78f, 0.10f, Kind.SHOULDER),
                    ButtonSpec("L2", L2, 0.22f, 0.02f, Kind.SHOULDER),
                    ButtonSpec("R2", R2, 0.78f, 0.02f, Kind.SHOULDER),
                    ButtonSpec("L3", L3, 0.38f, 0.80f, Kind.PILL),
                    ButtonSpec("R3", R3, 0.62f, 0.80f, Kind.PILL),
                ) + startSelect(),
            ),
            ControllerSpec(
                "PSP",
                listOf("psp"),
                listOf(
                    ButtonSpec("△", X, 0.78f, 0.32f, icon = TouchR.drawable.psx_triangle),
                    ButtonSpec("□", Y, 0.70f, 0.52f, icon = TouchR.drawable.psx_square),
                    ButtonSpec("○", A, 0.86f, 0.52f, icon = TouchR.drawable.psx_circle),
                    ButtonSpec("✕", B, 0.78f, 0.72f, icon = TouchR.drawable.psx_cross),
                ) + shoulders() + startSelect(),
            ),
            ControllerSpec("DS", listOf("nds"), diamond + shoulders() + startSelect()),
            ControllerSpec("3DS", listOf("3ds"), diamond + shoulders() + startSelect()),
            ControllerSpec("Master System", listOf("sms"), twoButtons("1" to B, "2" to A)),
            ControllerSpec("Game Gear", listOf("gg"), twoButtons("1" to B, "2" to A) + startSelect(select = null)),
            ControllerSpec("PC Engine", listOf("pce"), twoButtons("II" to A, "I" to B) + startSelect(start = "Run")),
            ControllerSpec(
                "Atari 2600",
                listOf("atari2600"),
                listOf(ButtonSpec("Fire", B, 0.80f, 0.55f)) + shoulders("Diff. A", "Diff. B").let {
                    listOf(it[0], it[1].copy(key = L2))
                } + startSelect(start = "Reset"),
            ),
            ControllerSpec("Atari 7800", listOf("atari7800"), twoButtons("1" to B, "2" to A) + startSelect(start = "Pause")),
            ControllerSpec("Lynx", listOf("lynx"), twoButtons("B" to B, "A" to A) + shoulders("Option 1", "Option 2") + startSelect(start = "Pause", select = null)),
            ControllerSpec("Neo Geo Pocket", listOf("ngp", "ngc"), twoButtons("A" to B, "B" to A) + startSelect(start = "Option", select = null)),
            ControllerSpec("WonderSwan", listOf("ws", "wsc"), twoButtons("B" to B, "A" to A) + startSelect(select = null)),
            ControllerSpec(
                "Arcade",
                listOf("fbneo", "mame2003plus"),
                listOf(
                    ButtonSpec("1", B, 0.62f, 0.62f),
                    ButtonSpec("2", A, 0.74f, 0.58f),
                    ButtonSpec("3", Y, 0.62f, 0.38f),
                    ButtonSpec("4", X, 0.74f, 0.34f),
                    ButtonSpec("5", L1, 0.86f, 0.30f),
                    ButtonSpec("6", R1, 0.86f, 0.54f),
                    ButtonSpec("Coin", SELECT, 0.42f, 0.56f, Kind.PILL, icon = TouchR.drawable.button_coin),
                    ButtonSpec("Start", START, 0.50f, 0.72f, Kind.PILL, icon = TouchR.drawable.button_start),
                ),
                stick = true,
            ),
        )

    fun forSystem(systemId: String): ControllerSpec? = all.firstOrNull { systemId in it.systemIds }
}
