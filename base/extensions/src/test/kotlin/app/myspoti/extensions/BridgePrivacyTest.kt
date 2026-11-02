package app.myspoti.extensions

import org.junit.Assert.*
import org.junit.Test

class BridgePrivacyTest {
    @Test fun nestedCredentialsAreRemovedBeforeTheBridge() {
        val input = """{"track":"ok","nested":[{"accessToken":"SECRET","refresh_token":"SECRET","name":"music"}],"Cookie":"SECRET"}"""
        val safe = BridgePrivacy.body(input)
        assertFalse(safe.contains("SECRET"))
        assertTrue(safe.contains("music"))
    }
    @Test fun malformedOrNonJsonRawResponsesAreNotBridged() {
        assertEquals("", BridgePrivacy.body("Bearer SECRET"))
        assertEquals("", BridgePrivacy.body("<script>token=SECRET</script>"))
        assertEquals("", BridgePrivacy.body("\"SECRET\""))
        val safe = BridgePrivacy.body("""{"id_token":"SECRET","client_secret":"SECRET","apiKey":"SECRET","credential":"SECRET","csrf":"SECRET","name":"ok"}""")
        assertFalse(safe.contains("SECRET"))
        assertTrue(safe.contains("ok"))
    }

    @Test fun rawAuthEndpointsAreRefused() {
        for (url in listOf("https://accounts.spotify.com/api/token", "https://open.spotify.com/get_access_token", "https://clienttoken.spotify.com/v1/clienttoken", "https://open.spotify.com/get_access_%74oken", "https://user:secret@api.spotify.com/v1/me")) {
            try { RawRequest.check("GET", url); fail() } catch (_: IllegalArgumentException) { }
        }
    }
}
