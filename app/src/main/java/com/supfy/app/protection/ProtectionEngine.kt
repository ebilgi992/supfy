package com.supfy.app.protection

import com.supfy.app.web.UrlPolicy
import java.net.URI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProtectionOptions(
    val enabled: Boolean = false,
    val muteAds: Boolean = true,
    val hideBanners: Boolean = true,
    val hidePromos: Boolean = true,
    val filterNetwork: Boolean = true,
)

enum class EngineStatus { DISABLED, WAITING_FOR_PLAYER, STARTING, ACTIVE, ERROR }
enum class ComponentStatus { DISABLED, WAITING, READY, PROCESSING, ERROR }

data class PageSnapshot(
    val detected: Int = 0, val muted: Int = 0, val banners: Int = 0,
    val promos: Int = 0, val restored: Int = 0, val errors: Int = 0,
    val phase: String = "idle", val eme: Boolean = false, val playing: Boolean = false,
    val ready: Boolean = true, val audioReady: Boolean = true,
    val audioError: Boolean = false, val visualError: Boolean = false,
    val evidence: Int = 3, val resumed: Int = 0,
)

data class EngineState(
    val options: ProtectionOptions = ProtectionOptions(),
    val status: EngineStatus = EngineStatus.DISABLED,
    val detectedAds: Long = 0, val mutedMedia: Long = 0,
    val hiddenBanners: Long = 0, val hiddenPromos: Long = 0,
    val restoredMedia: Long = 0, val blockedRequests: Long = 0,
    val errors: Long = 0, val phase: String = "idle", val eme: Boolean? = null, val playing: Boolean = false,
    val logs: List<String> = emptyList(),
    val audioError: Boolean = false, val visualError: Boolean = false, val audioReady: Boolean = false,
    val nativeAudioSupported: Boolean = false, val nativeAudioError: Boolean = false,
    val nativeMutes: Long = 0, val nativeRestores: Long = 0, val resumedMusic: Long = 0,
    val lastError: String? = null,
) {
    private val common: ComponentStatus get() = when (status) {
        EngineStatus.DISABLED -> ComponentStatus.DISABLED
        EngineStatus.ERROR -> ComponentStatus.ERROR
        EngineStatus.ACTIVE -> ComponentStatus.READY
        else -> ComponentStatus.WAITING
    }
    val detectionStatus: ComponentStatus get() = if (common == ComponentStatus.READY && phase == "ad") ComponentStatus.PROCESSING else common
    val networkStatus: ComponentStatus get() = if (!options.filterNetwork) ComponentStatus.DISABLED else when (status) {
        EngineStatus.STARTING -> ComponentStatus.READY
        else -> common
    }
    val audioStatus: ComponentStatus get() = when {
        !options.muteAds -> ComponentStatus.DISABLED
        common != ComponentStatus.READY -> common
        (audioError && (!nativeAudioSupported || nativeAudioError)) || (!audioReady && nativeAudioError) -> ComponentStatus.ERROR
        !audioReady && !nativeAudioSupported -> ComponentStatus.WAITING
        phase == "ad" -> ComponentStatus.PROCESSING
        else -> ComponentStatus.READY
    }
    val visualStatus: ComponentStatus get() = when {
        !options.hideBanners && !options.hidePromos -> ComponentStatus.DISABLED
        common != ComponentStatus.READY -> common
        visualError -> ComponentStatus.ERROR
        else -> ComponentStatus.READY
    }
}

/** Thread safe core. No page text, tokens, URLs or credentials are logged. */
class ProtectionEngine {
    private val mutableState = MutableStateFlow(EngineState())
    val state: StateFlow<EngineState> = mutableState.asStateFlow()
    private var onPlayer = false
    private var fault = false
    private var previous = PageSnapshot()
    private val adHosts = setOf("doubleclick.net", "pagead2.googlesyndication.com", "moatads.com")

    @Synchronized fun configure(options: ProtectionOptions) {
        if (!options.enabled) fault = false
        val status = when {
            !options.enabled -> EngineStatus.DISABLED
            fault -> EngineStatus.ERROR
            !onPlayer -> EngineStatus.WAITING_FOR_PLAYER
            mutableState.value.status == EngineStatus.ACTIVE -> EngineStatus.ACTIVE
            else -> EngineStatus.STARTING
        }
        mutableState.value = mutableState.value.copy(options = options, status = status,
            playing = options.enabled && mutableState.value.playing,
            eme = if (options.enabled) mutableState.value.eme else null,
            audioError = if (!options.enabled) false else mutableState.value.audioError,
            visualError = if (!options.enabled) false else mutableState.value.visualError,
            nativeAudioError = if (!options.enabled) false else mutableState.value.nativeAudioError)
        log(if (options.enabled) "Koruma tercihleri uygulandı." else "Koruma kapalı; sayfa geri yükleniyor.")
    }

    @Synchronized fun pageStarted(url: String?) {
        onPlayer = UrlPolicy.isTrustedPlayerOrigin(url)
        previous = PageSnapshot()
        val status = when {
            !mutableState.value.options.enabled -> EngineStatus.DISABLED
            fault -> EngineStatus.ERROR
            onPlayer -> EngineStatus.STARTING
            else -> EngineStatus.WAITING_FOR_PLAYER
        }
        mutableState.value = mutableState.value.copy(status = status, phase = "idle", eme = null, playing = false,
            audioReady = false, audioError = false, visualError = false)
    }

