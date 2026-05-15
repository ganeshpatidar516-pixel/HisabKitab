import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.ganesh.hisabkitabpro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ganesh.hisabkitabpro"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.ganesh.hisabkitabpro.HiltTestRunner"
        // Phase 7: enable only after real SPKI pins replace PLACEHOLDER entries in cert_pinning.xml.
        buildConfigField("Boolean", "CERT_PINNING_ENABLED", "false")
        // Phase 8: numeric Play Integrity / GCP project number. Use "0" until configured in Gradle/CI.
        buildConfigField("String", "PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER", "\"0\"")
        // Business identity Phase 2 stub: asset taxonomy + search UI writing only businessCategory.
        // Enabled for all build types; set to false here to instantly roll back to plain text field.
        buildConfigField("Boolean", "BUSINESS_IDENTITY_TAXONOMY_STUB", "true")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias").orEmpty()
                keyPassword = keystoreProperties.getProperty("keyPassword").orEmpty()
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile").orEmpty())
                storePassword = keystoreProperties.getProperty("storePassword").orEmpty()
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        getByName("release") {
            // Release hardening: obfuscate code and shrink unused resources.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/INDEX.LIST"
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

val validateReleaseFirebaseConfig by tasks.registering {
    group = "verification"
    description = "Fails release build if app/google-services.json is placeholder or missing."
    doLast {
        val file = file("google-services.json")
        if (!file.exists()) {
            throw GradleException(
                "Missing app/google-services.json. Add your Firebase production config before release."
            )
        }
        val json = file.readText()
        val hasPlaceholderProject = json.contains("hisabkitabpro-placeholder-replace")
        val hasPlaceholderKey = json.contains("AIzaSy00000000000000000000000000000000000")
        if (hasPlaceholderProject || hasPlaceholderKey) {
            throw GradleException(
                "Placeholder Firebase config detected in app/google-services.json. " +
                    "Replace with the real Firebase Console file before release."
            )
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        dependsOn(validateReleaseFirebaseConfig)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(project(":core:common"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // BCrypt
    implementation(libs.jbcrypt)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // Align with Room 2.6.1 (libs.versions.toml) — avoid alpha skew / metadata mismatch.
    implementation("androidx.room:room-paging:2.6.1")
    ksp(libs.androidx.room.compiler)

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.3.6")
    implementation("androidx.paging:paging-compose:3.3.6")

    // SQLCipher
    implementation(libs.android.database.sqlcipher)
    implementation(libs.androidx.sqlite.ktx)

    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")

    // Core and Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // Icons and UI
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Network & Google Drive
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation("com.google.android.play:integrity:1.3.0")
    implementation(libs.google.auth)
    implementation(libs.google.api.drive) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)

    // Media Loading & Playback
    implementation(libs.coil.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)

    // ML and QR
    implementation(libs.zxing.core)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.kotlinx.coroutines.play.services)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.exifinterface)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)

    // Play Store: in-app updates (flexible) — only applies when installed from Play
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Firebase: Crashlytics + Analytics only. Performance SDK removed: with a placeholder
    // google-services.json it still auto-registers and crashes the process on a background thread
    // (FirebaseInstallations.getId + invalid API key). Re-add firebase-perf when using a real Firebase project.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.sdk)
    implementation(libs.firebase.analytics.sdk)
    implementation(libs.firebase.auth.sdk)
    implementation(libs.firebase.firestore.sdk)

    // Fix for Kotlin Metadata version mismatch
    implementation(libs.kotlinx.metadata.jvm)

    // iText PDF
    implementation(libs.itext.kernel)
    implementation(libs.itext.layout)
    implementation(libs.itext.io)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    /** Kakao Compose — same DSL layer Kaspresso wraps for Compose (Kaspresso ComposeSupport pulls this transitively). */
    androidTestImplementation("io.github.kakaocup:compose:0.4.4")

    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest(libs.hilt.compiler)
}
