import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // NO kotlin.android here — AGP 9.0+ applies Kotlin itself.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ---------------------------------------------------------------------------
// RELEASE SIGNING — APK Signature Schemes V1 + V2 + V3 (+ V4 bonus)
// ---------------------------------------------------------------------------
// Credentials come ONLY from local.properties (on the builder machine) or
// from environment variables of the same names. Nothing is hardcoded here
// and the keystore / passwords are never committed (see .gitignore).
//
// local.properties keys:
//   NOVAPROFILE_STORE_FILE=/absolute/path/to/novaprofile-release.jks
//   NOVAPROFILE_STORE_PASSWORD=<password>
//   NOVAPROFILE_KEY_ALIAS=novaprofile-release
//   NOVAPROFILE_KEY_PASSWORD=<password>
//
// The SAME keystore must be reused for every future update: Play Store and
// Android reject updates signed with a different key.
// ---------------------------------------------------------------------------
val keystoreProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { keystoreProps.load(it) }
}

fun signingProp(name: String): String? =
    System.getenv(name) ?: keystoreProps.getProperty(name)

val releaseStoreFile: java.io.File? =
    signingProp("NOVAPROFILE_STORE_FILE")?.let { file(it) }?.takeIf { it.exists() }

android {
    namespace = "com.novaprofiles.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.novaprofiles.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "2.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseStoreFile != null) {
                storeFile = releaseStoreFile
                storePassword = signingProp("NOVAPROFILE_STORE_PASSWORD")
                keyAlias = signingProp("NOVAPROFILE_KEY_ALIAS")
                keyPassword = signingProp("NOVAPROFILE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
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
            // Only wire the release signing config when a keystore actually
            // exists. Unconditional assignment fails CI with
            // "Keystore file not set for signing config release"
            // (validateSigningRelease, Run #4) when no keystore secret is
            // set — CI then builds an unsigned APK for local signing.
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // NOTE (AGP 9): android.kotlinOptions {} does NOT exist under AGP 9's
    // built-in Kotlin (Run #3 failed on it as an unresolved reference).
    // JVM target is set in the top-level kotlin {} block below instead.

    buildFeatures {
        compose = true
    }
}

// JVM target for Kotlin compilation (AGP 9 built-in Kotlin: the
// top-level kotlin {} extension is available without applying any
// Kotlin plugin; kotlinOptions inside android {} is gone in AGP 9).
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.webkit)
    ksp(libs.androidx.room.compiler)
}
