plugins {
    kotlin("jvm")
   // id("org.jetbrains.kotlinx.dataframe")
    // only mandatory if `kotlin.dataframe.add.ksp=false` in gradle.properties
   // id("com.google.devtools.ksp")
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

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}