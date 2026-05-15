# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Hilt/Dagger generated wiring used through reflection/proguard-sensitive paths.
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Retrofit service interfaces and model metadata for Gson.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.ganesh.hisabkitabpro.network.api.** { *; }
-keep class com.ganesh.hisabkitabpro.domain.model.** { *; }
-keep class com.ganesh.hisabkitabpro.domain.businessidentity.** { *; }
-keep class com.ganesh.hisabkitabpro.addon.audit.** { *; }

# Keep Gson type adapters and prevent warnings from optional libs.
-keep class com.google.gson.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Room (release minify)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Play Core / in-app update (reflection in some paths)
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# Firebase (Crashlytics readable stack traces)
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firestore pulls gRPC transitively. R8 reports a single missing reference
# (io.grpc.InternalGlobalInterceptors) auto-suggested in missing_rules.txt.
# -dontwarn only suppresses the warning; it does not keep or strip any class.
-dontwarn io.grpc.InternalGlobalInterceptors

# iText 7 (PDF engine: invoice PDFs, business card PDFs).
# iText loads font registries, IO factories, and PDF object types via reflection;
# stripping under R8 has historically caused release-only ClassNotFoundException
# on the first PDF generation. Additive keeps; no behavior change.
-keep class com.itextpdf.** { *; }
-keepclassmembers class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**

# ZXing core (QR generation + scanner hint encoding).
# Plain Java JAR with no consumer-rules; defensive keep to survive aggressive R8.
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ML Kit (text recognition + barcode scanning).
# AARs ship consumer-rules, but ML Kit pipelines also dispatch through
# com.google.android.gms.internal.mlkit_* and Tasks reflection. Defensive keeps.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_**