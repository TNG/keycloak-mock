import org.gradle.internal.os.OperatingSystem

plugins {
    id("keycloak-mock.base")
    alias(libs.plugins.node)
}

val nodeDir = "bin/nodejs"
val yarnDir = "bin/yarn"
val nodeVersionString = libs.versions.node.get()

node {
    // Version of node to use.
    version.set(nodeVersionString)

    // Base URL for fetching node distributions (change if you have a mirror).
    distBaseUrl.set("https://nodejs.org/dist")

    // If true, it will download node using above parameters.
    // If false, it will try to use globally installed node.
    download.set(true)

    // Set the work directory for unpacking node
    workDir.set(file(nodeDir))

    // Set the work directory for Yarn
    yarnWorkDir.set(file(yarnDir))

    // Set the work directory where node_modules should be located
    nodeProjectDir.set(file(projectDir))
}

tasks.named("yarn_build") {
    dependsOn("yarn_install")
    val files = projectDir.walkTopDown().filter {
        it.isFile && (it.extension == "js" || it.extension == "json")
    }.toList()
    inputs.files(
        fileTree("$projectDir/src"),
        fileTree("$projectDir/public"),
        fileTree("$projectDir/tests"),
        files,
    )
    outputs.dir("$projectDir/dist")
}

tasks.register<Copy>("copyNode") {
    description = "Move node version into unversioned directory to be used by wrapper script"
    dependsOn("nodeSetup")
    val os = OperatingSystem.current()
    val platform = when {
        os.isLinux -> "linux-x64"
        os.isMacOsX -> "darwin-x64"
        os.isWindows -> "win-x64"
        else -> error("Unsupported operating system: $os")
    }
    from("${projectDir}/${nodeDir}/node-v${nodeVersionString}-${platform}/")
    into("${projectDir}/${nodeDir}/node/")
}

tasks.named("yarnSetup") { dependsOn("copyNode") }

tasks.register("build") {
    description = "Hook yarn build into regular build task"
    dependsOn("yarn_build")
}

tasks.named("yarn_e2e") {
    mustRunAfter(":example-integration-docker:composeUp")
}
