plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.coroutines)
    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain(21)
}
