package app.myspoti.host.android

/** Compatibility-only sink: no platform logging, buffering, formatting or persistence. */
@Suppress("UNUSED_PARAMETER")
object PrivacyLog {
    const val VERBOSE=2; const val DEBUG=3; const val INFO=4; const val WARN=5; const val ERROR=6; const val ASSERT=7
    fun v(tag:String?, message:String?, error:Throwable?=null):Int=0
    fun d(tag:String?, message:String?, error:Throwable?=null):Int=0
    fun i(tag:String?, message:String?, error:Throwable?=null):Int=0
    fun w(tag:String?, message:String?, error:Throwable?=null):Int=0
    fun w(tag:String?, error:Throwable?):Int=0
    fun e(tag:String?, message:String?, error:Throwable?=null):Int=0
    fun wtf(tag:String?, message:String?, error:Throwable?=null):Int=0
    fun println(priority:Int,tag:String?,message:String?):Int=0
    fun isLoggable(tag:String?,level:Int):Boolean=false
    fun getStackTraceString(error:Throwable?):String=""
}
