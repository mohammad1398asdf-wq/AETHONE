package com.aethone.client.util

import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Formatting helpers shared by the UI, the notification and the widgets. */
object Formatters {

    /** 145 MB, 1.2 GB … (session volume) */
    fun bytes(v: Long): String {
        if (v < 1024) return "$v B"
        val units = listOf("KB", "MB", "GB", "TB")
        var value = v.toDouble()
        var unit = -1
        while (value >= 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return if (value >= 100) "${value.toInt()} ${units[unit]}"
        else String.format(Locale.US, "%.1f %s", value, units[unit])
    }

    /** 2.4 MB/s, 850 KB/s … (live rate, input = bytes per second) */
    fun speed(bps: Long): String = if (bps < 1024) "$bps B/s" else bytes(bps) + "/s"

    /** 01:23:09 style session timer */
    fun elapsed(millis: Long): String {
        val total = millis / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    /** Green / amber / red latency class used for the ping dot. */
    fun pingClass(ms: Int): PingQuality = when {
        ms < 0 -> PingQuality.UNKNOWN
        ms < 150 -> PingQuality.GOOD
        ms < 400 -> PingQuality.OK
        else -> PingQuality.BAD
    }

    enum class PingQuality { GOOD, OK, BAD, UNKNOWN }
}
