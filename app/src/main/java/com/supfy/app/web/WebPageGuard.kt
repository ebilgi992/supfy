package com.supfy.app.web

import android.os.Handler
import android.os.Looper
import android.webkit.WebView

/** Rendering health only: a boolean, no page text, field values, cookies, bridge or logging. */
class WebPageGuard(private val state: WebUiState) {
    private val handler=Handler(Looper.getMainLooper())
    private val load=PageLoadTracker()
    private var ticket=0L
    private var activeUrl: String?=null
    private var view: WebView?=null
    private var probePending=false
    private var disposed=false
    private val probe=Runnable {probe()}
    private val deadline=Runnable {
        load.timeout(ticket)
        if(load.problem!=null) {
            state.error=PageError(if(load.problem==PageLoadProblem.EMPTY)PageErrorKind.EMPTY else PageErrorKind.TIMEOUT,activeUrl)
            state.loading=false
            handler.removeCallbacks(probe)
        }
    }

    fun started(web: WebView,url: String?) {
        if(disposed)return
        handler.removeCallbacksAndMessages(null)
        probePending=false
        view=web;activeUrl=url;ticket=load.begin()
        state.currentUrl=url;state.error=null;state.loading=true;state.progress=0
        handler.postDelayed(deadline,30_000)
    }
    fun committed(url: String?) {
        if(!disposed && view!=null && url==activeUrl) {load.committed(ticket);probe()}
    }
    private fun probe() {
        val web=view?:return
        if(disposed || !load.canProbe || probePending || UrlPolicy.decide(activeUrl)!=UrlPolicy.Decision.LOAD_IN_APP)return
        probePending=true
        val document=ticket
        web.evaluateJavascript(VISIBLE_CONTENT) {result->
            if(document!=ticket)return@evaluateJavascript
            probePending=false
            load.observed(document,result=="true")
            if(load.waiting) {
                handler.removeCallbacks(probe);handler.postDelayed(probe,750)
            } else if(load.problem==null) {
                state.loading=false;handler.removeCallbacksAndMessages(null)
            }
        }
    }
    fun failed(url: String?,kind: PageErrorKind,httpStatus: Int?=null) {
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
        state.loading=false;state.error=PageError(kind,url,httpStatus)
    }
    fun close() {
        if(disposed)return
        disposed=true
        handler.removeCallbacksAndMessages(null)
        load.cancel();ticket++
        probePending=false;view=null;activeUrl=null;state.loading=false
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
