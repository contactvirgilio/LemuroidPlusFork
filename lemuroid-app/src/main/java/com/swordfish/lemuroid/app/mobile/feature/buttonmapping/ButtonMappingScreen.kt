package com.swordfish.lemuroid.app.mobile.feature.buttonmapping

import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.layout.widthIn
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
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
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.compose.ui.graphics.luminance

private val TEAL = Color(0xFF5DCAA5)
private val GOLD = Color(0xFFF2C94C)
private val TILE = Color(0xFF1D2126)
private val TILE_BORDER = Color(0xFF2C3137)
private val MUTED = Color(0xFF8A8F95)
private val CHIP_GREY = Color(0xFF5F5E5A)
private val TEXT = Color(0xFFE8E8E8)
private val AMBER = Color(0xFFEF9F27)

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

/** Settings > Controls > Button mapping: a tile per system, then that system's list. */
@Composable
fun ButtonMappingSettingsScreen(
    modifier: Modifier = Modifier,
    translate: suspend (KeyEvent) -> Int,
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<ControllerSpec?>(null) }
    var shortcutsOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val current = selected

    if (shortcutsOpen) {
        BackHandler { shortcutsOpen = false }
        ShortcutsEditor(modifier = modifier, translate = translate, onDone = { shortcutsOpen = false })
        return
    }

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
        // The shortcuts for every system, as one wide line on top (only while shortcuts are on).
        if (ButtonMapping.shortcutsEnabled(context)) item(key = "shortcuts", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(TILE)
                        .border(2.dp, AMBER)
                        .focusHighlight(shape = RectangleShape, zoom = 1f)
                        .clickable { shortcutsOpen = true }
                        .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShortcutChip(ButtonMapping.Shortcut.MENU, 26.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = stringResource(com.swordfish.lemuroid.R.string.settings_title_shortcuts), fontSize = 14.sp, color = TEXT)
                    Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_shortcuts_line_text), fontSize = 11.sp, color = MUTED)
                }
            }
        }
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
                // The system's face buttons as small chips, in their colours.
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    spec.buttons.filter { it.kind == Kind.ROUND && !it.dualShockOnly }.take(6).forEach { ButtonChip(it, size = 18.dp) }
                }
                Text(text = spec.name, fontSize = 13.sp, color = TEXT, modifier = Modifier.padding(top = 8.dp))
                if (edited) Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_edited), fontSize = 11.sp, color = TEAL)
            }
        }
    }
}

/**
 * One system's buttons as a list (Cannoli's logic, our look, 2026-11-22). Left: the system's button,
 * as a chip in its touch-control colour (grey otherwise). Right: which of your buttons triggers it.
 * A on a row, then press the button you want for it; if that button had another job, that row shows
 * "—". Y unbinds the row, X resets all. Changes apply when you leave: with [fileName] (from a game)
 * Save for the system / Save for this game / Discard; from Settings, Save / Discard.
 */
