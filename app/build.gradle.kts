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
            packageVersion = "1.1.2"
            description = "TextTools 1.1.2"
            vendor = "BelyaevAD"
            modules("java.sql", "java.net.http", "java.management", "jdk.unsupported")
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

tasks.register("prepareLightShim") {
    val outDir = layout.buildDirectory.dir("wix-shim")
    val source = rootProject.layout.projectDirectory.file("packaging/windows/LightShim.cs")
    val script = rootProject.layout.projectDirectory.file("packaging/windows/light-multilang.ps1")
    inputs.file(source)
    inputs.file(script)
    outputs.dir(outDir)
    mustRunAfter("clean")
    doLast {
        val dir = outDir.get().asFile
        dir.mkdirs()
        val csc = File(System.getenv("WINDIR"), "Microsoft.NET/Framework64/v4.0.30319/csc.exe")
        exec {
            commandLine(csc.absolutePath, "/nologo", "/out:${dir.resolve("light.exe")}", source.asFile.absolutePath)
        }
        script.asFile.copyTo(dir.resolve("light-multilang.ps1"), overwrite = true)
    }
}

tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    if (targetFormat != TargetFormat.Msi) return@configureEach
    // light.exe in the WiX directory is replaced in doFirst. WiX 3 cannot link every
    // culture in one invocation (LGHT0100); the shim builds one MSI per culture.
    dependsOn("prepareLightShim")
    freeArgs.addAll(
        "--win-shortcut-prompt",
        "--license-file",
        rootProject.file("LICENSE").absolutePath,
        "--about-url",
        "https://github.com/BelyaevAD/TextTools",
        "--win-help-url",
        "https://github.com/BelyaevAD/TextTools",
        "--win-update-url",
        "https://github.com/BelyaevAD/TextTools/releases",
    )
    // Compose 1.12 deletes compose/tmp/resources inside the task, after doFirst.
    // jpackage reads that directory as --resource-dir, so main.wxs has to be put back
    // before the tool starts. appResourcesDir is the app's own resources folder, not this one.
    val windowsPackaging = rootProject.layout.projectDirectory.dir("packaging/windows")
    val resourcesDir = layout.buildDirectory.dir("compose/tmp/resources")
    val stop =
        object {
            @Volatile var value: Boolean = false
        }
    doFirst {
        val wixDir = wixToolsetDir.get().asFile
        val realLight = wixDir.resolve("light-wix.exe")
        val light = wixDir.resolve("light.exe")
        if (!realLight.isFile) light.copyTo(realLight)
        val config = wixDir.resolve("light.exe.config")
        val realConfig = wixDir.resolve("light-wix.exe.config")
        if (config.isFile) {
            if (!realConfig.isFile) config.copyTo(realConfig)
            config.delete()
        }
        val shimDir =
            layout.buildDirectory
                .dir("wix-shim")
                .get()
                .asFile
        shimDir
            .resolve("light.exe")
            .copyTo(light, overwrite = true)
        shimDir
            .resolve("light-multilang.ps1")
            .copyTo(wixDir.resolve("light-multilang.ps1"), overwrite = true)
        val sourceDir = windowsPackaging.asFile
        val destDir = resourcesDir.get().asFile

        fun patchRuntimeCfg() {
            val candidates = mutableListOf<File>()
            File(System.getProperty("java.io.tmpdir"))
                .listFiles()
                ?.forEach { dir ->
                    if (dir.name.startsWith("jdk.jpackage")) {
                        candidates.add(File(dir, "images/win-msi.image/TextTools/app/TextTools.cfg"))
                    }
                }
            candidates.add(
                layout.buildDirectory
                    .get()
                    .asFile
                    .resolve("compose/binaries/main/app/TextTools/app/TextTools.cfg"),
            )
            for (cfg in candidates) {
                if (!cfg.isFile) continue
                val text = cfg.readText()
                if (text.contains("app.runtime=")) continue
                val patched = text.replaceFirst("[Application]", "[Application]\r\napp.runtime=\$APPDIR\\..\\runtime")
                if (patched != text) cfg.writeText(patched)
            }
        }
        Thread {
            val deadline = System.currentTimeMillis() + 180_000
            try {
                Thread.sleep(500)
            } catch (_: InterruptedException) {
                return@Thread
            }
            while (!stop.value && System.currentTimeMillis() < deadline) {
                try {
                    patchRuntimeCfg()
                    if (destDir.isDirectory) {
                        sourceDir
                            .listFiles()
                            ?.filter { it.isFile && (it.name == "main.wxs" || it.extension.equals("wxl", true)) }
                            ?.forEach { file ->
                                val target = destDir.resolve(file.name)
                                if (!target.isFile) file.copyTo(target, overwrite = true)
                            }
                    }
                } catch (_: Exception) {
                }
                try {
                    Thread.sleep(20)
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
