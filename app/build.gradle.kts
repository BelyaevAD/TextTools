import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":platform"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(libs.coroutines)
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "dev.belyaevad.texttools.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "TextTools"
            packageVersion = "1.0.0"
            description = "TextTools 1.0.0-alpha"
            vendor = "BelyaevAD"
            windows {
                menuGroup = "TextTools"
                perUserInstall = true
            }
            linux {
                packageName = "texttools"
            }
        }
    }
}
