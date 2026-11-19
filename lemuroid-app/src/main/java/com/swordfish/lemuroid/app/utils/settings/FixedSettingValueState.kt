package com.swordfish.lemuroid.app.utils.settings

import com.alorma.compose.settings.storage.base.SettingValueState

/** A setting value that never changes (shown for locked switches). */
class FixedSettingValueState<T>(private val fixed: T) : SettingValueState<T> {
    override var value: T
        get() = fixed
        set(_) {}

    override fun reset() {}
}
