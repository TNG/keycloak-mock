import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.SigningExtension

plugins {
    `maven-publish`
}

val javaExtension = extensions.getByType<JavaPluginExtension>()

tasks.register<Jar>("javadocJar") {
    from(project.tasks.named("javadoc"))
    archiveClassifier.set("javadoc")
    metaInf {
        from("$rootDir/NOTICE", "$rootDir/LICENSE")
    }
}

tasks.register<Jar>("sourceJar") {
    from(javaExtension.sourceSets.getByName("main").allSource)
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

extensions.configure<PublishingExtension>("publishing") {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components.getByName("java"))
            artifact(tasks.named("javadocJar"))
            artifact(tasks.named("sourceJar"))
        }
    }
}

extensions.configure<SigningExtension>("signing") {
    val publishing = extensions.getByType<PublishingExtension>()
    sign(publishing.publications.named<MavenPublication>("mavenJava").get())
}
