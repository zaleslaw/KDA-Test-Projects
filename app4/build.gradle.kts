plugins {
    kotlin("jvm")
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