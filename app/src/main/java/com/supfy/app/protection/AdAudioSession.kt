package com.supfy.app.protection

/** A per-WebView audio boundary; never Android's device/global music stream. */
interface AudioPort { var muted: Boolean }

class AdAudioSession(private val port: AudioPort) {
    private var previous: Boolean? = null
    val restorationPending: Boolean get() = previous == false
    fun enterAd(): Boolean {
        if (previous != null) return false
        val wasMuted = port.muted
        previous = wasMuted
        if (wasMuted) return false
        port.muted = true
        check(port.muted) { "WebView mute did not apply" }
        return true
    }
    fun restore(): Boolean {
        val original = previous ?: return false
        if (!original && port.muted) {
            port.muted = false
            check(!port.muted) { "WebView mute did not restore" }
            previous = null
            return true
        }
        previous = null
        return false
    }
}

enum class AudioResult { NONE, MUTED, RESTORED, FAILED }

/** Failed new mutes are disabled, but outstanding ownership is always restored/retried. */
class AudioRecovery(port: AudioPort) {
    private val session = AdAudioSession(port)
    var failed = false
        private set
    val restorationPending get() = session.restorationPending
    fun apply(ad: Boolean): AudioResult = try {
        val muting = ad && !failed
        val changed = if (muting) session.enterAd() else session.restore()
        if (!changed) AudioResult.NONE else if (muting) AudioResult.MUTED else AudioResult.RESTORED
    } catch (_: Exception) { failed=true; AudioResult.FAILED }
    fun reset() { if (!restorationPending) failed=false }
}
