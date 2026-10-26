# Keep the Zeptun JNI bridge class: libzeptun-jni.so looks it up by name
-keep class dev.zeptun.Zeptun { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.aethone.client.**$$serializer { *; }
-keepclassmembers class com.aethone.client.** { *** Companion; }
-keepclasseswithmembers class com.aethone.client.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
