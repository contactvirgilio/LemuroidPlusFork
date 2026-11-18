package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.game.GameService
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelInput
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelRetroGameView
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelSaves
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelSideEffects
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTilt
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.rumble.RumbleManager
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.common.longAnimationDuration
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.game.GameLoader
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.inputevents.InputEvent
import gg.padkit.inputstate.InputState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class BaseGameScreenViewModel(
    private val appContext: Context,
    private val game: Game,
    settingsManager: SettingsManager,
    inputDeviceManager: InputDeviceManager,
    controllerConfigsManager: ControllerConfigsManager,
    private val system: GameSystem,
    private val systemCoreConfig: SystemCoreConfig,
    private val sharedPreferences: SharedPreferences,
    savesManager: SavesManager,
    private val statesManager: StatesManager,
    statesPreviewManager: StatesPreviewManager,
    coreVariablesManager: CoreVariablesManager,
    rumbleManager: RumbleManager,
) : ViewModel(), DefaultLifecycleObserver {
    class Factory(
        private val appContext: Context,
        private val game: Game,
        private val settingsManager: SettingsManager,
        private val inputDeviceManager: InputDeviceManager,
        private val controllerConfigsManager: ControllerConfigsManager,
        private val system: GameSystem,
        private val systemCoreConfig: SystemCoreConfig,
        private val sharedPreferences: SharedPreferences,
        private val savesManager: SavesManager,
        private val statesManager: StatesManager,
        private val statesPreviewManager: StatesPreviewManager,
        private val coreVariablesManager: CoreVariablesManager,
        private val rumbleManager: RumbleManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BaseGameScreenViewModel(
                appContext,
                game,
                settingsManager,
                inputDeviceManager,
                controllerConfigsManager,
                system,
                systemCoreConfig,
                sharedPreferences,
                savesManager,
                statesManager,
                statesPreviewManager,
                coreVariablesManager,
                rumbleManager,
            ) as T
        }
    }

    private val sideEffects = GameViewModelSideEffects(viewModelScope)
    val retroGameView =
        GameViewModelRetroGameView(
            appContext,
            system,
            systemCoreConfig,
            settingsManager,
            coreVariablesManager,
            sideEffects,
            rumbleManager,
            viewModelScope,
        )
    private val tilt = GameViewModelTilt(appContext, settingsManager)
    private val inputs =
        GameViewModelInput(
            appContext,
            system,
            systemCoreConfig,
            inputDeviceManager,
            controllerConfigsManager,
            retroGameView,
            tilt,
            sideEffects,
            viewModelScope,
        )
    private val touchControls =
        GameViewModelTouchControls(
            settingsManager,
            TouchControllerSettingsManager(sharedPreferences),
            retroGameView,
            inputs,
            tilt,
            sideEffects,
            viewModelScope,
        )
    private val saves =
        GameViewModelSaves(
            appContext,
            system,
            game,
            systemCoreConfig,
            retroGameView,
            settingsManager,
            savesManager,
            statesManager,
            statesPreviewManager,
            sideEffects,
        )

    val loadingState = MutableStateFlow(false)

    private inline fun withLoading(block: () -> Unit) {
        loadingState.value = true
        block()
        loadingState.value = false
    }

    fun getGameState(): Flow<GameViewModelRetroGameView.GameState> {
        return retroGameView.getGameState()
    }

    fun getSideEffects(): Flow<GameViewModelSideEffects.UiEffect> {
        return sideEffects.getUiEffects()
    }

    fun getTiltConfiguration(): Flow<TiltConfiguration> {
        return tilt.getTiltConfiguration()
    }

    fun getSimulatedTiltEvents(): Flow<InputState> {
        return tilt.getSimulatedTiltEvents()
    }

    fun getTouchControlsSettings(
        density: Density,
        insets: WindowInsets,
    ): Flow<TouchControllerSettingsManager.Settings?> {
        return touchControls.getTouchControlsSettings(density, insets)
    }

    fun getTouchHapticFeedbackMode(): Flow<HapticFeedbackMode> {
        return touchControls.getTouchHapticFeedbackMode()
    }

    fun createRetroView(
        context: Context,
        lifecycle: LifecycleOwner,
    ): GLRetroView {
        val (gameData, result) = retroGameView.createRetroView(context, lifecycle)
        viewModelScope.launch {
            gameData.quickSaveData?.let {
                saves.restoreAutoSaveAsync(it)
            }
        }
        return result
    }

    suspend fun loadGame(
        applicationContext: Context,
        game: Game,
        systemCoreConfig: SystemCoreConfig,
        gameLoader: GameLoader,
        requestLoadSave: Boolean,
    ) {
        Timber.i("Calling load game: $game")
        retroGameView.initialize(applicationContext, game, systemCoreConfig, gameLoader, requestLoadSave)
    }

    fun showEditControls(show: Boolean) {
        touchControls.showEditControls(show)
    }

    fun isEditControlShown(): Flow<Boolean> {
        return touchControls.isEditControlsShown()
    }

    fun updateTouchControllerSettings(touchControllerSettings: TouchControllerSettingsManager.Settings) {
        touchControls.updateTouchControllerSettings(touchControllerSettings)
    }

    fun resetTouchControls() {
        touchControls.resetTouchControls()
    }

    fun onScreenOrientationChanged(orientation: TouchControllerSettingsManager.Orientation) {
        touchControls.updateScreenOrientation(orientation)
    }

    fun isTouchControllerVisible(): Flow<Boolean> {
        return touchControls.isTouchControllerVisible()
    }

    // Current value, for the menu: "Edit touch controls" only makes sense when they're on screen
    // (they're hidden while a gamepad, like the Retroid's own buttons, is in use).
    private val touchControlsVisibleState =
        touchControls.isTouchControllerVisible()
            .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun areTouchControlsVisible(): Boolean = touchControlsVisibleState.value

    fun getTouchControllerConfig(): Flow<ControllerConfig> {
        return touchControls.getTouchControllerConfig()
    }

    fun changeTiltConfiguration(tiltConfig: TiltConfiguration) {
        tilt.changeTiltConfiguration(tiltConfig)
    }

    fun isMenuPressed(): Flow<Boolean> {
        return touchControls.isMenuPressed()
    }

    suspend fun saveSlot(index: Int) {
        if (loadingState.value) return
        withLoading {
            saves.saveSlot(index)
        }
    }

    suspend fun loadSlot(index: Int) {
        if (loadingState.value) return
        withLoading {
            saves.loadSlot(index)
        }
    }

    fun saveQuickSave() {
        Timber.d("Saving quick save")
        if (loadingState.value) return
        withLoading {
            saves.saveQuickSave()
        }
    }

    fun loadQuickSave() {
        Timber.d("Loading quick save")
        if (loadingState.value) return
        withLoading {
            saves.loadQuickSave()
        }
    }

    /**
     * Called once a card has been displayed, so it isn't shown again when the overlay comes back
     * (e.g. after the loading screen of a save made from the menu).
     */
    fun onOverlayCardShown(id: Long) {
        if (overlayCardState.value?.id == id) overlayCardState.value = null
    }

    // Fast forward is either off or 2x.
    private val fastForwardSpeedState = MutableStateFlow(1)

    fun isFastForwardSupported(): Boolean = system.fastForwardSupport

    fun isFastForwardEnabled(): Flow<Boolean> = fastForwardSpeedState.map { it > 1 }

    fun getFastForwardSpeed(): Flow<Int> = fastForwardSpeedState

    fun toggleFastForward() {
        val currentIndex = FAST_FORWARD_SPEEDS.indexOf(fastForwardSpeedState.value)
        val next = FAST_FORWARD_SPEEDS[(currentIndex + 1) % FAST_FORWARD_SPEEDS.size]
        setFastForwardSpeed(next)
    }

    fun setFastForwardEnabled(enabled: Boolean) {
        setFastForwardSpeed(if (enabled) FAST_FORWARD_SPEEDS[1] else 1)
    }

    private fun setFastForwardSpeed(speed: Int) {
        Timber.d("Setting fast forward speed: $speed")
        val gameView = retroGameView.retroGameView ?: return
        gameView.frameSpeed = speed
        fastForwardSpeedState.value = speed
        applyAudio()
    }

    // The mute choice is remembered per game, so a game muted once starts muted next time.
    private fun muteKey() = "pref_key_game_muted_${game.id}"

    private val audioEnabledState = MutableStateFlow(!sharedPreferences.getBoolean(muteKey(), false))

    fun isAudioEnabled(): Flow<Boolean> = audioEnabledState

    private val muteNudgesState = MutableStateFlow(0)

    /** Goes up each time a volume key is pressed while this game is muted. */
    fun getMuteNudges(): StateFlow<Int> = muteNudgesState

    /**
     * Volume up or down while this game is muted: the muted speaker shortcut pops out so the user
     * sees why there's no sound. The game stays muted (other audio, e.g. music, is untouched).
     */
    fun onVolumeKey() {
        if (!audioEnabledState.value) muteNudgesState.value++
    }

    fun setAudioEnabled(enabled: Boolean) {
        audioEnabledState.value = enabled
        sharedPreferences.edit().putBoolean(muteKey(), !enabled).apply()
        applyAudio()
    }

    /**
     * Sound plays only when it isn't muted and fast forward is off. Fast forward silences the game
     * without touching the mute choice, so stopping it brings the sound back only if it was on.
     */
    private fun applyAudio() {
        val gameView = retroGameView.retroGameView ?: return
        gameView.audioEnabled = audioEnabledState.value && fastForwardSpeedState.value <= 1
    }

    fun toggleMute() {
        setAudioEnabled(!audioEnabledState.value)
    }

    fun areStatesSupported(): Boolean = systemCoreConfig.statesSupported

    // Picture size reported by the core while the game runs (arcade, DOS): their size isn't
    // known in advance. Null until the core reports it.
    private val runtimeProfile = MutableStateFlow<GameDisplayOptions.Profile?>(null)

    fun getRuntimeProfile(): StateFlow<GameDisplayOptions.Profile?> = runtimeProfile

    // Only OLED and Glow unless shell skins are on in Settings, or while the picture's shape is
    // still unknown (a colour has to hug the picture).
    private fun lookCount(): Int =
        if (!GameShellSkin.isEnabled(appContext) || pictureAspectRatio() == null) 2 else GameShellSkin.count

    private val lookState =
        MutableStateFlow(
            GameShellSkin.load(appContext, game).let {
                // A colour saved for a game whose shape comes later (arcade) is applied once known.
                if (it >= GameShellSkin.count || (it >= 2 && !GameShellSkin.isEnabled(appContext))) GameShellSkin.GLOW else it
            },
        )

    private val immersiveState = MutableStateFlow(lookState.value == GameShellSkin.GLOW)

    fun isImmersiveEnabled(): StateFlow<Boolean> = immersiveState

    /** Shape of the game picture (width / height), or null when it varies (arcade). */
    fun pictureAspectRatio(): Float? =
        com.swordfish.lemuroid.app.shared.game.viewmodel.GamePreviewCropper.aspectRatioFor(system.id)
            // Arcade can be horizontal or vertical, but Neo Geo and CPS3 games are all 4:3.
            ?: if (NeoGeoSets.isNeoGeo(game.fileName) || CpsSets.generation(game.fileName) == 3) 4f / 3f else null
            // Other arcade games and DOS: the shape the core reports once the game runs.
            ?: runtimeProfile.value?.aspectRatio

    /** The look around the picture: Off, Glow, or a shell colour (see [GameShellSkin]). */
    fun getLook(): StateFlow<Int> = lookState

    /** Next look for this game: Off, Glow, Teal, ...; applies at once and is remembered for the game. */
    fun toggleImmersive() {
        val count = lookCount()
        val next = (lookState.value + 1) % count
        lookState.value = next
        val glow = next == GameShellSkin.GLOW
        immersiveState.value = glow
        retroGameView.retroGameView?.immersiveModeEnabled = glow
        GameShellSkin.save(appContext, game, next)
        overlayCardState.value =
            OverlayCard(
                appContext.getString(R.string.game_overlay_skin_title, next + 1, count),
                appContext.getString(GameShellSkin.nameRes(next)),
                next,
                count,
                swatch = GameShellSkin.shellFor(next)?.color,
            )
    }

    /** Opens the in-game menu, the same one as the L1+R1 / menu shortcut. */
    fun openMenu() {
        if (loadingState.value) return
        sideEffects.showMenu(tilt, inputs)
    }

    /** A card in the middle of the game ("Filter 3 · CRT") with the position in the list. */
    data class OverlayCard(
        val title: String,
        val value: String,
        val index: Int,
        val count: Int,
        val id: Long = System.nanoTime(),
        val hint: String? = null,
        // A small colour sample next to the value (skins).
        val swatch: Long? = null,
    )

    private val overlayCardState = MutableStateFlow<OverlayCard?>(null)

    /** A message in the same card ("Auto save · State resumed"), with no position bars. */
    fun showCard(
        title: String,
        value: String,
    ) {
        overlayCardState.value = OverlayCard(title, value, 0, 0)
    }

    fun getOverlayCards(): Flow<OverlayCard?> = overlayCardState

    private val displayOptions = GameDisplayOptions(sharedPreferences, system.id)

    // Null until the game view exists, or when an HD mode shader (not in the list) is active.
    private val filterState = MutableStateFlow<GameDisplayOptions.Filter?>(null)

    fun getFilter(): Flow<GameDisplayOptions.Filter?> = filterState

    fun cycleFilter() {
        val gameView = retroGameView.retroGameView ?: return
        val filters = GameDisplayOptions.Filter.values()
        val next = filters[((filterState.value?.ordinal ?: -1) + 1) % filters.size]
        gameView.shader = next.shader
        filterState.value = next
        displayOptions.saveFilter(next)
        overlayCardState.value =
            OverlayCard(
                appContext.getString(R.string.game_overlay_filter_title, next.ordinal + 1),
                appContext.getString(next.labelRes),
                next.ordinal,
                filters.size,
            )
    }

    init {
        // Before the scaling profile: a single-screen DS game has a different picture size.
        DsSingleScreen.start(appContext, system.id, systemCoreConfig)
    }

    private val staticProfile = GameDisplayOptions.profileFor(system.id, game.fileName)

    // Known in advance for consoles; reported by the core for arcade and DOS.
    private val scalingProfile: GameDisplayOptions.Profile?
        get() = staticProfile ?: runtimeProfile.value

    // Every system now: arcade and DOS get their size from the core.
    fun isScalingSupported(): Boolean = true

    /** Whether the picture fills its viewport exactly (Stretch, and Pixel-perfect once sized). */
    fun fillsViewport(scaling: GameDisplayOptions.Scaling): Boolean =
        scaling == GameDisplayOptions.Scaling.STRETCH ||
            (scaling == GameDisplayOptions.Scaling.PIXEL_PERFECT && scalingProfile?.baseWidth != null)

    // Stretch only looks right on 4:3-ish screens (e.g. Retroid Pocket Classic); on phones and
    // widescreen devices it would pull the picture far out of shape, so it isn't offered there.
    private val scalingModes =
        GameDisplayOptions.Scaling.values().filter {
            // Fit and Integer aren't offered (owner, 2026-11-18): Fine integer covers both. Fit is
            // still used inside for games without a fixed picture size (most arcade, DOS).
            it != GameDisplayOptions.Scaling.FIT &&
                it != GameDisplayOptions.Scaling.INTEGER &&
                (it != GameDisplayOptions.Scaling.STRETCH || GameShortcutsSetting.defaultNested(appContext))
        }

    private val scalingState =
        MutableStateFlow(
            displayOptions.savedScaling().takeIf { it in scalingModes } ?: GameDisplayOptions.Scaling.FINE_INTEGER,
        )

    fun getScaling(): StateFlow<GameDisplayOptions.Scaling> = scalingState

    /** Position of a scaling mode among the ones offered here, from 1 (shown on the shortcut). */
    fun scalingNumber(scaling: GameDisplayOptions.Scaling): Int = scalingModes.indexOf(scaling) + 1

    fun cycleScaling() {
        val modes = scalingModes
        val next = modes[(modes.indexOf(scalingState.value) + 1) % modes.size]
        scalingState.value = next
        displayOptions.saveScaling(next)
        overlayCardState.value =
            OverlayCard(
                appContext.getString(R.string.game_overlay_scaling_title, modes.indexOf(next) + 1),
                appContext.getString(next.labelRes),
                modes.indexOf(next),
                modes.size,
            )
    }

    /** Viewport for the current scaling, from the area the layout gives the game (in pixels). */
    fun computeScaledViewport(
        scaling: GameDisplayOptions.Scaling,
        alignment: com.swordfish.libretrodroid.ViewportAlignment,
        viewWidth: Float,
        viewHeight: Float,
        area: android.graphics.RectF,
    ) = GameDisplayOptions.computeViewport(
        scaling,
        scalingProfile,
        alignment,
        viewWidth,
        viewHeight,
        area.left,
        area.top,
        area.right,
        area.bottom,
    )

    init {
        // Applies the filter remembered for this system (or reads the one from Settings) once the
        // game view exists.
        viewModelScope.launch {
            val gameView = retroGameView.retroGameViewFlow()
            val saved = displayOptions.savedFilter()
            if (saved != null) {
                gameView.shader = saved.shader
                filterState.value = saved
            } else {
                filterState.value = GameDisplayOptions.Filter.fromShader(gameView.shader)
            }
            // Glow follows the look (an arcade game saved with a colour falls back to Glow).
            gameView.immersiveModeEnabled = lookState.value == GameShellSkin.GLOW
            // Starts muted if this game was left muted last time.
            applyAudio()
        }
    }

    /** Save from the menu: same as before, plus the "Saved to State N" card. */
    suspend fun saveSlotFromMenu(index: Int) {
        saveSlot(index)
        val slots = runCatching { statesManager.getSavedSlotsInfo(game, systemCoreConfig.coreID).size }.getOrDefault(4)
        overlayCardState.value =
            OverlayCard(
                appContext.getString(R.string.game_overlay_saved_title),
                appContext.getString(R.string.game_overlay_state_number, index + 1),
                index,
                slots,
            )
    }

    private var quickSaveInProgress = false

    /**
     * Saves a state into the first empty slot, or over the oldest one when all four are used.
     * Runs in the background without the loading state, so the game screen and its buttons stay put.
     */
    fun quickSaveToNextSlot() {
        if (loadingState.value || quickSaveInProgress) return
        quickSaveInProgress = true
        viewModelScope.launch {
            try {
                val slots = statesManager.getSavedSlotsInfo(game, systemCoreConfig.coreID)
                if (slots.isEmpty()) return@launch
                val index =
                    slots.indexOfFirst { !it.exists }
                        .takeIf { it >= 0 }
                        ?: slots.indices.minBy { slots[it].date }
                saves.saveSlot(index)
                // Same card as filter/scaling: "Saved to" / "State 3", with the slot highlighted.
                overlayCardState.value =
                    OverlayCard(
                        appContext.getString(R.string.game_overlay_saved_title),
                        appContext.getString(R.string.game_overlay_state_number, index + 1),
                        index,
                        slots.size,
                    )
            } catch (e: Throwable) {
                Timber.e(e, "Quick save failed")
            } finally {
                quickSaveInProgress = false
            }
        }
    }

    suspend fun reset() =
        withLoading {
            try {
                delay(appContext.longAnimationDuration().toLong())
                retroGameView.retroGameViewFlow().reset()
            } catch (e: Throwable) {
                Timber.e(e, "Error in reset")
            }
        }

    fun requestFinish() {
        if (loadingState.value) return
        viewModelScope.launch {
            withLoading {
                val snapshot = saves.captureSaveSnapshot(true) ?: return@launch
                saves.writeSaveSnapshot(snapshot)
                if (snapshot.autoSave != null) saves.takeAutoSavePreview()
                sideEffects.requestSuccessfulFinish()
            }
        }
    }

    /**
     * Closes the game after it was left with Home (home screen mode). The game view is already
     * paused, so the save is captured without the emulation thread, like a background save.
     */
    fun requestFinishFromBackground() {
        if (loadingState.value) return
        viewModelScope.launch {
            runCatching {
                val snapshot = saves.captureSaveSnapshot(false)
                saves.writeSaveSnapshot(snapshot)
            }.onFailure { Timber.e(it, "Save before closing the game failed") }
            sideEffects.requestSuccessfulFinish()
        }
    }

    fun requestBackgroundSave() {
        if (loadingState.value) return
        GameService.schedule {
            val snapshot = saves.captureSaveSnapshot(false)
            saves.writeSaveSnapshot(snapshot)
        }
    }

    /** When the touch controls last reported anything (a press, a release, a move). */
    @Volatile
    var lastTouchControlInputAt = 0L
        private set

    fun handleVirtualInputEvent(events: List<InputEvent>) {
        if (events.isNotEmpty()) {
            lastTouchControlInputAt = android.os.SystemClock.uptimeMillis()
            lastPlayerInputAt = lastTouchControlInputAt
        }
        touchControls.handleVirtualInputEvent(events)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        owner.lifecycle.addObserver(tilt)
        owner.lifecycle.addObserver(inputs)
        owner.lifecycle.addObserver(retroGameView)
        owner.lifecycle.addObserver(touchControls)
    }

    fun sendKeyEvent(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        lastPlayerInputAt = android.os.SystemClock.uptimeMillis()
        return inputs.sendKeyEvent(keyCode, event)
    }

    fun sendMotionEvent(event: MotionEvent): Boolean {
        lastPlayerInputAt = android.os.SystemClock.uptimeMillis()
        return inputs.sendMotionEvent(event)
    }

    // Arcade and DOS: read the picture size from the core while the game runs (it is only known
    // once the game has loaded, and a few games change it).
    init {
        if (staticProfile == null) {
            viewModelScope.launch {
                while (true) {
                    delay(RUNTIME_SIZE_POLL_MILLIS)
                    val profile = runCatching { coreReportedProfile() }.getOrNull() ?: continue
                    if (profile != runtimeProfile.value) runtimeProfile.value = profile
                }
            }
        }
    }

    private fun coreReportedProfile(): GameDisplayOptions.Profile? {
        val width = com.swordfish.libretrodroid.LibretroDroid.getGameWidth()
        val height = com.swordfish.libretrodroid.LibretroDroid.getGameHeight()
        if (width <= 0 || height <= 0) return null
        val aspect = com.swordfish.libretrodroid.LibretroDroid.getGameAspectRatio().takeIf { it > 0f } ?: (width.toFloat() / height)
        // A core that asks for a quarter turn (vertical arcade games) is shown on its side.
        return if (com.swordfish.libretrodroid.LibretroDroid.getGameRotation() % 2 == 1) {
            GameDisplayOptions.Profile(width, 1f / aspect, height)
        } else {
            GameDisplayOptions.Profile(height, aspect, width)
        }
    }

    // Timed auto-save: every 5 minutes of play (game on screen, menu closed) into the regular
    // auto-save, the one also written when leaving the game. Skipped while nothing was pressed
    // since the last one; then it checks again every second. Each one flashes the save shortcut.
    @Volatile
    private var lastPlayerInputAt = 0L

    @Volatile
    private var gameResumed = false

    private val autoSaveFlashes = MutableStateFlow(0)

    fun getAutoSaveFlashes(): StateFlow<Int> = autoSaveFlashes

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        gameResumed = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        gameResumed = false
    }

    init {
        viewModelScope.launch {
            var playedMillis = 0L
            var lastAutoSaveAt = 0L
            while (true) {
                delay(AUTO_SAVE_TICK_MILLIS)
                if (!gameResumed || loadingState.value) continue
                playedMillis += AUTO_SAVE_TICK_MILLIS
                if (playedMillis < AUTO_SAVE_INTERVAL_MILLIS) continue
                if (lastPlayerInputAt <= lastAutoSaveAt) continue
                try {
                    val snapshot = saves.captureSaveSnapshot(true) ?: continue
                    saves.writeSaveSnapshot(snapshot)
                    if (snapshot.autoSave != null) {
                        saves.takeAutoSavePreview()
                        autoSaveFlashes.value += 1
                    }
                } catch (e: Throwable) {
                    Timber.e(e, "Timed auto-save failed")
                }
                lastAutoSaveAt = android.os.SystemClock.uptimeMillis()
                playedMillis = 0L
            }
        }
    }

    companion object {
        private val FAST_FORWARD_SPEEDS = listOf(1, 2)
        private const val AUTO_SAVE_TICK_MILLIS = 1_000L
        private const val RUNTIME_SIZE_POLL_MILLIS = 500L
        private const val AUTO_SAVE_INTERVAL_MILLIS = 5 * 60_000L
    }
}
