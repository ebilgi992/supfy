package com.supfy.app.quantum

import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.supfy.app.MainActivity
import com.supfy.app.testing.WebViewTestActivity
import com.supfy.app.ui.quantum.QuantumLoginScreen
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class QuantumNativeUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test(timeout = 90_000)
    fun largeFontNativeWelcomeScrollReachesLoginSignupAndContinueCallbacks() {
        val login = AtomicInteger()
        val signup = AtomicInteger()
        val guest = AtomicInteger()
        val scenario = ActivityScenario.launch(WebViewTestActivity::class.java)
        var owner: TestOwner? = null
        try {
            scenario.onActivity { activity ->
                val currentOwner = TestOwner()
                owner = currentOwner
                activity.container.setViewTreeLifecycleOwner(currentOwner)
                activity.container.setViewTreeSavedStateRegistryOwner(currentOwner)
                val compose = ComposeView(activity)
                activity.container.addView(compose, android.view.ViewGroup.LayoutParams(-1, -1))
                compose.setContent {
                    val density = LocalDensity.current.density
                    CompositionLocalProvider(LocalDensity provides Density(density, 1.8f)) {
                        MaterialTheme {
                            QuantumLoginScreen(false, null, { login.incrementAndGet() },
                                { signup.incrementAndGet() }, { guest.incrementAndGet() })
                        }
                    }
                }
            }
            assertTrue("Login must remain reachable at 1.8x font scale", clickWithScroll("E-posta ile giriş yap"))
            assertTrue("Signup must remain reachable at 1.8x font scale", clickWithScroll("Hesap oluştur"))
            assertTrue("Guest action must remain reachable at 1.8x font scale", clickWithScroll("Giriş yapmadan devam et"))
            assertEquals(1, login.get())
            assertEquals(1, signup.get())
            assertEquals(1, guest.get())
        } finally {
            scenario.onActivity { owner?.stop() }
            scenario.close()
        }
    }

    @Test(timeout = 90_000)
    fun productionBottomBarSettingsDiagnosticsAndBackKeepNativeNavigationReachable() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            Thread.sleep(1_000)
            assumeTrue("BLOCKED: welcome overlay is active; this test preserves existing app preferences and does not acknowledge it",
                find("E-posta ile giriş yap") == null)
            for (label in listOf("Ana sayfa", "Ara", "Kitaplığın", "Ayarlar")) {
                assertTrue("Production native destination must be visible: $label", waitVisible(label))
            }
            assertTrue(clickWithScroll("Ayarlar"))
            assertTrue("Settings must expose real diagnostics action", clickWithScroll("Durumu kontrol et"))
            assertTrue("Actual production diagnostics sheet must open", waitVisible("Oynatıcı ve ses"))
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            assertTrue("Back must restore settings", waitVisible("Durumu kontrol et"))
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            assertTrue("Back must restore native player navigation", waitVisible("Ana sayfa"))
        } finally { scenario.close() }
    }

    private fun roots(): List<AccessibilityNodeInfo> {
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        return (listOfNotNull(automation.rootInActiveWindow) + automation.windows.mapNotNull { it.root })
            .filter { it.packageName?.toString() == "com.supfy.app" }
    }

    private fun find(label: String): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (node.isVisibleToUser && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { visit(it)?.let { found -> return found } }
            return null
        }
        return roots().firstNotNullOfOrNull { visit(it) }
    }

    private fun waitVisible(label: String): Boolean {
        repeat(32) { if (find(label) != null) return true; SystemClock.sleep(200) }
        return false
    }

    private fun clickWithScroll(label: String): Boolean {
        repeat(18) {
            val target = find(label)
            if (target != null) {
                var action: AccessibilityNodeInfo? = target
                while (action != null && !action.isClickable) action = action.parent
                if (action?.isEnabled == true && action.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    instrumentation.waitForIdleSync()
                    return true
                }
            }
            fun scroll(node: AccessibilityNodeInfo): Boolean {
                if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
                for (i in 0 until node.childCount) node.getChild(i)?.let { if (scroll(it)) return true }
                return false
            }
            roots().any { scroll(it) }
            SystemClock.sleep(200)
        }
        return false
    }

    private class TestOwner : SavedStateRegistryOwner, LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        private val saved = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry get() = saved.savedStateRegistry
        init { saved.performAttach(); saved.performRestore(null); registry.currentState = Lifecycle.State.RESUMED }
        fun stop() { registry.currentState = Lifecycle.State.DESTROYED }
    }
}
