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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
    val greet: Boolean,
    /** Index into [TTS_STREAMS]. */
    val ttsStreamIndex: Int,
    val boostVolume: Boolean,
    val speechRateTenths: Int,
    /** true = ROUTE_SPEAKER, false = ROUTE_EARPIECE */
    val routeSpeaker: Boolean,
    val greeting: String,
    val hangupAfterSec: Int,
)

/** Labels for the TTS stream choice; MainActivity maps indices to AudioManager constants. */
val TTS_STREAMS = listOf("VOICE_CALL", "MUSIC", "ALARM")

@Composable
fun HomeScreen(
    info: BuildInfo,
    state: HomeState,
    onRequestRole: () -> Unit,
    onRequestPermissions: () -> Unit,
    onAnswerAllChange: (Boolean) -> Unit,
    onDelayChange: (Int) -> Unit,
    onGreetChange: (Boolean) -> Unit,
    onTtsStreamChange: (Int) -> Unit,
    onBoostChange: (Boolean) -> Unit,
    onRateChange: (Int) -> Unit,
    onRouteChange: (Boolean) -> Unit,
    onGreetingChange: (String) -> Unit,
    onHangupAfterChange: (Int) -> Unit,
    onTestGreeting: () -> Unit,
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
            SwitchRow(
                label = stringResource(R.string.setting_answer_all),
                hint = stringResource(R.string.setting_answer_all_hint),
                checked = state.answerAll,
                onChange = onAnswerAllChange,
            )
            Spacer(Modifier.height(8.dp))
            StepperRow(
                label = stringResource(R.string.setting_answer_delay),
                value = state.answerDelaySec,
                onChange = onDelayChange,
            )
            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.section_speak))
            SwitchRow(
                label = stringResource(R.string.setting_greet),
                hint = stringResource(R.string.setting_greet_hint),
                checked = state.greet,
                onChange = onGreetChange,
            )
            Spacer(Modifier.height(8.dp))
            ChoiceRow(
                label = stringResource(R.string.setting_tts_stream),
                options = TTS_STREAMS,
                selected = state.ttsStreamIndex,
                onChange = onTtsStreamChange,
            )
            Spacer(Modifier.height(8.dp))
            SwitchRow(
                label = stringResource(R.string.setting_boost),
                hint = stringResource(R.string.setting_boost_hint),
                checked = state.boostVolume,
                onChange = onBoostChange,
            )
            Spacer(Modifier.height(8.dp))
            StepperRow(
                label = stringResource(R.string.setting_rate),
                value = state.speechRateTenths,
                display = "%.1fx".format(state.speechRateTenths / 10f),
                onChange = onRateChange,
            )
            Spacer(Modifier.height(8.dp))
            ChoiceRow(
                label = stringResource(R.string.setting_route),
                options = listOf("SPEAKER", "EARPIECE"),
                selected = if (state.routeSpeaker) 0 else 1,
                onChange = { onRouteChange(it == 0) },
            )
            Spacer(Modifier.height(8.dp))
            StepperRow(
                label = stringResource(R.string.setting_hangup_after),
                value = state.hangupAfterSec,
                onChange = onHangupAfterChange,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.greeting,
                onValueChange = onGreetingChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.setting_greeting)) },
                minLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onTestGreeting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_test_greeting))
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.section_log))
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

@Composable
private fun SwitchRow(label: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StepperRow(label: String, value: Int, onChange: (Int) -> Unit, display: String = "$value s") {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedButton(onClick = { onChange(value - 1) }) { Text("−") }
        Text(
            text = display,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedButton(onClick = { onChange(value + 1) }) { Text("+") }
    }
}

@Composable
private fun ChoiceRow(label: String, options: List<String>, selected: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = index == selected,
                    onClick = { onChange(index) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) { Text(option, fontFamily = FontFamily.Monospace) }
            }
        }
    }
}
