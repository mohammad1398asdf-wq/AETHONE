package com.aethone.client.ui.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.R
import com.aethone.client.core.ConnectionBus
import com.aethone.client.core.VpnStatus
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.components.AethoneMark
import com.aethone.client.ui.theme.Aethone

/** Settings root – the entry list mirroring the home screen's design language. */
@Composable
fun SettingsRootScreen(
    vm: AppViewModel,
    onOpen: (String) -> Unit,
) {
    val extras = Aethone.extras
    val state by ConnectionBus.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AethoneMark(
                status = if (state.status == VpnStatus.CONNECTED) VpnStatus.CONNECTED
                else VpnStatus.DISCONNECTED,
                color = if (state.status == VpnStatus.CONNECTED) extras.connected else extras.disconnected,
                glowColor = extras.connected,
                modifier = Modifier.width(26.dp).height(26.dp),
                glow = false,
            )
            Text(
                stringResource(R.string.settings_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = extras.text,
            )
        }
        Spacer(Modifier.height(14.dp))

        Column(Modifier.verticalScrollable()) {
            SettingsSection {
                NavRow(
                    Icons.Outlined.CallSplit,
                    stringResource(R.string.settings_section_connection),
                    stringResource(R.string.settings_section_connection_desc),
                ) { onOpen("connection") }
            }
            SettingsSection {
                NavRow(
                    Icons.Outlined.Apps,
                    stringResource(R.string.settings_section_routing),
                    stringResource(R.string.settings_section_routing_desc),
                ) { onOpen("routing") }
                NavRow(
                    Icons.Filled.Tune,
                    stringResource(R.string.settings_section_apps),
                    stringResource(R.string.settings_section_apps_desc),
                ) { onOpen("apps") }
            }
            SettingsSection {
                NavRow(
                    Icons.Outlined.Palette,
                    stringResource(R.string.settings_section_appearance),
                    stringResource(R.string.settings_section_appearance_desc),
                ) { onOpen("appearance") }
                NavRow(
                    Icons.Outlined.Star,
                    stringResource(R.string.settings_section_features),
                    stringResource(R.string.settings_section_features_desc),
                ) { onOpen("features") }
            }
            SettingsSection {
                NavRow(
                    Icons.Outlined.Construction,
                    stringResource(R.string.settings_section_advanced),
                    stringResource(R.string.settings_section_advanced_desc),
                ) { onOpen("advanced") }
                NavRow(
                    Icons.Outlined.Terminal,
                    stringResource(R.string.settings_section_diagnostics),
                    stringResource(R.string.settings_section_diagnostics_desc),
                ) { onOpen("diagnostics") }
            }
            SettingsSection {
                NavRow(
                    Icons.Outlined.Info,
                    stringResource(R.string.settings_section_about),
                    stringResource(R.string.settings_section_about_desc),
                ) { onOpen("about") }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** small helper so the root screen scrolls */
@Composable
private fun Modifier.verticalScrollable(): Modifier =
    this.verticalScroll(androidx.compose.foundation.rememberScrollState())
