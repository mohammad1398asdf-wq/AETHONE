package com.aethone.client.data

import kotlinx.serialization.Serializable

/** Tunnel transports of the Aether core, in the order shown in the picker. */
enum class Transport(val key: String, val flag: String?) {
    MASQUE("masque", null),          // default transport
    WIREGUARD("wireguard", "--wg"),
    GOOL("gool", "--gool"),
    GOOL_CLASSIC("gool_classic", "--gool-classic"),
    MIM("mim", "--mim");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: WIREGUARD
    }
}

/** Extra layer that can ride on top of a transport (Tor / Psiphon). */
enum class ExtraMode(val key: String) {
    OFF("off"),
    TOR("tor"),            // Tor through the tunnel
    TOR_REVERSE("tor_reverse"),
    TOR_ONLY("tor_only"),
    PSIPHON("psiphon");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: OFF
    }
}

enum class ScanMode(val key: String) {
    TURBO("turbo"), BALANCED("balanced"), THOROUGH("thorough"), STEALTH("stealth"), IRONCLAD("ironclad");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: BALANCED
    }
}

enum class Noize(val key: String) {
    OFF("off"), LIGHT("light"), BALANCED("balanced"), AGGRESSIVE("aggressive");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: BALANCED
    }
}

enum class RoutingMode(val key: String) {
    GLOBAL("global"),
    BYPASS_IRAN("bypass_iran"),
    BYPASS_IRAN_ADS("bypass_iran_ads"),
    CUSTOM("custom");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: BYPASS_IRAN
    }
}

enum class AppsMode(val key: String) {
    ALL("all"), EXCLUDE("exclude"), INCLUDE("include");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: ALL
    }
}

enum class ThemeMode(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: SYSTEM
    }
}

enum class MasqueCarrier(val key: String) {
    H3("h3"), H2("h2");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: H3
    }
}

enum class StackMode(val key: String) {
    USERSPACE("userspace"), HYBRID("hybrid");

    companion object {
        fun fromKey(k: String?) = entries.firstOrNull { it.key == k } ?: HYBRID
    }
}

/** One routing rule for the Aether core (domain / cidr / port / …). */
@Serializable
data class RouteRule(
    val kind: String,   // domain | full | keyword | regexp | cidr | port | private
    val value: String,
) {
    /** Renders the rule the way `aether --routes` expects it. */
    fun render(): String = when (kind) {
        "domain" -> value.trim()
        "full" -> "full:${value.trim()}"
        "keyword" -> "keyword:${value.trim()}"
        "regexp" -> "regexp:${value.trim()}"
        "cidr" -> value.trim()
        "port" -> "port:${value.trim()}"
        "private" -> "private"
        else -> value.trim()
    }
}

@Serializable
data class AppSettings(
    // Connection
    val transport: Transport = Transport.WIREGUARD,
    val extraMode: ExtraMode = ExtraMode.OFF,
    val scanMode: ScanMode = ScanMode.BALANCED,
    val noize: Noize = Noize.BALANCED,
    val quickReconnect: Boolean = true,

    // Routing
    val routingMode: RoutingMode = RoutingMode.BYPASS_IRAN,
    val directRules: List<RouteRule> = emptyList(),
    val blockRules: List<RouteRule> = emptyList(),

    // Apps
    val appsMode: AppsMode = AppsMode.ALL,
    val selectedApps: Set<String> = emptySet(),

    // Appearance
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    // language is handled through AppCompatDelegate per-app locales

    // Features
    val killSwitch: Boolean = true,
    val autoConnectOnBoot: Boolean = false,
    val notificationStats: Boolean = true,
    val lastConnected: Boolean = false,

    // Advanced - endpoint & carrier
    val peer: String = "",
    val goolPeer: String = "",
    val wiwOuter: String = "",
    val wiwInner: String = "",
    val mimOuter: String = "",
    val mimInner: String = "",
    val carrier: MasqueCarrier = MasqueCarrier.H3,
    val quicV2: Boolean = true,
    val fragment: Boolean = false,
    val fragmentSize: Int = 40,
    val fragmentDelay: Int = 30,

    // Advanced - DNS / network
    val dnsFakeIp: Boolean = false,
    val dnsHijack: Boolean = true,
    val ipv6: Boolean = true,
    val mtu: Int = 8500,
    val stackMode: StackMode = StackMode.USERSPACE,
    val upstream: String = "",

    // Advanced - TLS / ECH
    val ech: Boolean = false,
    val tlsCiphers: String = "",
    val tls13Ciphers: String = "",
    val tlsGroups: String = "",
    val tlsKeyShares: String = "",
    val tlsSigalgs: String = "",
    val tlsCertCompression: String = "",
    val tlsAlps: String = "",      // "" | new | old
    val masqueSni: String = "",
    val tlsGrease: Boolean = true,
    val tlsGreaseSigalgs: Boolean = false,
    val tlsOcsp: Boolean = true,
    val tlsSct: Boolean = true,
    val tlsSessionTicket: Boolean = true,
    val tlsPermutation: Boolean = true,
    val tlsVerify: Boolean = true,

    // Advanced - Zero Trust
    val team: String = "",
    val accessEmail: String = "",
    val accessId: String = "",
    val accessSecret: String = "",
    val gateway: Boolean = false,

    // Advanced - Tor / Psiphon
    val torBridges: String = "",
    val torCountry: String = "",
    val psiphonRegion: String = "",

    // Advanced - local proxies
    val socksPort: Int = 1819,
    val httpProxy: Boolean = false,
    val httpPort: Int = 1821,

    // Advanced - core limits
    val routeSniff: Boolean = true,
    val sniffMs: Int = 400,
    val tcpConnectTimeout: Int = 30,
    val reprovision: Boolean = true,
) {
    val socksBind: String get() = "127.0.0.1:$socksPort"
}
