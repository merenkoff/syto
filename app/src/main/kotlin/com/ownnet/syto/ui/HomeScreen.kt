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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ownnet.syto.R

/** Everything the home screen needs to know; refreshed by MainActivity on resume. */
data class HomeState(
    val roleAvailable: Boolean,
    val isDefaultDialer: Boolean,
    val permissionsGranted: Boolean,
    val answerAll: Boolean,
    val answerDelaySec: Int,
)

@Composable
fun HomeScreen(
    info: BuildInfo,
    state: HomeState,
    onRequestRole: () -> Unit,
    onRequestPermissions: () -> Unit,
    onAnswerAllChange: (Boolean) -> Unit,
    onDelayChange: (Int) -> Unit,
    onOpenLog: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
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
            Spacer(Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.section_spike))
            StatusRow(
                label = stringResource(R.string.status_default_dialer),
                ok = state.isDefaultDialer,
                okText = stringResource(R.string.status_yes),
                failText = stringResource(if (state.roleAvailable) R.string.status_no else R.string.status_role_unavailable),
            )
            if (!state.isDefaultDialer && state.roleAvailable) {
                Button(onClick = onRequestRole, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_request_dialer_role))
                }
            }
            Spacer(Modifier.height(8.dp))
            StatusRow(
                label = stringResource(R.string.status_permissions),
                ok = state.permissionsGranted,
                okText = stringResource(R.string.status_granted),
                failText = stringResource(R.string.status_missing),
            )
            if (!state.permissionsGranted) {
                Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_request_permissions))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setting_answer_all),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.setting_answer_all_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.answerAll, onCheckedChange = onAnswerAllChange)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.setting_answer_delay),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(onClick = { onDelayChange(state.answerDelaySec - 1) }) { Text("−") }
                Text(
                    text = "${state.answerDelaySec} s",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(onClick = { onDelayChange(state.answerDelaySec + 1) }) { Text("+") }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onOpenLog, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_open_log))
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.section_build))
            Text(
                text = "${info.versionName} (${info.versionCode}) · ${info.gitSha} · build ${info.buildNumber}\n" +
                    "Android ${info.androidRelease} (API ${info.apiLevel}) · ${info.device}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun StatusRow(label: String, ok: Boolean, okText: String, failText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = if (ok) okText else failText,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
