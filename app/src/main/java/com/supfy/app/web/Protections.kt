package com.supfy.app.web

/** Supfy'nin uyguladığı güvenlik korumaları. Reklam engelleme bu listede yoktur. */
enum class Protection {
    HTTPS_ONLY,
    DOMAIN_ALLOWLIST,
    SAFE_BROWSING,
    DRM_GATE,
    THIRD_PARTY_COOKIES_BLOCKED,
    NO_JS_BRIDGE,
    FILE_ACCESS_OFF,
    SERVICE_WORKER_HARDENING,
}

enum class ProtectionStatus { ACTIVE, UNSUPPORTED }

/** Cihazdaki WebView'in desteklediği isteğe bağlı özellikler (çalışma anında ölçülür). */
data class DeviceSupport(
    val safeBrowsing: Boolean,
    val serviceWorkerHardening: Boolean,
)

object Protections {

    fun statusOf(protection: Protection, support: DeviceSupport): ProtectionStatus = when (protection) {
        Protection.SAFE_BROWSING ->
            if (support.safeBrowsing) ProtectionStatus.ACTIVE else ProtectionStatus.UNSUPPORTED
        Protection.SERVICE_WORKER_HARDENING ->
            if (support.serviceWorkerHardening) ProtectionStatus.ACTIVE else ProtectionStatus.UNSUPPORTED
        // Diğerleri koddaki sabit ayarlardır; her cihazda etkindir.
        else -> ProtectionStatus.ACTIVE
    }

    fun activeCount(support: DeviceSupport): Int =
        Protection.entries.count { statusOf(it, support) == ProtectionStatus.ACTIVE }
}

/**
 * Bu uygulama oturumunda GERÇEKTEN gerçekleşen olaylar. Tahmin yoktur; her sayı,
 * WebView'in bildirdiği somut bir olayda bir artar. Uygulama kapanınca sıfırlanır.
 */
data class SessionStats(
    val blockedNavigations: Int = 0,
    val sslRejected: Int = 0,
    val deniedPermissions: Int = 0,
    val drmGranted: Int = 0,
    val externalOpens: Int = 0,
) {
    fun withBlockedNavigation() = copy(blockedNavigations = blockedNavigations + 1)
    fun withSslRejected() = copy(sslRejected = sslRejected + 1)
    fun withDeniedPermission() = copy(deniedPermissions = deniedPermissions + 1)
    fun withDrmGranted() = copy(drmGranted = drmGranted + 1)
    fun withExternalOpen() = copy(externalOpens = externalOpens + 1)

    val total: Int get() = blockedNavigations + sslRejected + deniedPermissions + drmGranted + externalOpens
}
