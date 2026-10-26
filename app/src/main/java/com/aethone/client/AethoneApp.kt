package com.aethone.client

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.aethone.client.core.AethoneVpnService

class AethoneApp : Application() {

    companion object {
        const val CHANNEL_STATUS = "vpn_status"
        const val CHANNEL_ALERTS = "vpn_alerts"
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()

        // First run: default the app to Persian (user picked a bilingual UI).
        val stored = AppCompatDelegate.getApplicationLocales()
        if (stored.isEmpty) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("fa")
            )
        }
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                getString(R.string.notif_channel_status),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_status_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.notif_channel_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_alerts_desc)
            }
        )
    }
}
