package com.aethone.client.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Where the tunnel exits, as seen from the internet. */
data class ExitInfo(
    val ip: String = "",
    val country: String = "",
    val countryCode: String = "",
    val city: String = "",
    val isp: String = "",
    val warpOn: Boolean = false,
) {
    val flagEmoji: String
        get() = if (countryCode.length == 2) {
            val base = 0x1F1E6 - 'A'.code
            String(Character.toChars(base + countryCode[0].code)) +
                String(Character.toChars(base + countryCode[1].code))
        } else "🌐"

    val label: String
        get() = buildString {
            if (country.isNotBlank()) append(country.uppercase())
            if (city.isNotBlank()) {
                if (isNotEmpty()) append(" / ")
                append(city.uppercase())
            }
            if (isEmpty()) append("UNKNOWN")
        }
}

enum class VpnStatus {
    DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR
}

/** A snapshot of the live session, published by the VPN service. */
data class ConnectionState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val connectedAtMillis: Long = 0L,
    val downBytes: Long = 0L,
    val upBytes: Long = 0L,
    val downRateBps: Long = 0L,
    val upRateBps: Long = 0L,
    val exitInfo: ExitInfo = ExitInfo(),
    val exitIsRealLocation: Boolean = true,
    val pingMs: Int = -1,
    val lastError: String? = null,
    val coreLog: List<String> = emptyList(),
    val activeProtocol: String = "",
)

/**
 * Process-wide connection state. The VPN service is the only writer; the UI,
 * widgets and notification read from it.
 */
object ConnectionBus {
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun update(transform: (ConnectionState) -> ConnectionState) {
        _state.value = transform(_state.value)
    }

    fun resetForNewSession() {
        _state.value = ConnectionState(activeProtocol = _state.value.activeProtocol)
    }

    fun appendLog(lines: List<String>) {
        if (lines.isEmpty()) return
        _state.update { s ->
            val merged = (s.coreLog + lines).takeLast(600)
            s.copy(coreLog = merged)
        }
    }

    fun postError(message: String) {
        _state.update { it.copy(status = VpnStatus.ERROR, lastError = message) }
    }
}
