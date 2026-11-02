package com.music.spotui.privacy

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** No personal data is stored here: only the revision of the provider disclosure accepted. */
object ConsentRevision {
    fun accepts(accepted: String?, required: String): Boolean = accepted == required
}

object ConsentPolicy {
    enum class Feature(val revision: String) {
        LYRICS("lyrics:spotify-lrclib-lyricsplus-lrcred-kugou-netease-genius:1"),
        TRANSLATION("translation:google:1"),
        GENIUS_MEANING("meaning:genius:1"),
        MUSIC_VIDEO("video:youtube:1"),
    }
    class ConsentRequired(val feature: Feature) : IllegalStateException("Optional service consent required")
    @Volatile private var preferences: SharedPreferences? = null
    private val requests = ConsentQueue<Feature>()
    private val _grantedFeatures = MutableStateFlow<Set<Feature>>(emptySet())
    val grantedFeatures: StateFlow<Set<Feature>> = _grantedFeatures.asStateFlow()
    private fun publishGrants() { _grantedFeatures.value = Feature.entries.filter(::isGranted).toSet() }
    /** FIFO feature disclosure queue; grant/dismiss resolves all operations waiting for that feature. */
    val pending: StateFlow<Feature?> = requests.pending

    fun initialize(context: Context) {
        preferences = context.applicationContext.getSharedPreferences("supfy_optional_services", Context.MODE_PRIVATE)
        publishGrants()
    }
    fun isGranted(feature: Feature): Boolean = ConsentRevision.accepts(preferences?.getString(feature.name, null), feature.revision)

    private fun decision(feature: Feature): CompletableDeferred<Boolean> =
        if (preferences == null) CompletableDeferred(false) else requests.decision(feature, isGranted(feature))
    /** Suspends the operation before any network request, then continues immediately on grant. */
    suspend fun await(feature: Feature): Boolean = decision(feature).await()

    /** For synchronous provider libraries running on IO threads; never block the UI thread. */
    fun awaitBlocking(feature: Feature): Boolean {
        if (isGranted(feature)) return true
        val answer = decision(feature)
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return false
        return kotlinx.coroutines.runBlocking { answer.await() }
    }
    /** A deliberate retry/settings action can request a feature declined earlier in this session. */
    fun requestConsent(feature: Feature) {
        requests.reopen(feature)
        decision(feature)
    }
    /** Compatibility API for callers that explicitly handle typed consent errors. */
    fun require(feature: Feature) {
        if (!isGranted(feature)) { requestConsent(feature); throw ConsentRequired(feature) }
    }
    fun grant(feature: Feature) {
        checkNotNull(preferences) { "Consent storage is not initialized" }.edit().putString(feature.name, feature.revision).apply()
        publishGrants()
        finish(feature, true)
    }
    fun revoke(feature: Feature) {
        preferences?.edit()?.remove(feature.name)?.apply()
        publishGrants()
        finish(feature, false)
    }
    /** Decline resolves waiters safely and avoids repeated automatic prompts during this session. */
    fun dismiss(feature: Feature) = finish(feature, false)
    private fun finish(feature: Feature, granted: Boolean) = requests.finish(feature, granted)
}
