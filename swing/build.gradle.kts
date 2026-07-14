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
    implementation(kotlin("scripting-jsr223"))
    implementation("com.formdev:flatlaf:3.4.1")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
