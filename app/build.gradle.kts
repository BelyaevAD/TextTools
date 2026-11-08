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
    implementation("org.jetbrains.compose.material3:material3:1.12.0-alpha03")
    implementation(compose.components.resources)
    implementation(libs.coroutines)
    implementation(libs.sqldelight.driver)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.sentry)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines)
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
            description = "TextTools 1.0"
            vendor = "BelyaevAD"
            windows {
                iconFile.set(project.file("packaging/icon.ico"))
                menuGroup = "TextTools"
                perUserInstall = true
                shortcut = true
                menu = true
            }
            linux {
                iconFile.set(project.file("packaging/icon.png"))
                packageName = "texttools"
            }
        }
    }
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    if (targetFormat == TargetFormat.Msi) {
        appResourcesDir.set(project.layout.projectDirectory.dir("packaging/windows"))
    }
}
