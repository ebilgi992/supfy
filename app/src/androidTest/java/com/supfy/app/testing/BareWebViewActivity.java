package com.supfy.app.testing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

/** Test-APK control with only framework dependencies; no Supfy or Kotlin application runtime. */
public final class BareWebViewActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean javascript = getIntent().getBooleanExtra("javascript", false);
        Log.i("SupfyBareWebView", "Application=" + getApplication().getClass().getName()
                + "; JavaScript=" + javascript + "; provider=" + WebView.getCurrentWebViewPackage().versionName);
        WebView web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(javascript);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageCommitVisible(WebView view, String url) {
                Log.i("SupfyBareWebView", "document committed");
            }
            @Override public void onPageFinished(WebView view, String url) {
                Log.i("SupfyBareWebView", "document finished");
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                Log.e("SupfyBareWebView", "renderer exited; crashed=" + detail.didCrash());
                TextView message = new TextView(BareWebViewActivity.this);
                message.setText("Bare WebView renderer exited");
                setContentView(message);
                view.destroy();
                return true;
            }
        });
        setContentView(web);
        web.loadDataWithBaseURL(null, "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='background:#fff;color:#111;font:20px sans-serif;padding:16px'>"
                + "<h1>Bare WebView</h1><label>Email<input type='email'></label><button>Continue</button>"
                + "<p id='status'>JavaScript disabled</p>"
                + "<script>document.getElementById('status').textContent='JavaScript enabled';</script>"
                + "</body></html>", "text/html", "UTF-8", null);
    }
}
