package com.ownnet.syto.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.VideoProfile
import java.util.Locale

/**
 * Bound by telecom while Syto is the default dialer.
 * 1.1: answers ringing calls from unknown numbers (or every call in test mode) after a short delay.
 * 1.2: once the call is active, switches the audio route, speaks the greeting, hangs up after a pause.
 * Every step goes to the spike journal.
 */
class SytoInCallService : InCallService() {
    private val handler = Handler(Looper.getMainLooper())
    private val callbacks = HashMap<Call, Call.Callback>()
    private val greeted = HashSet<Call>()
    private var talker: Talker? = null

    override fun onCreate() {
        super.onCreate()
        SpikeLog.log("svc", "InCallService created")
        // TTS init is async and takes a few hundred ms; start it now so it is ready by the time we answer.
        if (SpikeSettings(this).greet) talker = Talker(this, "tts")
    }

    override fun onDestroy() {
        SpikeLog.log("svc", "InCallService destroyed")
        handler.removeCallbacksAndMessages(null)
        talker?.shutdown()
        talker = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        val number = call.details.handle?.schemeSpecificPart
        val incoming = call.details.callDirection == Call.Details.DIRECTION_INCOMING
        val state = call.currentState()
        val known = ContactsLookup.isKnown(this, number)
        SpikeLog.log(
            "call",
            "added number=${number ?: "<hidden>"} state=${stateName(state)} " +
                "direction=${if (incoming) "incoming" else "outgoing"} known=${known ?: "no-permission"}",
        )

        CallRegistry.set(call, CallUi(number ?: "hidden number", state, known, incoming))
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                SpikeLog.log("call", "state=${stateName(newState)}")
                CallRegistry.updateState(call, newState)
                when (newState) {
                    Call.STATE_ACTIVE -> if (incoming && greeted.add(call)) startGreeting(call)
                    Call.STATE_DISCONNECTED -> {
                        val cause = call.details.disconnectCause
                        SpikeLog.log("call", "disconnect cause=${cause?.let { causeName(it) }} reason=${cause?.reason}")
                    }
                }
            }
        }
        call.registerCallback(callback)
        callbacks[call] = callback

        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )

        if (incoming && state == Call.STATE_RINGING) scheduleAutoAnswer(call, known)
    }

    private fun scheduleAutoAnswer(call: Call, known: Boolean?) {
        val settings = SpikeSettings(this)
        val reason = when {
            settings.answerAll -> "test mode: answer all"
            known == false -> "unknown number"
            known == null -> "contacts permission missing, treating as unknown"
            else -> null
        }
        if (reason == null) {
            SpikeLog.log("auto", "known contact, leaving the call to ring")
            return
        }
        val delay = settings.answerDelaySec
        SpikeLog.log("auto", "answer scheduled in ${delay}s ($reason)")
        CallRegistry.note(call, "auto-answer in ${delay}s")
        handler.postDelayed({
            val now = call.currentState()
            if (now == Call.STATE_RINGING) {
                SpikeLog.log("auto", "answer()")
                call.answer(VideoProfile.STATE_AUDIO_ONLY)
            } else {
                SpikeLog.log("auto", "skipped, state=${stateName(now)}")
            }
        }, delay * 1000L)
    }

    /** 1.2: route → greeting → pause → hangup. */
    private fun startGreeting(call: Call) {
        val settings = SpikeSettings(this)
        if (!settings.greet) {
            SpikeLog.log("greet", "disabled in settings, staying silent")
            CallRegistry.note(call, "silent (greeting off)")
            return
        }
        val talker = talker ?: Talker(this, "tts").also { talker = it }
        val route = settings.audioRoute
        val stream = settings.ttsStream
        logVolumes()
        SpikeLog.log("greet", "setAudioRoute(${CallAudioState.audioRouteToString(route)}) then speak on ${Talker.streamName(stream)}")
        // Deprecated since API 34 (requestCallEndpointChange); still the simplest way to reach SPEAKER on minSdk 29.
        @Suppress("DEPRECATION")
        setAudioRoute(route)
        CallRegistry.note(call, "greeting…")

        handler.postDelayed({
            if (call.currentState() != Call.STATE_ACTIVE) {
                SpikeLog.log("greet", "call no longer active, not speaking")
                return@postDelayed
            }
            talker.speak(settings.greeting, Locale.forLanguageTag(SpikeSettings.LANGUAGE_TAG), stream) {
                val pause = settings.hangupAfterSec
                SpikeLog.log("greet", "greeting finished, hangup in ${pause}s")
                CallRegistry.note(call, "listening window ${pause}s, then hangup")
                handler.postDelayed({
                    val now = call.currentState()
                    if (now == Call.STATE_ACTIVE) {
                        SpikeLog.log("greet", "disconnect()")
                        call.disconnect()
                    } else {
                        SpikeLog.log("greet", "hangup skipped, state=${stateName(now)}")
                    }
                }, pause * 1000L)
            }
        }, ROUTE_SETTLE_MS)
    }

    private fun logVolumes() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        fun v(stream: Int) = "${am.getStreamVolume(stream)}/${am.getStreamMaxVolume(stream)}"
        SpikeLog.log("audio", "volume VOICE_CALL=${v(AudioManager.STREAM_VOICE_CALL)} MUSIC=${v(AudioManager.STREAM_MUSIC)} mode=${am.mode}")
    }

    override fun onCallRemoved(call: Call) {
        SpikeLog.log("call", "removed")
        callbacks.remove(call)?.let(call::unregisterCallback)
        greeted.remove(call)
        handler.removeCallbacksAndMessages(null)
        talker?.stop()
        CallRegistry.clear(call)
    }

    // Deprecated since API 34 in favour of onCallEndpointChanged; still delivered on every minSdk 29+ device.
    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        SpikeLog.log(
            "audio",
            "route=${CallAudioState.audioRouteToString(audioState.route)} muted=${audioState.isMuted} " +
                "supported=${CallAudioState.audioRouteToString(audioState.supportedRouteMask)}",
        )
    }

    private fun causeName(cause: DisconnectCause): String = when (cause.code) {
        DisconnectCause.LOCAL -> "LOCAL"
        DisconnectCause.REMOTE -> "REMOTE"
        DisconnectCause.REJECTED -> "REJECTED"
        DisconnectCause.MISSED -> "MISSED"
        DisconnectCause.BUSY -> "BUSY"
        DisconnectCause.ERROR -> "ERROR"
        DisconnectCause.CANCELED -> "CANCELED"
        DisconnectCause.RESTRICTED -> "RESTRICTED"
        DisconnectCause.OTHER -> "OTHER"
        DisconnectCause.UNKNOWN -> "UNKNOWN"
        else -> "CODE_${cause.code}"
    }

    private companion object {
        /** Give telecom a moment to actually switch the route before TTS starts. */
        const val ROUTE_SETTLE_MS = 500L
    }
}
