import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.chroma.studio.lite"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chroma.studio.lite"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.4.2-lite"
    }

    signingConfigs {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            val props = Properties().apply {
                keystorePropertiesFile.inputStream().use { load(it) }
            }
            val releaseStore = rootProject.file(props.getProperty("storeFile", "oscellion-release.jks"))
            if (releaseStore.exists()) {
                create("release") {
                    storeFile = releaseStore
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "kotlin/**",
                "DebugProbesKt.bin"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
