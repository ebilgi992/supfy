package com.supfy.app.update

import java.net.URI

/** GitHub Releases'tan okunan tek bir sürüm kaydı (yalnızca gereken alanlar). */
data class ReleaseCandidate(
    val tagName: String,
    val name: String?,
    val htmlUrl: String,
    val draft: Boolean,
)

/** Kullanıcıya önerilecek güncelleme. */
data class AvailableUpdate(
    val versionCode: Int,
    val versionName: String,
    val pageUrl: String,
)

/**
 * Güncelleme kararlarının saf (Android'den bağımsız) mantığı; birim testleriyle doğrulanır.
 * Etiket biçimi CI tarafından üretilir: `build-<versionCode>`.
 */
object ReleaseSelector {

    const val CHECK_INTERVAL_MS: Long = 12L * 60 * 60 * 1000 // 12 saat
    const val SNOOZE_MS: Long = 24L * 60 * 60 * 1000 // "Daha Sonra" → 24 saat

    private val tagPattern = Regex("""^build-(\d{1,9})$""")
    private val namePattern = Regex("""^Supfy\s+([0-9][0-9A-Za-z.\-]*)""")

    fun versionCodeOf(tagName: String): Int? =
        tagPattern.find(tagName.trim())?.groupValues?.get(1)?.toIntOrNull()

    fun versionNameOf(candidate: ReleaseCandidate): String =
        candidate.name?.let { namePattern.find(it.trim())?.groupValues?.get(1) } ?: candidate.tagName

    /** Yalnızca Supfy'nin kendi deposundaki Release sayfalarına güvenilir. */
    fun isTrustedReleaseUrl(url: String, repo: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return false
        }
        val path = uri.rawPath ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals("github.com", ignoreCase = true) &&
            uri.port == -1 &&
            path.startsWith("/$repo/releases/") &&
            !path.contains("..")
    }

    /** Yüklü sürümden yeni olan en büyük sürüm kodunu seçer; yoksa null. */
    fun pickUpdate(
        candidates: List<ReleaseCandidate>,
        installedVersionCode: Int,
        repo: String,
    ): AvailableUpdate? = candidates
        .asSequence()
        .filter { !it.draft && isTrustedReleaseUrl(it.htmlUrl, repo) }
        .mapNotNull { c -> versionCodeOf(c.tagName)?.let { code -> c to code } }
        .filter { (_, code) -> code > installedVersionCode }
        .maxByOrNull { (_, code) -> code }
        ?.let { (c, code) -> AvailableUpdate(code, versionNameOf(c), c.htmlUrl) }

    /** Otomatik denetim yalnızca son denetimden yeterli süre geçtiyse yapılır. */
    fun shouldAutoCheck(nowMs: Long, lastCheckMs: Long): Boolean =
        lastCheckMs <= 0L || nowMs < lastCheckMs || nowMs - lastCheckMs >= CHECK_INTERVAL_MS

    /** "Daha Sonra" denilen sürüm, erteleme süresi dolana kadar tekrar önerilmez. */
    fun shouldOffer(update: AvailableUpdate, snoozedCode: Int, snoozedUntilMs: Long, nowMs: Long): Boolean =
        !(update.versionCode == snoozedCode && nowMs < snoozedUntilMs)
}
