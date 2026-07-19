import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

plugins {
    `maven-publish`
    signing
}

apply(plugin = "com.gradleup.nmcp")

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val jvmProdVersion = libs.findVersion("jvmProd").get().requiredVersion.toInt()
val isReleaseVersion = !project.version.toString().endsWith("-SNAPSHOT")

val licenseName = "The Apache License, Version 2.0"
val licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
val companyName = "TNG Technology Consulting GmbH"
val companyUrl = "https://www.tngtech.com"

extensions.configure<JavaPluginExtension>("java") {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmProdVersion))
    }
}

extensions.configure<SigningExtension>("signing") {
    val signingKey = findProperty("signingKey") as String?
    val signingPassword = findProperty("signingPassword") as String?
    useInMemoryPgpKeys(signingKey, signingPassword)
}

tasks.withType<Sign>().configureEach {
    onlyIf { isReleaseVersion }
}

afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        publications.withType<MavenPublication>().configureEach {
            pom {
                licenses {
                    license {
                        name = licenseName
                        url = licenseUrl
                    }
                }
                name = "${project.group}:${project.name}"
                url = "https://github.com/TNG/keycloak-mock"
                description = project.description

                organization {
                    name = companyName
                    url = companyUrl
                }

                scm {
                    url = "https://github.com/TNG/keycloak-mock"
                    connection = "scm:git:git://github.com/TNG/keycloak-mock"
                    developerConnection = "scm:git:ssh://github.com/TNG/keycloak-mock"
                }

                developers {
                    developer {
                        id = "ostrya"
                        name = "Kai Helbig"
                        email = "kai.helbig@tngtech.com"
                    }
                    developer {
                        id = "ripssi"
                        name = "Simon Rips"
                        email = "simon.rips@tngtech.com"
                    }
                    developer {
                        id = "christian-ertl"
                        name = "Christian Ertl"
                        email = "christian.ertl@tngtech.com"
                    }
                }
            }
        }
    }
}
