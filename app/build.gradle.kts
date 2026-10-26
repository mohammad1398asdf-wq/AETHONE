import java.net.URL

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aethone.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aethone.client"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // ABIs that the Aether project ships Android binaries for.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        // The Aether core is executed as a standalone binary, so it must be
        // extracted from the APK to disk by the system package manager.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.documentfile)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.ui.tooling)
}

// ---------------------------------------------------------------------------
// Core fetcher: downloads the Aether + Zeptun binaries straight from their
// GitHub releases and lays them out under src/main/jniLibs as lib*.so files
// (the only way Android extracts + marks native files executable).
// Run ./gradlew fetchCores (also wired before preBuild).
// Force a re-download with -PrefreshCores
// ---------------------------------------------------------------------------
val aetherVersion = providers.gradleProperty("aetherVersion").getOrElse("v2.4.0")
val zeptunVersion = providers.gradleProperty("zeptunVersion")
    .getOrElse("v1.1.3")

val jniLibsDir = layout.projectDirectory.dir("src/main/jniLibs")

val aetherAbiMap = mapOf(
    "arm64-v8a" to "arm64",
    "armeabi-v7a" to "armv7",
    "x86_64" to "x86_64",
)

fun download(url: String, dest: File) {
    println("fetching $url")
    dest.parentFile.mkdirs()
    val tmp = File(dest.parentFile, dest.name + ".part")
    URL(url).openStream().use { input ->
        tmp.outputStream().use { output -> input.copyTo(output) }
    }
    if (!tmp.renameTo(dest)) {
        tmp.copyTo(dest, overwrite = true)
        tmp.delete()
    }
}

val fetchCores = tasks.register("fetchCores") {
    group = "aethone"
    description = "Downloads Aether and Zeptun Android binaries from GitHub releases."

    val refresh = providers.gradleProperty("refreshCores").isPresent

    onlyIf {
        val allPresent = aetherAbiMap.keys.all { abi ->
            File(jniLibsDir.asFile, "$abi/libaether.so").isFile &&
                File(jniLibsDir.asFile, "$abi/libzeptun-jni.so").isFile
        }
        refresh || !allPresent
    }

    doLast {
        val buildTmp = layout.buildDirectory.dir("coreFetch").get().asFile
        buildTmp.mkdirs()

        // ---- Aether -------------------------------------------------------
        aetherAbiMap.forEach { (abi, aetherArch) ->
            val target = File(jniLibsDir.asFile, abi)
            val tar = File(buildTmp, "aether-$aetherArch.tar.gz")
            download(
                "https://github.com/CluvexStudio/Aether/releases/download/$aetherVersion/aether-android-$aetherArch.tar.gz",
                tar
            )
            copy {
                from(tarTree(resources.gzip(tar)))
                into(target)
                include("aether", "pt/lyrebird", "pt/psiphon-tunnel-core")
                includeEmptyDirs = false
                eachFile {
                    // flatten: binaries live as lib*.so at the ABI root so the
                    // system package manager extracts and marks them executable
                    path = when (file.name) {
                        "aether" -> "libaether.so"
                        "lyrebird" -> "liblyrebird.so"
                        "psiphon-tunnel-core" -> "libpsiphon.so"
                        else -> file.name
                    }
                }
            }
            println("aether -> $target")
        }

        // ---- Zeptun -------------------------------------------------------
        val zdest = File(buildTmp, "zeptun-jniLibs.zip")
        download(
            "https://github.com/Noisemux/zeptun/releases/download/$zeptunVersion/zeptun-android-jniLibs.zip",
            zdest
        )
        val zdir = File(buildTmp, "zeptun")
        copy {
            from(zipTree(zdest))
            into(zdir)
        }
        aetherAbiMap.keys.forEach { abi ->
            val srcDir = File(zdir, "jniLibs/$abi")
            if (srcDir.isDirectory) {
                copy {
                    from(srcDir)
                    into(File(jniLibsDir.asFile, abi))
                }
            }
        }
        println("zeptun -> $jniLibsDir")
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(fetchCores)
}
