plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint) apply false
}

subprojects {
    repositories {
        mavenCentral()
        google()
    }

    // ktlint-gradle 12.1.2 lints SQLDelight output under data/build even with a src-only filter.
    if (name != "data") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
    }
    apply(plugin = "dev.detekt")

    if (name != "data") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set("1.5.0")
            filter {
                include("**/src/**")
            }
        }
    }

    extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
        source.setFrom(
            fileTree(projectDir) {
                include("src/main/kotlin/**/*.kt")
            },
        )
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
