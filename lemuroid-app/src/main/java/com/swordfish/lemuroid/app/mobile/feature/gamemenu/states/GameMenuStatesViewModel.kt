package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import com.swordfish.lemuroid.app.shared.GameMenuContract
import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.gamemenu.GameMenuHelper
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import kotlinx.coroutines.flow.flow

class GameMenuStatesViewModel(
    private val application: Application,
    private val gameMenuRequest: GameMenuActivity.GameMenuRequest,
    private val statesManager: StatesManager,
    private val disableMissingEntries: Boolean,
    private val statesPreviewManager: StatesPreviewManager,
) : ViewModel() {
    class Factory(
        private val application: Application,
        private val gameMenuRequest: GameMenuActivity.GameMenuRequest,
        private val statesManager: StatesManager,
        private val disableMissingEntries: Boolean,
        private val statesPreviewManager: StatesPreviewManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameMenuStatesViewModel(
                application,
                gameMenuRequest,
                statesManager,
                disableMissingEntries,
                statesPreviewManager,
            ) as T
        }
    }

    data class StateEntry(
        val title: String,
        val description: String,
        val enabled: Boolean,
        val preview: Bitmap?,
        // The most recently saved slot, marked with a "Latest" pill.
        val latest: Boolean = false,
        // Slot to load or save; GameMenuContract.AUTO_SLOT for the auto-save (Load only).
        val slot: Int = 0,
        val auto: Boolean = false,
    )

    data class State(val entries: List<StateEntry> = emptyList())

    val uiStates =
        flow {
            val slotsInfo = statesManager.getSavedSlotsInfo(gameMenuRequest.game, gameMenuRequest.coreConfig.coreID)

            val latestIndex =
                slotsInfo.indices.filter { slotsInfo[it].exists }.maxByOrNull { slotsInfo[it].date } ?: -1
            val entries =
                slotsInfo.mapIndexed { index, slotInfo ->
                    val title =
                        application.applicationContext.getString(
                            R.string.game_menu_state,
                            (index + 1).toString(),
                        )
                    val description = GameMenuHelper.getSaveStateDescription(slotInfo)
                    val isEnabled = !disableMissingEntries || slotInfo.exists
                    val preview =
                        GameMenuHelper.getSaveStateBitmap(
                            application.applicationContext,
                            statesPreviewManager,
                            slotInfo,
                            gameMenuRequest.game,
                            gameMenuRequest.coreConfig.coreID,
                            index,
                            fullImage = true,
                        )

                    StateEntry(title, description, isEnabled, preview, latest = index == latestIndex, slot = index)
                }

            // Load only: the auto-save first, with an "Auto" pill.
            val autoEntry =
                if (disableMissingEntries) {
                    val coreID = gameMenuRequest.coreConfig.coreID
                    val autoInfo = statesManager.getAutoSaveInfo(gameMenuRequest.game, coreID)
                    val autoPreview =
                        if (autoInfo.exists) {
                            statesPreviewManager.getFullPreviewForSlot(gameMenuRequest.game, coreID, GameMenuContract.AUTO_SLOT)
                        } else {
                            null
                        }
                    StateEntry(
                        title = application.applicationContext.getString(R.string.game_menu_state_auto_title),
                        description = GameMenuHelper.getSaveStateDescription(autoInfo),
                        enabled = autoInfo.exists,
                        preview = autoPreview,
                        slot = GameMenuContract.AUTO_SLOT,
                        auto = true,
                    )
                } else {
                    null
                }

            emit(State(listOfNotNull(autoEntry) + entries))
        }
}
