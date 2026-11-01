package com.supfy.app.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import com.supfy.app.MainActivity
import com.supfy.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Framework media session + mediaPlayback foreground service; no external media/token API. */
class PlaybackNotificationService:Service() {
    private lateinit var session:MediaSession
    private val handler=Handler(Looper.getMainLooper())
    private val expire=Runnable {mutableHealth.value=mutableHealth.value.copy(stopCode=3);stopSelf()}
    private val idleLease=PausedPlaybackLease()
    override fun onCreate(){
        super.onCreate();running=true;mutableHealth.value=PlaybackServiceHealth(running=1)
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"Supfy oynatıcı",NotificationManager.IMPORTANCE_LOW).apply {description="Müzik ve kilit ekranı kontrolleri";setSound(null,null)})
        session=MediaSession(this,"Supfy").apply {
            setCallback(object:MediaSession.Callback(){
                override fun onPlay()=PlaybackConnection.dispatch(PlaybackCommand.PLAY)
                override fun onPause()=PlaybackConnection.dispatch(PlaybackCommand.PAUSE)
                override fun onSkipToNext()=PlaybackConnection.dispatch(PlaybackCommand.NEXT)
                override fun onSkipToPrevious()=PlaybackConnection.dispatch(PlaybackCommand.PREVIOUS)
                override fun onStop(){PlaybackConnection.dispatch(PlaybackCommand.STOP);stopSelf()}
            },handler)
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive=true
        }
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(!PlaybackConnection.hasOwner()){mutableHealth.value=mutableHealth.value.copy(stopCode=1);stopSelf();return START_NOT_STICKY}
        intent?.getStringExtra(COMMAND)?.let {name->
            PlaybackCommand.entries.firstOrNull {it.name==name}?.let {PlaybackConnection.dispatch(it)}
        }
        val data=PlaybackConnection.snapshot
        if(!data.available){mutableHealth.value=mutableHealth.value.copy(stopCode=2);stopSelf();return START_NOT_STICKY}
        val title=data.title.ifBlank {"Spotify oynatıcı"}
        session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST,data.artist).putLong(MediaMetadata.METADATA_KEY_DURATION,data.durationMs).build())
        var actions=if(data.canPlayPause)PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP else 0L
        if(data.next)actions=actions or PlaybackState.ACTION_SKIP_TO_NEXT
        if(data.previous)actions=actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        session.setPlaybackState(PlaybackState.Builder().setActions(actions)
            .setState(if(data.playing)PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,data.positionMs,if(data.playing)1f else 0f,SystemClock.elapsedRealtime()).build())
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder=Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_supfy_mark).setContentTitle(title)
            .setContentText(data.artist.ifBlank {"Supfy"}).setContentIntent(open).setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_TRANSPORT).setOngoing(data.playing)
        var actionIndex=0;val compact=ArrayList<Int>()
        if(data.previous){builder.addAction(action(android.R.drawable.ic_media_previous,"Önceki",PlaybackCommand.PREVIOUS));compact.add(actionIndex++)}
        if(data.canPlayPause){builder.addAction(action(if(data.playing)android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if(data.playing)"Duraklat" else "Oynat",if(data.playing)PlaybackCommand.PAUSE else PlaybackCommand.PLAY));compact.add(actionIndex++)}
        if(data.next){builder.addAction(action(android.R.drawable.ic_media_next,"Sonraki",PlaybackCommand.NEXT));compact.add(actionIndex)}
        builder.setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(*compact.toIntArray()))
        val notification=builder.build()
        try {
            if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            else startForeground(NOTIFICATION_ID,notification)
        }catch(_:RuntimeException){
            mutableHealth.value=mutableHealth.value.copy(stopCode=4,notificationFlags=notificationFlags())
            stopSelf();return START_NOT_STICKY
        }
        mutableHealth.value=PlaybackServiceHealth(running=1,foreground=1,notificationFlags=notificationFlags())
        handler.removeCallbacks(expire)
        idleLease.remainingMs(data.playing,SystemClock.elapsedRealtime())?.let {remaining->
            handler.postDelayed(expire,remaining)
        }
        return START_NOT_STICKY
    }
    private fun notificationFlags():Int {
        val manager=getSystemService(NotificationManager::class.java)
        var flags=if(manager.areNotificationsEnabled())1 else 0
        manager.getNotificationChannel(CHANNEL)?.let {channel->flags=flags or 2;if(channel.importance==NotificationManager.IMPORTANCE_NONE)flags=flags or 4}
        if(manager.activeNotifications.any {it.id==NOTIFICATION_ID})flags=flags or 8
        return flags
    }
    private fun action(icon:Int,label:String,command:PlaybackCommand):Notification.Action {
        val pending=PendingIntent.getService(this,command.ordinal,Intent(this,PlaybackNotificationService::class.java)
            .putExtra(COMMAND,command.name),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Action.Builder(icon,label,pending).build()
    }
    override fun onDestroy(){handler.removeCallbacksAndMessages(null);if(::session.isInitialized){session.isActive=false;session.release()};running=false;stopForeground(STOP_FOREGROUND_REMOVE)
        mutableHealth.value=mutableHealth.value.copy(running=0,foreground=0,stopCode=mutableHealth.value.stopCode.takeIf {it!=0}?:5,notificationFlags=notificationFlags())
        super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
    companion object {
        const val CHANNEL="supfy_playback"
        const val NOTIFICATION_ID=1027
        private const val COMMAND="playback_command"
        @Volatile var running=false;private set
        private val mutableHealth=MutableStateFlow(PlaybackServiceHealth())
        val health=mutableHealth.asStateFlow()
    }
}
