package app.myspoti.extensions

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Credential fields never cross the native bridge into extension JavaScript. */
object BridgePrivacy {
    fun sensitiveKey(key: String): Boolean {
        val normalized = key.lowercase().filter { it.isLetterOrDigit() }
        return normalized in setOf("token", "accesstoken", "refreshtoken", "clienttoken", "authorization", "cookie", "setcookie", "password", "credentials", "spdc", "spkey", "idtoken", "secret", "clientsecret", "apikey", "key", "csrf", "credential") ||
            normalized.endsWith("token") || normalized.endsWith("secret") || normalized.contains("credential") || normalized.contains("password") || normalized.contains("cookie")
    }
    fun sanitize(value: JsonElement): JsonElement = when (value) {
        is JsonObject -> JsonObject(value.filterKeys { !sensitiveKey(it) }.mapValues { sanitize(it.value) })
        is JsonArray -> JsonArray(value.map(::sanitize))
        else -> value
    }
    fun body(value: String): String = runCatching {
        val parsed = ExtJson.parseToJsonElement(value)
        if (parsed is JsonObject || parsed is JsonArray) sanitize(parsed).toString() else ""
    }.getOrDefault("")
}
