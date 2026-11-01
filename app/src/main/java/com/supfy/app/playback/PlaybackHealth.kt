package com.supfy.app.playback

/** Numeric diagnostics only; never contains a URL, raw JS result, title or artist. */
data class PlaybackHealth(
    /** 0 not sampled, 1 valid, 2 timeout, 3 malformed, 4 evaluation error, 5 outside player. */
    val probeCode:Int=0,
    /** Exact/usable-exact/scoped/usable-scoped/media/session/disabled/hidden bits (1..128). */
    val probeFlags:Int=0,
    /** 0 no evidence, 1 visible usable control, 2 active media, 3 explicit standard session state. */
    val evidence:Int=0,
    /** 0 idle, 1 disabled, 2 unavailable, 3 dismissed, 4 background gate, 5 requested, 6 running, 7 start error. */
    val startCode:Int=0,
    val enabled:Int=0,
    val foreground:Int=1,
    val available:Int=0,
    val playing:Int=0,
)

data class PlaybackServiceHealth(
    val running:Int=0,
    val foreground:Int=0,
    /** 0 none, 1 missing owner, 2 unavailable, 3 paused lease expired, 4 foreground error, 5 stopped. */
    val stopCode:Int=0,
    /** App notifications enabled/channel exists/channel blocked/notification posted bits (1/2/4/8).
     * App permission state is diagnostic only: media-session notifications have an OS exemption. */
    val notificationFlags:Int=0,
)
