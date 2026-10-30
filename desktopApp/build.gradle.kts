import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.coroutinesCore)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.bundled)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
}

compose.desktop {
    application {
        mainClass = "org.mexa.clube.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "BilliardClubManager"
            packageVersion = "2.6.0"
            description = "Billiard Club Manager - bilyard xonasi boshqaruv dasturi"
            copyright = "Copyright (c) 2026 MEXA"
            vendor = "MEXA"

            // Windows (MSI/EXE installer) sozlamalari
            windows {
                console = false
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
                menuGroup = "Billiard Club Manager"
                // MSI yangilanishlar uchun O'ZGARTIRILMASIN (bo'lmasa yangisi alohida dastur bo'lib o'rnatiladi)
                upgradeUuid = "8f3a2b1e-7c4d-4a5e-9f01-2b6c8d4e5a01"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }

            linux {
                shortcut = true
                menuGroup = "Office"
            }

            macOS {
                bundleID = "org.mexa.clube"
            }
        }
    }
}
