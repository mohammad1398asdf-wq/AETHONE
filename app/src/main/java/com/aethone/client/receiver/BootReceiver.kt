package com.aethone.client.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aethone.client.core.AethoneVpnService
import com.aethone.client.core.ConnectionBus
import com.aethone.client.core.VpnStatus
import com.aethone.client.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reconnects after a reboot (or an app update) when the user asked for it and
 * the tunnel was up before the restart.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val repo = SettingsRepository(context)
                val s = repo.current()
                val wasUp = s.lastConnected
                val serviceAlive = ConnectionBus.state.value.status == VpnStatus.CONNECTED
                if (s.autoConnectOnBoot && wasUp && !serviceAlive) {
                    AethoneVpnService.start(context, AethoneVpnService.ACTION_CONNECT)
                }
            } finally {
                result.finish()
            }
        }
    }
}
