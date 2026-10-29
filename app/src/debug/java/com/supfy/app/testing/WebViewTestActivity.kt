package com.supfy.app.testing

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout

/** Debug-only, non-exported host for instrumented WebView fixtures. Not a product screen. */
class WebViewTestActivity : Activity() {
    lateinit var container: FrameLayout
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container=FrameLayout(this)
        setContentView(container)
    }
}
