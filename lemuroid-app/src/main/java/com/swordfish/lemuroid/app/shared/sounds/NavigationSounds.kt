package com.swordfish.lemuroid.app.shared.sounds

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.view.KeyEvent
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/**
 * A short click when the selection moves with the D-pad or a controller (never on touch, never in
 * game). Plays only when the key actually moved the selection, so it stays silent at the edges.
 */
object NavigationSounds {
    const val ENABLED_KEY = "pref_key_navigation_sounds"

    private const val VOLUME = 0.5f

    private var soundPool: SoundPool? = null
    private var clickId = 0
    private var loaded = false

    private val NAVIGATION_KEYS =
        setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        )

    private fun ensureLoaded(context: Context) {
        if (soundPool != null) return
        val pool =
            SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .build()
        pool.setOnLoadCompleteListener { _, _, status -> loaded = status == 0 }
        clickId = pool.load(context.applicationContext, R.raw.nav_click, 1)
        soundPool = pool
    }

    /** Call from Activity.dispatchKeyEvent with whether the event was handled. */
    fun onKeyEvent(
        context: Context,
        event: KeyEvent,
        handled: Boolean,
    ) {
        if (!handled || event.action != KeyEvent.ACTION_DOWN || event.keyCode !in NAVIGATION_KEYS) return
        if (!isEnabled(context)) return
        ensureLoaded(context)
        if (loaded) soundPool?.play(clickId, VOLUME, VOLUME, 1, 0, 1f)
    }

    /** Loads the sound ahead of time so the first move already clicks. */
    fun preload(context: Context) {
        if (isEnabled(context)) ensureLoaded(context)
    }

    fun isEnabled(context: Context): Boolean {
        return SharedPreferencesHelper.getSharedPreferences(context).getBoolean(ENABLED_KEY, true)
    }
}
