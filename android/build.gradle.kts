plugins {
    id("org.jetbrains.compose")
    id("com.android.application")
    kotlin("android")
}

group = "com.h0tk3y.flashcards"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation("androidx.activity:activity-compose:1.7.1")
    implementation("com.google.accompanist:accompanist-webview:0.31.2-alpha")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.27.0")
    implementation("com.google.accompanist:accompanist-insets:0.27.0")
}

android {
    namespace = "com.h0tk3y.flashcards"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.h0tk3y.flashcards.android"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = version.toString()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        compose = true
    }
    kotlin {
        jvmToolchain(11)
    }
}