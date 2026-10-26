package com.aethone.client.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aethone.client.AethoneApp
import com.aethone.client.MainActivity
import com.aethone.client.R
import com.aethone.client.data.AppSettings
import com.aethone.client.data.SettingsRepository
import com.aethone.client.data.AppsMode
import com.aethone.client.widget.WidgetUpdater
import com.aethone.client.util.Formatters
import dev.zeptun.Zeptun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * The foreground VPN service.
 *
 * Pipeline: Aether core (child process, SOCKS5 on loopback) ← Zeptun engine
 * (in-process via JNI, reads the TUN fd, forwards to the SOCKS5 listener).
 * The app's own traffic – including the core's – is exempt from the VPN by
 * Android, so the loop never closes.
 */
class AethoneVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.aethone.client.CONNECT"
        const val ACTION_DISCONNECT = "com.aethone.client.DISCONNECT"
        const val ACTION_TOGGLE = "com.aethone.client.TOGGLE"
        const val ACTION_RECONNECT = "com.aethone.client.RECONNECT"
        const val TAG = "AethoneVpn"

        const val NOTIF_ID = 1001
        const val ALERT_NOTIF_ID = 1002

        fun start(context: Context, action: String) {
            val intent = Intent(context, AethoneVpnService::class.java).setAction(action)
            context.startForegroundService(intent)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var settingsRepo: SettingsRepository
    private val core by lazy { AetherCore(this) }

    private var tunFd: ParcelFileDescriptor? = null
    private var tunFdInt: Int? = null
    private var tickerJob: Job? = null
    private var exitJob: Job? = null
    private var reconnectAttempts = 0
    private var connecting = false

    private var lastRx = 0L
    private var lastTx = 0L
    private var lastTickAt = 0L
    private var lastNotifAt = 0L

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT, ACTION_RECONNECT -> connect()
            ACTION_DISCONNECT -> disconnect()
            ACTION_TOGGLE -> {
                val st = ConnectionBus.state.value.status
                if (st == VpnStatus.CONNECTED || st == VpnStatus.CONNECTING) disconnect() else connect()
            }
            else -> if (ConnectionBus.state.value.status == VpnStatus.CONNECTED) {
                // restarted by the system while connected – keep the session
                connect()
            }
        }
        return START_NOT_STICKY
    }

    // --------------------------------------------------------------- connect

    private fun connect() {
        if (connecting) return
        connecting = true
        reconnectAttempts = 0
        scope.launch { startSession() }
    }

    private suspend fun startSession() {
        val settings = settingsRepo.current()
        ConnectionBus.update {
            it.copy(
                status = VpnStatus.CONNECTING,
                connectedAtMillis = 0L,
                downBytes = 0, upBytes = 0, downRateBps = 0, upRateBps = 0,
                pingMs = -1,
                lastError = null,
                activeProtocol = activeProtocolLabel(settings),
            )
        }
        startForegroundWithType(buildNotification())

        try {
            if (settings.appsMode == AppsMode.INCLUDE && settings.selectedApps.isEmpty()) {
                throw AetherCore.CoreException(
                    "Select at least one app in app-splitting settings before connecting."
                )
            }

            // 1) the Aether core (registers on first run, scans for an endpoint)
            core.start(
                settings,
                firstAttempt = !core.hasIdentity(),
            ) { onCoreDied(settings) }

            // 2) the TUN interface
            val fd = establishVpn(settings)
            tunFd = fd

            // 3) the Zeptun engine on top of the TUN (takes ownership of the fd)
            val toml = buildZeptunConfig(settings)
            val fdInt = fd.detachFd()
            tunFdInt = fdInt
            val rc = Zeptun.nativeStart(this, fdInt, toml)
            if (rc != 0) {
                throw AetherCore.CoreException("zeptun start failed: $rc (${Zeptun.nativeVersion()})")
            }

            // 4) live!
            ConnectionBus.update {
                it.copy(status = VpnStatus.CONNECTED, connectedAtMillis = System.currentTimeMillis())
            }
            settingsRepo.setLastConnected(true)
            startTicker(settings)
            startExitRefresher(settings)
            notifyStatus()
            WidgetUpdater.updateAll(this)
        } catch (e: Exception) {
            Log.e(TAG, "connect failed", e)
            fullStop()
            ConnectionBus.postError(e.message ?: "connection failed")
            notifyStatus(error = e.message)
            WidgetUpdater.updateAll(this)
        } finally {
            connecting = false
        }
    }

    /** Human name of the active protocol shown on the home screen. */
    private fun activeProtocolLabel(s: AppSettings): String = when (s.extraMode) {
        com.aethone.client.data.ExtraMode.OFF -> when (s.transport) {
            com.aethone.client.data.Transport.MASQUE -> "MASQUE"
            com.aethone.client.data.Transport.WIREGUARD -> "WIREGUARD"
            com.aethone.client.data.Transport.GOOL -> "GOOL"
            com.aethone.client.data.Transport.GOOL_CLASSIC -> "GOOL CLASSIC"
            com.aethone.client.data.Transport.MIM -> "MIM"
        }
        com.aethone.client.data.ExtraMode.TOR -> "TOR"
        com.aethone.client.data.ExtraMode.TOR_REVERSE -> "TOR REVERSE"
        com.aethone.client.data.ExtraMode.TOR_ONLY -> "TOR"
        com.aethone.client.data.ExtraMode.PSIPHON -> "PSIPHON"
    }

    private fun establishVpn(s: AppSettings): ParcelFileDescriptor {
        val builder = Builder()
            .setSession("AETHONE")
            .setMtu(s.mtu.coerceIn(1280, 65535))
            .addAddress("172.19.0.1", 30)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)
        if (s.ipv6) {
            builder.addAddress("fdfe:dcba:9876::1", 126)
            builder.addDnsServer("2606:4700:4700::1111")
            builder.addRoute("::", 0)
        }
        when (s.appsMode) {
            AppsMode.ALL -> {}
            AppsMode.EXCLUDE -> s.selectedApps.forEach { pkg ->
                runCatching { builder.addDisallowedApplication(pkg) }
            }
            AppsMode.INCLUDE -> {
                var allowedCount = 0
                s.selectedApps.forEach { pkg ->
                    try {
                        builder.addAllowedApplication(pkg)
                        allowedCount++
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping unavailable app in VPN allowlist: $pkg", e)
                    }
                }
                if (allowedCount == 0) {
                    throw AetherCore.CoreException(
                        "None of the selected apps is installed or available. Update the app selection."
                    )
                }
            }
        }
        return builder.establish()
            ?: throw AetherCore.CoreException("VPN establish() returned null (permission revoked?)")
    }

    /** Zeptun engine config (TOML). Unknown keys are rejected by the engine. */
    private fun buildZeptunConfig(s: AppSettings): String = buildString {
        appendLine("preset = \"mobile\"")
        appendLine("log_level = \"warn\"")
        appendLine("[tun]")
        appendLine("mtu = ${s.mtu.coerceIn(1280, 65535)}")
        appendLine("[stack]")
        appendLine("mode = \"${if (s.stackMode == com.aethone.client.data.StackMode.HYBRID) "hybrid" else "userspace"}\"")
        appendLine("udp_nat = \"endpoint_independent\"")
        appendLine("[handler]")
        appendLine("kind = \"socks5\"")
        appendLine("[handler.socks5]")
        appendLine("server = \"${s.socksBind}\"")
        appendLine("udp_mode = \"udp\"")
        appendLine("pool_size = 4")
        appendLine("[dns]")
        appendLine("fake_ip = ${if (s.dnsFakeIp) "true" else "false"}")
        appendLine("hijack = ${if (s.dnsHijack) "true" else "false"}")
        appendLine("[io]")
        appendLine("elastic = \"auto\"")
    }.also {
        ConnectionBus.appendLog(listOf("[zeptun] " + Zeptun.nativeVersion()))
    }

    // ------------------------------------------------------------ statistics

    private fun startTicker(s: AppSettings) {
        tickerJob?.cancel()
        lastRx = -1L; lastTx = -1L; lastTickAt = 0L
        tickerJob = scope.launch {
            while (isActive && ConnectionBus.state.value.status == VpnStatus.CONNECTED) {
                // rx = bytes read from the TUN (device -> net = upload)
                val rx = runCatching { Zeptun.nativeCounter(Zeptun.COUNTER_RX_BYTES) }.getOrDefault(-1)
                val tx = runCatching { Zeptun.nativeCounter(Zeptun.COUNTER_TX_BYTES) }.getOrDefault(-1)
                val now = System.currentTimeMillis()
                if (rx >= 0 && tx >= 0) {
                    ConnectionBus.update { st ->
                        var next = st
                        if (lastRx >= 0 && lastTickAt > 0) {
                            val dt = (now - lastTickAt).coerceAtLeast(1)
                            next = next.copy(
                                downRateBps = ((tx - lastTx).coerceAtLeast(0) * 1000 / dt),
                                upRateBps = ((rx - lastRx).coerceAtLeast(0) * 1000 / dt),
                            )
                        } else {
                            next = next.copy(downRateBps = 0, upRateBps = 0)
                        }
                        next.copy(downBytes = tx, upBytes = rx)
                    }
                }
                lastRx = rx; lastTx = tx; lastTickAt = now

                if (s.notificationStats && now - lastNotifAt > 2000) {
                    lastNotifAt = now
                    notifyStatus()
                }
                delay(1000)
            }
        }
    }

    private fun startExitRefresher(s: AppSettings) {
        exitJob?.cancel()
        exitJob = scope.launch {
            val probe = ExitInfoProbe(s.socksPort)
            val lang = if (Locale.getDefault().language == "fa") "fa" else "en"
            while (isActive && ConnectionBus.state.value.status == VpnStatus.CONNECTED) {
                val info = probe.lookup(lang)
                val ping = probe.measurePing()
                ConnectionBus.update {
                    it.copy(exitInfo = info, exitIsRealLocation = false, pingMs = ping)
                }
                notifyStatus()
                WidgetUpdater.updateAll(this@AethoneVpnService)
                delay(60_000)
            }
        }
    }

    // ----------------------------------------------------------- kill switch

    /**
     * Called from the stdout-watcher thread when the core process ends.
     * Kill switch ON  -> keep the (dead) tunnel up so nothing leaks, alert.
     * Kill switch OFF -> try to restart the core a few times, then give up.
     */
    private fun onCoreDied(settings: AppSettings) {
        val status = ConnectionBus.state.value.status
        if (status != VpnStatus.CONNECTED) return

        scope.launch {
            if (settings.killSwitch) {
                // Stop the engine: the TUN stays, every packet is dropped.
                runCatching { Zeptun.nativeStop() }
                ConnectionBus.postError(applicationContext.getString(R.string.error_core_died))
                notifyKillSwitch()
                WidgetUpdater.updateAll(this@AethoneVpnService)
            } else {
                if (reconnectAttempts < 3) {
                    reconnectAttempts++
                    ConnectionBus.update { it.copy(status = VpnStatus.CONNECTING) }
                    notifyStatus()
                    runCatching {
                        core.start(settings, firstAttempt = false) { onCoreDied(settings) }
                        ConnectionBus.update {
                            it.copy(status = VpnStatus.CONNECTED, connectedAtMillis = System.currentTimeMillis())
                        }
                        startTicker(settings)
                        startExitRefresher(settings)
                        notifyStatus()
                    }.onFailure {
                        onCoreDied(settings)
                    }
                } else {
                    fullStop()
                    ConnectionBus.postError(applicationContext.getString(R.string.error_core_died))
                    notifyStatus(error = applicationContext.getString(R.string.error_core_died))
                    WidgetUpdater.updateAll(this@AethoneVpnService)
                }
            }
        }
    }

    // -------------------------------------------------------------- teardown

    private fun disconnect() {
        scope.launch {
            ConnectionBus.update { it.copy(status = VpnStatus.DISCONNECTING) }
            notifyStatus()
            fullStop()
            settingsRepo.setLastConnected(false)
            ConnectionBus.update {
                ConnectionState(activeProtocol = it.activeProtocol)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            WidgetUpdater.updateAll(this@AethoneVpnService)
        }
    }

    private fun fullStop() {
        tickerJob?.cancel(); tickerJob = null
        exitJob?.cancel(); exitJob = null
        runCatching { Zeptun.nativeStop() }
        // the engine released the descriptor – close it for real
        tunFdInt?.let { fd -> runCatching { ParcelFileDescriptor.adoptFd(fd).close() } }
        tunFdInt = null
        teardownCore()
        tunFd?.let { runCatching { it.close() } }
        tunFd = null
    }

    private fun teardownCore() {
        core.stop()
    }

    override fun onRevoke() {
        // system (or another VPN) took over – nothing we can do but clean up
        scope.launch {
            fullStop()
            ConnectionBus.update { ConnectionState() }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            WidgetUpdater.updateAll(this@AethoneVpnService)
        }
    }

    override fun onDestroy() {
        fullStop()
        scope.cancel()
        super.onDestroy()
    }

    // ---------------------------------------------------------- notifications

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            ServiceCompat.startForeground(
                this, NOTIF_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun servicePending(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this, requestCode,
            Intent(this, AethoneVpnService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(error: String? = null): Notification {
        val st = ConnectionBus.state.value
        val ctx = this
        val builder = NotificationCompat.Builder(ctx, AethoneApp.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_stat_aethone)
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        when (st.status) {
            VpnStatus.CONNECTED -> {
                builder.setContentTitle(ctx.getString(R.string.notif_title_connected))
                    .setWhen(st.connectedAtMillis)
                    .setUsesChronometer(true)
                    .setContentText(
                        "${st.exitInfo.label}  ·  ↓ ${Formatters.speed(st.downRateBps)}  ↑ ${Formatters.speed(st.upRateBps)}"
                    )
                    .addAction(
                        0, ctx.getString(R.string.notif_action_disconnect),
                        servicePending(ACTION_DISCONNECT, 1)
                    )
            }
            VpnStatus.CONNECTING -> {
                builder.setContentTitle(ctx.getString(R.string.notif_title_connecting))
                    .setContentText(st.activeProtocol)
                    .setProgress(0, 0, true)
                    .addAction(
                        0, ctx.getString(R.string.notif_action_disconnect),
                        servicePending(ACTION_DISCONNECT, 1)
                    )
            }
            VpnStatus.ERROR -> {
                builder.setContentTitle(ctx.getString(R.string.notif_title_error))
                    .setContentText(error ?: st.lastError ?: "")
                    .addAction(
                        0, ctx.getString(R.string.notif_action_connect),
                        servicePending(ACTION_RECONNECT, 2)
                    )
            }
            else -> {
                builder.setContentTitle(ctx.getString(R.string.notif_title_disconnected))
                    .addAction(
                        0, ctx.getString(R.string.notif_action_connect),
                        servicePending(ACTION_CONNECT, 2)
                    )
            }
        }
        builder.addAction(
            0, ctx.getString(R.string.notif_action_settings), contentIntent()
        )
        return builder.build()
    }

    private fun notifyStatus(error: String? = null) {
        val nm = androidx.core.app.NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) {
            runCatching { nm.notify(NOTIF_ID, buildNotification(error)) }
        }
    }

    private fun notifyKillSwitch() {
        val ctx = this
        val n = NotificationCompat.Builder(ctx, AethoneApp.CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_aethone)
            .setContentTitle(ctx.getString(R.string.notif_kill_switch))
            .setContentText(ctx.getString(R.string.notif_kill_switch_desc))
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .addAction(
                0, ctx.getString(R.string.notif_action_connect),
                servicePending(ACTION_RECONNECT, 2)
            )
            .build()
        val nm = androidx.core.app.NotificationManagerCompat.from(this)
        if (nm.areNotificationsEnabled()) {
            runCatching { nm.notify(ALERT_NOTIF_ID, n) }
        }
    }
}
