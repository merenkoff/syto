package com.ownnet.syto.voice

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.VideoProfile

/**
 * Phase 1.1: bound by telecom while Syto is the default dialer. Answers ringing calls from unknown
 * numbers (or every call in test mode) after a short delay and journals every state change.
 */
class SytoInCallService : InCallService() {
    private val handler = Handler(Looper.getMainLooper())
    private val callbacks = HashMap<Call, Call.Callback>()

    override fun onCreate() {
        super.onCreate()
        SpikeLog.log("svc", "InCallService created")
    }

    override fun onDestroy() {
        SpikeLog.log("svc", "InCallService destroyed")
        handler.removeCallbacksAndMessages(null)
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
                if (newState == Call.STATE_DISCONNECTED) {
                    val cause = call.details.disconnectCause
                    SpikeLog.log("call", "disconnect cause=${cause?.let { causeName(it) }} reason=${cause?.reason}")
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

    override fun onCallRemoved(call: Call) {
        SpikeLog.log("call", "removed")
        callbacks.remove(call)?.let(call::unregisterCallback)
        handler.removeCallbacksAndMessages(null)
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
}