@Composable
fun ButtonMappingEditor(
    modifier: Modifier = Modifier,
    spec: ControllerSpec,
    // The mapping id first (a system, or "neogeo" / "cps2"); a system save goes to all of them.
    systemIds: List<String>,
    fileName: String?,
    translate: suspend (KeyEvent) -> Int,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val systemId = systemIds.first()
    val saved = remember { ButtonMapping.effective(context, systemId, fileName) }
    var map by remember { mutableStateOf(saved) }
    var listening by remember { mutableStateOf<Int?>(null) }
    var focused by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf(false) }
    val changed = map.filter { it.key != it.value } != saved.filter { it.key != it.value }
    val root = remember { FocusRequester() }
    val firstRow = remember { FocusRequester() }
    val dualShock = remember { systemId != "psx" || ButtonMapping.psxDualShock(context) }
    val rows = remember { spec.buttons.filter { !it.dualShockOnly || dualShock } + DPAD_ROWS }
    val yours = ButtonMapping.MAPPABLE
    val savedShortcuts = remember { ButtonMapping.effectiveShortcuts(context, systemId, fileName) }
    var shortcuts by remember { mutableStateOf(savedShortcuts) }
    var listeningShortcut by remember { mutableStateOf<ButtonMapping.Shortcut?>(null) }
    var focusedShortcut by remember { mutableStateOf<ButtonMapping.Shortcut?>(null) }
    val captured = remember { mutableStateListOf<Int>() }
    val shortcutsChanged = shortcuts != savedShortcuts
    val shortcutsOn = remember { ButtonMapping.shortcutsEnabled(context) }

    fun sends(button: Int) = map[button] ?: button

    fun sourcesOf(target: Int) = yours.filter { sends(it) == target }

    fun leave() {
        if (changed || shortcutsChanged) prompt = true else onDone()
    }

    fun saveSystem() {
        val delta = ButtonMapping.systemDelta(map)
        systemIds.forEach {
            ButtonMapping.set(context, ButtonMapping.systemKey(it), delta)
            if (shortcutsChanged) ButtonMapping.setShortcuts(context, ButtonMapping.shortcutsKey(ButtonMapping.systemKey(it)), shortcuts)
        }
        onDone()
    }

    fun saveGame() {
        val system = ButtonMapping.defaults(systemId) + ButtonMapping.get(context, ButtonMapping.systemKey(systemId))
        ButtonMapping.set(context, ButtonMapping.gameKey(systemId, fileName!!), ButtonMapping.gameDelta(map, system))
        if (shortcutsChanged) ButtonMapping.setShortcuts(context, ButtonMapping.shortcutsKey(ButtonMapping.gameKey(systemId, fileName)), shortcuts)
        onDone()
    }

    BackHandler(enabled = !prompt) {
        if (listening != null || listeningShortcut != null) {
            listening = null
            listeningShortcut = null
        } else {
            leave()
        }
    }
    BackHandler(enabled = prompt) { prompt = false }
    LaunchedEffect(Unit) { runCatching { firstRow.requestFocus() } }
    // While waiting for a button: a 5-second countdown, then it gives up on its own (like Cannoli),
    // so a controller alone can back out (B is a real button and would be taken as the answer).
    var secondsLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(listening) {
        val waitingFor = listening ?: return@LaunchedEffect
        // Focus stays on the row being changed: the list catches the next press anyway.
        for (left in 5 downTo 1) {
            secondsLeft = left
            kotlinx.coroutines.delay(1000)
        }
        if (listening == waitingFor) listening = null
    }
    LaunchedEffect(listeningShortcut) {
        val waitingFor = listeningShortcut ?: return@LaunchedEffect
        captured.clear()
        for (left in 5 downTo 1) {
            secondsLeft = left
            kotlinx.coroutines.delay(1000)
        }
        if (listeningShortcut == waitingFor) listeningShortcut = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(root)
                    .onPreviewKeyEvent { event ->
                        if (prompt) return@onPreviewKeyEvent false
                        val native = event.nativeKeyEvent
                        if (native.keyCode == KeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                        val shortcut = listeningShortcut
                        if (shortcut != null) {
                            // A combo: every button held down; it's set when the first one is let go.
                            if (native.repeatCount > 0) return@onPreviewKeyEvent true
                            if (event.type == KeyEventType.KeyDown) {
                                scope.launch {
                                    val pressed = translate(native)
                                    if (pressed in yours && pressed !in captured) captured.add(pressed)
                                }
                            } else if (event.type == KeyEventType.KeyUp && captured.size >= 2) {
                                shortcuts = shortcuts + (shortcut to captured.toSet())
                                listeningShortcut = null
                            }
                            return@onPreviewKeyEvent true
                        }
                        val target = listening
                        if (target != null) {
                            if (event.type != KeyEventType.KeyDown || native.repeatCount > 0) return@onPreviewKeyEvent true
                            scope.launch {
                                val pressed = translate(native)
                                if (pressed in yours) {
                                    // Your button now triggers this one; whatever triggered it before stops.
                                    val next = map.toMutableMap()
                                    sourcesOf(target).filter { it != pressed }.forEach { next[it] = ButtonMapping.UNBOUND }
                                    next[pressed] = target
                                    map = next
                                    listening = null
                                }
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_BUTTON_Y -> {
                                focusedShortcut?.let {
                                    shortcuts = shortcuts + (it to emptySet())
                                    return@onPreviewKeyEvent true
                                }
                                val target = rows.getOrNull(focused)?.key ?: return@onPreviewKeyEvent true
                                map = map + sourcesOf(target).associateWith { ButtonMapping.UNBOUND }
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_X -> {
                                map = ButtonMapping.defaults(systemId)
                                shortcuts = ButtonMapping.effectiveShortcuts(context, null, null)
                                true
                            }
                            else -> false
                        }
                    },
        ) {
            Text(
                text = stringResource(com.swordfish.lemuroid.R.string.bm_title, spec.name),
                fontSize = 16.sp,
                color = TEXT,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 12.dp, 16.dp, 4.dp),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 4.dp)) {
                Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_system_button, spec.name), fontSize = 11.sp, color = MUTED, modifier = Modifier.weight(1f))
                Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_your_button), fontSize = 11.sp, color = MUTED)
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rows.forEachIndexed { index, button ->
                    val sources = sourcesOf(button.key)
                    val isListening = listening == button.key
                    val isDefault = sources == listOf(button.key) || (sources.isEmpty() && ButtonMapping.defaults(systemId)[button.key] == ButtonMapping.UNBOUND)
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .background(TILE)
                                .border(1.dp, if (isListening) GOLD else TILE_BORDER)
                                .then(if (index == 0) Modifier.focusRequester(firstRow) else Modifier)
                                .onFocusChanged {
                                    if (it.isFocused) {
                                        focused = index
                                        focusedShortcut = null
                                    }
                                }
                                .focusHighlight(shape = RectangleShape, zoom = 1f)
                                .clickable { listening = button.key }
                                .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ButtonChip(button, size = 26.dp)
                        if (button.note != null) {
                            Text(text = button.note, fontSize = 11.sp, color = MUTED, modifier = Modifier.padding(start = 10.dp))
                        }
                        Box(modifier = Modifier.weight(1f))
                        when {
                            isListening -> Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_press, secondsLeft), fontSize = 13.sp, color = GOLD)
                            sources.isEmpty() -> Text(text = "—", fontSize = 14.sp, color = MUTED)
                            else ->
                                Text(
                                    text = sources.joinToString(", ") { ButtonMapping.keyName(it) },
                                    fontSize = 14.sp,
                                    color = if (isDefault) Color(0xFFC9C8D2) else TEAL,
                                )
                        }
                    }
                }

                if (shortcutsOn) ShortcutSection(
                    shortcuts = shortcuts,
                    listening = listeningShortcut,
                    captured = captured,
                    secondsLeft = secondsLeft,
                    onFocus = { focusedShortcut = it },
                    onPick = {
                        listening = null
                        listeningShortcut = it
                    },
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp, 16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                listOf("A" to stringResource(com.swordfish.lemuroid.R.string.bm_key_change), "Y" to stringResource(com.swordfish.lemuroid.R.string.bm_key_clear), "X" to stringResource(com.swordfish.lemuroid.R.string.bm_key_reset_all), "B" to stringResource(com.swordfish.lemuroid.R.string.bm_key_back)).forEach { (key, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.border(1.dp, CHIP_GREY).padding(horizontal = 4.dp)) {
                            Text(text = key, fontSize = 11.sp, color = Color(0xFFC9C8D2))
                        }
                        Text(text = " $label", fontSize = 11.sp, color = MUTED)
                    }
                }
            }
        }

        if (prompt) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                val promptFirst = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { promptFirst.requestFocus() } }
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF111417)).border(1.dp, TILE_BORDER).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_save_prompt), fontSize = 15.sp, color = TEXT, modifier = Modifier.padding(bottom = 4.dp))
                    val saveSystemLabel = stringResource(com.swordfish.lemuroid.R.string.bm_save_system, spec.name)
                    val saveGameLabel = stringResource(com.swordfish.lemuroid.R.string.bm_save_game)
                    val discardLabel = stringResource(com.swordfish.lemuroid.R.string.bm_discard)
                    val options =
                        buildList {
                            add(saveSystemLabel to { saveSystem() })
                            if (fileName != null) add(saveGameLabel to { saveGame() })
                            add(discardLabel to { onDone() })
                        }
                    options.forEachIndexed { i, (label, action) ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(TILE)
                                    .border(1.dp, TILE_BORDER)
                                    .then(if (i == 0) Modifier.focusRequester(promptFirst) else Modifier)
                                    .focusHighlight(shape = RectangleShape, zoom = 1f)
                                    .clickable { action() }
                                    .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(text = label, fontSize = 14.sp, color = Color(0xFFC9C8D2))
                        }
                    }
                }
            }
        }
    }
}

