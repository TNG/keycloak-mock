import org.sonarqube.gradle.SonarExtension

plugins {
    idea
    alias(libs.plugins.axion.release)
    alias(libs.plugins.jib) apply false
    alias(libs.plugins.nmcp) apply false
    alias(libs.plugins.nmcp.aggregation)
    alias(libs.plugins.sonarqube)
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.ALL
}

scmVersion {
    tag {
        prefix = "v"
        versionSeparator = ""
    }
    branchVersionIncrementer = mapOf(
        "feature/.*" to "incrementMinor",
        "main" to "incrementMinor",
    )
}

group = "com.tngtech.keycloakmock"
version = scmVersion.version

repositories {
    mavenCentral()
    google()
}

configure<SonarExtension> {
    isSkipProject = System.getenv("SONAR_TOKEN")?.trim()?.isEmpty() ?: true
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "tng")
        property("sonar.projectKey", "TNG_keycloak-mock")
        property("sonar.cpd.exclusions", "mock-junit/**")
        property("sonar.gradle.skipCompile", "true")
    }
}

nmcpAggregation {
    centralPortal {
        username = findProperty("sonatypeUsername") as String?
        password = findProperty("sonatypePassword") as String?
        publishingType = "AUTOMATIC"
    }
}

dependencies {
    nmcpAggregation(project(":mock"))
    nmcpAggregation(project(":mock-junit"))
    nmcpAggregation(project(":mock-junit5"))
    nmcpAggregation(project(":standalone"))
}

// Run the build-logic tests as part of every subproject build, so the
// tamper-guard script (unit-tested in build-logic) is verified before any
// subproject is built. The root project applies no `base` plugin, so there is
// no aggregate `build` task to hook; attach to each subproject's `build` instead.
subprojects {
    tasks.matching { it.name == "build" }.configureEach {
        dependsOn(gradle.includedBuild("build-logic").task(":test"))
    }
}
