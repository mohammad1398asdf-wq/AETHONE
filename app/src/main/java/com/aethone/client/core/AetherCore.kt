package com.aethone.client.core

import android.content.Context
import com.aethone.client.data.AppSettings
import com.aethone.client.data.ExtraMode
import com.aethone.client.data.MasqueCarrier
import com.aethone.client.data.RouteRule
import com.aethone.client.data.RoutingMode
import com.aethone.client.data.Transport
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.file.Files
import java.nio.file.LinkOption
import java.util.concurrent.TimeUnit

/**
 * Owns the Aether core process: installs the bundled binaries, translates
 * [AppSettings] into CLI flags + environment variables, waits for the SOCKS5
 * listener to come up and watches the process for unexpected exits.
 */
class AetherCore(private val context: Context) {

    companion object {
        // Binaries shipped under jniLibs with lib*.so names so the package
        // manager extracts them and marks them executable.
        private const val BIN_LIB = "libaether.so"
        private const val PT_LYREBIRD_LIB = "liblyrebird.so"
        private const val PT_PSIPHON_LIB = "libpsiphon.so"

        private const val INSTALL_DIR = "core"
        private const val RUN_TIMEOUT_FIRST_MS = 200_000L   // first run: registration + scan
        private const val RUN_TIMEOUT_RETRY_MS = 120_000L
        private const val PORT_POLL_MS = 500L

        /** Semver of the bundled Aether release, kept in sync with app/build.gradle.kts. */
        const val AETHER_VERSION = "v2.4.0"
    }

    val installDir: File get() = File(context.filesDir, INSTALL_DIR)
    val binDir: File get() = File(installDir, "bin")
    val ptDir: File get() = File(binDir, "pt")
    val homeDir: File get() = File(installDir, "home")
    // Android 10+ forbids execve() from the writable app data directory.
    // Use the package-manager-extracted executable in nativeLibraryDir directly.
    val coreBinary: File get() = File(context.applicationInfo.nativeLibraryDir, BIN_LIB)
    val routesFile: File get() = File(installDir, "routes.conf")

    @Volatile
    private var process: Process? = null

    @Volatile
    private var diedListener: (() -> Unit)? = null

    /**
     * Verify packaged executables and prepare the writable data directories.
     * Android 10+ blocks execve() of files copied into the writable app home.
     * Keep Aether in nativeLibraryDir and link optional PT tools to their
     * package-manager-extracted binaries rather than copying executable bytes.
     */
    @Throws(IOException::class)
    fun ensureInstalled() {
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val aetherSrc = File(nativeDir, BIN_LIB)
        if (!aetherSrc.isFile) throw IOException("libaether.so missing from nativeLibraryDir")
        if (!aetherSrc.canExecute()) throw IOException("libaether.so is not executable in nativeLibraryDir")

        homeDir.mkdirs()
        ptDir.mkdirs()

        // Aether expects these exact names in AETHER_TOR_PT_DIR. Symlinks keep
        // the executable target in nativeLibraryDir, not in writable filesDir.
        linkPackagedExecutable(
            File(nativeDir, PT_LYREBIRD_LIB), File(ptDir, "lyrebird")
        )
        linkPackagedExecutable(
            File(nativeDir, PT_PSIPHON_LIB), File(ptDir, "psiphon-tunnel-core")
        )
    }

    private fun linkPackagedExecutable(source: File, destination: File) {
        val link = destination.toPath()
        if (!source.isFile) {
            // These transports are optional for non-Tor / non-Psiphon modes.
            if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) Files.delete(link)
            return
        }

