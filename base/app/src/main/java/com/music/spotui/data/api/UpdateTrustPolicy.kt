package com.music.spotui.data.api

import java.net.URI

/** Pure trust decisions shared by release discovery and the APK preflight. */
object UpdateTrustPolicy {
    const val REPOSITORY = "ebilgi992/supfy"
    const val PACKAGE = "com.supfy.app"
    const val INTERVAL_MS = 24 * 60 * 60 * 1000L
    private fun uri(value: String): URI? = runCatching { URI(value) }.getOrNull()?.takeIf {
        it.scheme == "https" && it.host == "github.com" && it.userInfo == null && (it.port == -1 || it.port == 443) && it.fragment == null && it.path.split('/').none { part -> part == "." || part == ".." }
    }
    fun releaseUrl(value: String): Boolean = uri(value)?.path?.startsWith("/$REPOSITORY/releases/") == true
    fun assetUrl(value: String): Boolean = uri(value)?.path?.let { path ->
        path.startsWith("/$REPOSITORY/releases/download/") && path.substringAfterLast('/').let { it.startsWith("supfy-") && it.endsWith(".apk") }
    } == true
    fun automaticDue(enabled: Boolean, attemptedAt: Long, now: Long): Boolean =
        enabled && (attemptedAt == 0L || now >= attemptedAt && now - attemptedAt >= INTERVAL_MS)
    fun checksum(actual: String, expected: String): Boolean = expected.matches(Regex("[0-9a-fA-F]{64}")) && actual.equals(expected, ignoreCase = true)
    fun identity(packageName: String, candidateVersion: Long, installedVersion: Long, candidateSigners: Set<String>, installedSigners: Set<String>): Boolean =
        packageName == PACKAGE && candidateVersion > installedVersion && installedSigners.isNotEmpty() && candidateSigners == installedSigners
}
