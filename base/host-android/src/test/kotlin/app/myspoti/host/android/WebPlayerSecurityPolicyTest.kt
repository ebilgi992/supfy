package app.myspoti.host.android

import org.junit.Assert.*
import org.junit.Test

class WebPlayerSecurityPolicyTest {
    @Test fun previousNativeRequestsLoseTheirAuthorityAcrossLogout() {
        val requests = NativeRequestEpoch()
        val beforeLogout = requests.current()
        assertTrue(requests.isCurrent(beforeLogout))
        requests.invalidate()
        assertFalse(requests.isCurrent(beforeLogout))
        assertTrue(requests.isCurrent(requests.current()))
    }

    @Test fun stalePageCannotRegainAuthorityAfterSeveralNavigations() {
        val requests = NativeRequestEpoch()
        val first = requests.current()
        requests.invalidate()
        val second = requests.current()
        requests.invalidate()
        assertFalse(requests.isCurrent(first))
        assertFalse(requests.isCurrent(second))
    }

    @Test fun bridgeRequiresExactPlayerOriginAndMainFrame() {
        assertTrue(WebPlayerSecurityPolicy.acceptMessage("https://open.spotify.com", true))
        assertTrue(WebPlayerSecurityPolicy.acceptMessage("https://open.spotify.com:443", true))
        assertFalse(WebPlayerSecurityPolicy.acceptMessage("https://open.spotify.com", false))
        for (origin in listOf("http://open.spotify.com", "https://open.spotify.com:444", "https://user@open.spotify.com", "https://open.spotify.com.evil.test", "https://accounts.spotify.com", "file:///open.spotify.com", "https://open.spotify.com.", "https://open.spotify.com\\@evil.test")) {
            assertFalse(origin, WebPlayerSecurityPolicy.acceptMessage(origin, true))
        }
    }

    @Test fun protectedMediaRequiresTrustedOriginAndResource() {
        val protected = "android.webkit.resource.PROTECTED_MEDIA_ID"
        assertEquals(listOf(protected), WebPlayerSecurityPolicy.allowedPermissions("https://open.spotify.com", listOf(protected, "android.webkit.resource.VIDEO_CAPTURE")))
        for (origin in listOf("https://accounts.spotify.com", "https://open.spotify.com:444", "https://user@open.spotify.com", "https://evil.test")) {
            assertTrue(origin, WebPlayerSecurityPolicy.allowedPermissions(origin, listOf(protected)).isEmpty())
        }
    }

    @Test fun providerNavigationNeedsKnownHostAndUserInitiatedLogin() {
        assertTrue(WebPlayerSecurityPolicy.allowNavigation("https://accounts.spotify.com/login", true, false))
        assertFalse(WebPlayerSecurityPolicy.allowNavigation("https://accounts.google.com/signin", true, false))
        assertTrue(WebPlayerSecurityPolicy.allowNavigation("https://accounts.google.com/signin", true, true))
        for (url in listOf("https://mail.google.com/", "https://evil.facebook.com/", "https://apple.com/", "http://accounts.google.com/", "https://user@accounts.google.com/", "https://accounts.google.com:444/")) {
            assertFalse(url, WebPlayerSecurityPolicy.allowNavigation(url, true, true))
        }
        assertTrue(WebPlayerSecurityPolicy.allowNavigation("https://www.facebook.com/login", true, true))
        assertTrue(WebPlayerSecurityPolicy.allowNavigation("https://appleid.apple.com/auth", true, true))
        assertFalse(WebPlayerSecurityPolicy.allowNavigation("file:///tmp/local", false, true))
    }

    @Test fun loginClassificationDoesNotTrustPathAlone() {
        assertTrue(WebPlayerSecurityPolicy.isLoginPage("https://accounts.spotify.com/login"))
        assertTrue(WebPlayerSecurityPolicy.isLoginPage("https://open.spotify.com/intl-tr/login"))
        assertTrue(WebPlayerSecurityPolicy.isLoginPage("https://accounts.google.com/signin"))
        for (url in listOf("https://evil.test/login", "https://open.spotify.com/album/id?continue=/login", "http://accounts.spotify.com/login", "https://user@accounts.spotify.com/login")) {
            assertFalse(url, WebPlayerSecurityPolicy.isLoginPage(url))
        }
    }
}
