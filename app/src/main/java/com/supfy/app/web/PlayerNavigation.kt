package com.supfy.app.web

/** Public web URLs only; no intent extras, private APIs or subscription bypass. */
object PlayerNavigation {
    const val LOGIN_URL = "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F"
    const val SIGNUP_URL = "https://www.spotify.com/signup/"
    private val contentLink = Regex("^spotify:(track|album|playlist|episode|artist):([A-Za-z0-9]{22})$")

    fun webUrl(url: String?): String? {
        if(url=="spotify:signup")return SIGNUP_URL
        val match = url?.let(contentLink::matchEntire) ?: return null
        return "https://open.spotify.com/${match.groupValues[1]}/${match.groupValues[2]}"
    }
}
