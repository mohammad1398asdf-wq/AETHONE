package com.aethone.client.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "aethone_settings"
)

/**
 * Persists [AppSettings] in Preferences DataStore. Enum values are stored as
 * their keys; the rule lists are stored as JSON.
 */
class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object K {
        val transport = stringPreferencesKey("transport")
        val extraMode = stringPreferencesKey("extra_mode")
        val scanMode = stringPreferencesKey("scan_mode")
        val noize = stringPreferencesKey("noize")
        val quickReconnect = booleanPreferencesKey("quick_reconnect")
        val routingMode = stringPreferencesKey("routing_mode")
        val directRules = stringPreferencesKey("direct_rules")
        val blockRules = stringPreferencesKey("block_rules")
        val appsMode = stringPreferencesKey("apps_mode")
        val selectedApps = stringSetPreferencesKey("selected_apps")
        val themeMode = stringPreferencesKey("theme_mode")
        val killSwitch = booleanPreferencesKey("kill_switch")
        val autoConnectOnBoot = booleanPreferencesKey("auto_connect_boot")
        val notificationStats = booleanPreferencesKey("notification_stats")
        val lastConnected = booleanPreferencesKey("last_connected")
        val peer = stringPreferencesKey("peer")
        val goolPeer = stringPreferencesKey("gool_peer")
        val wiwOuter = stringPreferencesKey("wiw_outer")
        val wiwInner = stringPreferencesKey("wiw_inner")
        val mimOuter = stringPreferencesKey("mim_outer")
        val mimInner = stringPreferencesKey("mim_inner")
        val carrier = stringPreferencesKey("carrier")
        val quicV2 = booleanPreferencesKey("quic_v2")
        val fragment = booleanPreferencesKey("fragment")
        val fragmentSize = intPreferencesKey("fragment_size")
        val fragmentDelay = intPreferencesKey("fragment_delay")
        val dnsFakeIp = booleanPreferencesKey("dns_fake_ip")
        val dnsHijack = booleanPreferencesKey("dns_hijack")
        val ipv6 = booleanPreferencesKey("ipv6")
        val mtu = intPreferencesKey("mtu")
        val stackMode = stringPreferencesKey("stack_mode")
        val upstream = stringPreferencesKey("upstream")
        val ech = booleanPreferencesKey("ech")
        val tlsCiphers = stringPreferencesKey("tls_ciphers")
        val tls13Ciphers = stringPreferencesKey("tls13_ciphers")
        val tlsGroups = stringPreferencesKey("tls_groups")
        val tlsKeyShares = stringPreferencesKey("tls_key_shares")
        val tlsSigalgs = stringPreferencesKey("tls_sigalgs")
        val tlsCertCompression = stringPreferencesKey("tls_cert_compression")
        val tlsAlps = stringPreferencesKey("tls_alps")
        val masqueSni = stringPreferencesKey("masque_sni")
        val tlsGrease = booleanPreferencesKey("tls_grease")
        val tlsGreaseSigalgs = booleanPreferencesKey("tls_grease_sigalgs")
        val tlsOcsp = booleanPreferencesKey("tls_ocsp")
        val tlsSct = booleanPreferencesKey("tls_sct")
        val tlsSessionTicket = booleanPreferencesKey("tls_session_ticket")
        val tlsPermutation = booleanPreferencesKey("tls_permutation")
        val tlsVerify = booleanPreferencesKey("tls_verify")
        val team = stringPreferencesKey("team")
        val accessEmail = stringPreferencesKey("access_email")
        val accessId = stringPreferencesKey("access_id")
        val accessSecret = stringPreferencesKey("access_secret")
        val gateway = booleanPreferencesKey("gateway")
        val torBridges = stringPreferencesKey("tor_bridges")
        val torCountry = stringPreferencesKey("tor_country")
        val psiphonRegion = stringPreferencesKey("psiphon_region")
        val socksPort = intPreferencesKey("socks_port")
        val httpProxy = booleanPreferencesKey("http_proxy")
        val httpPort = intPreferencesKey("http_port")
        val routeSniff = booleanPreferencesKey("route_sniff")
        val sniffMs = intPreferencesKey("sniff_ms")
        val tcpConnectTimeout = intPreferencesKey("tcp_connect_timeout")
        val reprovision = booleanPreferencesKey("reprovision")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { p -> p.toSettings() }

