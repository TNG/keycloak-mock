import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ApacheNoticeResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.PreserveFirstFoundResourceTransformer

plugins {
    application
    id("keycloak-mock.base")
    id("keycloak-mock.testing")
    id("keycloak-mock.publishing")
    id("keycloak-mock.shadow-publication")
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.jib)
}

description = "Standalone keycloak-mock server for use in frontend development"

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set(null as String?)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    failOnDuplicateEntries.set(true)
    exclude("META-INF/LICENSE.*", "META-INF/LICENSE")
    transform(ApacheNoticeResourceTransformer::class.java) {
        addHeader.set(true)
        charsetName.set("UTF-8")
        inceptionYear.set("2019")
        organizationName.set("TNG Technology Consulting GmbH")
        organizationURL.set("https://www.tngtech.com")
        projectName.set("Keycloak Mock Standalone")
    }
    transform(PreserveFirstFoundResourceTransformer::class.java) {
        include("META-INF/io.netty.versions.properties")
    }
    metaInf {
        from("$rootDir/NOTICE", "$rootDir/LICENSE")
    }
}

// dependencies as suggested by gradle
tasks.named("startScripts") { dependsOn("shadowJar") }
tasks.named("distZip") { dependsOn("shadowJar") }
tasks.named("distTar") { dependsOn("shadowJar") }
tasks.named("startShadowScripts") { dependsOn("jar") }
afterEvaluate {
    tasks.named("generateMetadataFileForShadowPublication").configure { dependsOn("jar") }
}

configurations {
    all { exclude(group = "javax.servlet") }
}

dependencies {
    implementation(project(":mock"))
    implementation(libs.jsr305)
    implementation(libs.picocli)
    implementation(libs.slf4j.simple)
}

sonar {
    properties {
        property("sonar.coverage.exclusions", "src/main/java/com/tngtech/keycloakmock/standalone/Main.java")
    }
}

application {
    mainClass.set("com.tngtech.keycloakmock.standalone.Main")
}

buildConfig {
    buildConfigField("String", "NAME", "\"${project.name}\"")
    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

jib {
    from {
        image = "eclipse-temurin:${libs.versions.temurinJre.get()}"
    }
    to {
        image = "ghcr.io/tng/keycloak-mock:${project.version}"
        tags = setOf("latest")
    }
    container {
        ports = listOf("8000")
        mainClass = "com.tngtech.keycloakmock.standalone.Main"
        labels.set(
            mapOf(
                "org.opencontainers.image.source" to "https://github.com/TNG/keycloak-mock",
                "org.opencontainers.image.description" to "Docker image for keycloak mock",
                "org.opencontainers.image.licenses" to "Apache-2.0",
            ),
        )
    }
}
