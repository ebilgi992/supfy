package com.supfy.app.web

import android.os.Handler
import android.os.Looper
import android.webkit.WebView

/** Local rendering health. Never reads page text, field values, cookies or tokens. */
class WebPageGuard(private val state: WebUiState) {
    private val handler=Handler(Looper.getMainLooper())
    private val load=PageLoadTracker()
    private var ticket=0L
    private var activeUrl:String?=null
    private var view:WebView?=null
    private var probePending=false
    private var visualConfirmed=false
    private var visualPending=false
    private var visualAttempt=0L
    private var visualUnavailable=false
    private var lastContentPresent=false
    private var lastInteractive=false
    private var documentCommitted=false
    private var foreground=true
    private var disposed=false
    private var authenticationScript:String?=null
    private val probe=Runnable {probe()}
    private val deadline=Runnable {
        if(!disposed && foreground) {
            load.timeout(ticket)
            load.problem?.let {
                state.error=PageError(if(it==PageLoadProblem.EMPTY)PageErrorKind.EMPTY else PageErrorKind.TIMEOUT,activeUrl)
                state.loading=false;state.pageStage=PageStage.FAILED
                handler.removeCallbacks(probe)
                // A late form may recover an EMPTY page without replaying a form POST.
                if(state.authenticating && load.canProbe)scheduleProbe()
            }
        }
    }

    fun requested(web:WebView,url:String) {started(web,url);state.pageStage=PageStage.REQUESTED}

    fun started(web:WebView,url:String?) {
        if(disposed)return
        handler.removeCallbacksAndMessages(null)
        probePending=false;visualConfirmed=false;visualPending=false;visualUnavailable=false;visualAttempt++
        lastContentPresent=false;lastInteractive=false;documentCommitted=false
        state.loginProbePending=false
        view=web;activeUrl=url;ticket=load.begin()
        state.currentUrl=url;state.authenticating=AuthenticationPolicy.isAuthenticationUrl(url)
        if(state.authenticating)state.loginDiagnostics.beginDocument()
        state.error=null;state.loading=true;state.progress=0;state.pageStage=PageStage.STARTED
        if(foreground)handler.postDelayed(deadline,30_000)
    }

    fun committed(url:String?) {
        if(disposed || view==null || UrlPolicy.decide(url)!=UrlPolicy.Decision.LOAD_IN_APP)return
        // Redirect chains need not emit onPageStarted for every final URL.
        activeUrl=url;state.currentUrl=url
        state.authenticating=AuthenticationPolicy.isAuthenticationUrl(url)
        documentCommitted=true;load.committed(ticket);state.pageStage=PageStage.COMMITTED
        probe()
    }

    fun historyChanged(web:WebView,url:String?) {
        if(disposed || url==activeUrl || UrlPolicy.decide(url)!=UrlPolicy.Decision.LOAD_IN_APP)return
        if(documentCommitted) {
            // pushState/replaceState does not start a new document, but can replace the form.
            started(web,url);committed(url)
        } else {
            activeUrl=url;state.currentUrl=url
            state.authenticating=AuthenticationPolicy.isAuthenticationUrl(url)
        }
    }

    fun setForeground(value:Boolean) {
        foreground=value
        handler.removeCallbacks(probe);handler.removeCallbacks(deadline)
        if(value && !disposed && view!=null) {
            if(load.waiting)handler.postDelayed(deadline,30_000)
            probe()
        }
    }

    /** A controlled renderer comparison; never reloads or mutates the document. */
    fun retryRendering(web:WebView):Boolean {
        if(disposed || !foreground || view!==web || !load.canProbe || state.error!=null ||
            !state.authenticating || !AuthenticationPolicy.isAuthenticationUrl(web.url) ||
            state.loginRenderingRetried || state.pageStage !in setOf(PageStage.INTERACTION,PageStage.DRAWING))return false
        state.loginRenderingRetried=true
        WebViewFactory.configureDocumentRendering(web,web.url,normalAuthenticationRendering=true)
        visualConfirmed=false;visualPending=false;visualUnavailable=false;visualAttempt++
        web.requestLayout();web.postInvalidateOnAnimation()
        requestVisualState();probe()
        return true
    }

    private fun scheduleProbe() {
        handler.removeCallbacks(probe)
        if(foreground && !disposed)handler.postDelayed(probe,if(load.waiting)750 else 2_000)
    }

    private fun presentedStage() {
        state.pageStage=when {
            !visualConfirmed && !visualUnavailable->PageStage.DRAWING
            visualConfirmed && lastInteractive->PageStage.READY
            else->PageStage.INTERACTION
        }
    }

