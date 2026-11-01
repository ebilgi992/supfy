package com.supfy.app.web

import java.net.URI

/** One short-lived grant from interaction on the official auth page, not arbitrary popups. */
class AuthRedirectGate {
    private var expiresAt=Long.MIN_VALUE
    fun arm(source:String?,now:Long) {if(AuthenticationPolicy.isAuthenticationUrl(source))expiresAt=now+15_000}
    fun clear(){expiresAt=Long.MIN_VALUE}
    fun consume(url:String?,now:Long,hasGesture:Boolean):Boolean {
        if(!isProvider(url))return false
        val allowed=hasGesture || (expiresAt!=Long.MIN_VALUE && now>=0 && now in (expiresAt-15_000)..expiresAt)
        if(allowed)clear()
        return allowed
    }
    companion object {
        fun isProvider(url:String?):Boolean=runCatching {
            val u=URI(url.orEmpty())
            u.scheme=="https" && u.userInfo==null && u.port in listOf(-1,443) &&
                u.host?.lowercase() in setOf("accounts.google.com","appleid.apple.com","www.facebook.com","m.facebook.com","facebook.com")
        }.getOrDefault(false)
    }
}
