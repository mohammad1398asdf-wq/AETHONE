package dev.zeptun

/**
 * Bridge to libzeptun-jni.so.
 *
 * The library registers its native methods on exactly this class
 * (`dev/zeptun/Zeptun`) at load time — see zeptun's `src/jni/zeptun_jni.c`.
 * `nativeStart` takes the TUN descriptor returned by
 * `VpnService.Builder.establish()` plus an optional TOML document; passing
 * null selects the built-in mobile preset. The service instance is held as a
 * global reference and its `protect(int)` method is called for every upstream
 * socket so tunnel traffic never loops back into the TUN device.
 */
object Zeptun {
    init {
        System.loadLibrary("zeptun-jni")
    }

    /** Stats counter indices (ZeptunStats fields from rx_packets onwards). */
    const val COUNTER_RX_PACKETS = 0
    const val COUNTER_RX_BYTES = 1
    const val COUNTER_TX_PACKETS = 2
    const val COUNTER_TX_BYTES = 3
    const val COUNTER_TCP_ACTIVE = 14
    const val COUNTER_UDP_ACTIVE = 22
    const val COUNTER_DNS_HIJACKED = 37

    external fun nativeStart(service: Any?, fd: Int, config: String?): Int
    external fun nativeStop()
    external fun nativeVersion(): String
    external fun nativeCounter(index: Int): Long
}