        destination.parentFile?.mkdirs()
        val target = source.canonicalFile.toPath()
        if (Files.isSymbolicLink(link)) {
            val currentTarget = runCatching { link.toRealPath() }.getOrNull()
            if (currentTarget == target) return
        }
        if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) Files.delete(link)
        Files.createSymbolicLink(link, target)
    }

    /** True when a WARP identity already exists (makes the next connect fast). */
    fun hasIdentity(): Boolean =
        File(homeDir, "aether.toml").isFile || File(homeDir, "aether-masque.toml").isFile

    /** Deletes the identity files so the core registers a fresh device. */
    fun resetIdentity() {
        homeDir.listFiles()?.forEach {
            if (it.name.startsWith("aether")) it.delete()
        }
        File(homeDir, "tor").deleteRecursively()
    }

    /** Runs `aether --version` and returns the first output line. */
    fun queryVersion(): String = runCatching {
        val p = ProcessBuilder(coreBinary.absolutePath, "--version")
            .directory(homeDir)
            .redirectErrorStream(true)
            .start()
        val out = p.inputStream.bufferedReader().readText().trim()
        p.waitFor(5, TimeUnit.SECONDS)
        out.ifBlank { "aether $AETHER_VERSION" }
    }.getOrDefault("aether $AETHER_VERSION")

    // ------------------------------------------------------------------ args

    /** CLI arguments derived from the user's settings. */
    fun buildArgs(s: AppSettings): List<String> {
        val args = mutableListOf<String>()
        // Transport
        s.transport.flag?.let { args.add(it) }

        // Extra layer (Tor / Psiphon)
        when (s.extraMode) {
            ExtraMode.TOR -> args.add("--tor")
            ExtraMode.TOR_REVERSE -> args.add("--tor-reverse")
            ExtraMode.TOR_ONLY -> args.add("--tor-only")
            ExtraMode.PSIPHON, ExtraMode.OFF -> {}
        }

        // Scan + obfuscation
        args.add("--scan"); args.add(s.scanMode.key)
        args.add("--noize"); args.add(s.noize.key)

        // Bind the SOCKS5 listener where Zeptun expects it
        args.add("--bind"); args.add(s.socksBind)

        // Optional HTTP CONNECT listener
        if (s.httpProxy) {
            args.add("--http-proxy")
            args.add("127.0.0.1:${s.httpPort}")
        }

        // MASQUE carrier over TCP when chosen (also required behind HTTP upstream)
        if (s.carrier == MasqueCarrier.H2) args.add("--h2")

        // Hand-picked endpoints
        fun endpoint(v: String) = v.trim().takeIf { it.isNotEmpty() }
        endpoint(s.peer)?.let { args.add("--peer"); args.add(it) }
        endpoint(s.goolPeer)?.let { args.add("--gool-peer"); args.add(it) }
        endpoint(s.wiwOuter)?.let { args.add("--wiw-outer"); args.add(it) }
        endpoint(s.wiwInner)?.let { args.add("--wiw-inner"); args.add(it) }
        endpoint(s.mimOuter)?.let { args.add("--mim-outer"); args.add(it) }
        endpoint(s.mimInner)?.let { args.add("--mim-inner"); args.add(it) }

        // ClientHello fragmentation
        if (s.fragment) {
            args.add("--fragment")
            if (s.fragmentSize > 0) { args.add("--fragment-size"); args.add(s.fragmentSize.toString()) }
            if (s.fragmentDelay >= 0) { args.add("--fragment-delay"); args.add(s.fragmentDelay.toString()) }
        }

        // Routing rules file (direct/block lists)
        args.add("--routes"); args.add(routesFile.absolutePath)

        // Upstream proxy chain
        endpoint(s.upstream)?.let { args.add("--upstream"); args.add(it) }

        // Zero Trust enrolment
        if (s.team.isNotBlank()) {
            args.add("--team"); args.add(s.team.trim())
            when {
                s.accessEmail.isNotBlank() -> {
                    args.add("--access-email")
                    args.add(s.accessEmail.trim())
                }
                s.accessId.isNotBlank() && s.accessSecret.isNotBlank() -> {
                    args.add("--access-id")
                    args.add(s.accessId.trim())
                    args.add("--access-secret")
                    args.add(s.accessSecret.trim())
                }
            }
            if (s.gateway) args.add("--gateway")
        }

        // ECH
        if (s.ech) { args.add("--ech"); args.add("auto") }

        // TLS fingerprint tuning (only non-default values are passed)
        fun tlsOpt(flag: String, value: String) {
            if (value.isNotBlank()) { args.add(flag); args.add(value.trim()) }
        }
        tlsOpt("--tls-ciphers", s.tlsCiphers)
        tlsOpt("--tls13-ciphers", s.tls13Ciphers)
        tlsOpt("--tls-groups", s.tlsGroups)
        tlsOpt("--tls-key-shares", s.tlsKeyShares)
        tlsOpt("--tls-sigalgs", s.tlsSigalgs)
        tlsOpt("--tls-cert-compression", s.tlsCertCompression)
        if (s.tlsAlps == "new" || s.tlsAlps == "old") { args.add("--tls-alps"); args.add(s.tlsAlps) }
        tlsOpt("--masque-sni", s.masqueSni)
        if (!s.tlsGrease) args.add("--disable-grease")
        if (s.tlsGreaseSigalgs) args.add("--enable-grease-sigalgs")
        if (!s.tlsOcsp) args.add("--disable-ocsp-stapling")
        if (!s.tlsSct) args.add("--disable-sct")
        if (!s.tlsSessionTicket) args.add("--disable-session-ticket")
        if (!s.tlsPermutation) args.add("--disable-extension-permutation")
        if (s.tlsVerify) args.add("--tls-verify")

        return args
    }

    /** Environment variables derived from the settings. */
    fun buildEnv(s: AppSettings): Map<String, String> {
        val env = mutableMapOf(
            "HOME" to homeDir.absolutePath,
            "TMPDIR" to context.cacheDir.absolutePath,
            "AETHER_TOR_PT_DIR" to ptDir.absolutePath,
        )
        if (s.extraMode == ExtraMode.PSIPHON) {
            env["AETHER_PSIPHON_MODE"] = "1"
            if (s.psiphonRegion.isNotBlank()) env["AETHER_PSIPHON_REGION"] = s.psiphonRegion.trim()
        }
        if (s.torCountry.isNotBlank()) env["AETHER_TOR_COUNTRY"] = s.torCountry.trim()
        if (s.torBridges.isNotBlank()) {
            // one bridge per line -> semicolon separated list for the env var
            val bridges = s.torBridges.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(";")
            if (bridges.isNotEmpty()) env["AETHER_TOR_BRIDGES"] = bridges
        }
        if (!s.quicV2) env["AETHER_QUIC_V2"] = "0"
        if (!s.routeSniff) env["AETHER_ROUTE_SNIFF"] = "0"
        if (s.routeSniff && s.sniffMs != 400) env["AETHER_ROUTE_SNIFF_MS"] = s.sniffMs.toString()
        if (s.tcpConnectTimeout != 30) env["AETHER_TCP_CONNECT_SECS"] = s.tcpConnectTimeout.toString()
        if (!s.reprovision) env["AETHER_REPROVISION"] = "0"
        if (s.quickReconnect && hasIdentity()) env["AETHER_QUICK_RECONNECT"] = "1"
        return env
    }

    // ----------------------------------------------------------------- rules

    /**
     * Writes the aether routes file from the bundled lists + user rules.
     * Routing modes decide which bundled lists are included.
     */
    fun writeRoutesFile(s: AppSettings) {
        val direct = mutableListOf<String>()
        val block = mutableListOf<String>()

        when (s.routingMode) {
            RoutingMode.GLOBAL, RoutingMode.CUSTOM -> { /* no bundled direct list */ }
            RoutingMode.BYPASS_IRAN, RoutingMode.BYPASS_IRAN_ADS -> {
                direct += readAssetLines("routes/iran_domains.txt")
                direct += readAssetLines("routes/iran_cidrs.txt")
            }
        }
        if (s.routingMode == RoutingMode.BYPASS_IRAN_ADS) {
            block += readAssetLines("routes/ad_domains.txt")
        }

        direct += s.directRules.map { it.render() }
        block += s.blockRules.map { it.render() }

        val text = buildString {
            if (block.isNotEmpty()) {
                appendLine("[block]")
                block.forEach { appendLine(it) }
            }
            if (direct.isNotEmpty()) {
                appendLine("[direct]")
                direct.forEach { appendLine(it) }    
            }
        }
        routesFile.parentFile?.mkdirs()
        routesFile.writeText(text)
    }

    private fun readAssetLines(path: String): List<String> =
        context.assets.open(path).bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

    // ----------------------------------------------------------------- start

    /**
     * Starts the core and suspends until the SOCKS5 listener accepts
     * connections or the timeout expires.
     *
     * @param firstAttempt longer budget for the initial WARP registration.
     * @throws CoreException when the process dies or the port never opens.
     */
    @Throws(CoreException::class)
    fun start(s: AppSettings, firstAttempt: Boolean, onDied: () -> Unit) {
        stop()
        ensureInstalled()
        writeRoutesFile(s)

        val args = buildArgs(s)
        val cmd = mutableListOf(coreBinary.absolutePath) + args
        ConnectionBus.appendLog(listOf("\$ " + cmd.joinToString(" ")))

        diedListener = onDied
        val p = try {
            ProcessBuilder(cmd)
                .directory(homeDir)
                .redirectErrorStream(true)
                .apply { environment().putAll(buildEnv(s)) }
                .start()
        } catch (e: IOException) {
            throw CoreException("failed to spawn core: ${e.message}", e)
        }
        process = p

        // Pipe output into the shared log + detect the listener line.
        Thread({
            runCatching {
                p.inputStream.bufferedReader().forEachLine { line ->
                    if (line.isNotBlank()) ConnectionBus.appendLog(listOf(line))
                }
            }
            // EOF = process ended
            val code = runCatching { p.waitFor() }.getOrDefault(-1)
            ConnectionBus.appendLog(listOf("[core exited: $code]"))
            diedListener?.invoke()
        }, "aether-stdout").start()

        val timeout = if (firstAttempt) RUN_TIMEOUT_FIRST_MS else RUN_TIMEOUT_RETRY_MS
        val deadline = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive) throw CoreException("core exited (code ${p.exitValue()})")
            if (isSocksListening(s.socksPort)) return
            try {
                Thread.sleep(PORT_POLL_MS)
            } catch (_: InterruptedException) {
                throw CoreException("interrupted while waiting for the core")
            }
        }
        stop()
        throw CoreException("timeout: no reachable endpoint / SOCKS port never opened")
    }

    private fun isSocksListening(port: Int): Boolean = try {
        Socket().use { s ->
            s.connect(InetSocketAddress("127.0.0.1", port), 400)
            true
        }
    } catch (_: Exception) {
        false
    }

    /** Kills the core process if it is running. */
    fun stop() {
        diedListener = null
        process?.let { p ->
            runCatching {
                p.destroy()
                if (!p.waitFor(2, TimeUnit.SECONDS)) p.destroyForcibly()
            }
        }
        process = null
    }

    class CoreException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
