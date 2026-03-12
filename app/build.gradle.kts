import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val localSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("keystore.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun signingValue(name: String): String? {
    return providers.environmentVariable(name).orNull
        ?: providers.gradleProperty(name).orNull
        ?: localSigningProperties.getProperty(name)
}

val signingKeyStoreFile = file("key.jks")
val signingKeyAlias = signingValue("PLAYSPOOFER_KEY_ALIAS")
val signingStorePassword = signingValue("PLAYSPOOFER_STORE_PASSWORD")
val signingKeyPassword = signingValue("PLAYSPOOFER_KEY_PASSWORD")
val hasManagedSigning = signingKeyStoreFile.exists() &&
    !signingKeyAlias.isNullOrBlank() &&
    !signingStorePassword.isNullOrBlank() &&
    !signingKeyPassword.isNullOrBlank()

android {
    val appId = "com.mymod.playspoofer"

    namespace = appId
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        if (hasManagedSigning) {
            create("config") {
                storeFile = signingKeyStoreFile
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = appId
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionName = libs.versions.app.versionName.get()
        versionCode = libs.versions.app.versionCode.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasManagedSigning) {
                signingConfig = signingConfigs.getByName("config")
            }
        }

        debug {
            if (hasManagedSigning) {
                signingConfig = signingConfigs.getByName("config")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.activity.compose)

    val compose = platform(libs.compose)
    implementation(compose)

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    compileOnly(libs.xposed.api)
}
