package app.myspoti.host.android

import android.content.Context
import java.io.File

/**
 * Compatibility boundary for existing engine call sites. Diagnostics are disabled in V5:
 * inputs are discarded without logging, retaining suppliers, collecting facts, or writing files.
 */
@Suppress("UNUSED_PARAMETER")
object Logbook {
    fun log(raw: String) = Unit

    fun logEvery(key: String, intervalMs: Long, line: String) = Unit

    fun fact(key: String, value: String) = Unit

    /** A diagnostic URL must expose no part of its input, including the path. */
    fun noQuery(url: String): String = ""

    var headerExtras: (() -> List<Pair<String, String>>)?
        get() = null
        set(value) = Unit

    fun recent(): String = ""

    /** Retained for source compatibility; never reads context or creates a file. */
    fun export(context: Context): File = throw UnsupportedOperationException("Diagnostics are disabled")
}
