import java.util.Base64

plugins {
    id("com.android.application")
}

android {
    namespace = "com.veytrix.autopilot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.veytrix.autopilot"
        minSdk = 26
        targetSdk = 35
        versionCode = 10000
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val keystoreBase64 = System.getenv("VEYTRIX_KEYSTORE_BASE64")
            val storePassword = System.getenv("VEYTRIX_KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("VEYTRIX_KEY_ALIAS")
            val keyPassword = System.getenv("VEYTRIX_KEY_PASSWORD")
            if (!keystoreBase64.isNullOrBlank() && !storePassword.isNullOrBlank() &&
                !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()) {
                val keystoreFile = layout.buildDirectory.file("signing/veytrix-release.keystore").get().asFile
                if (!keystoreFile.exists()) {
                    keystoreFile.parentFile.mkdirs()
                    keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
                }
                storeFile = keystoreFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Keep the production application ID so the first phone-test APK exercises
            // the same update identity. Release signing remains separate and secure.
        }
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
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
