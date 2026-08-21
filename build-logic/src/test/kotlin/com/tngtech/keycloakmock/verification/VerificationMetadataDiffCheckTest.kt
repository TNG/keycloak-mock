package com.tngtech.keycloakmock.verification

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

/**
 * Verifies that [scripts/check-verification-metadata-additive.sh] correctly
 * classifies diffs to the Gradle dependency verification metadata as purely
 * additive (acceptable) or as a tamper signal (rejected).
 *
 * The script is the production guard invoked by the
 * "Update verification metadata" workflow; these tests pin its behavior so the
 * build fails before a change to the guard logic can silently weaken it.
 */
class VerificationMetadataDiffCheckTest {

    private val projectDir = File(".").canonicalFile
    private val script = File(projectDir, "../scripts/check-verification-metadata-additive.sh").canonicalFile
    private val fixtures = File(projectDir, "src/test/resources/verification-diffs")

    init {
        assertTrue(script.isFile, "guard script not found at $script")
    }

    @ParameterizedTest
    @CsvSource(
        "additive-version-bump.txt, 0",
        "empty.txt, 0",
        "tamper-changed-sha256.txt, 1",
        "removed-component.txt, 1",
    )
    fun `script classifies diff as expected`(fixtureName: String, expectedExit: Int) {
        val fixture = File(fixtures, fixtureName)
        assertTrue(fixture.isFile, "fixture not found: $fixture")

        val process = ProcessBuilder("bash", script.absolutePath, fixture.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()

        assertEquals(expectedExit, exit, "unexpected exit code for $fixtureName; output was:\n$output")
    }

    @Test
    fun `tamper fixture output mentions the offending line`() {
        val fixture = File(fixtures, "tamper-changed-sha256.txt")
        val process = ProcessBuilder("bash", script.absolutePath, fixture.absolutePath)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        assertTrue(output.contains("<sha256"), "expected output to reference the offending sha256 line; was:\n$output")
    }
}
