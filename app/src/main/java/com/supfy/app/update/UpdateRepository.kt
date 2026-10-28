package com.supfy.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases API'sinden Supfy sürümlerini okur. Kimlik bilgisi göndermez,
 * kullanıcı verisi göndermez; yalnızca herkese açık sürüm listesini ister.
 */
class UpdateRepository(private val repo: String) {

    sealed interface Result {
        data class UpToDate(val checked: Int) : Result
        data class Available(val update: AvailableUpdate) : Result
        data class Failed(val reason: String) : Result
    }

    suspend fun check(installedVersionCode: Int): Result = withContext(Dispatchers.IO) {
        try {
            val releases = fetchReleases()
            val update = ReleaseSelector.pickUpdate(releases, installedVersionCode, repo)
            if (update != null) Result.Available(update) else Result.UpToDate(releases.size)
        } catch (e: Exception) {
            // Ağ/ayrıştırma hatası uygulamanın açılışını asla engellemez.
            Result.Failed(e.javaClass.simpleName)
        }
    }

    private fun fetchReleases(): List<ReleaseCandidate> {
        val url = URL("https://api.github.com/repos/$repo/releases?per_page=20")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false
            useCaches = false
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Supfy-Android")
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                error("HTTP ${conn.responseCode}")
            }
            val body = conn.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    check(out.size() <= MAX_BYTES) { "Yanıt çok büyük" }
                }
                out.toString(Charsets.UTF_8.name())
            }
            val array = JSONArray(body)
            return (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val tag = o.optString("tag_name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val html = o.optString("html_url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                ReleaseCandidate(
                    tagName = tag,
                    name = o.optString("name").takeIf { it.isNotBlank() },
                    htmlUrl = html,
                    draft = o.optBoolean("draft", false),
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_BYTES = 512 * 1024
    }
}
