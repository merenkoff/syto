package com.ownnet.syto

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ownnet.syto.ui.BuildInfo
import com.ownnet.syto.ui.BuildInfoScreen
import com.ownnet.syto.ui.theme.SytoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                BuildInfoScreen(info)
            }
        }
    }
}
