import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:${libs.versions.sonarqube.get()}")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:${libs.versions.shadow.get()}")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTest.get().toInt()))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTest.get()))
    }
}
