import java.util.Properties
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}

fun obfuscateKey(raw: String): String {
    if (raw.isEmpty()) return ""
    val bytes = raw.toByteArray(Charsets.UTF_8)
    val masked = ByteArray(bytes.size) { i -> (bytes[i].toInt() xor 0x5A).toByte() }
    return Base64.getEncoder().encodeToString(masked)
}

val agnesApiKey = localProperties.getProperty("AGNES_API_KEY") ?: ""
val agnesBaseUrl = localProperties.getProperty("AGNES_BASE_URL") ?: "https://apihub.agnes-ai.com/v1"
val tavilyApiKey = localProperties.getProperty("TAVILY_API_KEY") ?: ""

android {
    namespace = "id.gustyxganteng.agent"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.gustyxganteng.agent"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "AGNES_API_KEY", "\"${obfuscateKey(agnesApiKey)}\"")
        buildConfigField("String", "AGNES_BASE_URL", "\"$agnesBaseUrl\"")
        buildConfigField("String", "TAVILY_API_KEY", "\"${obfuscateKey(tavilyApiKey)}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = file("keystore/release.jks")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: "ggagent123"
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: "ggagent"
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: "ggagent123"
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Networking & SSE
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)

    // Serialization & Coroutines
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Storage: Room & DataStore
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    // Web & Images
    implementation(libs.coil.compose)
    implementation(libs.jsoup)

    // DI: Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