/** "SHORTCUTS" and a row per shortcut: amber icon chip, name, and the combo (or "—"). */
@Composable
private fun ShortcutSection(
    shortcuts: Map<ButtonMapping.Shortcut, Set<Int>>,
    listening: ButtonMapping.Shortcut?,
    captured: List<Int>,
    secondsLeft: Int,
    onFocus: (ButtonMapping.Shortcut) -> Unit,
    onPick: (ButtonMapping.Shortcut) -> Unit,
) {
    Text(text = stringResource(com.swordfish.lemuroid.R.string.settings_title_shortcuts).uppercase(), fontSize = 11.sp, letterSpacing = 1.5.sp, color = AMBER, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp, start = 2.dp))
    ButtonMapping.Shortcut.values().forEach { type ->
        val keys = shortcuts[type].orEmpty()
        val isListening = listening == type
        val isDefault = keys == ButtonMapping.DEFAULT_SHORTCUTS[type]
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(TILE)
                    .border(1.dp, if (isListening) GOLD else TILE_BORDER)
                    .onFocusChanged { if (it.isFocused) onFocus(type) }
                    .focusHighlight(shape = RectangleShape, zoom = 1f)
                    .clickable { onPick(type) }
                    .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShortcutChip(type, 26.dp)
            Text(text = stringResource(shortcutName(type)), fontSize = 14.sp, color = TEXT, modifier = Modifier.padding(start = 10.dp).weight(1f))
            when {
                isListening ->
                    Text(
                        text = if (captured.isEmpty()) stringResource(com.swordfish.lemuroid.R.string.sc_hold_two, secondsLeft) else captured.joinToString(" + ") { ButtonMapping.keyName(it) },
                        fontSize = 13.sp,
                        color = GOLD,
                    )
                keys.isEmpty() -> Text(text = "—", fontSize = 14.sp, color = MUTED)
                else -> Combo(keys, if (isDefault) Color(0xFFC9C8D2) else TEAL)
            }
        }
    }
}

