plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.transport)
    runtimeOnly(libs.slf4j.nop)
}

compose.resources {
    packageOfResClass = "ai.hev.desktop.resources"
}

compose.desktop {
    application {
        mainClass = "hev"
    }
}
