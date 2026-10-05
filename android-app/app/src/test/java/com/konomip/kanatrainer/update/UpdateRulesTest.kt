package com.konomip.kanatrainer.update

import java.net.URI
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class UpdateRulesTest {
    private val repository = "HentaiMikuy/kana-trainer"
    private val applicationId = "com.konomip.kanatrainer"
    private val release = sampleRelease()

    private fun parse(value: UpdateManifest): UpdateManifest =
        UpdateRules.parse(UpdateRules.json.encodeToString(value), repository, applicationId)

    @Test fun `version comparison uses code and does not offer downgrades`() {
        assertEquals(UpdateAvailability.AVAILABLE, UpdateRules.availability(release.copy(versionName = "0.1"), 3, 36))
        assertEquals(UpdateAvailability.CURRENT, UpdateRules.availability(release, 10001, 36))
        assertEquals(UpdateAvailability.CURRENT, UpdateRules.availability(release, 10002, 36))
        assertEquals(UpdateAvailability.UNSUPPORTED, UpdateRules.availability(release.copy(minSdk = 37), 3, 36))
        assertEquals(UpdateAvailability.CURRENT, UpdateRules.availability(release.copy(minSdk = 37), 10002, 36))
    }

    @Test fun `manifest round trip permits forward compatible unknown fields`() {
        val text = UpdateRules.json.encodeToString(release).dropLast(1) + ",\"future\":true}"
        assertEquals(release, UpdateRules.parse(text, repository, applicationId))
    }

    @Test fun `rejects foreign insecure mutable and malformed download addresses`() {
        listOf(
            "http://github.com/$repository/releases/download/android-10001/app.apk",
            "https://github.com/other/repo/releases/download/android-10001/app.apk",
            "https://github.com/$repository/releases/latest/download/app.apk",
            "https://github.com.evil.test/$repository/releases/download/android-10001/app.apk",
            "https://user@github.com/$repository/releases/download/android-10001/app.apk",
            "https://github.com:8443/$repository/releases/download/android-10001/app.apk",
            "https://github.com/$repository/releases/download/../app.apk",
            "https://github.com/$repository/releases/download/x/app.apk?token=x",
            "https://github.com/$repository/releases/download/x/%2e%2e.apk",
        ).forEach { url -> assertThrows(Exception::class.java) { parse(release.copy(apkUrl = url)) } }
    }

    @Test fun `rejects invalid schema identity sizes hashes and incomplete manifests`() {
        listOf(release.copy(schemaVersion = 2), release.copy(applicationId = "other.app"),
            release.copy(sizeBytes = 0), release.copy(sizeBytes = UpdateRules.MAX_APK_BYTES + 1),
            release.copy(sha256 = "bad"), release.copy(versionCode = -1),
            release.copy(versionCode = 2_100_000_001), release.copy(minSdk = 0),
            release.copy(notes = "x".repeat(32_001))).forEach {
            assertThrows(Exception::class.java) { parse(it) }
        }
        assertThrows(Exception::class.java) { UpdateRules.parse("{}", repository, applicationId) }
        assertThrows(Exception::class.java) {
            UpdateRules.parse(" ".repeat(UpdateRules.MAX_METADATA_BYTES + 1), repository, applicationId)
        }
    }

    @Test fun `redirects permit github assets but reject downgrade and lookalike hosts`() {
        assertTrue(UpdateRules.allowedTransportUrl(URI("https://release-assets.githubusercontent.com/path?signature=value")))
        assertTrue(UpdateRules.allowedTransportUrl(URI("https://objects.githubusercontent.com/path")))
        assertFalse(UpdateRules.allowedTransportUrl(URI("http://release-assets.githubusercontent.com/path")))
        assertFalse(UpdateRules.allowedTransportUrl(URI("https://github.com.evil.test/path")))
        assertFalse(UpdateRules.allowedTransportUrl(URI("https://user@github.com/path")))
    }
}

internal fun sampleRelease() = UpdateManifest(1, "com.konomip.kanatrainer", 10001, "1.2.0", 26,
    "https://github.com/HentaiMikuy/kana-trainer/releases/download/android-10001/app.apk",
    3, "0".repeat(64), "修复与改进", "2026-10-06T00:00:00Z")
