package com.ownnet.syto.voice

import android.os.Build
import android.telecom.Call
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** What the in-call screen shows. */
data class CallUi(
    val number: String,
    val state: Int,
    /** null = contacts permission missing, lookup skipped. */
    val known: Boolean?,
    val incoming: Boolean,
    /** What the spike is doing right now, shown under the state. */
    val note: String = "",
)

/**
 * The single current call, shared between [SytoInCallService] (writer, main thread)
 * and [InCallActivity] (reader, Compose). One call at a time is enough for the spike.
 */
object CallRegistry {
    var current: CallUi? by mutableStateOf(null)
        private set

    @Volatile
    var call: Call? = null
        private set

    fun set(call: Call, ui: CallUi) {
        this.call = call
        current = ui
    }

    fun updateState(call: Call, state: Int) {
        if (this.call === call) current = current?.copy(state = state)
    }

    fun note(call: Call, note: String) {
        if (this.call === call) current = current?.copy(note = note)
    }

    fun clear(call: Call) {
        if (this.call === call) {
            this.call = null
            current = null
        }
    }
}

@Suppress("DEPRECATION")
fun Call.currentState(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) details.state else state

fun stateName(state: Int): String = when (state) {
    Call.STATE_NEW -> "NEW"
    Call.STATE_DIALING -> "DIALING"
    Call.STATE_RINGING -> "RINGING"
    Call.STATE_HOLDING -> "HOLDING"
    Call.STATE_ACTIVE -> "ACTIVE"
    Call.STATE_DISCONNECTED -> "DISCONNECTED"
    Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
    Call.STATE_CONNECTING -> "CONNECTING"
    Call.STATE_DISCONNECTING -> "DISCONNECTING"
    Call.STATE_PULLING_CALL -> "PULLING_CALL"
    Call.STATE_AUDIO_PROCESSING -> "AUDIO_PROCESSING"
    Call.STATE_SIMULATED_RINGING -> "SIMULATED_RINGING"
    else -> "STATE_$state"
}
