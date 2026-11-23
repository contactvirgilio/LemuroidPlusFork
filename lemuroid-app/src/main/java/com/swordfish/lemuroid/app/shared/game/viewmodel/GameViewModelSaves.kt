package com.swordfish.lemuroid.app.shared.game.viewmodel

import com.swordfish.lemuroid.app.shared.GameMenuContract
import android.content.Context
import android.graphics.Bitmap
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.common.graphics.GraphicsUtils
import com.swordfish.lemuroid.common.graphics.takeScreenshot
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.IncompatibleStateException
import com.swordfish.lemuroid.lib.saves.SaveState
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.libretrodroid.GLRetroView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.math.roundToInt

class GameViewModelSaves(
    private val appContext: Context,
    private val system: GameSystem,
    private val game: Game,
    private val systemCoreConfig: SystemCoreConfig,
    private val retroGameView: GameViewModelRetroGameView,
    private val settingsManager: SettingsManager,
    private val savesManager: SavesManager,
    private val statesManager: StatesManager,
    private val statesPreviewManager: StatesPreviewManager,
    private val sideEffects: GameViewModelSideEffects,
) {
    private var currentQuickSave: SaveState? = null

    data class SaveSnapshot(
        val sram: ByteArray,
        val autoSave: SaveState?,
    )

    suspend fun saveSlot(index: Int) {
        getCurrentSaveState()?.let {
            statesManager.setSlotSave(game, it, systemCoreConfig.coreID, index)
            runCatching {
                takeScreenshotPreview(index)
            }
        }
    }

    /** Picture for the auto-save (shown in the Load menu); only while the game is on screen. */
    suspend fun takeAutoSavePreview() {
        runCatching { takeScreenshotPreview(GameMenuContract.AUTO_SLOT) }
    }

    suspend fun loadSlot(index: Int) {
        try {
            val saveState =
                if (index == GameMenuContract.AUTO_SLOT) {
                    statesManager.getAutoSave(game, systemCoreConfig.coreID)
                } else {
                    statesManager.getSlotSave(game, systemCoreConfig.coreID, index)
                }
            saveState?.let {
                val loaded =
                    withContext(Dispatchers.IO) {
                        loadSaveState(it)
                    }

                if (!loaded) {
                    sideEffects.showCard(
                        appContext.getString(R.string.game_card_load_title),
                        appContext.getString(R.string.game_card_load_failed),
                    )
                }
            }
        } catch (e: Throwable) {
            val errorMessageId =
                when (e) {
                    is IncompatibleStateException -> R.string.game_card_load_incompatible
                    else -> R.string.game_card_load_failed
                }
            sideEffects.showCard(
                appContext.getString(R.string.game_card_load_title),
                appContext.getString(errorMessageId),
            )
        }
    }

    suspend fun captureSaveSnapshot(useEmulationThread: Boolean): SaveSnapshot? {
        val retroGameView = retroGameView.retroGameView ?: return null
        val sramState = retroGameView.serializeSRAM(useEmulationThread)
        val autoSaveState = if (isAutoSaveEnabled()) getCurrentSaveState(useEmulationThread) else null
        return SaveSnapshot(sramState, autoSaveState)
    }

    suspend fun writeSaveSnapshot(snapshot: SaveSnapshot?) {
        if (snapshot == null) return
        Timber.i(
            "GameViewModelSaves.write game=%s core=%s writingSram=%s writingAutoSave=%s",
            game.id,
            systemCoreConfig.coreID,
            true,
            snapshot.autoSave != null,
        )
        savesManager.setSaveRAM(game, snapshot.sram)
        snapshot.autoSave?.let {
            statesManager.setAutoSave(game, systemCoreConfig.coreID, it)
            // The old picture no longer matches; callers that can, take a new one right after.
            runCatching { statesPreviewManager.deletePreviewForSlot(game, systemCoreConfig.coreID, GameMenuContract.AUTO_SLOT) }
        }
    }

    // On some cores unserialize fails with no reason. So we need to try multiple times.
    suspend fun restoreAutoSaveAsync(saveState: SaveState) {
        // PPSSPP and Mupen64 initialize some state while rendering the first frame, so we have to wait before restoring
        // the autosave. Do not change thread here. Stick to the GL one to avoid issues with PPSSPP.
        if (!isAutoSaveEnabled()) return

        try {
            retroGameView.waitGLEvent<GLRetroView.GLRetroEvents.FrameRendered>()
            if (restoreQuickSave(saveState)) {
                sideEffects.showCard(
                    appContext.getString(R.string.game_card_auto_save_title),
                    appContext.getString(R.string.game_card_auto_save_resumed),
                )
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error while loading auto-save")
        }
    }

    private fun getCurrentSaveState(useEmulationThread: Boolean = true): SaveState? {
        val retroGameView = retroGameView.retroGameView ?: return null
        val currentDisk =
            if (system.hasMultiDiskSupport) {
                retroGameView.getCurrentDisk(useEmulationThread)
            } else {
                0
            }
        return SaveState(
            retroGameView.serializeState(useEmulationThread),
            SaveState.Metadata(currentDisk, systemCoreConfig.statesVersion),
        )
    }

    private suspend fun isAutoSaveEnabled(): Boolean {
        // Always on (owner, 2026-11-19): saving costs the player nothing. The setting now only
        // decides whether games resume from it when opened (see GameInteractor.onGamePlay).
        return systemCoreConfig.statesSupported
    }

    /**
     * The game's own picture (no filter, no layout), refreshed every few seconds while playing, so
     * saves made after the game left the screen (Home, screen lock) still get a picture.
     */
    @Volatile
    private var lastRawFrame: Bitmap? = null

    /** Reads the game's own picture from the emulator (no filter). Null if the view isn't running. */
    private suspend fun captureRawFrame(): Bitmap? {
        val gameView = retroGameView.retroGameView ?: return null
        val frame =
            withTimeoutOrNull(RAW_FRAME_TIMEOUT_MILLIS) {
                suspendCancellableCoroutine<Bitmap?> { cont ->
                    gameView.captureRawFrame { bitmap -> if (cont.isActive) cont.resumeWith(Result.success(bitmap)) }
                }
            }
        if (frame != null) lastRawFrame = frame
        return frame
    }

    /** Called every few seconds while the game is on screen. */
    suspend fun refreshRawFrame() {
        runCatching { captureRawFrame() }
    }

    /** Auto-save picture for saves made in the background, from the last frame seen while playing. */
    suspend fun writeAutoSavePreviewFromLastFrame() {
        val frame = lastRawFrame ?: return
        runCatching {
            val preview = withContext(Dispatchers.Default) { sharpPreview(frame) }
            statesPreviewManager.setPreviewForSlot(game, preview, systemCoreConfig.coreID, GameMenuContract.AUTO_SLOT)
        }.onFailure { Timber.w(it, "Auto-save picture from the last frame failed") }
    }

    // DS with one screen shown: the game's own picture has both screens, so use the screen capture.
    private fun rawFrameFits(): Boolean = !(system.id == com.swordfish.lemuroid.lib.library.SystemID.NDS && com.swordfish.lemuroid.app.shared.game.DsSingleScreen.active)

    /**
     * Save picture from the game's own pixels, enlarged with crisp square pixels (whole-number
     * enlargement, then a smooth shrink to the preview size) and the system's screen shape.
     */
    private fun sharpPreview(frame: Bitmap): Bitmap {
        val sizeInDp = StatesPreviewManager.PREVIEW_SIZE_DP
        val previewSize = GraphicsUtils.convertDpToPixel(sizeInDp, appContext).roundToInt().coerceAtLeast(16)
        val aspect =
            GamePreviewCropper.aspectRatioFor(system.id)
                ?: if (frame.width >= frame.height) 4f / 3f else 3f / 4f
        val width: Int
        val height: Int
        if (aspect >= 1f) {
            width = previewSize
            height = (previewSize / aspect).roundToInt().coerceAtLeast(1)
        } else {
            height = previewSize
            width = (previewSize * aspect).roundToInt().coerceAtLeast(1)
        }
        val factor =
            maxOf(
                1,
                kotlin.math.ceil(maxOf(width.toFloat() / frame.width, height.toFloat() / frame.height)).toInt(),
            )
        val enlarged =
            if (factor == 1) frame else Bitmap.createScaledBitmap(frame, frame.width * factor, frame.height * factor, false)
        return Bitmap.createScaledBitmap(enlarged, width, height, true)
    }

    private suspend fun takeScreenshotPreview(index: Int) {
        // Preferred: the game's own picture, with no filter (owner, 2026-11-23).
        if (rawFrameFits()) {
            val frame = runCatching { captureRawFrame() }.getOrNull()
            if (frame != null) {
                val preview = withContext(Dispatchers.Default) { sharpPreview(frame) }
                statesPreviewManager.setPreviewForSlot(game, preview, systemCoreConfig.coreID, index)
                return
            }
        }
        val gameView = retroGameView.retroGameView ?: return
        val sizeInDp = StatesPreviewManager.PREVIEW_SIZE_DP
        val previewSize = GraphicsUtils.convertDpToPixel(sizeInDp, appContext).roundToInt()

        // The view covers the whole screen; the game is only a part of it. Capture at a higher
        // resolution, then keep just the game picture.
        val viewport = gameView.viewport
        val viewportFraction = maxOf(viewport.width(), viewport.height()).coerceIn(0.1f, 1f)
        val captureSize = (previewSize / viewportFraction).roundToInt()
        // The capture sometimes comes back empty (the frame isn't ready): try again a few times.
        var captured: Bitmap? = null
        for (attempt in 0 until SCREENSHOT_ATTEMPTS) {
            captured = runCatching { gameView.takeScreenshot(captureSize, 3) }.getOrNull()
            if (captured != null) break
            kotlinx.coroutines.delay(SCREENSHOT_RETRY_MILLIS)
        }
        val screenshot =
            captured ?: run {
                Timber.w("Save-state picture: capture failed for slot %d", index)
                return
            }

        val preview =
            withContext(Dispatchers.Default) {
                val gameOnly =
                    runCatching {
                        GamePreviewCropper.crop(
                            screenshot,
                            viewport,
                            gameView.viewportAlignment,
                            // Stretched: the picture fills the viewport, so no shape to cut out.
                            if (gameView.stretch) null else GamePreviewCropper.aspectRatioFor(system.id),
                        )
                    }.getOrDefault(screenshot)
                val scale = previewSize.toFloat() / maxOf(gameOnly.width, gameOnly.height)
                if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        gameOnly,
                        (gameOnly.width * scale).roundToInt().coerceAtLeast(1),
                        (gameOnly.height * scale).roundToInt().coerceAtLeast(1),
                        true,
                    )
                } else {
                    gameOnly
                }
            }
        statesPreviewManager.setPreviewForSlot(game, preview, systemCoreConfig.coreID, index)
    }

    // Now that we wait for the first rendered frame this is probably no longer needed, but we'll keep it just to be sure
    private suspend fun restoreQuickSave(saveState: SaveState): Boolean {
        var times = 10

        while (!loadSaveState(saveState) && times > 0) {
            delay(200)
            times--
        }
        return times > 0
    }

    private fun loadSaveState(saveState: SaveState): Boolean {
        val retroGameView = retroGameView.retroGameView ?: return false

        if (systemCoreConfig.statesVersion != saveState.metadata.version) {
            throw IncompatibleStateException()
        }

        if (system.hasMultiDiskSupport &&
            retroGameView.getAvailableDisks() > 1 &&
            retroGameView.getCurrentDisk() != saveState.metadata.diskIndex
        ) {
            retroGameView.changeDisk(saveState.metadata.diskIndex)
        }

        return retroGameView.unserializeState(saveState.state)
    }

    fun saveQuickSave() {
        currentQuickSave = getCurrentSaveState()
        sideEffects.showCard(
            appContext.getString(R.string.game_card_quick_save_title),
            appContext.getString(R.string.game_card_quick_save_saved),
        )
    }

    fun loadQuickSave() {
        loadSaveState(currentQuickSave ?: return)
        sideEffects.showCard(
            appContext.getString(R.string.game_card_quick_save_title),
            appContext.getString(R.string.game_card_quick_save_loaded),
        )
    }
}

private const val SCREENSHOT_ATTEMPTS = 4
private const val RAW_FRAME_TIMEOUT_MILLIS = 600L
private const val SCREENSHOT_RETRY_MILLIS = 150L
