plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.chroma.studio"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chroma.studio"
        minSdk = 26 // Haze blur needs 26+ for best results (uses RenderEffect on 31+, fallback below)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.core:core-ktx:1.13.1")

    // Haze - glassmorphism / backdrop blur for Jetpack Compose
    // https://github.com/chrisbanes/haze
    implementation("dev.chrisbanes.haze:haze:0.7.3")
    implementation("dev.chrisbanes.haze:haze-materials:0.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
