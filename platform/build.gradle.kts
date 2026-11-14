plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.jna)
    implementation(libs.jna.platform)
    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain(21)
}