private fun shortcutName(type: ButtonMapping.Shortcut) =
    when (type) {
        ButtonMapping.Shortcut.MENU -> com.swordfish.lemuroid.R.string.sc_menu
        ButtonMapping.Shortcut.FAST_FORWARD -> com.swordfish.lemuroid.R.string.sc_fast_forward
        ButtonMapping.Shortcut.SAVE_STATE -> com.swordfish.lemuroid.R.string.sc_quick_save
        ButtonMapping.Shortcut.LOAD_MENU -> com.swordfish.lemuroid.R.string.sc_open_saves
    }

/** Keys of a combo in small boxes: Select + R1. Select is listed first. */
@Composable
private fun Combo(
    keys: Set<Int>,
    color: Color,
) {
    val ordered = keys.sortedBy { if (it == KeyEvent.KEYCODE_BUTTON_SELECT) 0 else 1 }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ordered.forEachIndexed { i, key ->
            if (i > 0) Text(text = " + ", fontSize = 12.sp, color = MUTED)
            Box(modifier = Modifier.border(1.dp, CHIP_GREY).padding(horizontal = 6.dp, vertical = 1.dp)) {
                Text(text = ButtonMapping.keyName(key), fontSize = 12.sp, color = color)
            }
        }
    }
}

/** The shortcut's icon, the same ones the in-game shortcut buttons and game menu use, on amber. */
@Composable
private fun ShortcutChip(
    type: ButtonMapping.Shortcut,
    size: Dp,
) {
    val ink = Color(0xFF412402)
    Box(modifier = Modifier.size(size).background(AMBER, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
        val iconModifier = Modifier.size(size * 0.68f)
        when (type) {
            ButtonMapping.Shortcut.MENU ->
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = null, tint = ink, modifier = iconModifier)
            ButtonMapping.Shortcut.SAVE_STATE ->
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Outlined.Save, contentDescription = null, tint = ink, modifier = iconModifier)
            ButtonMapping.Shortcut.FAST_FORWARD ->
                androidx.compose.material3.Icon(
                    androidx.compose.ui.res.painterResource(com.swordfish.lemuroid.R.drawable.ic_menu_fast_forward),
                    contentDescription = null,
                    tint = ink,
                    modifier = iconModifier,
                )
            ButtonMapping.Shortcut.LOAD_MENU ->
                androidx.compose.material3.Icon(
                    androidx.compose.ui.res.painterResource(com.swordfish.lemuroid.R.drawable.ic_menu_load),
                    contentDescription = null,
                    tint = ink,
                    modifier = iconModifier,
                )
        }
    }
}

