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
    if (targetFormat != TargetFormat.Msi) return@configureEach
    // Compose 1.12 deletes compose/tmp/resources inside the task, after doFirst.
    // jpackage reads that directory as --resource-dir, so main.wxs has to be put back
    // before the tool starts. appResourcesDir is the app's own resources folder, not this one.
    val wxs = rootProject.layout.projectDirectory.file("packaging/windows/main.wxs")
    val resourcesDir = layout.buildDirectory.dir("compose/tmp/resources")
    val stop = object {
        @Volatile var value: Boolean = false
    }
    doFirst {
        val source = wxs.asFile
        val destDir = resourcesDir.get().asFile
        Thread {
            val deadline = System.currentTimeMillis() + 180_000
            while (!stop.value && System.currentTimeMillis() < deadline) {
                try {
                    if (destDir.isDirectory) {
                        val target = destDir.resolve("main.wxs")
                        if (!target.isFile) source.copyTo(target, overwrite = true)
                    }
                } catch (_: Exception) {
                }
                try {
                    Thread.sleep(1)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.apply {
            isDaemon = true
            name = "texttools-msi-wxs"
            start()
        }
    }
    doLast { stop.value = true }
}
