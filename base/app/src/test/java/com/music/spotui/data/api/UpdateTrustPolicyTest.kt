package com.music.spotui.data.api

import org.junit.Assert.*
import org.junit.Test

class UpdateTrustPolicyTest {
    @Test fun onlySupfyRepositoryAndAssetsAreTrusted() {
        assertTrue(UpdateTrustPolicy.releaseUrl("https://github.com/ebilgi992/supfy/releases/tag/v5.0.1"))
        assertFalse(UpdateTrustPolicy.releaseUrl("https://github.com/kota-zo/myspoti/releases/tag/v5.0.1"))
        assertTrue(UpdateTrustPolicy.assetUrl("https://github.com/ebilgi992/supfy/releases/download/v5.0.1/supfy-stable-5.0.1.apk"))
        assertFalse(UpdateTrustPolicy.assetUrl("http://github.com/ebilgi992/supfy/releases/download/v5.0.1/supfy.apk"))
        assertFalse(UpdateTrustPolicy.assetUrl("https://github.com.evil.test/ebilgi992/supfy/releases/download/v5/supfy.apk"))
        assertFalse(UpdateTrustPolicy.assetUrl("https://user@github.com/ebilgi992/supfy/releases/download/v5/supfy.apk"))
    }
    @Test fun checksumRejectsTamperingAndMissingDigest() {
        fun hash(bytes: String) = java.security.MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).joinToString("") { "%02x".format(it) }
        val published = hash("signed APK")
        assertTrue(UpdateTrustPolicy.checksum(hash("signed APK"), published.uppercase()))
        assertFalse(UpdateTrustPolicy.checksum(hash("modified APK"), published))
        assertFalse(UpdateTrustPolicy.checksum(hash("signed APK"), ""))
        assertFalse(UpdateTrustPolicy.checksum("abc", "abc"))
    }

    @Test fun identityRequiresPackageNewVersionAndSameSigners() {
        assertTrue(UpdateTrustPolicy.identity("com.supfy.app", 51, 50, setOf("abc"), setOf("abc")))
        assertFalse(UpdateTrustPolicy.identity("app.myspoti", 51, 50, setOf("abc"), setOf("abc")))
        assertFalse(UpdateTrustPolicy.identity("com.supfy.app", 50, 50, setOf("abc"), setOf("abc")))
        assertFalse(UpdateTrustPolicy.identity("com.supfy.app", 51, 50, emptySet(), emptySet()))
        assertFalse(UpdateTrustPolicy.identity("com.supfy.app", 51, 50, setOf("abc"), setOf("def")))
    }
    @Test fun automaticChecksUseAttemptTimeAndOneDayMinimum() {
        assertFalse(UpdateTrustPolicy.automaticDue(false, 0, 100_000_000))
        assertTrue(UpdateTrustPolicy.automaticDue(true, 0, 100_000_000))
        assertFalse(UpdateTrustPolicy.automaticDue(true, 100_000_000, 100_001_000))
        assertFalse(UpdateTrustPolicy.automaticDue(true, 100_000_000, 99_000_000))
        assertTrue(UpdateTrustPolicy.automaticDue(true, 100_000_000, 186_400_000))
    }
}
