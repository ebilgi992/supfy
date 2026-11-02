package android.content

import java.io.File

/** System boundary fixture; production history code and filesystem operations stay real. */
open class Context(val filesDir: File) {
    open val cacheDir: File get() = File(filesDir, "cache")
    open fun getExternalFilesDir(type: String?): File? = File(filesDir, "external")
    companion object { const val MODE_PRIVATE = 0 }
    private val preferences = mutableMapOf<String, Preferences>()
    open fun getSharedPreferences(name: String, mode: Int): Preferences = preferences.getOrPut(name) { Preferences() }
}

class Preferences {
    val values = mutableMapOf<String, Any?>()
    val all: Map<String, Any?> get() = values.toMap()
    fun contains(key: String) = values.containsKey(key)
    fun getString(key: String, fallback: String?): String? = values[key] as? String ?: fallback
    @Suppress("UNCHECKED_CAST")
    fun getStringSet(key: String, fallback: Set<String>?): Set<String>? = values[key] as? Set<String> ?: fallback
    fun edit(): Editor = Editor()
    inner class Editor {
        fun putString(key: String, value: String): Editor = apply { values[key] = value }
        fun putStringSet(key: String, value: Set<String>): Editor = apply { values[key] = value }
        fun clear(): Editor = apply { values.clear() }
        fun remove(key: String): Editor = apply { values.remove(key) }
        fun apply() = Unit
    }
}
