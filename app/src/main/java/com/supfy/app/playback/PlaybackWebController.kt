package com.supfy.app.playback

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.core.content.ContextCompat
import com.supfy.app.web.UrlPolicy
import com.supfy.app.web.AuthenticationPolicy
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.json.JSONTokener

/** Main-thread WebView adapter. JS bridge and private Spotify APIs are deliberately absent. */
class PlaybackWebController(context:Context,private val onError:(String)->Unit = {}):PlaybackCommandReceiver {
    private val context=context.applicationContext
    private val handler=Handler(Looper.getMainLooper())
    private val script=context.assets.open("playback.js").bufferedReader().use {it.readText()}
    private var view:WebView?=null
    private var generation=0L
    private var foreground=true
    private val startGate=PlaybackStartGate(initiallyVisible=true)
    private var enabled=false
    private var pending=false
    private var closed=false
    private var dismissed=false
    private var lastPlaying=false
    private var staleSince=0L
    private var lastError:String?=null
    private var lastErrorAt=0L
    private val mutable=MutableStateFlow(PlaybackSnapshot())
    val state=mutable.asStateFlow()
    private val poll=Runnable {sample()}
    private val timeout=Runnable {
        generation++;pending=false
        if(staleSince==0L)staleSince=SystemClock.elapsedRealtime()
        // Do not remove an established session for one throttled background JS callback.
        // No fresh playing claim is published; expire unverified state after a bounded grace.
        if(foreground || !mutable.value.available || SystemClock.elapsedRealtime()-staleSince>=60_000)clearPlayback()
        schedule()
    }

    fun attach(web:WebView){
        detach();closed=false;view=web;PlaybackConnection.attach(this);sample()
    }
    fun setEnabled(value:Boolean){
        enabled=value
        if(!value)stopService()
        sample()
    }
    fun setForeground(value:Boolean){
        val changed=foreground!=value
        foreground=value
        startGate.setForeground(value,SystemClock.elapsedRealtime())
        handler.removeCallbacks(poll)
        if(changed){
            // A pre-play probe must not suppress the fresh Home-transition observation.
            generation++;pending=false;handler.removeCallbacks(timeout)
        }
        sample()
    }
    fun command(command:PlaybackCommand)=onPlaybackCommand(command)
    override fun onPlaybackCommand(command:PlaybackCommand) {
        val web=view?:return
        if(closed||!isPlayer(web))return
        if(command==PlaybackCommand.STOP){dismissed=true;stopService()}
        val ticket=generation
        try {
            web.evaluateJavascript("$script\n;(function(){var p=window.__supfyPlayback;return !!p&&p.command('${command.scriptName}');})();") {result->
                if(ticket!=generation||closed)return@evaluateJavascript
                if(result!="true")reportError("Oynatıcı bu komutu kabul etmedi.")
                sample()
            }
        } catch(_:RuntimeException){reportError("Oynatıcıya ulaşılamadı.");clearPlayback()}
    }
    private fun sample(){
        handler.removeCallbacks(poll)
        val web=view?:return
        if(closed||pending)return
        if(!foreground&&!enabled)return
        if(!isPlayer(web)){clearPlayback();schedule();return}
        pending=true
        val ticket=generation
        handler.postDelayed(timeout,8_000)
        try {
            web.evaluateJavascript("$script\n;(function(){var p=window.__supfyPlayback;return p?JSON.stringify(p.snapshot()):null;})();") {raw->
                if(closed||ticket!=generation)return@evaluateJavascript
                handler.removeCallbacks(timeout);pending=false
                if(!isPlayer(web)){clearPlayback();schedule();return@evaluateJavascript}
                runCatching {decode(raw)}.onSuccess {snapshot->
                    staleSince=0
                    mutable.value=snapshot
                    if(!snapshot.playing)lastPlaying=false
                    if(snapshot.playing&&!lastPlaying){dismissed=false;lastPlaying=true}
                    PlaybackConnection.publish(this,snapshot)
                    publishNotification(snapshot)
                }.onFailure {clearPlayback()}
                schedule()
            }
        }catch(_:RuntimeException){handler.removeCallbacks(timeout);pending=false;clearPlayback();schedule()}
    }
    private fun publishNotification(snapshot:PlaybackSnapshot){
        if(!enabled||!snapshot.available||dismissed){stopService();return}
        if(!PlaybackNotificationService.running&&!startGate.canStart(snapshot.playing,enabled,SystemClock.elapsedRealtime()))return
        val intent=Intent(context,PlaybackNotificationService::class.java)
        try {
            if(PlaybackNotificationService.running)context.startService(intent)
            else ContextCompat.startForegroundService(context,intent)
        }catch(_:RuntimeException){stopService();reportError("Arka plan oynatıcı bildirimi başlatılamadı.")}
    }
    private fun reportError(message:String){
        val now=SystemClock.elapsedRealtime()
        if(lastError!=message || now-lastErrorAt>=30_000){lastError=message;lastErrorAt=now;onError(message)}
    }
    private fun isPlayer(web:WebView)=UrlPolicy.isTrustedPlayerOrigin(web.url)&&!AuthenticationPolicy.isAuthenticationUrl(web.url)
    private fun schedule(){
        handler.removeCallbacks(poll)
        if(!closed&&view!=null&&(foreground||enabled))handler.postDelayed(poll,if(mutable.value.playing)1500 else if(foreground)2500 else 5000)
    }
    private fun clearPlayback(){mutable.value=PlaybackSnapshot();PlaybackConnection.publish(this,PlaybackSnapshot());stopService();lastPlaying=false}
    private fun stopService(){context.stopService(Intent(context,PlaybackNotificationService::class.java))}
    fun detach(){
        generation++;handler.removeCallbacksAndMessages(null);pending=false
        PlaybackConnection.detach(this);stopService();view=null;mutable.value=PlaybackSnapshot();lastPlaying=false;dismissed=false;staleSince=0
    }
    fun close(){detach();closed=true}

    internal fun decode(raw:String?):PlaybackSnapshot {
        require(raw!=null&&raw.length<=8192&&raw!="null")
        val decoded=JSONTokener(raw).nextValue()
        val json=if(decoded is String)JSONObject(decoded) else decoded as JSONObject
        require(json.optInt("v")==1)
        return PlaybackSnapshot(json.getBoolean("available"),json.getBoolean("playing"),json.optString("title").take(200),
            json.optString("artist").take(120),json.optLong("positionMs"),json.optLong("durationMs"),json.optBoolean("next"),json.optBoolean("previous"),json.optBoolean("canPlayPause",false)).bounded()
    }
}
