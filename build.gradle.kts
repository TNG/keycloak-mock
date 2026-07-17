import org.sonarqube.gradle.SonarExtension

plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.dependencyVersions)
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

val computedVersion: String = scmVersion.version

val junit5Version = libs.versions.junit5.get()
val junitRuntimeDeps = libs.bundles.junit.runtime.get()
val jvmProdVersion = libs.versions.jvmProd.get().toInt()
val jvmTestVersion = libs.versions.jvmTest.get().toInt()

val releaseProjects = listOf("mock", "mock-junit", "mock-junit5", "standalone")

val licenseName = "The Apache License, Version 2.0"
val licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0.txt"
val companyName = "TNG Technology Consulting GmbH"
val companyUrl = "https://www.tngtech.com"

allprojects {
    apply(plugin = "idea")

    repositories {
        mavenCentral()
        google()
    }

    group = "com.tngtech.keycloakmock"
    version = computedVersion
}

subprojects {
    val isReleaseVersion = !project.version.toString().endsWith("-SNAPSHOT")

    if (releaseProjects.contains(project.name) || project.name == "example-backend") {
        if (project.name == "standalone") {
            apply(plugin = "java")
        } else {
            apply(plugin = "java-library")
        }
        apply(plugin = "jvm-test-suite")

        val toolchainService = extensions.getByType<JavaToolchainService>()

        configurations {
            named("testCompileClasspath") {
                attributes {
                    attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, jvmTestVersion)
                }
            }
            named("testRuntimeClasspath") {
                attributes {
                    attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, jvmTestVersion)
                }
            }
        }

        tasks.named<Test>("test") {
            useJUnitPlatform()
            javaLauncher = toolchainService.launcherFor {
                languageVersion = JavaLanguageVersion.of(jvmTestVersion)
            }
            testLogging {
                events("passed", "skipped", "failed")
            }
            finalizedBy("jacocoTestReport")
        }

        tasks.named<JavaCompile>("compileTestJava") {
            javaCompiler = toolchainService.compilerFor {
                languageVersion.set(JavaLanguageVersion.of(jvmTestVersion))
            }
        }

        dependencies {
            add("testImplementation", platform("org.junit:junit-bom:$junit5Version"))
            junitRuntimeDeps.forEach { add("testRuntimeOnly", it) }
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)
                csv.required.set(false)
                html.required.set(false)
            }
        }

        configure<SonarExtension> {
            properties {
                property("sonar.coverage.jacoco.xmlReportPaths", "${project.projectDir}/build/reports/jacoco/test/jacocoTestReport.xml")
            }
        }
    }

    if (releaseProjects.contains(project.name)) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")
        apply(plugin = "com.gradleup.nmcp")
        apply(plugin = "jacoco")

        extensions.configure<JavaPluginExtension>("java") {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(jvmProdVersion))
            }
        }

        if (project.name != "standalone") {
            tasks.register<Jar>("javadocJar") {
                from(project.tasks.named("javadoc"))
                archiveClassifier.set("javadoc")
                metaInf {
                    from("$rootDir/NOTICE", "$rootDir/LICENSE")
                }
            }
            tasks.register<Jar>("sourceJar") {
                from(project.extensions.getByType<JavaPluginExtension>().sourceSets.getByName("main").allSource)
                archiveClassifier.set("sources")
                metaInf {
                    from("$rootDir/NOTICE", "$rootDir/LICENSE")
                }
            }
            tasks.named<Jar>("jar") {
                metaInf {
                    from("$rootDir/NOTICE", "$rootDir/LICENSE")
                }
            }
        }

        extensions.configure<PublishingExtension>("publishing") {
            if (project.name != "standalone") {
                publications {
                    register<MavenPublication>("mavenJava") {
                        from(components.getByName("java"))
                        artifact(tasks.named("javadocJar"))
                        artifact(tasks.named("sourceJar"))
                    }
                }
            }
        }

        extensions.configure<SigningExtension>("signing") {
            if (project.name != "standalone") {
                val signingKey = findProperty("signingKey") as String?
                val signingPassword = findProperty("signingPassword") as String?
                useInMemoryPgpKeys(signingKey, signingPassword)
                val publishing = extensions.getByType<PublishingExtension>()
                sign(publishing.publications.named<MavenPublication>("mavenJava").get())
            }
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
    }
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
    releaseProjects.forEach { p ->
        nmcpAggregation(project(":$p"))
    }
}
