package com.ownnet.syto

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
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

class MainActivity : ComponentActivity() {
    private lateinit var settings: SpikeSettings
    private var homeState by mutableStateOf(HomeState(false, false, false, false, 0))

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
            // Registered for DIAL only to qualify for ROLE_DIALER. The spike does not place calls.
            SpikeLog.log("ui", "ACTION_DIAL received, outgoing calls are not supported by the spike")
            Toast.makeText(this, R.string.toast_no_outgoing, Toast.LENGTH_LONG).show()
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
                        onOpenLog = { screen = SCREEN_LOG },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
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
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
        )
    }
}