    private fun requestVisualState() {
        val web=view?:return
        if(visualConfirmed || visualPending || visualUnavailable)return
        val document=ticket
        val attempt=++visualAttempt
        visualPending=true
        try {
            // Presence must trigger the first frame. A hit-test is not a prerequisite for drawing.
            web.postVisualStateCallback(document,object:WebView.VisualStateCallback() {
                override fun onComplete(requestId:Long) {
                    if(disposed || document!=ticket || attempt!=visualAttempt)return
                    visualPending=false
                    if(!load.canProbe || !lastContentPresent)return
                    visualConfirmed=true
                    web.requestLayout();web.postInvalidateOnAnimation()
                    presentedStage()
                }
            })
        } catch(_:RuntimeException) {
            visualPending=false;visualUnavailable=true
            web.requestLayout();web.postInvalidateOnAnimation()
            // A drawing-health failure must not conceal an existing document.
            if(lastContentPresent)presentedStage()
        }
    }

    private fun accept(contentPresent:Boolean,interactive:Boolean=contentPresent) {
        val wasWaiting=load.waiting
        lastContentPresent=contentPresent;lastInteractive=interactive
        load.observed(ticket,contentPresent)
        if(contentPresent) {
            // The native opaque error/loading UI must leave as soon as content is present.
            state.error=null;state.loading=false
            handler.removeCallbacks(deadline)
            if(state.authenticating) {presentedStage();requestVisualState()}
            else state.pageStage=PageStage.READY
        } else if(load.problem==null) {
            visualConfirmed=false;state.loading=true;state.pageStage=PageStage.CHECKING
            if(!wasWaiting)handler.postDelayed(deadline,10_000)
        }
        if(load.waiting || state.authenticating)scheduleProbe()
    }

    private fun probe() {
        val web=view?:return
        if(disposed || !foreground || !load.canProbe || probePending || UrlPolicy.decide(activeUrl)!=UrlPolicy.Decision.LOAD_IN_APP)return
        probePending=true
        val document=ticket
        val authentication=state.authenticating
        if(authentication)state.loginProbePending=true
        val check=if(authentication)authenticationScript?:web.context.assets.open("login_probe.js").bufferedReader().use {it.readText()}.also {authenticationScript=it} else VISIBLE_CONTENT
        try {
            web.evaluateJavascript(check) {result->
                if(disposed || document!=ticket)return@evaluateJavascript
                if(authentication)state.loginProbePending=false
                if(!load.canProbe) {probePending=false;return@evaluateJavascript}
                probePending=false
                if(authentication) {
                    val snapshot=LoginProbeSnapshot.decode(result)
                    state.loginDiagnostics.observed(snapshot)
                    if(snapshot.result!=LoginProbeSnapshot.Result.VALID) {
                        failed(activeUrl,PageErrorKind.GENERIC)
                        return@evaluateJavascript
                    }
                    accept(snapshot.contentPresent,snapshot.ready)
                } else accept(result=="true")
            }
        } catch(_:RuntimeException) {probePending=false;state.loginProbePending=false;failed(activeUrl,PageErrorKind.GENERIC)}
    }

    fun failed(url:String?,kind:PageErrorKind,httpStatus:Int?=null) {
        if(disposed || view==null)return
        if(url!=null && activeUrl!=null && url!=activeUrl)return
        load.failed(ticket,when(kind) {
            PageErrorKind.SSL->PageLoadProblem.SSL
            PageErrorKind.HTTP->PageLoadProblem.HTTP
            PageErrorKind.EMPTY->PageLoadProblem.EMPTY
            PageErrorKind.TIMEOUT->PageLoadProblem.TIMEOUT
            else->PageLoadProblem.NETWORK
        })
        handler.removeCallbacksAndMessages(null)
        state.loading=false;state.error=PageError(kind,url,httpStatus);state.pageStage=PageStage.FAILED
    }

    fun close() {
        if(disposed)return
        disposed=true;handler.removeCallbacksAndMessages(null);load.cancel();ticket++
        probePending=false;view=null;activeUrl=null;state.loading=false
        state.loginProbePending=false
    }

    private companion object {
        const val VISIBLE_CONTENT="""(function(){
          if(!document.body)return false;
          return Array.from(document.body.querySelectorAll('input,button,a,form,h1,h2,p,img,svg,canvas,iframe,label,[role="button"],[role="alert"]')).some(function(el){
            var r=el.getBoundingClientRect();if(r.width<=0||r.height<=0)return false;
            for(var n=el;n&&n.nodeType===1;n=n.parentElement){
              var s=getComputedStyle(n);if(n.hidden||n.getAttribute('aria-hidden')==='true'||s.display==='none'||s.visibility==='hidden'||s.opacity==='0')return false;
            }return true;
          });
        })();"""
    }
}
