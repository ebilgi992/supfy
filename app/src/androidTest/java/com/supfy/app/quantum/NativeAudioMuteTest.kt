package com.supfy.app.quantum

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Public native API state roundtrip on playing PCM. Does not measure acoustic output or silence. */
@RunWith(AndroidJUnit4::class)
class NativeAudioMuteTest {
    @Test(timeout = 90_000)
    fun supportedNativeMuteRoundTripRestoresOriginalStateOnRealPlayingAudio() {
        assumeTrue(
            "BLOCKED: installed Android System WebView does not support public MUTE_AUDIO; JavaScript .muted is not native/acoustic proof",
            WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)
        )
        LocalAudioFixture().use { fixture ->
            var original = false
            fixture.onMain { original = WebViewCompat.isAudioMuted(fixture.web) }
            try {
                fixture.nativePlayTap()
                fixture.assertAdvances("Real PCM before native mute")
                fixture.onMain {
                    WebViewCompat.setAudioMuted(fixture.web, true)
                    assertTrue("Public native getter must acknowledge mute", WebViewCompat.isAudioMuted(fixture.web))
                }
                fixture.assertAdvances("Native mute preserves playback clock")
                fixture.onMain {
                    WebViewCompat.setAudioMuted(fixture.web, false)
                    assertFalse("Public native getter must acknowledge unmute", WebViewCompat.isAudioMuted(fixture.web))
                }
                fixture.assertAdvances("Native unmute preserves playback clock")
            } finally {
                fixture.onMain {
                    WebViewCompat.setAudioMuted(fixture.web, original)
                    assertEquals("Restore original native mute state", original, WebViewCompat.isAudioMuted(fixture.web))
                }
            }
        }
    }
}