/** Settings > Button mapping > Shortcuts: the shortcuts for every system. */
@Composable
fun ShortcutsEditor(
    modifier: Modifier = Modifier,
    translate: suspend (KeyEvent) -> Int,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = remember { ButtonMapping.effectiveShortcuts(context, null, null) }
    var shortcuts by remember { mutableStateOf(saved) }
    var listening by remember { mutableStateOf<ButtonMapping.Shortcut?>(null) }
    var focused by remember { mutableStateOf<ButtonMapping.Shortcut?>(null) }
    val captured = remember { mutableStateListOf<Int>() }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf(false) }

    BackHandler(enabled = !prompt) {
        if (listening != null) listening = null else if (shortcuts != saved) prompt = true else onDone()
    }
    BackHandler(enabled = prompt) { prompt = false }
    LaunchedEffect(listening) {
        val waitingFor = listening ?: return@LaunchedEffect
        captured.clear()
        for (left in 5 downTo 1) {
            secondsLeft = left
            kotlinx.coroutines.delay(1000)
        }
        if (listening == waitingFor) listening = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { event ->
                        if (prompt) return@onPreviewKeyEvent false
                        val native = event.nativeKeyEvent
                        if (native.keyCode == KeyEvent.KEYCODE_BACK) return@onPreviewKeyEvent false
                        val shortcut = listening
                        if (shortcut != null) {
                            if (native.repeatCount > 0) return@onPreviewKeyEvent true
                            if (event.type == KeyEventType.KeyDown) {
                                scope.launch {
                                    val pressed = translate(native)
                                    if (pressed in ButtonMapping.MAPPABLE && pressed !in captured) captured.add(pressed)
                                }
                            } else if (event.type == KeyEventType.KeyUp && captured.size >= 2) {
                                shortcuts = shortcuts + (shortcut to captured.toSet())
                                listening = null
                            }
                            return@onPreviewKeyEvent true
                        }
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_BUTTON_Y -> {
                                focused?.let { shortcuts = shortcuts + (it to emptySet()) }
                                true
                            }
                            KeyEvent.KEYCODE_BUTTON_X -> {
                                shortcuts = ButtonMapping.DEFAULT_SHORTCUTS
                                true
                            }
                            else -> false
                        }
                    }
                    .padding(16.dp),
        ) {
            Text(text = stringResource(com.swordfish.lemuroid.R.string.sc_title), fontSize = 16.sp, color = TEXT, modifier = Modifier.padding(bottom = 4.dp))
            Text(text = stringResource(com.swordfish.lemuroid.R.string.sc_hint), fontSize = 11.sp, color = MUTED)
            ShortcutSection(
                shortcuts = shortcuts,
                listening = listening,
                captured = captured,
                secondsLeft = secondsLeft,
                onFocus = { focused = it },
                onPick = { listening = it },
            )
        }
        if (prompt) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
                val first = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
                Column(
                    modifier = Modifier.fillMaxWidth(0.8f).background(Color(0xFF111417)).border(1.dp, TILE_BORDER).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = stringResource(com.swordfish.lemuroid.R.string.bm_save_prompt), fontSize = 15.sp, color = TEXT, modifier = Modifier.padding(bottom = 4.dp))
                    val saveAllLabel = stringResource(com.swordfish.lemuroid.R.string.sc_save_all)
                    val discardLabel = stringResource(com.swordfish.lemuroid.R.string.bm_discard)
                    listOf(
                        saveAllLabel to {
                            ButtonMapping.setShortcuts(context, ButtonMapping.GLOBAL_SHORTCUTS, shortcuts)
                            onDone()
                        },
                        discardLabel to { onDone() },
                    ).forEachIndexed { i, (label, action) ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(TILE)
                                    .border(1.dp, TILE_BORDER)
                                    .then(if (i == 0) Modifier.focusRequester(first) else Modifier)
                                    .focusHighlight(shape = RectangleShape, zoom = 1f)
                                    .clickable { action() }
                                    .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(text = label, fontSize = 14.sp, color = Color(0xFFC9C8D2))
                        }
                    }
                }
            }
        }
    }
}

