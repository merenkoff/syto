package com.ownnet.syto.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Phase 1.2: speaks one phrase through the system TTS engine and reports start / end / error
 * to the spike log. Initialisation is asynchronous; a speak() issued before the engine is ready
 * is queued and fired from onInit.
 */
class Talker(context: Context, private val tag: String) {
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var failed = false
    private var pending: (() -> Unit)? = null
    private var onDone: (() -> Unit)? = null
    private var utteranceCounter = 0
    private var speakStartedAt = 0L

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        main.post {
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val engine = runCatching { tts.defaultEngine }.getOrNull()
                val engines = runCatching { tts.engines.joinToString { it.name } }.getOrNull()
                SpikeLog.log(tag, "engine ready: default=$engine installed=[$engines]")
                pending?.invoke()
            } else {
                failed = true
                SpikeLog.log(tag, "engine init FAILED status=$status")
                pending = null
                onDone?.invoke()
            }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                main.post {
                    speakStartedAt = System.currentTimeMillis()
                    SpikeLog.log(tag, "onStart $utteranceId")
                }
            }

            override fun onDone(utteranceId: String) {
                main.post {
                    SpikeLog.log(tag, "onDone $utteranceId after ${System.currentTimeMillis() - speakStartedAt} ms")
                    onDone?.invoke()
                    onDone = null
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = onError(utteranceId, -1)

            override fun onError(utteranceId: String, errorCode: Int) {
                main.post {
                    SpikeLog.log(tag, "onError $utteranceId code=$errorCode")
                    onDone?.invoke()
                    onDone = null
                }
            }
        })
    }

    /**
     * Speak [text] in [locale] on audio [stream] (AudioManager.STREAM_VOICE_CALL or STREAM_MUSIC).
     * [onDone] fires once on the main thread: after the utterance ends, on error, or if the engine is unusable.
     */
    fun speak(text: String, locale: Locale, stream: Int, rate: Float = 1f, onDone: () -> Unit) {
        if (failed) {
            SpikeLog.log(tag, "speak skipped: engine failed")
            onDone()
            return
        }
        if (!ready) {
            SpikeLog.log(tag, "engine not ready yet, queueing")
            pending = { speak(text, locale, stream, rate, onDone) }
            return
        }
        pending = null
        this.onDone = onDone

        val langResult = tts.setLanguage(locale)
        SpikeLog.log(tag, "setLanguage($locale) -> ${langResultName(langResult)} voice=${runCatching { tts.voice?.name }.getOrNull()}")
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            SpikeLog.log(tag, "language unusable, speaking anyway with engine default")
        }

        tts.setSpeechRate(rate)
        val usage = when (stream) {
            AudioManager.STREAM_VOICE_CALL -> AudioAttributes.USAGE_VOICE_COMMUNICATION
            AudioManager.STREAM_ALARM -> AudioAttributes.USAGE_ALARM
            else -> AudioAttributes.USAGE_MEDIA
        }
        tts.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        val id = "u${++utteranceCounter}"
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, stream)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
        SpikeLog.log(tag, "speak $id stream=${streamName(stream)} rate=$rate result=${if (result == TextToSpeech.SUCCESS) "queued" else "ERROR $result"} text=\"$text\"")
        if (result != TextToSpeech.SUCCESS) {
            this.onDone = null
            onDone()
        }
    }

    fun stop() {
        pending = null
        onDone = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
        SpikeLog.log(tag, "engine shut down")
    }

    private fun langResultName(r: Int) = when (r) {
        TextToSpeech.LANG_AVAILABLE -> "LANG_AVAILABLE"
        TextToSpeech.LANG_COUNTRY_AVAILABLE -> "LANG_COUNTRY_AVAILABLE"
        TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> "LANG_COUNTRY_VAR_AVAILABLE"
        TextToSpeech.LANG_MISSING_DATA -> "LANG_MISSING_DATA"
        TextToSpeech.LANG_NOT_SUPPORTED -> "LANG_NOT_SUPPORTED"
        else -> "code $r"
    }

    companion object {
        fun streamName(stream: Int) = when (stream) {
            AudioManager.STREAM_VOICE_CALL -> "VOICE_CALL"
            AudioManager.STREAM_MUSIC -> "MUSIC"
            AudioManager.STREAM_ALARM -> "ALARM"
            else -> "stream $stream"
        }
    }
}
