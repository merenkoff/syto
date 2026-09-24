package com.ownnet.syto

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ownnet.syto.ui.BuildInfo
import com.ownnet.syto.ui.HomeScreen
import com.ownnet.syto.ui.HomeState
import com.ownnet.syto.ui.SpikeLogScreen
import com.ownnet.syto.ui.theme.SytoTheme
import com.ownnet.syto.voice.SpikeLog
import com.ownnet.syto.voice.SpikeSettings
import com.ownnet.syto.voice.Talker
import android.telecom.CallAudioState
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var settings: SpikeSettings
    private var homeState by mutableStateOf(
        HomeState(false, false, false, false, 0, true, 1, true, 10, true, "", 0),
    )
    private var testTalker: Talker? = null

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh()
        SpikeLog.log("role", "dialer role request result=${it.resultCode} held=${homeState.isDefaultDialer}")
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        SpikeLog.log("perm", it.entries.joinToString { (p, g) -> "${p.substringAfterLast('.')}=$g" })
        refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SpikeSettings(this)
        enableEdgeToEdge()
        if (intent?.action == Intent.ACTION_DIAL) {
            // Registered for DIAL only to qualify for ROLE_DIALER. The spike does not place calls itself:
            // hand the request to another dialer so the user does not have to switch roles to make a call.
            if (forwardDialToSystemDialer(intent)) {
                finish()
                return
            }
        }
        val info = BuildInfo(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            gitSha = BuildConfig.GIT_SHA,
            buildNumber = BuildConfig.BUILD_NUMBER,
            androidRelease = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
        )
        setContent {
            SytoTheme {
                var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
                BackHandler(enabled = screen != SCREEN_HOME) { screen = SCREEN_HOME }
                when (screen) {
                    SCREEN_LOG -> SpikeLogScreen(onBack = { screen = SCREEN_HOME })
                    else -> HomeScreen(
                        info = info,
                        state = homeState,
                        onRequestRole = ::requestDialerRole,
                        onRequestPermissions = { permissionLauncher.launch(REQUIRED_PERMISSIONS) },
                        onAnswerAllChange = {
                            settings.answerAll = it
                            SpikeLog.log("settings", "answerAll=$it")
                            refresh()
                        },
                        onDelayChange = {
                            settings.answerDelaySec = it
                            SpikeLog.log("settings", "answerDelaySec=${settings.answerDelaySec}")
                            refresh()
                        },
                        onGreetChange = {
                            settings.greet = it
                            SpikeLog.log("settings", "greet=$it")
                            refresh()
                        },
                        onTtsStreamChange = { index ->
                            settings.ttsStream = STREAM_BY_INDEX[index]
                            SpikeLog.log("settings", "ttsStream=${Talker.streamName(settings.ttsStream)}")
                            refresh()
                        },
                        onBoostChange = {
                            settings.boostVolume = it
                            SpikeLog.log("settings", "boostVolume=$it")
                            refresh()
                        },
                        onRateChange = {
                            settings.speechRateTenths = it
                            SpikeLog.log("settings", "speechRateTenths=${settings.speechRateTenths}")
                            refresh()
                        },
                        onRouteChange = { speaker ->
                            settings.audioRoute = if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
                            SpikeLog.log("settings", "audioRoute=${CallAudioState.audioRouteToString(settings.audioRoute)}")
                            refresh()
                        },
                        onGreetingChange = {
                            settings.greeting = it
                            refresh()
                        },
                        onHangupAfterChange = {
                            settings.hangupAfterSec = it
                            SpikeLog.log("settings", "hangupAfterSec=${settings.hangupAfterSec}")
                            refresh()
                        },
                        onTestGreeting = ::testGreeting,
                        onOpenLog = { screen = SCREEN_LOG },
                    )
                }
            }
        }
    }

    /** Opens the same DIAL intent in the first dialer that is not Syto. Returns false if there is none. */
    private fun forwardDialToSystemDialer(original: Intent): Boolean {
        val probe = Intent(Intent.ACTION_DIAL, original.data)
        val target = packageManager.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            .map { it.activityInfo }
            .firstOrNull { it.packageName != packageName }
        if (target == null) {
            SpikeLog.log("ui", "ACTION_DIAL received, no other dialer found")
            Toast.makeText(this, R.string.toast_no_outgoing, Toast.LENGTH_LONG).show()
            return false
        }
        SpikeLog.log("ui", "ACTION_DIAL data=${original.data} forwarded to ${target.packageName}")
        startActivity(
            Intent(Intent.ACTION_DIAL, original.data)
                .setClassName(target.packageName, target.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        testTalker?.shutdown()
        testTalker = null
        super.onDestroy()
    }

    /** Speaks the greeting outside a call on the MUSIC stream: checks the engine and the uk voice. */
    private fun testGreeting() {
        SpikeLog.log("tts-test", "test greeting tapped")
        val talker = testTalker ?: Talker(this, "tts-test").also { testTalker = it }
        val rate = settings.speechRateTenths / 10f
        talker.speak(settings.greeting, Locale.forLanguageTag(SpikeSettings.LANGUAGE_TAG), AudioManager.STREAM_MUSIC, rate) {
            SpikeLog.log("tts-test", "done")
        }
    }

    private fun refresh() {
        val roleManager = getSystemService(RoleManager::class.java)
        homeState = HomeState(
            roleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_DIALER),
            isDefaultDialer = roleManager.isRoleHeld(RoleManager.ROLE_DIALER),
            permissionsGranted = REQUIRED_PERMISSIONS.all {
                checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            },
            answerAll = settings.answerAll,
            answerDelaySec = settings.answerDelaySec,
            greet = settings.greet,
            ttsStreamIndex = STREAM_BY_INDEX.indexOf(settings.ttsStream).coerceAtLeast(0),
            boostVolume = settings.boostVolume,
            speechRateTenths = settings.speechRateTenths,
            routeSpeaker = settings.audioRoute == CallAudioState.ROUTE_SPEAKER,
            greeting = settings.greeting,
            hangupAfterSec = settings.hangupAfterSec,
        )
    }

    private fun requestDialerRole() {
        SpikeLog.log("role", "requesting ROLE_DIALER")
        val roleManager = getSystemService(RoleManager::class.java)
        roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
    }

    private companion object {
        const val SCREEN_HOME = "home"
        const val SCREEN_LOG = "log"
        /** Same order as ui.TTS_STREAMS. */
        val STREAM_BY_INDEX = listOf(AudioManager.STREAM_VOICE_CALL, AudioManager.STREAM_MUSIC, AudioManager.STREAM_ALARM)
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
        )
    }
}
