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

# Kotlin + Gson + Room metadata (required for release reflection).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keepattributes KotlinMetadata, *Annotation*

# ---------------------------------------------------------------------------
# SQLCipher (P0) — release crash: JNI_OnLoad looks up field "mNativeHandle" by name.
# Without these rules R8 strips/renames it → SIGABRT on Application.onCreate / AppDatabase.
# ---------------------------------------------------------------------------
-keep class net.sqlcipher.** { *; }
-keepclassmembers class net.sqlcipher.** { *; }
-keepclasseswithmembernames class net.sqlcipher.** {
    native <methods>;
}
-keepclassmembers class net.sqlcipher.database.SQLiteDatabase {
    long mNativeHandle;
}
-keep class net.sqlcipher.database.SupportFactory { *; }
-keep class net.sqlcipher.database.SQLiteOpenHelper { *; }
-dontwarn net.sqlcipher.**

# ---------------------------------------------------------------------------
# Hilt / Dagger (generated injectors + entry points)
# ---------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keep class com.ganesh.hisabkitabpro.Hilt_* { *; }
-keep class com.ganesh.hisabkitabpro.**_HiltModules* { *; }
-keep class com.ganesh.hisabkitabpro.**_MembersInjector { *; }
-keep class com.ganesh.hisabkitabpro.**_Factory { *; }
-keep class com.ganesh.hisabkitabpro.**_Impl { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

# ---------------------------------------------------------------------------
# HisabKitab data layer — Room entities, DAOs, DTOs, sync payloads (user mandate)
# ---------------------------------------------------------------------------
-keep class com.ganesh.hisabkitabpro.data.** { *; }
-keep interface com.ganesh.hisabkitabpro.data.repository.local.** { *; }
-keep class com.ganesh.hisabkitabpro.domain.model.** { *; }
-keep class com.ganesh.hisabkitabpro.domain.sync.** { *; }
-keep class com.ganesh.hisabkitabpro.domain.businessidentity.** { *; }
-keep class com.ganesh.hisabkitabpro.addon.audit.** { *; }
-keep class com.ganesh.hisabkitabpro.data.repository.converters.** { *; }

# Retrofit / API
-keep class com.ganesh.hisabkitabpro.network.api.** { *; }

# Keep Gson type adapters and prevent warnings from optional libs.
-keep class com.google.gson.** { *; }
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Room (release minify) — keep generated *_Impl and Kotlin property columns
-keep class * extends androidx.room.RoomDatabase {
    <init>(...);
    <fields>;
    <methods>;
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase_Impl { *; }
-keep class **_Impl { *; }
-keep class com.ganesh.hisabkitabpro.data.local.AppDatabase { *; }
-keep class com.ganesh.hisabkitabpro.data.local.AppDatabase_Impl { *; }
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