    @Synchronized fun shouldBlock(url: String, method: String, mainFrame: Boolean): Boolean {
        val s = mutableState.value
        if (!onPlayer || fault || !s.options.enabled || !s.options.filterNetwork || mainFrame || method != "GET") return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme != "https" || uri.userInfo != null || uri.port !in listOf(-1, 443)) return false
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
        return adHosts.any { host == it || host.endsWith(".$it") }
    }

    @Synchronized fun requestBlocked() {
        mutableState.value = mutableState.value.copy(blockedRequests = mutableState.value.blockedRequests + 1)
        log("Üçüncü taraf reklam isteği engellendi.")
    }

    @Synchronized fun acceptSnapshot(report: PageSnapshot) {
        if (!onPlayer || fault || !mutableState.value.options.enabled) return
        val current = counts(report)
        val old = counts(previous)
        if (current.any { it !in 0..1_000_000 } || current.zip(old).any { (a,b) -> a < b } ||
            report.evidence !in 0..3 || report.phase !in setOf("idle", "music", "ad", "error")) { fail(); return }
        val s = mutableState.value
        mutableState.value = s.copy(
            status = if (report.ready) EngineStatus.ACTIVE else EngineStatus.STARTING,
            detectedAds = s.detectedAds + report.detected - previous.detected,
            mutedMedia = s.mutedMedia + report.muted - previous.muted,
            hiddenBanners = s.hiddenBanners + report.banners - previous.banners,
            hiddenPromos = s.hiddenPromos + report.promos - previous.promos,
            errors = s.errors + report.errors - previous.errors,
            restoredMedia = s.restoredMedia + report.restored - previous.restored,
            phase = report.phase, eme = report.eme, playing = report.playing,
            audioReady = report.audioReady, audioError = report.audioError, visualError = report.visualError,
            resumedMusic = s.resumedMusic + report.resumed - previous.resumed,
            lastError = when {
                report.audioError -> "Medya öğesi susturulamadı; ses bileşeni geri alındı."
                report.visualError -> "Görsel filtre hatası; sayfa stilleri geri alındı."
                else -> s.lastError
            },
        )
        if (report.detected > previous.detected) log("Açık reklam işareti algılandı.")
        if (report.muted > previous.muted) log("Reklam medya sesi susturuldu.")
        if (report.restored > previous.restored) log("Medya sessiz durumu geri yüklendi.")
        previous = report
        // Counters include historical errors acknowledged after an explicit off/on.
        // Only the current failure phase disables a healthy page again.
        if (report.phase == "error") {
            fault = true
            mutableState.value = mutableState.value.copy(status = EngineStatus.ERROR, playing = false,
                lastError = "Reklam algılama hatası; ses ve sayfa müdahaleleri geri alındı.")
            log("Sayfa koruması durduruldu; kapatıp açarak yeniden deneyin.")
        }
    }

    @Synchronized fun fail() {
        if (fault) return
        fault = true
        mutableState.value = mutableState.value.copy(status = EngineStatus.ERROR, errors = mutableState.value.errors + 1, playing = false,
            lastError = "Sayfa yanıtı doğrulanamadı veya zaman aşımı oluştu; koruma durduruldu.")
        log("Koruma hatası: müdahaleler durduruldu. Yeniden açmak için kapatıp açın.")
    }

    @Synchronized fun nativeAudioSupport(supported: Boolean) {
        mutableState.value = mutableState.value.copy(nativeAudioSupported = supported, nativeAudioError = false)
    }
    @Synchronized fun nativeAudioChanged(muted: Boolean) {
        val s = mutableState.value
        mutableState.value = s.copy(nativeMutes = s.nativeMutes + if (muted) 1 else 0,
            nativeRestores = s.nativeRestores + if (muted) 0 else 1)
        log(if (muted) "Yalnızca Supfy WebView sesi susturuldu." else "WebView ses durumu geri yüklendi.")
    }
    @Synchronized fun nativeAudioFailed() {
        val s = mutableState.value
        mutableState.value = s.copy(nativeAudioError = true, audioReady = false, errors = s.errors + 1,
            lastError = "WebView ses API hatası; medya öğesi yöntemine dönülüyor.")
        log("WebView ses bileşeni devre dışı bırakıldı.")
    }
    @Synchronized fun stopForAudioFailure() {
        fault=true
        mutableState.value=mutableState.value.copy(status=EngineStatus.ERROR,playing=false,
            lastError="Ses geri yüklenemedi. Normal oynatmaya dön seçeneğiyle sayfayı sıfırlayın.")
        log("Ses geri yükleme denemeleri durduruldu; kullanıcıya sıfırlama sunuldu.")
    }
    @Synchronized fun rendererFailed() {
        fault=true
        mutableState.value=mutableState.value.copy(status=EngineStatus.ERROR,playing=false,
            errors=mutableState.value.errors+1,
            lastError="Android WebView görüntüleme işlemi sonlandı; reklam motoru durduruldu.")
        log("WebView işlem kaybı nedeniyle koruma durduruldu.")
    }

    private fun counts(s: PageSnapshot) = listOf(s.detected, s.muted, s.banners, s.promos, s.restored, s.errors, s.resumed)
    private fun log(message: String) {
        mutableState.value = mutableState.value.copy(logs = (mutableState.value.logs + message).takeLast(40))
    }
}
