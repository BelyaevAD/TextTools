plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines)
}

kotlin {
    jvmToolchain(21)
}
