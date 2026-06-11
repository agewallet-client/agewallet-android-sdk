import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Read AgeWallet config from gitignored local.properties at repo root.
// Falls back to prod public defaults so the demo source ships with safe placeholders.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun cfg(key: String, default: String): String =
    (localProps.getProperty(key) ?: default)

android {
    namespace = "io.agewallet.sdk.demo.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.agewallet.sdk.demo.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "AGEWALLET_CLIENT_ID",
            "\"${cfg("agewallet.clientId", "your-client-id")}\"")
        buildConfigField("String", "AGEWALLET_AUTH",
            "\"${cfg("agewallet.auth", "https://app.agewallet.io/user/authorize")}\"")
        buildConfigField("String", "AGEWALLET_TOKEN",
            "\"${cfg("agewallet.token", "https://app.agewallet.io/user/token")}\"")
        buildConfigField("String", "AGEWALLET_USERINFO",
            "\"${cfg("agewallet.userinfo", "https://app.agewallet.io/user/userinfo")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":agewallet-sdk"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    debugImplementation(libs.androidx.ui.tooling)
}
