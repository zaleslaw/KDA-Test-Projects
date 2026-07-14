plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("df", providers.gradleProperty("dataframeVersion").get())
            library("dataframe", "org.jetbrains.kotlinx", "dataframe").versionRef("df")
            version("df-geo", providers.gradleProperty("dataframeGeoVersion").get())
            library("dataframe-geo", "org.jetbrains.kotlinx", "dataframe-geo").versionRef("df-geo")
            version("kandy", providers.gradleProperty("kandyVersion").get())
            library("kandy", "org.jetbrains.kotlinx", "kandy-lets-plot").versionRef("kandy")
            version("kandy-geo", providers.gradleProperty("kandyGeoVersion").get())
            library("kandy-geo", "org.jetbrains.kotlinx", "kandy-geo").versionRef("kandy-geo")
        }
    }
}

rootProject.name = "KDA-Test-Projects"
include("files")
include("compilerPlugin")
include("databases")
include("geo")
include("kandy")
include("swing")