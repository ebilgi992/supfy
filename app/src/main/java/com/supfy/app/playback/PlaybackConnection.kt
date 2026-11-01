package com.supfy.app.playback

import java.lang.ref.WeakReference

/** The service never owns an Activity, WebView or context-bearing callback strongly. */
interface PlaybackCommandReceiver {fun onPlaybackCommand(command:PlaybackCommand)}

internal object PlaybackConnection {
    private var receiver=WeakReference<PlaybackCommandReceiver>(null)
    var snapshot=PlaybackSnapshot();private set
    fun attach(owner:PlaybackCommandReceiver){receiver=WeakReference(owner);snapshot=PlaybackSnapshot()}
    fun detach(owner:PlaybackCommandReceiver){if(receiver.get()===owner){receiver.clear();snapshot=PlaybackSnapshot()}}
    fun publish(owner:PlaybackCommandReceiver,value:PlaybackSnapshot){if(receiver.get()===owner)snapshot=value.bounded()}
    fun hasOwner()=receiver.get()!=null
    fun dispatch(command:PlaybackCommand){receiver.get()?.onPlaybackCommand(command)}
}
