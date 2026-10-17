import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Load .env properties from root project or system env
val envProps = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
    envFile.inputStream().use { envProps.load(it) }
}

fun getEnvOrProperty(key: String, default: String): String {
    return System.getenv(key)
        ?: (project.findProperty(key) as? String)
        ?: envProps.getProperty(key)
        ?: default
}

android {
    namespace = "bizora.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "bizora.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 20
        versionName = "5.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiUrl = getEnvOrProperty("API_BASE_URL", "https://www.attendance-tracker.shivamshankhdhar.online/api/v1")
        val googleClientId = getEnvOrProperty("GOOGLE_CLIENT_ID", "667573150359-4cff25jgf98hqq00pdrnojslk4bri5sb.apps.googleusercontent.com")
        val googleCallbackUrl = getEnvOrProperty("GOOGLE_CALLBACK_URL", "https://www.attendance-tracker.shivamshankhdhar.online/api/v1/auth/google/callback")
        val playStoreUrl = getEnvOrProperty("PLAY_STORE_URL", "https://play.google.com/store/apps/details?id=bizora.app")

        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "GOOGLE_CALLBACK_URL", "\"$googleCallbackUrl\"")
        buildConfigField("String", "PLAY_STORE_URL", "\"$playStoreUrl\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/bizora-release.keystore")
            storePassword = getEnvOrProperty("KEYSTORE_PASSWORD", "ShivamIsAnEngineerSince2021@")
            keyAlias = getEnvOrProperty("KEYSTORE_ALIAS", "bizora-key")
            keyPassword = getEnvOrProperty("KEY_PASSWORD", "ShivamIsAnEngineerSince2021@")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Networking (Retrofit, Moshi, OkHttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging)

    // Image loading
    implementation(libs.coil.compose)

    // Hardware & Security
    implementation(libs.androidx.biometric)
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)

    // AdMob Ads
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
