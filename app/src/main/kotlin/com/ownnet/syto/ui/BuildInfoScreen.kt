package com.ownnet.syto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ownnet.syto.R
import com.ownnet.syto.ui.theme.SytoTheme

/** Everything the phase-0 screen shows. Filled from BuildConfig and android.os.Build in MainActivity. */
data class BuildInfo(
    val versionName: String,
    val versionCode: Int,
    val gitSha: String,
    val buildNumber: String,
    val androidRelease: String,
    val apiLevel: Int,
    val device: String,
)

@Composable
fun BuildInfoScreen(info: BuildInfo, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            InfoRow(stringResource(R.string.label_version), "${info.versionName} (${info.versionCode})")
            InfoRow(stringResource(R.string.label_commit), info.gitSha)
            InfoRow(stringResource(R.string.label_build), info.buildNumber)
            InfoRow(stringResource(R.string.label_android), "${info.androidRelease} (API ${info.apiLevel})")
            InfoRow(stringResource(R.string.label_device), info.device)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BuildInfoScreenPreview() {
    SytoTheme {
        BuildInfoScreen(
            BuildInfo(
                versionName = "0.1.0",
                versionCode = 1,
                gitSha = "abc1234",
                buildNumber = "42",
                androidRelease = "15",
                apiLevel = 35,
                device = "realme RMX3999",
            ),
        )
    }
}
