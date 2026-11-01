package com.supfy.app.playback

/** Track metadata explicitly requested for the media notification; never account data. */
data class PlaybackSnapshot(
    val available:Boolean=false,
    val playing:Boolean=false,
    val title:String="",
    val artist:String="",
    val positionMs:Long=0,
    val durationMs:Long=0,
    val next:Boolean=false,
    val previous:Boolean=false,
    val canPlayPause:Boolean=true,
    val probeFlags:Int=0,
    val evidence:Int=0,
) {
    fun bounded()=copy(title=title.take(200),artist=artist.take(120),
        positionMs=positionMs.coerceIn(0,if(durationMs>0)durationMs.coerceAtMost(MAX_TIME) else MAX_TIME),
        durationMs=durationMs.coerceIn(0,MAX_TIME),probeFlags=probeFlags.coerceIn(0,255),evidence=evidence.coerceIn(0,3))
    companion object {const val MAX_TIME=86_400_000L}
}

enum class PlaybackCommand(val scriptName:String) { PLAY("play"), PAUSE("pause"), NEXT("next"), PREVIOUS("previous"), STOP("stop") }


/** Polling paused metadata must not renew the foreground-service idle lifetime. */
internal class PausedPlaybackLease(private val timeoutMs:Long=300_000L) {
    private var pausedAt:Long?=null
    fun remainingMs(playing:Boolean,nowMs:Long):Long? {
        if(playing){pausedAt=null;return null}
        val start=pausedAt?:nowMs.also {pausedAt=it}
        return (timeoutMs-(nowMs-start).coerceAtLeast(0)).coerceAtLeast(0)
    }
}

/** Android remains the authority: this only permits trying within a short visible transition. */
internal class PlaybackStartGate(initiallyVisible:Boolean=false,private val graceMs:Long=3_000L) {
    private var visible=initiallyVisible
    private var lastVisibleAt:Long?=null
    fun setForeground(value:Boolean,nowMs:Long) {
        if(value||visible)lastVisibleAt=nowMs
        visible=value
    }
    fun canStart(playing:Boolean,enabled:Boolean,nowMs:Long):Boolean {
        if(!playing||!enabled)return false
        if(visible)return true
        val previous=lastVisibleAt?:return false
        return nowMs>=previous&&nowMs-previous<=graceMs
    }
}
