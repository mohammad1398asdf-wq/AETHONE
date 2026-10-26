# AETHONE 🜂

**A free, open-source Android VPN client built on the Aether core and the Zeptun tun2socks engine.**

AETHONE turns Cloudflare WARP into a full-device tunnel and wraps it in a
bilingual (فارسی / English) UI designed for normal users: every option of the
core is exposed as a plain-language setting.

| | |
|---|---|
| Transports | MASQUE (HTTP/3 + HTTP/2), WireGuard, Gool (WG-in-MASQUE), Gool-Classic (WG-in-WG), Mim (MASQUE-in-MASQUE) |
| Extra layers | Tor (3 modes, auto bridges), Psiphon (experimental) |
| Full-device tunnel | Zeptun engine (in-process JNI) on top of `VpnService` |
| Iran bypass | Official RIPE IP space (~1,955 prefixes) + curated domestic domain list, editable in-app |
| UI | Light & dark themes, home widgets (3 sizes), rich notifications, per-app split tunneling |
| Safety | In-app kill switch, optional system always-on guidance |
| Languages | فارسی (default) + English |
| Min Android | 10 (API 29) |

## Building

Requirements: **JDK 17+** (Temurin 21 recommended) and Android SDK 35
(the easiest route is opening the project in a current Android Studio).

```bash
./gradlew assembleDebug
```

The first build automatically downloads the cores from GitHub releases into
`app/src/main/jniLibs` (task `fetchCores`):

- [Aether v2.4.0](https://github.com/CluvexStudio/Aether/releases) — arm64, armv7, x86_64
- [Zeptun v1.1.3](https://github.com/Noisemux/zeptun/releases) — JNI libraries

Offline? Pre-fetch them with `sh scripts/fetch_cores.sh`, or force a refresh
with `./gradlew fetchCores -PrefreshCores`.

The APK lands in `app/build/outputs/apk/debug/`.

Pin different core versions with Gradle properties:

```bash
./gradlew assembleDebug -PaetherVersion=v2.4.0 -PzeptunVersion=v1.1.3
```

## How it works

```
┌────────────── your apps ──────────────┐
│                ↓                      │ Android VpnService TUN fd
│         Zeptun (userspace)            │ tun2socks, DNS hijack, fake-IP
│                ↓                      │
│      SOCKS5 · 127.0.0.1:1819          │
│                ↓                      │
│     Aether core (child process)       │ WARP registration + tunnel
│         ↓              ↓              │
│   WARP/MASQUE      route-direct       │ matched Iranian/domestic
│   (the tunnel)     (real interface)   │ traffic bypasses the tunnel
└───────────────────────────────────────┘
```

- The **Aether core** runs as a child process out of `filesDir/core/bin`
  (installed from the packaged `libaether.so`). Registration, endpoint
  scanning and identity files all live in `filesDir/core/home`.
- **Zeptun** is loaded in-process through `libzeptun-jni.so`, reads the
  established TUN descriptor and forwards every flow to the core's SOCKS5
  listener. Its `protect()` callback keeps upstream sockets out of the loop.
- **Iran bypass** writes a combined `[direct]`/`[block]` rules file
  (`routes.conf`) from the bundled RIPE prefix list + your custom rules, then
  hands it to the core via `--routes`. Domains are matched by SNI/Host
  sniffing behind the tun.

## Features in the UI

- **Home** — connect button (the brand mark), status, session timer, live
  down/up speeds, exit location + flag, ping, protocol picker
- **Routing** — 4 presets (everything / bypass Iran / bypass Iran + adblock /
  custom) plus a full pro editor with every rule type the core understands
  (domain, full, keyword, regexp, CIDR, port, private/LAN)
- **Apps** — exclude/include split tunneling
- **Advanced** — endpoints, MASQUE carrier (H3/H2), QUIC v2 opener,
  ClientHello fragmentation, DNS (fake-IP, hijack), MTU, TCP stack, upstream
  proxy, ECH, 16 TLS fingerprint knobs, Cloudflare Zero Trust, Tor bridges,
  Psiphon, local proxy ports, core limits
- **Diagnostics** — live core log, tunnel test, identity reset
- **Widgets** — 3 home-screen widgets following the app theme
- **Kill switch** — if the core dies the TUN stays up and drops everything;
  plus a shortcut to the system "always-on VPN" hard guarantee

## Licensing

- **AETHONE app code** — GNU AGPL-3.0 (see [LICENSE](LICENSE))
- **Aether core** — AGPL-3.0, © CluvexStudio — binaries fetched at build time
- **Zeptun engine** — MIT, © Noisemux
- **Vazirmatn font** — SIL OFL 1.1, © Saber Rastikerdar

This project is not affiliated with Cloudflare. Use it responsibly and in
accordance with the laws of your jurisdiction.