/** A system button drawn as a chip: round for face buttons, a pill for the rest, arrows for the D-pad. */
@Composable
private fun ButtonChip(
    button: ButtonSpec,
    size: Dp,
) {
    val fill = button.color?.let { Color(it) } ?: CHIP_GREY
    val ink = if (fill.luminance() > 0.5f) Color(0xFF412402) else Color.White
    when (button.kind) {
        // A bold filled arrow (owner picked "B", 2026-11-22), drawn pointing up and turned.
        Kind.DPAD -> {
            val turn =
                when (button.key) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> 90f
                    KeyEvent.KEYCODE_DPAD_DOWN -> 180f
                    KeyEvent.KEYCODE_DPAD_LEFT -> 270f
                    else -> 0f
                }
            Canvas(modifier = Modifier.size(size)) {
                val u = this.size.width / 26f
                val arrow =
                    androidx.compose.ui.graphics.Path().apply {
                        moveTo(13f * u, 4f * u)
                        lineTo(22f * u, 13f * u)
                        lineTo(16f * u, 13f * u)
                        lineTo(16f * u, 22f * u)
                        lineTo(10f * u, 22f * u)
                        lineTo(10f * u, 13f * u)
                        lineTo(4f * u, 13f * u)
                        close()
                    }
                rotate(turn) { drawPath(arrow, Color(0xFFD3D1C7)) }
            }
        }
        Kind.ROUND ->
            Box(modifier = Modifier.size(size).background(fill, CircleShape), contentAlignment = Alignment.Center) {
                if (button.icon != null) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.res.painterResource(button.icon),
                        contentDescription = button.label,
                        tint = ink,
                        modifier = Modifier.fillMaxSize().padding(size * 0.22f),
                    )
                } else {
                    val fs = (size.value * (if (button.label.length > 1) 0.36f else 0.46f)).sp
                    Text(
                        text = button.label,
                        color = ink,
                        maxLines = 1,
                        style =
                            androidx.compose.ui.text.TextStyle(
                                fontSize = fs,
                                lineHeight = fs,
                                fontWeight = FontWeight.Medium,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle =
                                    androidx.compose.ui.text.style.LineHeightStyle(
                                        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                                        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
                                    ),
                            ),
                    )
                }
            }
        // Start and Select: just their name, no pill (owner, 2026-11-22). Other pills: tall enough
        // for the text to sit in the middle, the same width so they line up.
        Kind.PILL ->
            if (button.label == "Start" || button.label == "Select") {
                Box(modifier = Modifier.height(size).widthIn(min = size * 2.3f), contentAlignment = Alignment.CenterStart) {
                    Text(text = button.label.uppercase(), color = Color(0xFFC9C8D2), maxLines = 1, style = centredText(12.sp))
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .height(size * 0.85f)
                            .widthIn(min = size * 2.3f)
                            .background(fill, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = button.label.uppercase(), color = ink, maxLines = 1, textAlign = TextAlign.Center, style = centredText(11.sp))
                }
            }
    }
}

