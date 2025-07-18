import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

android {
    val appId = "com.mymod.playspoofer"

    namespace = appId
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        create("config") {
            storeFile = file("key.jks")
            storePassword = "key123"
            keyAlias = "key0"
            keyPassword = "key123"
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
            signingConfig = signingConfigs.getByName("config")
        }

        debug {
            signingConfig = signingConfigs.getByName("config")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
