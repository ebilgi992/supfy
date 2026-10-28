package com.sametozdemir.izlek.web

object UserAgents {

    private val chromeVersion = Regex("""Chrome/([\d.]+)""")

    /**
     * Tarayıcılardaki "Masaüstü sitesi" seçeneğinin karşılığı: aynı Chrome sürümüyle
     * masaüstü Linux tanımı üretir. Sürüm bulunamazsa genel bir değer kullanılır.
     */
    fun desktopFrom(defaultUserAgent: String?): String {
        val version = defaultUserAgent
            ?.let { chromeVersion.find(it)?.groupValues?.getOrNull(1) }
            ?: "130.0.0.0"
        return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/$version Safari/537.36"
    }
}
