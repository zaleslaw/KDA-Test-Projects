plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("df", providers.gradleProperty("dataframeVersion").get())
            library("dataframe", "org.jetbrains.kotlinx", "dataframe").versionRef("df")
        }
    }
}

rootProject.name = "KDA-Test-Projects"
include("app1")
include("app2")
include("app3")