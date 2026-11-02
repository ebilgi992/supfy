package app.myspoti.host.android

import android.content.Context

/** Crash persistence and historical process-exit collection are disabled in V5. */
object LastExit {
    @Suppress("UNUSED_PARAMETER")
    fun install(context: Context) = Unit
}
