package com.music.spotui.data

/** Compatibility boundary: V5 never reads, retains, or exports diagnostic text. */
@Suppress("UNUSED_PARAMETER")
object BetaLogs {
    const val MINUTES = 3

    suspend fun recent(minutes: Int = MINUTES): String = ""

    internal fun useful(text: String): String = ""

    fun sanitize(text: String): String = ""
}