/** Text that sits exactly in the middle of its box: no font padding, line height = font size. */
private fun centredText(size: androidx.compose.ui.unit.TextUnit) =
    androidx.compose.ui.text.TextStyle(
        fontSize = size,
        lineHeight = size,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle =
            androidx.compose.ui.text.style.LineHeightStyle(
                alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
            ),
    )

enum class Kind { ROUND, PILL, DPAD }

/**
 * A button of a system: its name, the RetroPad key the core reads for it, how it's drawn, and its
 * colour (the touch controls' press colour, or the real buttons' for Neo Geo and CPS2).
 */
data class ButtonSpec(
    val label: String,
    val key: Int,
    val kind: Kind = Kind.ROUND,
    val icon: Int? = null,
    val color: Long? = null,
    val note: String? = null,
    // PlayStation L3/R3: only on the DualShock.
    val dualShockOnly: Boolean = false,
)

data class ControllerSpec(
    val name: String,
    val systemIds: List<String>,
    val buttons: List<ButtonSpec>,
)

private val DPAD_ROWS =
    listOf(
        ButtonSpec("↑", KeyEvent.KEYCODE_DPAD_UP, Kind.DPAD),
        ButtonSpec("↓", KeyEvent.KEYCODE_DPAD_DOWN, Kind.DPAD),
        ButtonSpec("←", KeyEvent.KEYCODE_DPAD_LEFT, Kind.DPAD),
        ButtonSpec("→", KeyEvent.KEYCODE_DPAD_RIGHT, Kind.DPAD),
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

    private const val RED = 0xFFD6403F
    private const val BLUE = 0xFF306ED6
    private const val GREEN = 0xFF34A85C
    private const val YELLOW = 0xFFF5C828

    private fun round(label: String, key: Int, color: Long? = null, note: String? = null) = ButtonSpec(label, key, Kind.ROUND, color = color, note = note)

    private fun pill(label: String, key: Int) = ButtonSpec(label, key, Kind.PILL)

    private fun startSelect(start: String = "Start", select: String? = "Select") = listOfNotNull(select?.let { pill(it, SELECT) }, pill(start, START))

    private val psSymbols =
        listOf(
            Triple("△", X, TouchR.drawable.psx_triangle),
            Triple("○", A, TouchR.drawable.psx_circle),
            Triple("✕", B, TouchR.drawable.psx_cross),
            Triple("□", Y, TouchR.drawable.psx_square),
        )

    val all: List<ControllerSpec> =
        listOf(
            ControllerSpec("NES", listOf("nes"), listOf(round("B", B, RED), round("A", A, RED)) + startSelect()),
            ControllerSpec(
                "SNES",
                listOf("snes"),
                listOf(round("A", A, 0xFFDE343C), round("B", B, YELLOW), round("X", X, BLUE), round("Y", Y, GREEN), pill("L", L1), pill("R", R1)) + startSelect(),
            ),
            ControllerSpec(
                "Genesis",
                listOf("md", "scd"),
                listOf(round("A", Y), round("B", B), round("C", A), round("X", L1), round("Y", X), round("Z", R1)) + startSelect(select = "Mode"),
            ),
            ControllerSpec("Game Boy", listOf("gb", "gbc"), listOf(round("A", A, 0xFF9B2257), round("B", B, 0xFF9B2257)) + startSelect()),
            ControllerSpec("GBA", listOf("gba"), listOf(round("A", A), round("B", B), pill("L", L1), pill("R", R1)) + startSelect()),
            ControllerSpec(
                "N64",
                listOf("n64"),
                listOf(round("A", B, BLUE), round("B", Y, GREEN), pill("Z", L2), pill("L", L1), pill("R", R1), pill("Start", START)),
            ),
            ControllerSpec(
                "PlayStation",
                listOf("psx"),
                psSymbols.map { (label, key, icon) ->
                    val color =
                        when (label) {
                            "△" -> 0xFF2FB57A
                            "○" -> RED
                            "✕" -> 0xFF4C7BE0
                            else -> 0xFFD66FA8
                        }
                    ButtonSpec(label, key, Kind.ROUND, icon = icon, color = color)
                } + listOf(pill("L1", L1), pill("R1", R1), pill("L2", L2), pill("R2", R2)) +
                    listOf(ButtonSpec("L3", L3, dualShockOnly = true), ButtonSpec("R3", R3, dualShockOnly = true)) + startSelect(),
            ),
            ControllerSpec(
                "PSP",
                listOf("psp"),
                psSymbols.map { (label, key, icon) -> ButtonSpec(label, key, Kind.ROUND, icon = icon) } + listOf(pill("L", L1), pill("R", R1)) + startSelect(),
            ),
            ControllerSpec("DS", listOf("nds"), listOf(round("A", A), round("B", B), round("X", X), round("Y", Y), pill("L", L1), pill("R", R1)) + startSelect()),
            ControllerSpec("3DS", listOf("3ds"), listOf(round("A", A), round("B", B), round("X", X), round("Y", Y), pill("L", L1), pill("R", R1)) + startSelect()),
            ControllerSpec("Master System", listOf("sms"), listOf(round("1", B), round("2", A), pill("Pause", START))),
            ControllerSpec("Game Gear", listOf("gg"), listOf(round("1", B), round("2", A), pill("Start", START))),
            ControllerSpec("PC Engine", listOf("pce"), listOf(round("II", A), round("I", B)) + startSelect(start = "Run")),
            ControllerSpec("Atari 2600", listOf("atari2600"), listOf(round("Fire", B), pill("Diff. A", L1), pill("Diff. B", L2)) + startSelect(start = "Reset")),
            ControllerSpec("Atari 7800", listOf("atari7800"), listOf(round("1", B), round("2", A)) + startSelect(start = "Pause")),
            ControllerSpec("Lynx", listOf("lynx"), listOf(round("A", A), round("B", B), pill("Option 1", L1), pill("Option 2", R1), pill("Pause", START))),
            ControllerSpec("Neo Geo Pocket", listOf("ngp", "ngc"), listOf(round("A", B), round("B", A), pill("Option", START))),
            ControllerSpec("WonderSwan", listOf("ws", "wsc"), listOf(round("A", A), round("B", B), pill("Start", START))),
            ControllerSpec(
                "Neo Geo",
                listOf("neogeo"),
                listOf(round("A", B, RED), round("B", A, YELLOW), round("C", Y, GREEN), round("D", X, BLUE), pill("Coin", SELECT), pill("Start", START)),
            ),
            ControllerSpec(
                "CPS2",
                listOf("cps2"),
                listOf(
                    round("1", Y, RED, "Light punch"),
                    round("2", X, RED, "Medium punch"),
                    round("3", L1, RED, "Heavy punch"),
                    round("4", B, BLUE, "Light kick"),
                    round("5", A, BLUE, "Medium kick"),
                    round("6", R1, BLUE, "Heavy kick"),
                    pill("Coin", SELECT),
                    pill("Start", START),
                ),
            ),
            ControllerSpec(
                "Arcade",
                listOf("fbneo", "mame2003plus"),
                listOf(round("1", B), round("2", A), round("3", Y), round("4", X), round("5", L1), round("6", R1), pill("Coin", SELECT), pill("Start", START)),
            ),
        )

    fun forSystem(mappingId: String): ControllerSpec? = all.firstOrNull { mappingId in it.systemIds }
}
