plugins {
    id("keycloak-mock.base")
    alias(libs.plugins.docker.compose)
}

tasks.named("composeUp") {
    dependsOn(":standalone:jibDockerBuild")
    dependsOn(":example-backend:jibDockerBuild")
}

dockerCompose {
    getOrCreateNested("realKeycloak").apply {
        useComposeFiles.set(listOf("docker-compose-real-keycloak.yml"))
    }
}

afterEvaluate {
    tasks.named("realKeycloakComposeUp").configure {
        dependsOn(":example-backend:jibDockerBuild")
    }
}

tasks.register("e2e") {
    description = "Run end-to-end test using docker containers"
    dependsOn("composeUp")
    dependsOn(":example-frontend-react:yarn_e2e")
    finalizedBy("composeDown")
}
