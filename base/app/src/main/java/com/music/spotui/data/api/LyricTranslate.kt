package com.music.spotui.data.api

import app.myspoti.host.android.PrivacyLog as Log
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Translates lyric lines in one request through Google's public endpoint. */
object LyricTranslate {
    private const val TAG = "LyricTranslate"

    data class Result(val lines: List<String>, val sourceLanguage: String?)

    suspend fun detectLanguage(lines: List<String>, targetLang: String): String? {
        val sample = lines.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "♪" }
            .take(3)
            .joinToString("\n")
        if (sample.isBlank()) return null
        return translateText(sample, targetLang)?.sourceLanguage
    }

    suspend fun translateLines(lines: List<String>, targetLang: String): Result? {
        if (lines.isEmpty()) return Result(emptyList(), null)

        val idx = ArrayList<Int>()
        val payload = ArrayList<String>()
        lines.forEachIndexed { i, line ->
            val t = line.trim()
            if (t.isNotEmpty() && t != "♪") {
                idx.add(i)
                payload.add(t)
            }
        }
        if (payload.isEmpty()) return Result(lines.map { "" }, null)

        val translated = translateText(payload.joinToString("\n"), targetLang) ?: return null
        val parts = translated.text.split("\n")
        val out = MutableList(lines.size) { "" }
        idx.forEachIndexed { j, lineIdx ->
            out[lineIdx] = parts.getOrElse(j) { payload[j] }.trim()
        }
        return Result(out, translated.sourceLanguage)
    }

    private data class Response(val text: String, val sourceLanguage: String?)

    private suspend fun translateText(text: String, targetLang: String): Response? {
        if (!com.music.spotui.privacy.ConsentPolicy.await(com.music.spotui.privacy.ConsentPolicy.Feature.TRANSLATION)) return null
        return runCatching {
        com.music.spotui.data.DataSaver.check()
        val q = URLEncoder.encode(text, "UTF-8")
        val tl = URLEncoder.encode(targetLang, "UTF-8")
        app.myspoti.extensions.SafeNet.client.newBuilder()
            .callTimeout(12, java.util.concurrent.TimeUnit.SECONDS).build()
            .newCall(okhttp3.Request.Builder().url(
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$tl&dt=t&q=$q"
            ).build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val responseBody = response.body.string()
            val root = JSONArray(responseBody)
            val chunks = root.optJSONArray(0)
                ?: return null
            val translated = buildString {
                for (i in 0 until chunks.length()) {
                    val piece = chunks.optJSONArray(i)?.optString(0) ?: continue
                    append(piece)
                }
            }.ifBlank { return null }
            Response(translated, root.optString(2).ifBlank { null })
        }
    }.getOrElse {
        Log.e(TAG, "translate failed")
        null
    }
    }
}
