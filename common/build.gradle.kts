plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    id("app.cash.sqldelight")
}

group = "com.h0tk3y.flashcards"
version = "1.0-SNAPSHOT"

kotlin {
    android {
        jvmToolchain(11)
    }
    jvm("desktop") {
        jvmToolchain(11)
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.ui)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.0-alpha05")
                api("moe.tlaster:precompose:1.4.1")
                api("moe.tlaster:precompose-viewmodel:1.4.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
                implementation("app.cash.sqldelight:android-driver:2.0.0-alpha05")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.0-alpha05")
                api(compose.preview)
            }
        }
        val desktopTest by getting
    }
}

sqldelight {
    databases.create("AppDatabase") {
        packageName.set("com.h0tk3y.flashcards.db")
    }
}


android {
    namespace = "com.h0tk3y.flashcrads"
    compileSdk = 33
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}