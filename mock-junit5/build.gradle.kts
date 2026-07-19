plugins {
    id("keycloak-mock.java-library")
    id("keycloak-mock.publishing")
    id("keycloak-mock.maven-java-publication")
}

description = "JUnit5 helper for keycloak-mock"

dependencies {
    api(project(":mock"))
    implementation(platform(libs.junit.bom))
    implementation(libs.jsr305)
    implementation(libs.junit.jupiter.api)
    testImplementation(libs.assertj.core)
    testImplementation(libs.bundles.vertx.test)
}
