plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.byemaxx.soterdiag"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.byemaxx.soterdiag"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
