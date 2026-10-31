import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    kotlin("plugin.serialization")
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
        multiDexEnabled = true
        
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }
        
        val supabaseUrl = properties.getProperty("SUPABASE_URL", "")
        val supabaseKey = properties.getProperty("SUPABASE_ANON_KEY", "")
        
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")
        
        val cloudinaryCloudName = properties.getProperty("CLOUDINARY_CLOUD_NAME", "")
        val cloudinaryUploadPreset = properties.getProperty("CLOUDINARY_UPLOAD_PRESET", "oscellion_posts")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"$cloudinaryCloudName\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"$cloudinaryUploadPreset\"")
        
        val cloudinaryVideoPreset = properties.getProperty("CLOUDINARY_VIDEO_UPLOAD_PRESET", "flash_videos")
        buildConfigField("String", "CLOUDINARY_VIDEO_UPLOAD_PRESET", "\"$cloudinaryVideoPreset\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    androidResources {
        noCompress += "tflite"
    }

    // composeOptions removed for Kotlin 2.0

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    configurations.all {
        resolutionStrategy {
            force("io.ktor:ktor-client-core:2.3.12")
            force("io.ktor:ktor-client-okhttp:2.3.12")
            force("io.ktor:ktor-client-content-negotiation:2.3.12")
            force("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            force("io.ktor:ktor-client-websockets:2.3.12")
            force("io.ktor:ktor-serialization-kotlinx:2.3.12")
            force("io.ktor:ktor-serialization:2.3.12")
            force("io.ktor:ktor-events:2.3.12")
            force("io.ktor:ktor-websocket-serialization:2.3.12")
            force("io.ktor:ktor-client-logging:2.3.12")
        }
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
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core-ktx:1.13.1")
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    // Coil video thumbnail support
    implementation("io.coil-kt:coil-video:2.6.0")

    // Jetpack Media3 — ExoPlayer for Flash Feed video playback
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.3.1")
    implementation("androidx.media3:media3-database:1.3.1")

    // Haze - glassmorphism / backdrop blur for Jetpack Compose
    // https://github.com/chrisbanes/haze
    implementation("dev.chrisbanes.haze:haze:0.7.3")
    implementation("dev.chrisbanes.haze:haze-materials:0.7.3")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.animation:animation-graphics")

    implementation("androidx.palette:palette-ktx:1.0.0")
    
    implementation("com.composables:icons-lucide:1.1.0")

    // Gson for work serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Firebase & Google Auth
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.github.webrtc-sdk:android:104.5112.09")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    
    // Firebase AI Logic (Gemini Developer API compatible) & App Check
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")

    // Network for Gemini API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Supabase & Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation(platform("io.github.jan-tennert.supabase:bom:2.2.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.ktor:ktor-client-okhttp:2.3.12") // Ktor client for Supabase
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Image Cropper
    implementation("com.vanniktech:android-image-cropper:4.6.0")

    // QR Code Generation
    implementation("com.google.zxing:core:3.5.3")

    // FFmpeg for Pro Video Editor Rendering
    implementation("com.github.microshow:RxFFmpeg:4.9.0")

    // TensorFlow Lite for Ultra-Fast On-Device Safety ML Inference
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // CameraX for Apple iOS Style In-App Camera
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
