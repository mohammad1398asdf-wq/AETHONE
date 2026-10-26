package com.aethone.client.ui.home

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aethone.client.R
import com.aethone.client.core.AethoneVpnService
import com.aethone.client.core.ConnectionBus
import com.aethone.client.core.VpnStatus
import com.aethone.client.data.AppSettings
import com.aethone.client.data.ExtraMode
import com.aethone.client.data.ThemeMode
import com.aethone.client.data.Transport
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.components.AethoneMark
import com.aethone.client.ui.components.IconChip
import com.aethone.client.ui.components.LocationCard
import com.aethone.client.ui.components.PingCard
import com.aethone.client.ui.components.ProtocolPill
import com.aethone.client.ui.components.StatCard
import com.aethone.client.ui.components.ToggleOrb
import com.aethone.client.ui.theme.Aethone
import com.aethone.client.util.Formatters
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val state by ConnectionBus.state.collectAsState()
    val extras = Aethone.extras
    val systemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var showProtocolPicker by remember { mutableStateOf(false) }

    // one-time real location for the disconnected card
    LaunchedEffect(Unit) { vm.probeRealLocation() }

    // VPN permission flow
    val vpnPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AethoneVpnService.start(context, AethoneVpnService.ACTION_CONNECT)
        } else {
            scope.launch { snackbar.showSnackbar(context.getString(R.string.error_vpn_permission)) }
        }
    }

    fun toggle() {
        when (state.status) {
            VpnStatus.CONNECTED, VpnStatus.CONNECTING ->
                AethoneVpnService.start(context, AethoneVpnService.ACTION_DISCONNECT)
            else -> {
                val prepared = VpnService.prepare(context)
                if (prepared != null) vpnPermission.launch(prepared)
                else AethoneVpnService.start(context, AethoneVpnService.ACTION_CONNECT)
            }
        }
    }

    // ticking session clock
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.status) {
        while (state.status == VpnStatus.CONNECTED) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val accent by animateColorAsState(
        targetValue = when (state.status) {
            VpnStatus.CONNECTED -> extras.connected
            VpnStatus.CONNECTING, VpnStatus.DISCONNECTING -> extras.connecting
            else -> extras.disconnected
        },
        animationSpec = tween(400), label = "accent"
    )

    // errors -> snackbar
    LaunchedEffect(state.lastError) {
        state.lastError?.let { snackbar.showSnackbar(it) }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        extras.background,
                        extras.background.copy(alpha = 0.94f),
                        when (state.status) {
                            VpnStatus.CONNECTED -> extras.mintWash.copy(alpha = 0.35f)
                            else -> extras.blushWash.copy(alpha = 0.28f)
                        }
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // -------------------------------------------------- top bar
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDarkUi = when (settings.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
                IconChip(
                    icon = if (isDarkUi) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = stringResource(R.string.cd_theme_toggle),
                    onClick = { vm.toggleTheme(systemDark) }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "AETHONE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp,
                    color = extras.text
                )
                Spacer(Modifier.weight(1f))
                IconChip(
                    icon = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    onClick = onOpenSettings
                )
            }

            Spacer(Modifier.height(20.dp))

            val locationSublabel = state.exitInfo.isp.ifBlank {
                stringResource(R.string.home_warp_on)
            }

            // -------------------------------------------- server row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LocationCard(
                    flag = state.exitInfo.flagEmoji,
                    label = state.exitInfo.label,
                    sublabel = locationSublabel,
                    modifier = Modifier.weight(1.6f),
                )
                PingCard(
                    pingMs = state.pingMs,
                    dotColor = when (Formatters.pingClass(state.pingMs)) {
                        Formatters.PingQuality.GOOD -> extras.connected
                        Formatters.PingQuality.OK -> extras.connecting
                        Formatters.PingQuality.BAD -> extras.disconnected
                        Formatters.PingQuality.UNKNOWN -> extras.textDim
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // -------------------------------------------- status text
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val statusLabel = when (state.status) {
                    VpnStatus.CONNECTED -> "CONNECTED"
                    VpnStatus.DISCONNECTED -> "DISCONNECTED"
                    VpnStatus.CONNECTING -> stringResource(R.string.home_connecting)
                    VpnStatus.DISCONNECTING -> stringResource(R.string.home_disconnecting)
                    VpnStatus.ERROR -> stringResource(R.string.home_error)
                }
                Text(
                    statusLabel,
                    color = accent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                if (state.status == VpnStatus.CONNECTED) {
                    Text(
                        Formatters.elapsed(now - state.connectedAtMillis),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = extras.text
                    )
                } else if (state.status == VpnStatus.DISCONNECTED) {
                    Text(
                        "(((((((((((((",
                        fontSize = 22.sp,
                        color = extras.textDim,
                        letterSpacing = 2.sp
                    )
                }
            }

            // -------------------------------------------- the mark
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AethoneMark(
                    status = state.status,
                    color = accent,
                    glowColor = if (state.status == VpnStatus.CONNECTED) extras.connected
                    else extras.disconnected,
                    glow = true,
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .aspectRatio(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple()
                        ) { toggle() },
                )
            }

            Spacer(Modifier.height(8.dp))

            // -------------------------------------------- stats row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "DOWN",
                    icon = Icons.Outlined.FileDownload,
                    iconTint = extras.connected,
                    iconBg = extras.iconBgDown,
                    volume = Formatters.bytes(state.downBytes),
                    rate = Formatters.speed(state.downRateBps),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "UP",
                    icon = Icons.Outlined.FileUpload,
                    iconTint = extras.disconnected,
                    iconBg = extras.iconBgUp,
                    volume = Formatters.bytes(state.upBytes),
                    rate = Formatters.speed(state.upRateBps),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ------------------------------------- protocol + toggle
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProtocolPill(
                    protocol = state.activeProtocol.ifBlank {
                        protocolLabel(settings.transport, settings.extraMode)
                    },
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    onClick = { showProtocolPicker = true },
                )
                ToggleOrb(
                    accent = accent,
                    wash = if (state.status == VpnStatus.CONNECTED) extras.mintWash else extras.blushWash,
                    onClick = { toggle() },
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    // ------------------------------------------------------ protocol picker
    if (showProtocolPicker) {
        ModalBottomSheet(
            onDismissRequest = { showProtocolPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = extras.surface,
        ) {
            ProtocolPickerContent(
                current = settings,
                onPick = { transport, extra ->
                    vm.update { it.copy(transport = transport, extraMode = extra) }
                    showProtocolPicker = false
                    // applying a protocol change reconnects when connected
                    if (state.status == VpnStatus.CONNECTED) {
                        AethoneVpnService.start(context, AethoneVpnService.ACTION_RECONNECT)
                    }
                },
            )
        }
    }
}
