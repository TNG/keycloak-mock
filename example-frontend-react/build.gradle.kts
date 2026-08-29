import org.gradle.internal.os.OperatingSystem

plugins {
    id("keycloak-mock.base")
    alias(libs.plugins.node)
}

val nodeDir = "bin/nodejs"
val pnpmDir = "bin/pnpm"
val nodeVersionString = libs.versions.node.get()
val pnpmVersionString = libs.versions.pnpm.get()

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

    // Set the work directory for pnpm
    pnpmWorkDir.set(file(pnpmDir))

    // Version of pnpm to download and use for the pnpm_* tasks.
    pnpmVersion.set(pnpmVersionString)

    // Set the work directory where node_modules should be located
    nodeProjectDir.set(file(projectDir))
}

tasks.named("pnpm_build") {
    dependsOn("pnpmInstall")
    inputs.files(
        fileTree("$projectDir/src"),
        "$projectDir/index.html",
        "$projectDir/vite.config.js",
        "$projectDir/package.json",
    )
    outputs.dir("$projectDir/dist")
}

tasks.register<Copy>("copyNode") {
    group = "build setup"
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

tasks.named("pnpmSetup") { dependsOn("copyNode") }

tasks.register("build") {
    group = "build"
    description = "Hook pnpm build into regular build task"
    dependsOn("pnpm_build")
}

tasks.named("pnpm_e2e") {
    mustRunAfter(":example-integration-docker:composeUp")
}
