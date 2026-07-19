pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "keycloak-mock"

includeBuild("build-logic")

include("mock")
include("mock-junit")
include("mock-junit5")
include("standalone")
include("example-backend")
include("example-frontend-react")
include("example-integration-docker")
