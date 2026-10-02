package com.vibra.bus.data.api

/**
 * Servidor de la API resuelto en runtime. Prioridad:
 * 1. URL elegida por el usuario (AppSettings) -> [override]
 * 2. URL del build (-PapiBaseUrl / BuildConfig) -> [BUILD_BASE_URL]
 */
object ServerConfig {
    /** Escrito por AppSettings al arrancar y al cambiar el servidor; vacío = usar el del build. */
    var override: String = ""

    val baseUrl: String get() = override.ifBlank { BUILD_BASE_URL }
}

/** URL base efectiva (sin barra final, termina en /api/v1). Se evalúa en cada petición. */
val BASE_URL: String get() = ServerConfig.baseUrl

sealed class ServerUrlResult {
    data class Valid(val url: String) : ServerUrlResult()
    data class Invalid(val reason: String) : ServerUrlResult()
}

private const val API_SUFFIX = "/api/v1"

private fun isLocalHost(host: String): Boolean {
    if (host == "localhost" || host.endsWith(".local")) return true
    val p = host.split('.')
    if (p.size != 4 || p.any { it.toIntOrNull() == null }) return false
    val a = p[0].toInt()
    val b = p[1].toInt()
    return a == 10 || a == 127 || (a == 192 && b == 168) || (a == 172 && b in 16..31)
}

/**
 * Normaliza lo que escribe el usuario: agrega esquema si falta (https; http para hosts de red
 * local), exige http/https, valida host y puerto, quita barras finales y fuerza el sufijo /api/v1.
 * [allowCleartext] es false en builds release (HTTPS-only).
 */
fun normalizeServerUrl(input: String, allowCleartext: Boolean): ServerUrlResult {
    var s = input.trim()
    if (s.isEmpty()) return ServerUrlResult.Invalid("Ingresa la dirección del servidor")
    if (s.any { it.isWhitespace() }) return ServerUrlResult.Invalid("La dirección no debe tener espacios")

    val explicit = s.substringBefore("://", "").lowercase()
    val hasScheme = s.contains("://")
    if (hasScheme && explicit != "http" && explicit != "https") {
        return ServerUrlResult.Invalid("Usa http:// o https://")
    }
    if (hasScheme) s = s.substringAfter("://")

    // host[:puerto][/ruta]
    val authority = s.substringBefore('/')
    var path = s.substring(authority.length).trimEnd('/')
    if (authority.isEmpty() || authority.contains('@')) return ServerUrlResult.Invalid("Dirección no válida")

    val host = authority.substringBefore(':').lowercase()
    val portText = if (authority.contains(':')) authority.substringAfter(':') else ""
    if (host.isEmpty() || !host.all { it.isLetterOrDigit() || it == '.' || it == '-' } ||
        host.startsWith('.') || host.endsWith('.') || host.startsWith('-')
    ) return ServerUrlResult.Invalid("Nombre de servidor no válido")
    if (portText.isNotEmpty()) {
        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) return ServerUrlResult.Invalid("Puerto no válido")
    }

    val scheme = when {
        hasScheme -> explicit
        isLocalHost(host) -> "http"
        else -> "https"
    }
    if (scheme == "http" && !allowCleartext) {
        return ServerUrlResult.Invalid("Esta versión solo acepta servidores HTTPS")
    }

    path = when {
        path.isEmpty() -> API_SUFFIX
        path.equals(API_SUFFIX, ignoreCase = true) -> API_SUFFIX
        path.equals("/api", ignoreCase = true) -> API_SUFFIX
        path.endsWith(API_SUFFIX, ignoreCase = true) -> path
        else -> path + API_SUFFIX
    }
    val hostPort = if (portText.isNotEmpty()) "$host:$portText" else host
    return ServerUrlResult.Valid("$scheme://$hostPort$path")
}
