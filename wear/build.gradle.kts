plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.meko123456.pomidori.wear"
    compileSdk = 37

    defaultConfig {
        // The phone app's id. To Play, and to the Wearable data layer should the two ever talk,
        // the watch app is the same app on another device rather than a second one.
        applicationId = "io.github.meko123456.pomidori"
        // Wear OS 3, the oldest release the tile and ongoing-activity libraries support.
        minSdk = 30
        // Wear OS 6. There is no Wear OS release on API 37 to target yet.
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":timer"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.ongoing)
}
