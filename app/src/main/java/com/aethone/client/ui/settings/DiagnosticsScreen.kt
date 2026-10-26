package com.aethone.client.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.BuildConfig
import com.aethone.client.R
import com.aethone.client.core.AetherCore
import com.aethone.client.core.ConnectionBus
import com.aethone.client.core.ExitInfoProbe
import com.aethone.client.core.VpnStatus
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.theme.Aethone
import dev.zeptun.Zeptun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Diagnostics: core versions, live log, identity reset, tunnel test. */
@Composable
fun DiagnosticsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by ConnectionBus.state.collectAsState()
    val extras = Aethone.extras

    var aetherVersion by remember { mutableStateOf("…") }
    var zeptunVersion by remember { mutableStateOf("…") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        aetherVersion = withContext(Dispatchers.IO) {
            runCatching { AetherCore(context).queryVersion() }
                .getOrDefault(context.getString(R.string.error_core_missing))
        }
        zeptunVersion = runCatching { Zeptun.nativeVersion() }
            .getOrDefault("?")
    }

    SettingsScaffold(stringResource(R.string.diag_title), onBack) {
        SettingsSection(stringResource(R.string.diag_core_version)) {
            SettingRow(title = "Aether", desc = aetherVersion)
            SettingRow(title = "Zeptun", desc = zeptunVersion)
            SettingRow(title = "AETHONE", desc = BuildConfig.VERSION_NAME)
        }

        SettingsSection(stringResource(R.string.diag_test_connection)) {
            SettingRow(
                title = stringResource(R.string.diag_test_connection),
                desc = testResult ?: stringResource(R.string.diag_test_desc),
                onClick = {
                    busy = true
                    testResult = "…"
                },
                trailing = { ValueText("›") }
            )
        }

        SettingsSection(stringResource(R.string.diag_identity)) {
            SettingRow(
                title = stringResource(R.string.diag_identity_reset),
                desc = stringResource(R.string.diag_identity_desc),
                onClick = {
                    AetherCore(context).resetIdentity()
                    Toast.makeText(
                        context,
                        context.getString(R.string.diag_identity_reset),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                trailing = { ValueText("›") }
            )
        }

        SettingsSection(stringResource(R.string.diag_core_log)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(extras.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = state.coreLog.takeLast(120).joinToString("\n")
                        .ifBlank { "—" },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = extras.textDim,
                )
            }
            SettingRow(
                title = stringResource(R.string.diag_copy_log),
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                    cm.setPrimaryClip(
                        ClipData.newPlainText("aethone log", state.coreLog.joinToString("\n"))
                    )
                },
                trailing = { ValueText("⧉") }
            )
        }
    }

    // tunnel test runs through the SOCKS listener when connected
    LaunchedEffect(busy) {
        if (!busy) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            val s = vm.settings.value
            if (ConnectionBus.state.value.status != VpnStatus.CONNECTED) {
                context.getString(R.string.notif_title_disconnected)
            } else {
                runCatching {
                    val body = ExitInfoProbe(s.socksPort).let { probe ->
                        probe.lookup("en")
                    }
                    if (body.ip.isNotBlank()) {
                        "warp=${if (body.warpOn) "on" else "off"} · ip=${body.ip}"
                    } else {
                        "trace failed"
                    }
                }.getOrElse { it.message ?: "error" }
            }
        }
        testResult = result
        busy = false
    }
}

/** About screen. */
@Composable
fun AboutScreen(vm: AppViewModel, onBack: () -> Unit) {
    SettingsScaffold(stringResource(R.string.about_title), onBack) {
        SettingsSection(stringResource(R.string.about_version)) {
            SettingRow(
                title = stringResource(R.string.about_version),
                desc = BuildConfig.VERSION_NAME
            )
            SettingRow(
                title = stringResource(R.string.about_licenses),
                desc = stringResource(
                    R.string.about_cores,
                    AetherCore.AETHER_VERSION,
                    "1.1.3"
                )
            )
        }
        SettingsSection {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.about_license_body),
                    fontSize = 12.sp,
                    color = Aethone.extras.textDim,
                )
            }
        }
    }
}
