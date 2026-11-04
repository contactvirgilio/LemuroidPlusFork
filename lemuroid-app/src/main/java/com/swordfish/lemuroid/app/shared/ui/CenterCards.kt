package com.swordfish.lemuroid.app.shared.ui

import android.app.Activity
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Short messages shown as the card in the middle of the screen (the same card the game buttons use
 * for the filter and scaling), instead of Android toasts.
 */
object CenterCards {
    data class Message(
        val title: String,
        val value: String,
        val millis: Long,
        val id: Long = System.nanoTime(),
    )

    private val state = MutableStateFlow<Message?>(null)
    val messages: StateFlow<Message?> = state

    /** True while a screen that can draw the card is on show. */
    @Volatile
    var hostAttached: Boolean = false

    fun show(
        title: String,
        value: String,
        millis: Long = DEFAULT_MILLIS,
    ) {
        state.value = Message(title, value, millis)
    }

    /** Shows the card, or a plain toast on screens that have no card to draw it with. */
    fun showOrToast(
        activity: Activity,
        title: String,
        value: String,
        millis: Long = DEFAULT_MILLIS,
    ) {
        if (hostAttached) {
            show(title, value, millis)
        } else {
            activity.runOnUiThread { Toast.makeText(activity, value, Toast.LENGTH_SHORT).show() }
        }
    }

    fun dismiss() {
        state.value = null
    }

    fun clear(id: Long) {
        if (state.value?.id == id) state.value = null
    }

    const val DEFAULT_MILLIS = 1500L
}
