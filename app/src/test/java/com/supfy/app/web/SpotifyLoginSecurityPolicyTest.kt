package com.supfy.app.web

import org.junit.Assert.*
import org.junit.Test

/** Public login/bootstrap origin contracts; not a live phone or account authentication test. */
class SpotifyLoginSecurityPolicyTest {
    @Test fun officialLoginRedirectsAndBootstrapOriginsArePermitted() {
        val publicUrls=listOf(
            PlayerNavigation.LOGIN_URL,
            "https://accounts.spotify.com/en/login",
            "https://accounts.spotify.com/tr/login",
            "https://accounts.scdn.co/",
            "https://www.scdn.co/",
        )
        for(url in publicUrls)assertEquals(url,UrlPolicy.Decision.LOAD_IN_APP,UrlPolicy.decide(url))
    }

    @Test fun httpsChallengeFramesArePermittedWithoutGivingThemPlayerPrivileges() {
        for(url in listOf("https://www.google.com/recaptcha/enterprise/anchor","https://www.recaptcha.net/")) {
            assertTrue(url,UrlPolicy.allowSubframe(url))
            assertFalse(url,UrlPolicy.isTrustedPlayerOrigin(url))
        }
        assertFalse(UrlPolicy.isTrustedPlayerOrigin("https://accounts.spotify.com"))
        assertFalse(UrlPolicy.isTrustedPlayerOrigin("https://accounts.scdn.co"))
    }
}
