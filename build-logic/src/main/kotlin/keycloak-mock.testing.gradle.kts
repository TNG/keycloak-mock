import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.sonarqube.gradle.SonarExtension

plugins {
    `jvm-test-suite`
    jacoco
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val jvmTestVersion = libs.findVersion("jvmTest").get().requiredVersion.toInt()
val junit5Version = libs.findVersion("junit5").get().requiredVersion

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
    listOf("junit-platform-launcher", "junit-jupiter-engine", "junit-vintage-engine").forEach { alias ->
        libs.findLibrary(alias).ifPresent { add("testRuntimeOnly", it) }
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

extensions.configure<SonarExtension>("sonar") {
    properties {
        property("sonar.coverage.jacoco.xmlReportPaths", "${project.projectDir}/build/reports/jacoco/test/jacocoTestReport.xml")
    }
}
