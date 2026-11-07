plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.coroutines)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines)
}

kotlin {
    jvmToolchain(21)
}
