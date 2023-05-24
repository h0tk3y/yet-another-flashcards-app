import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.h0tk3y.flashcards"
version = "1.0-SNAPSHOT"


kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(compose.desktop.currentOs)
                implementation("com.github.tkuenneth:nativeparameterstoreaccess:0.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        buildTypes.release {
            proguard {
                configurationFiles.from(project.file("compose-desktop.pro"))
            }
        }

        mainClass = "MainKt"

        nativeDistributions {
            modules("java.sql")

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "com.h0tk3y.flashcards"
            packageVersion = "1.0.0"
            jvmArgs(
                "-Dapple.awt.application.appearance=system"
            )

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}
