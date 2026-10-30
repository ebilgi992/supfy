package com.supfy.app.web

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DiagKind { PAGE_START, PAGE_DONE, NET_ERROR, HTTP_ERROR, BLOCKED, SSL, CONSOLE, PERMISSION, RENDERER }

data class DiagEvent(val timeMs: Long, val kind: DiagKind, val detail: String)

/**
 * Giriş/oynatma sorunlarını telefondan teşhis etmek için son olayların kaydı.
 * Gizlilik: adreslerin sorgu (?…) ve parça (#…) kısımları silinir; çerez, form içeriği
 * veya token kaydedilmez. Kayıt yalnızca bellekte tutulur, uygulama kapanınca silinir.
 */
data class DiagLog(val events: List<DiagEvent> = emptyList()) {

    fun add(kind: DiagKind, detail: String, nowMs: Long = System.currentTimeMillis()): DiagLog {
        val clean = scrubText(detail).take(MAX_DETAIL)
        // Aynı olay art arda tekrarlanıyorsa kaydı şişirmez.
        val last = events.lastOrNull()
        if (last != null && last.kind == kind && last.detail == clean) return this
        return DiagLog((events + DiagEvent(nowMs, kind, clean)).takeLast(MAX_EVENTS))
    }

    fun format(header: String): String {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.ROOT)
        return buildString {
            appendLine(header)
            events.forEach { appendLine("${fmt.format(Date(it.timeMs))} ${it.kind.name} ${it.detail}") }
        }
    }

    companion object {
        const val MAX_EVENTS = 40
        private const val MAX_DETAIL = 220
        private val urlInText = Regex("""https?://[^\s"'<>)]+""")

        /** `https://host/path?x=1#y` → `https://host/path` */
        fun sanitizeUrl(url: String?): String {
            if (url.isNullOrBlank()) return "-"
            return try {
                val u = URI(url.trim())
                val scheme = u.scheme ?: return url.substringBefore('?').substringBefore('#').take(MAX_DETAIL)
                if (u.host == null) return "$scheme:"
                "$scheme://${u.host}${u.rawPath ?: ""}"
            } catch (_: Exception) {
                url.substringBefore('?').substringBefore('#').take(MAX_DETAIL)
            }
        }

        /** Serbest metindeki tüm adresleri temizler (konsol mesajları için). */
        fun scrubText(text: String): String = urlInText.replace(text) { sanitizeUrl(it.value) }
    }
}
