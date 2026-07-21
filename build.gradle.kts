plugins {
    kotlin("jvm") version "2.4.20-Beta1"
    kotlin("plugin.dataframe") version "2.4.20-Beta1" apply false
}

group = "com.zaleslaw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}