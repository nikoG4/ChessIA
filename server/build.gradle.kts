plugins {
    kotlin("jvm")
    application
    kotlin("plugin.serialization") version "1.9.0"
}

group = "org.nko.chessia"
version = "1.0.0"

application {
    mainClass.set("org.nko.chessia.server.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
}
