plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("TextToolsDatabase") {
            packageName.set("dev.belyaevad.texttools.data.db")
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.coroutines)
    implementation(libs.sqldelight.driver)
    implementation(libs.ktor.client.core)
    implementation(libs.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.ktor.client.mock)
}

kotlin {
    jvmToolchain(21)
}
