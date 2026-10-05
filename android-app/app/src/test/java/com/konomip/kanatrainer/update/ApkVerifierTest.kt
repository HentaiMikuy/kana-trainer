package com.konomip.kanatrainer.update

import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkVerifierTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun `only an identical nonempty signer set can update an installed app`() {
        assertTrue(ApkVerifier.sameSigners(setOf("production"), setOf("production")))
        assertFalse(ApkVerifier.sameSigners(setOf("debug"), setOf("production")))
        assertFalse(ApkVerifier.sameSigners(emptySet(), emptySet()))
        assertFalse(ApkVerifier.sameSigners(setOf("production"), emptySet()))
        assertFalse(ApkVerifier.sameSigners(setOf("production"), setOf("production", "extra")))
    }

    @Test fun `verifies exact payload and rejects truncated or altered bytes`() = runTest {
        val file = temporary.newFile("app.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val hash = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        val release = sampleRelease().copy(sha256 = hash)
        ApkVerifier.verifyPayload(file, release)
        file.writeBytes(byteArrayOf(1, 2))
        try { ApkVerifier.verifyPayload(file, release); fail("Truncated APK accepted") } catch (_: IllegalStateException) { }
        file.writeBytes(byteArrayOf(1, 2, 4))
        try { ApkVerifier.verifyPayload(file, release); fail("Altered APK accepted") } catch (_: IllegalStateException) { }
        try { ApkVerifier.verifyPayload(File(temporary.root, "missing.apk"), release); fail("Missing APK accepted") }
        catch (_: IllegalStateException) { }
    }
}
