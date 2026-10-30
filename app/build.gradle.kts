plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Sürüm kodu: CI her derlemede `-PsupfyVersionCode=<1000 + çalışma no>` geçer, böylece her yeni APK
// bir öncekinden büyük olur ve Android onu güncelleme olarak kabul eder. Yerel derlemede 5 kullanılır.
val supfyVersionCode = (project.findProperty("supfyVersionCode") as String?)?.toIntOrNull() ?: 5

// İmza: anahtar dosyası ve şifreler yalnızca ortam değişkenlerinden okunur (CI'da GitHub Secrets).
// Depoda hiçbir anahtar veya şifre bulunmaz. Değişkenler yoksa Android'in debug anahtarı kullanılır.
val supfyKeystorePath: String? = System.getenv("SUPFY_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.supfy.app"
    compileSdk = 35

    defaultConfig {
        // DİKKAT: applicationId değişirse Android yeni APK'yı ayrı bir uygulama sayar. Sabit kalmalı.
        applicationId = "com.supfy.app"
        minSdk = 26
        targetSdk = 35
        versionCode = supfyVersionCode
        versionName = "0.4.1"

        // Uygulama içi güncelleme denetiminin sorguladığı depo (owner/repo).
        buildConfigField("String", "UPDATE_REPO", "\"ebilgi992/supfy\"")
    }

    signingConfigs {
        if (supfyKeystorePath != null) {
            create("supfy") {
                storeFile = file(supfyKeystorePath)
                storePassword = System.getenv("SUPFY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SUPFY_KEY_ALIAS")
                keyPassword = System.getenv("SUPFY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        val supfySigning = signingConfigs.findByName("supfy")
        debug {
            if (supfySigning != null) signingConfig = supfySigning
        }
        release {
            if (supfySigning != null) signingConfig = supfySigning
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        htmlReport = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    testImplementation(libs.junit)
}
