plugins {
    application
    kotlin("jvm") version "1.5.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("MultiClientKt")
}
val ktor_version = "1.6.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
}
