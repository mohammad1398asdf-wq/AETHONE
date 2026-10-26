package com.aethone.client.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Looks up the exit location and the tunnel latency.
 *
 * Everything goes through the local SOCKS5 listener (the Aether proxy), so
 * the result reflects where the tunnel really exits. When [socksPort] is
 * null the lookups run direct – used before the first connect to show the
 * user's real location on the home card.
 */
class ExitInfoProbe(private val socksPort: Int?) {

    private fun client(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
        if (socksPort != null) {
            builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort)))
        }
        return builder.build()
    }

    /** Latency in ms: median of 3 TCP connects to 1.1.1.1:443 through the proxy. */
    fun measurePing(): Int {
        if (socksPort == null) return -1
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", socksPort))
        val samples = mutableListOf<Long>()
        repeat(3) {
            val t0 = System.nanoTime()
            val ok = runCatching {
                Socket(proxy).use { s ->
                    s.soTimeout = 4000
                    s.connect(InetSocketAddress("1.1.1.1", 443), 4000)
                }
            }.isSuccess
            if (ok) samples += (System.nanoTime() - t0) / 1_000_000
            Thread.sleep(120)
        }
        if (samples.isEmpty()) return -1
        return samples.sorted()[samples.size / 2].toInt()
    }

    suspend fun lookup(lang: String): ExitInfo = withContext(Dispatchers.IO) {
        val http = client()
        runCatching {
            // 1) who does the internet see us as (through the tunnel)?
            val trace = http.newCall(
                Request.Builder().url("https://www.cloudflare.com/cdn-cgi/trace").build()
            ).execute().use { resp -> resp.body?.string() ?: "" }

            val map = trace.lines()
                .filter { it.contains('=') }
                .associate { line ->
                    val idx = line.indexOf('=')
                    line.substring(0, idx) to line.substring(idx + 1)
                }
            val ip = map["ip"] ?: ""
            val warpOn = map["warp"] == "on"

            // 2) geolocate that address (also through the tunnel)
            var country = ""; var cc = ""; var city = ""; var isp = ""
            if (ip.isNotBlank()) {
                val geo = http.newCall(
                    Request.Builder()
                        .url("http://ip-api.com/json/$ip?fields=status,country,countryCode,city,isp&lang=$lang")
                        .build()
                ).execute().use { resp -> resp.body?.string() ?: "" }
                fun field(name: String): String {
                    val needle = "\"$name\":\""
                    val i = geo.indexOf(needle)
                    if (i < 0) return ""
                    return geo.substring(i + needle.length).substringBefore('"')
                }
                country = field("country"); cc = field("countryCode")
                city = field("city"); isp = field("isp")
            }
            ExitInfo(
                ip = ip,
                country = country,
                countryCode = cc,
                city = city,
                isp = isp,
                warpOn = warpOn,
            )
        }.getOrElse { ExitInfo() }
    }
}
