package com.ownnet.syto.voice

import android.content.Context

/** Spike knobs. SharedPreferences on purpose: no database until phase 4 (D-004). */
class SpikeSettings(context: Context) {
    private val prefs = context.getSharedPreferences("spike", Context.MODE_PRIVATE)

    /** Test mode: answer every incoming call, even from contacts. */
    var answerAll: Boolean
        get() = prefs.getBoolean(KEY_ANSWER_ALL, false)
        set(value) = prefs.edit().putBoolean(KEY_ANSWER_ALL, value).apply()

    /** Seconds of ringing before the spike answers. */
    var answerDelaySec: Int
        get() = prefs.getInt(KEY_ANSWER_DELAY, DEFAULT_ANSWER_DELAY_SEC)
        set(value) = prefs.edit().putInt(KEY_ANSWER_DELAY, value.coerceIn(0, 30)).apply()

    private companion object {
        const val KEY_ANSWER_ALL = "answer_all"
        const val KEY_ANSWER_DELAY = "answer_delay_sec"
        const val DEFAULT_ANSWER_DELAY_SEC = 2
    }
}
