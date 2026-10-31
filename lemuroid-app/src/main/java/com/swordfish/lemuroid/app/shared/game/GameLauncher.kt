package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.content.Context
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.launcher.HomeLauncher
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.Paprium
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ): Boolean {
        val appContext = activity.applicationContext
        val gameStillClosing = GameProcessLock.isHeldByAnotherProcess(appContext)

        // As home screen, a game left with Home closes in the background (saving first), which takes
        // a moment. Wait for it instead of asking to close it. Otherwise keep the usual message.
        if (gameStillClosing && !HomeLauncher.isDefaultHome(activity)) {
            activity.displayToast(R.string.game_process_another_game_running)
            return false
        }
        if (launchInProgress) return true
        launchInProgress = true

        GlobalScope.launch {
            try {
                if (gameStillClosing && !waitForPreviousGameToClose(appContext)) {
                    withContext(Dispatchers.Main) {
                        activity.displayToast(R.string.game_process_another_game_running)
                    }
                    return@launch
                }
                startGame(activity, game, loadSave, leanback)
            } finally {
                launchInProgress = false
            }
        }

        return true
    }

    private suspend fun waitForPreviousGameToClose(appContext: Context): Boolean {
        val deadline = System.currentTimeMillis() + PREVIOUS_GAME_CLOSE_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            delay(PREVIOUS_GAME_POLL_MS)
            if (!GameProcessLock.isHeldByAnotherProcess(appContext)) return true
        }
        return false
    }

    private suspend fun startGame(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ) {
        val system = GameSystem.findById(game.systemId)
        val coreConfig =
            if (Paprium.isPaprium(game)) {
                Paprium.CORE_CONFIG
            } else {
                coresSelection.getCoreConfigForSystem(system)
            }
        gameLaunchTaskHandler.handleGameStart(activity.applicationContext, game)
        // As home screen, games run in their own task so Home goes back to the library.
        val ownTask = !leanback && HomeLauncher.isDefaultHome(activity)
        if (ownTask) pendingBackgroundWork = true
        BaseGameActivity.launchGame(activity, coreConfig, game, loadSave, leanback, ownTask)
    }

    companion object {
        /** A game was started in its own task; background work is rescheduled when the library returns. */
        @Volatile
        var pendingBackgroundWork = false

        @Volatile
        private var launchInProgress = false

        private const val PREVIOUS_GAME_CLOSE_TIMEOUT_MS = 8_000L
        private const val PREVIOUS_GAME_POLL_MS = 150L
    }
}
