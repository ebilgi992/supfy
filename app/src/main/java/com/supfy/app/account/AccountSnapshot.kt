package com.supfy.app.account

enum class AccountStatus { UNKNOWN, GUEST, AUTHENTICATED }

data class AccountSnapshot(
    val status: AccountStatus = AccountStatus.UNKNOWN,
    val displayName: String = "",
)

/** Public visible profile name only. Never accepts an email/token-looking value. */
object AccountNames {
    fun clean(value: String?): String {
        val text=value.orEmpty().replace(Regex("[\\p{Cc}\\u202a-\\u202e\\u2066-\\u2069]"),"")
            .trim().replace(Regex("\\s+")," ")
        if(text.isEmpty() || text.length>80 || '@' in text || Regex("https?:|token|bearer",RegexOption.IGNORE_CASE).containsMatchIn(text))return ""
        if(text.lowercase() in setOf("your profile","profile","profil","profilin","profiliniz","hesap","account","open user menu","kullanıcı menüsünü aç","user menu","hesabını aç","hesabı aç","hesabım","hesabınız"))return ""
        return text
    }
}
