package com.aethone.client.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.aethone.client.R
import com.aethone.client.data.AppSettings
import com.aethone.client.data.ExtraMode
import com.aethone.client.data.Noize
import com.aethone.client.data.ScanMode
import com.aethone.client.data.Transport
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.home.protocolLabel

/** Connection settings: protocol, scan mode, obfuscation, quick reconnect. */
@Composable
fun ConnectionScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsState()

    var showTransport by remember { mutableStateOf(false) }
    var showScan by remember { mutableStateOf(false) }
    var showNoize by remember { mutableStateOf(false) }

    val scanLabels = listOf(
        stringResource(R.string.conn_scan_turbo) to stringResource(R.string.conn_scan_turbo_desc),
        stringResource(R.string.conn_scan_balanced) to stringResource(R.string.conn_scan_balanced_desc),
        stringResource(R.string.conn_scan_thorough) to stringResource(R.string.conn_scan_thorough_desc),
        stringResource(R.string.conn_scan_stealth) to stringResource(R.string.conn_scan_stealth_desc),
        stringResource(R.string.conn_scan_ironclad) to stringResource(R.string.conn_scan_ironclad_desc),
    )
    val noizeLabels = listOf(
        stringResource(R.string.conn_noize_off) to stringResource(R.string.conn_noize_off_desc),
        stringResource(R.string.conn_noize_light) to stringResource(R.string.conn_noize_light_desc),
        stringResource(R.string.conn_noize_balanced) to stringResource(R.string.conn_noize_balanced_desc),
        stringResource(R.string.conn_noize_aggressive) to stringResource(R.string.conn_noize_aggressive_desc),
    )

    SettingsScaffold(stringResource(R.string.settings_section_connection), onBack) {
        SettingsSection(stringResource(R.string.conn_protocol)) {
            SettingRow(
                title = protocolLabel(settings.transport, settings.extraMode),
                desc = null,
                onClick = { showTransport = true },
                trailing = { ValueText("›") }
            )
        }

        SettingsSection(stringResource(R.string.conn_scan_mode)) {
            SettingRow(
                title = scanLabels[ScanMode.entries.indexOf(settings.scanMode)].first,
                desc = scanLabels[ScanMode.entries.indexOf(settings.scanMode)].second,
                onClick = { showScan = true },
                trailing = { ValueText("›") }
            )
        }

        SettingsSection(stringResource(R.string.conn_obfuscation)) {
            SettingRow(
                title = noizeLabels[Noize.entries.indexOf(settings.noize)].first,
                desc = noizeLabels[Noize.entries.indexOf(settings.noize)].second,
                onClick = { showNoize = true },
                trailing = { ValueText("›") }
            )
            SwitchRow(
                title = stringResource(R.string.conn_quick_reconnect),
                desc = stringResource(R.string.conn_quick_reconnect_desc),
                checked = settings.quickReconnect,
                onChange = { v -> vm.update { it.copy(quickReconnect = v) } },
            )
        }
    }

    if (showTransport) {
        ProtocolChoiceDialog(settings) { t, e ->
            vm.update { it.copy(transport = t, extraMode = e) }
            showTransport = false
        }
    }
    if (showScan) {
        ChoiceDialog(
            title = stringResource(R.string.conn_scan_mode),
            options = scanLabels.map { ChoiceOption(it.first, it.second) },
            selectedIndex = ScanMode.entries.indexOf(settings.scanMode),
            onDismiss = { showScan = false },
            onPick = { i ->
                vm.update { it.copy(scanMode = ScanMode.entries[i]) }
                showScan = false
            },
        )
    }
    if (showNoize) {
        ChoiceDialog(
            title = stringResource(R.string.conn_obfuscation),
            options = noizeLabels.map { ChoiceOption(it.first, it.second) },
            selectedIndex = Noize.entries.indexOf(settings.noize),
            onDismiss = { showNoize = false },
            onPick = { i ->
                vm.update { it.copy(noize = Noize.entries[i]) }
                showNoize = false
            },
        )
    }
}

@Composable
private fun ProtocolChoiceDialog(settings: AppSettings, onPick: (Transport, ExtraMode) -> Unit) {
    val entries = listOf(
        Triple(Transport.MASQUE, ExtraMode.OFF, R.string.protocol_masque),
        Triple(Transport.WIREGUARD, ExtraMode.OFF, R.string.protocol_wireguard),
        Triple(Transport.GOOL, ExtraMode.OFF, R.string.protocol_gool),
        Triple(Transport.GOOL_CLASSIC, ExtraMode.OFF, R.string.protocol_gool_classic),
        Triple(Transport.MIM, ExtraMode.OFF, R.string.protocol_mim),
        Triple(Transport.MASQUE, ExtraMode.TOR, R.string.protocol_tor),
        Triple(Transport.MASQUE, ExtraMode.TOR_REVERSE, R.string.protocol_tor_reverse),
        Triple(Transport.MASQUE, ExtraMode.TOR_ONLY, R.string.protocol_tor_only),
        Triple(Transport.MASQUE, ExtraMode.PSIPHON, R.string.protocol_psiphon),
    )
    ChoiceDialog(
        title = stringResource(R.string.conn_protocol),
        options = entries.map { (_, _, res) -> ChoiceOption(stringResource(res)) },
        selectedIndex = entries.indexOfFirst { it.first == settings.transport && it.second == settings.extraMode },
        onDismiss = { onPick(settings.transport, settings.extraMode) },
        onPick = { i -> onPick(entries[i].first, entries[i].second) },
    )
}

@Composable
fun ValueText(text: String) {
    val extras = com.aethone.client.ui.theme.Aethone.extras
    androidx.compose.material3.Text(
        text,
        color = extras.textDim,
        fontSize = 16.sp,
    )
}

