package com.ownnet.syto.voice

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.ownnet.syto.ui.InCallScreen
import com.ownnet.syto.ui.theme.SytoTheme

/** Minimal in-call UI, mandatory for a default dialer. Shows over the lock screen. */
class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()
        setContent {
            SytoTheme {
                val ui = CallRegistry.current
                LaunchedEffect(ui) { if (ui == null) finish() }
                if (ui != null) {
                    InCallScreen(
                        ui = ui,
                        onAnswer = {
                            SpikeLog.log("ui", "answer tapped")
                            CallRegistry.call?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                        },
                        onHangUp = {
                            val call = CallRegistry.call ?: return@InCallScreen
                            if (call.currentState() == Call.STATE_RINGING) {
                                SpikeLog.log("ui", "reject tapped")
                                call.reject(false, null)
                            } else {
                                SpikeLog.log("ui", "hang up tapped")
                                call.disconnect()
                            }
                        },
                    )
                }
            }
        }
    }
}
