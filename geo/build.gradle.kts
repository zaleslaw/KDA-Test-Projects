plugins {
    kotlin("jvm")
}

group = "com.zaleslaw"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://repo.osgeo.org/repository/release/")
    mavenCentral()
}

dependencies {
    implementation(libs.dataframe)
    implementation(libs.kandy)
    implementation(libs.kandy.geo)
    implementation(libs.dataframe.geo)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}