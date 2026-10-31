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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    sharedPreferences: SharedPreferences,
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

    /** A short label shown over the game for a moment ("Saved to State 3"). */
    data class OverlayMessage(val text: String, val id: Long = System.nanoTime())

    private val overlayMessageState = MutableStateFlow<OverlayMessage?>(null)

    fun getOverlayMessages(): Flow<OverlayMessage?> = overlayMessageState

    private fun showOverlayMessage(text: String) {
        overlayMessageState.value = OverlayMessage(text)
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
    }

    private val audioEnabledState = MutableStateFlow(true)

    fun isAudioEnabled(): Flow<Boolean> = audioEnabledState

    fun setAudioEnabled(enabled: Boolean) {
        val gameView = retroGameView.retroGameView ?: return
        gameView.audioEnabled = enabled
        audioEnabledState.value = enabled
    }

    fun toggleMute() {
        setAudioEnabled(!audioEnabledState.value)
    }

    fun areStatesSupported(): Boolean = systemCoreConfig.statesSupported

    /** A card in the middle of the game ("Filter 3 · CRT") with the position in the list. */
    data class OverlayCard(
        val title: String,
        val value: String,
        val index: Int,
        val count: Int,
        val id: Long = System.nanoTime(),
    )

    private val overlayCardState = MutableStateFlow<OverlayCard?>(null)

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

    fun isScalingSupported(): Boolean = GameDisplayOptions.isScalingSupported(system.id)

    private val scalingState = MutableStateFlow(displayOptions.savedScaling())

    fun getScaling(): StateFlow<GameDisplayOptions.Scaling> = scalingState

    fun cycleScaling() {
        val modes = GameDisplayOptions.Scaling.values()
        val next = modes[(scalingState.value.ordinal + 1) % modes.size]
        scalingState.value = next
        displayOptions.saveScaling(next)
        overlayCardState.value =
            OverlayCard(
                appContext.getString(R.string.game_overlay_scaling_title, next.ordinal + 1),
                appContext.getString(next.labelRes),
                next.ordinal,
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
        system.id,
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
        }
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
                showOverlayMessage(appContext.getString(R.string.game_overlay_saved_to_slot, index + 1))
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
                sideEffects.requestSuccessfulFinish()
            }
        }
    }

    fun requestBackgroundSave() {
        if (loadingState.value) return
        GameService.schedule {
            val snapshot = saves.captureSaveSnapshot(false)
            saves.writeSaveSnapshot(snapshot)
        }
    }

    fun handleVirtualInputEvent(events: List<InputEvent>) {
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
        return inputs.sendKeyEvent(keyCode, event)
    }

    fun sendMotionEvent(event: MotionEvent): Boolean {
        return inputs.sendMotionEvent(event)
    }

    companion object {
        private val FAST_FORWARD_SPEEDS = listOf(1, 2)
    }
}
