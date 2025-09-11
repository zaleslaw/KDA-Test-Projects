plugins {
    kotlin("jvm")
    kotlin("plugin.dataframe") version "2.2.0"
}

group = "com.zaleslaw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dataframe)
    implementation(libs.kandy)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}