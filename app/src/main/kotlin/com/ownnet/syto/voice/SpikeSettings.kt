package com.ownnet.syto.voice

import android.content.Context
import android.media.AudioManager
import android.telecom.CallAudioState

/** Spike knobs. SharedPreferences on purpose: no database until phase 4 (D-004). */
class SpikeSettings(context: Context) {
    private val prefs = context.getSharedPreferences("spike", Context.MODE_PRIVATE)

    // ---- 1.1 answer ----

    /** Test mode: answer every incoming call, even from contacts. */
    var answerAll: Boolean
        get() = prefs.getBoolean(KEY_ANSWER_ALL, false)
        set(value) = prefs.edit().putBoolean(KEY_ANSWER_ALL, value).apply()

    /** Seconds of ringing before the spike answers. */
    var answerDelaySec: Int
        get() = prefs.getInt(KEY_ANSWER_DELAY, DEFAULT_ANSWER_DELAY_SEC)
        set(value) = prefs.edit().putInt(KEY_ANSWER_DELAY, value.coerceIn(0, 30)).apply()

    // ---- 1.2 speak ----

    /** Say the greeting once the call is active, then hang up after [hangupAfterSec]. */
    var greet: Boolean
        get() = prefs.getBoolean(KEY_GREET, true)
        set(value) = prefs.edit().putBoolean(KEY_GREET, value).apply()

    /**
     * AudioManager stream for the TTS output: STREAM_VOICE_CALL, STREAM_MUSIC or STREAM_ALARM.
     * Default MUSIC: on realme the caller heard nothing on VOICE_CALL and something on MUSIC (2026-09-24).
     */
    var ttsStream: Int
        get() = prefs.getInt(KEY_TTS_STREAM, AudioManager.STREAM_MUSIC)
        set(value) = prefs.edit().putInt(KEY_TTS_STREAM, value).apply()

    /** Raise the TTS stream and the call stream to max while greeting, restore after the call. */
    var boostVolume: Boolean
        get() = prefs.getBoolean(KEY_BOOST, true)
        set(value) = prefs.edit().putBoolean(KEY_BOOST, value).apply()

    /** TextToSpeech speech rate in tenths (10 = 1.0x). Slower speech survives the mic → uplink path better. */
    var speechRateTenths: Int
        get() = prefs.getInt(KEY_RATE, DEFAULT_RATE_TENTHS)
        set(value) = prefs.edit().putInt(KEY_RATE, value.coerceIn(5, 15)).apply()

    /** CallAudioState.ROUTE_SPEAKER or ROUTE_EARPIECE requested right after answer. */
    var audioRoute: Int
        get() = prefs.getInt(KEY_AUDIO_ROUTE, CallAudioState.ROUTE_SPEAKER)
        set(value) = prefs.edit().putInt(KEY_AUDIO_ROUTE, value).apply()

    var greeting: String
        get() = prefs.getString(KEY_GREETING, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_GREETING
        set(value) = prefs.edit().putString(KEY_GREETING, value).apply()

    /** Seconds between the end of the greeting and hangup. */
    var hangupAfterSec: Int
        get() = prefs.getInt(KEY_HANGUP_AFTER, DEFAULT_HANGUP_AFTER_SEC)
        set(value) = prefs.edit().putInt(KEY_HANGUP_AFTER, value.coerceIn(0, 60)).apply()

    companion object {
        const val DEFAULT_GREETING = "Це автовідповідач. Розмова записується. Скажіть, хто ви і що потрібно."
        const val DEFAULT_ANSWER_DELAY_SEC = 2
        const val DEFAULT_HANGUP_AFTER_SEC = 8
        const val DEFAULT_RATE_TENTHS = 9
        const val LANGUAGE_TAG = "uk"

        private const val KEY_ANSWER_ALL = "answer_all"
        private const val KEY_ANSWER_DELAY = "answer_delay_sec"
        private const val KEY_GREET = "greet"
        private const val KEY_TTS_STREAM = "tts_stream"
        private const val KEY_AUDIO_ROUTE = "audio_route"
        private const val KEY_GREETING = "greeting"
        private const val KEY_HANGUP_AFTER = "hangup_after_sec"
        private const val KEY_BOOST = "boost_volume"
        private const val KEY_RATE = "speech_rate_tenths"
    }
}
