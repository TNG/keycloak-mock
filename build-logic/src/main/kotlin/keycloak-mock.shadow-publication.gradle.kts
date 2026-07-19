import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.SigningExtension

plugins {
    `maven-publish`
    id("com.gradleup.shadow")
}

tasks.register<Jar>("fakeJar") {
    from(file("${project.projectDir}/src/main/resources/README.md"))
    archiveClassifier.set("fake")
}

extensions.configure<PublishingExtension>("publishing") {
    publications {
        register<MavenPublication>("shadow") {
            from(components.getByName("shadow"))
            artifact(tasks.named<Jar>("fakeJar")) { classifier = "javadoc" }
            artifact(tasks.named<Jar>("fakeJar")) { classifier = "sources" }
        }
    }
}

extensions.configure<SigningExtension>("signing") {
    val publishing = extensions.getByType<PublishingExtension>()
    sign(publishing.publications.named<MavenPublication>("shadow").get())
}
