description = "Base module of keycloak-mock"

val jsResourceJar = configurations.register("jsResourceJar") {
    isTransitive = false
}
val htmlResourceJar = configurations.register("htmlResourceJar") {
    isTransitive = false
}

dependencies {
    implementation(libs.dagger)
    implementation(libs.jjwt.impl)
    implementation(libs.jjwt.jackson)
    implementation(libs.jsr305)
    implementation(libs.slf4j.api)
    implementation(libs.vertx.web)
    implementation(libs.vertx.web.templ.freemarker)
    add("jsResourceJar", "org.keycloak:keycloak-js-adapter:${libs.versions.keycloakJs.get()}@tar.gz")
    add("htmlResourceJar", libs.keycloak.services)
    add("htmlResourceJar", libs.keycloak.themes.vendor)
    testImplementation(libs.assertj.core)
    testImplementation(libs.bundles.vertx.test)
    testImplementation(libs.fusionauth.jwt)
    testImplementation(libs.json.unit.assertj)
    testImplementation(libs.jsoup)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.vertx.codegen)
    testRuntimeOnly(libs.slf4j.simple)
    annotationProcessor(libs.dagger.compiler)
}

tasks.register<Copy>("addResources") {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    from(tarTree(jsResourceJar.get().singleFile)) {
        include("/package/dist/keycloak.js")
        include("/package/LICENSE.txt")
    }
    from(htmlResourceJar.get().map { zipTree(it) }) {
        include("/org/keycloak/protocol/oidc/endpoints/3p-cookies-step1.html")
        include("/org/keycloak/protocol/oidc/endpoints/3p-cookies-step2.html")
        include("/org/keycloak/protocol/oidc/endpoints/login-status-iframe.ftl")
        include("/META-INF/NOTICE")
        include("/theme/keycloak/common/resources/vendor/web-crypto-shim/web-crypto-shim.js")
    }
    into("build/resources/main")
}

tasks.named("processResources") {
    dependsOn("addResources")
}
