# jniLibs

The core binaries live here after the first build:

| File | What it is | Size/ABI |
|---|---|---|
| `libaether.so` | Aether core (WARP/MASQUE) executable | ~19 MB |
| `liblyrebird.so` | Tor obfs4 pluggable transport (for bridges) | ~15 MB |
| `libpsiphon.so` | Psiphon transport | ~22 MB |
| `libzeptun.so` | Zeptun tun2socks engine (shared lib) | ~0.3 MB |
| `libzeptun-jni.so` | Zeptun JNI bridge | ~0.3 MB |

They are **not committed** (see .gitignore): run

    ./gradlew fetchCores          # or: sh scripts/fetch_cores.sh

to download them from the official GitHub releases of
[CluvexStudio/Aether](https://github.com/CluvexStudio/Aether) and
[Noisemux/zeptun](https://github.com/Noisemux/zeptun).
The `preBuild` task depends on `fetchCores`, so a plain
`./gradlew assembleDebug` fetches them automatically on first build.

Binaries are renamed `lib*.so` because that is the only naming Android's
package manager extracts to `nativeLibraryDir` with the executable bit set.
