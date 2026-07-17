plugins {
    alias(libs.plugins.spring.boot)
    // ensure Spring uses a compatible JUnit version in tests
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.jib)
}

extensions.configure<JavaPluginExtension>("java") {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTest.get().toInt()))
    }
}

dependencies {
    implementation(libs.keycloak.policy.enforcer)
    implementation(libs.spring.boot.configuration.processor)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.web)
    testImplementation(project(":mock-junit"))
    testImplementation(project(":mock-junit5"))
    testImplementation(libs.assertj.core)
    testImplementation(libs.bundles.vertx.test)
    testImplementation(libs.junit4)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)
}

jib {
    from {
        image = "eclipse-temurin:${libs.versions.temurinJre.get()}"
    }
    to {
        image = "keycloak-mock/example-backend"
    }
    container {
        ports = listOf("8080")
    }
}

tasks.register<Copy>("prepareFrontend") {
    dependsOn(":example-frontend-react:yarn_build")
    from("../example-frontend-react/build/")
    into("src/main/resources/static/")
}

tasks.named("processResources") {
    dependsOn("prepareFrontend")
}
