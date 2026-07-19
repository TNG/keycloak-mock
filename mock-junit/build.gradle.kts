plugins {
    id("keycloak-mock.java-library")
    id("keycloak-mock.publishing")
    id("keycloak-mock.maven-java-publication")
}

description = "JUnit4 helper for keycloak-mock"

dependencies {
    api(project(":mock"))
    implementation(libs.jsr305)
    implementation(libs.junit4)
    testImplementation(libs.assertj.core)
    testImplementation(libs.bundles.vertx.test)
}