    private fun Preferences.toSettings(): AppSettings {
        return AppSettings(
            transport = Transport.fromKey(this[K.transport]),
            extraMode = ExtraMode.fromKey(this[K.extraMode]),
            scanMode = ScanMode.fromKey(this[K.scanMode]),
            noize = Noize.fromKey(this[K.noize]),
            quickReconnect = this[K.quickReconnect] ?: true,
            routingMode = RoutingMode.fromKey(this[K.routingMode]),
            directRules = this[K.directRules]?.let { runCatching { json.decodeFromString<List<RouteRule>>(it) }.getOrNull() } ?: emptyList(),
            blockRules = this[K.blockRules]?.let { runCatching { json.decodeFromString<List<RouteRule>>(it) }.getOrNull() } ?: emptyList(),
            appsMode = AppsMode.fromKey(this[K.appsMode]),
            selectedApps = this[K.selectedApps] ?: emptySet(),
            themeMode = ThemeMode.fromKey(this[K.themeMode]),
            killSwitch = this[K.killSwitch] ?: true,
            autoConnectOnBoot = this[K.autoConnectOnBoot] ?: false,
            notificationStats = this[K.notificationStats] ?: true,
            lastConnected = this[K.lastConnected] ?: false,
            peer = this[K.peer] ?: "",
            goolPeer = this[K.goolPeer] ?: "",
            wiwOuter = this[K.wiwOuter] ?: "",
            wiwInner = this[K.wiwInner] ?: "",
            mimOuter = this[K.mimOuter] ?: "",
            mimInner = this[K.mimInner] ?: "",
            carrier = MasqueCarrier.fromKey(this[K.carrier]),
            quicV2 = this[K.quicV2] ?: true,
            fragment = this[K.fragment] ?: false,
            fragmentSize = this[K.fragmentSize] ?: 40,
            fragmentDelay = this[K.fragmentDelay] ?: 30,
            dnsFakeIp = this[K.dnsFakeIp] ?: false,
            dnsHijack = this[K.dnsHijack] ?: true,
            ipv6 = this[K.ipv6] ?: true,
            mtu = this[K.mtu] ?: 8500,
            stackMode = StackMode.fromKey(this[K.stackMode]),
            upstream = this[K.upstream] ?: "",
            ech = this[K.ech] ?: false,
            tlsCiphers = this[K.tlsCiphers] ?: "",
            tls13Ciphers = this[K.tls13Ciphers] ?: "",
            tlsGroups = this[K.tlsGroups] ?: "",
            tlsKeyShares = this[K.tlsKeyShares] ?: "",
            tlsSigalgs = this[K.tlsSigalgs] ?: "",
            tlsCertCompression = this[K.tlsCertCompression] ?: "",
            tlsAlps = this[K.tlsAlps] ?: "",
            masqueSni = this[K.masqueSni] ?: "",
            tlsGrease = this[K.tlsGrease] ?: true,
            tlsGreaseSigalgs = this[K.tlsGreaseSigalgs] ?: false,
            tlsOcsp = this[K.tlsOcsp] ?: true,
            tlsSct = this[K.tlsSct] ?: true,
            tlsSessionTicket = this[K.tlsSessionTicket] ?: true,
            tlsPermutation = this[K.tlsPermutation] ?: true,
            tlsVerify = this[K.tlsVerify] ?: true,
            team = this[K.team] ?: "",
            accessEmail = this[K.accessEmail] ?: "",
            accessId = this[K.accessId] ?: "",
            accessSecret = this[K.accessSecret] ?: "",
            gateway = this[K.gateway] ?: false,
            torBridges = this[K.torBridges] ?: "",
            torCountry = this[K.torCountry] ?: "",
            psiphonRegion = this[K.psiphonRegion] ?: "",
            socksPort = this[K.socksPort] ?: 1819,
            httpProxy = this[K.httpProxy] ?: false,
            httpPort = this[K.httpPort] ?: 1821,
            routeSniff = this[K.routeSniff] ?: true,
            sniffMs = this[K.sniffMs] ?: 400,
            tcpConnectTimeout = this[K.tcpConnectTimeout] ?: 30,
            reprovision = this[K.reprovision] ?: true,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.settingsStore.edit { p ->
            val s = p.toSettings()
            val n = transform(s)
            p[K.transport] = n.transport.key
            p[K.extraMode] = n.extraMode.key
            p[K.scanMode] = n.scanMode.key
            p[K.noize] = n.noize.key
            p[K.quickReconnect] = n.quickReconnect
            p[K.routingMode] = n.routingMode.key
            p[K.directRules] = json.encodeToString(n.directRules)
            p[K.blockRules] = json.encodeToString(n.blockRules)
            p[K.appsMode] = n.appsMode.key
            p[K.selectedApps] = n.selectedApps
            p[K.themeMode] = n.themeMode.key
            p[K.killSwitch] = n.killSwitch
            p[K.autoConnectOnBoot] = n.autoConnectOnBoot
            p[K.notificationStats] = n.notificationStats
            p[K.lastConnected] = n.lastConnected
            p[K.peer] = n.peer
            p[K.goolPeer] = n.goolPeer
            p[K.wiwOuter] = n.wiwOuter
            p[K.wiwInner] = n.wiwInner
            p[K.mimOuter] = n.mimOuter
            p[K.mimInner] = n.mimInner
            p[K.carrier] = n.carrier.key
            p[K.quicV2] = n.quicV2
            p[K.fragment] = n.fragment
            p[K.fragmentSize] = n.fragmentSize
            p[K.fragmentDelay] = n.fragmentDelay
            p[K.dnsFakeIp] = n.dnsFakeIp
            p[K.dnsHijack] = n.dnsHijack
            p[K.ipv6] = n.ipv6
            p[K.mtu] = n.mtu
            p[K.stackMode] = n.stackMode.key
            p[K.upstream] = n.upstream
            p[K.ech] = n.ech
            p[K.tlsCiphers] = n.tlsCiphers
            p[K.tls13Ciphers] = n.tls13Ciphers
            p[K.tlsGroups] = n.tlsGroups
            p[K.tlsKeyShares] = n.tlsKeyShares
            p[K.tlsSigalgs] = n.tlsSigalgs
            p[K.tlsCertCompression] = n.tlsCertCompression
            p[K.tlsAlps] = n.tlsAlps
            p[K.masqueSni] = n.masqueSni
            p[K.tlsGrease] = n.tlsGrease
            p[K.tlsGreaseSigalgs] = n.tlsGreaseSigalgs
            p[K.tlsOcsp] = n.tlsOcsp
            p[K.tlsSct] = n.tlsSct
            p[K.tlsSessionTicket] = n.tlsSessionTicket
            p[K.tlsPermutation] = n.tlsPermutation
            p[K.tlsVerify] = n.tlsVerify
            p[K.team] = n.team
            p[K.accessEmail] = n.accessEmail
            p[K.accessId] = n.accessId
            p[K.accessSecret] = n.accessSecret
            p[K.gateway] = n.gateway
            p[K.torBridges] = n.torBridges
            p[K.torCountry] = n.torCountry
            p[K.psiphonRegion] = n.psiphonRegion
            p[K.socksPort] = n.socksPort
            p[K.httpProxy] = n.httpProxy
            p[K.httpPort] = n.httpPort
            p[K.routeSniff] = n.routeSniff
            p[K.sniffMs] = n.sniffMs
            p[K.tcpConnectTimeout] = n.tcpConnectTimeout
            p[K.reprovision] = n.reprovision
        }
    }

    suspend fun setLastConnected(connected: Boolean) {
        context.settingsStore.edit { it[K.lastConnected] = connected }
    }
}
