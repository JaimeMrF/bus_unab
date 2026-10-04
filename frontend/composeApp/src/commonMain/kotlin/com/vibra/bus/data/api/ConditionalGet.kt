package com.vibra.bus.data.api

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Cache en memoria de respuestas con ETag (catalogo de buses y paradas, con max-age de 60 s en el
 * servidor). Se vacia al cerrar sesion o cambiar de organizacion: el contenido es del tenant.
 */
object ConditionalCache {
    class Entry(val etag: String, val body: String)

    private val entries = HashMap<String, Entry>()

    fun get(url: String): Entry? = entries[url]

    fun put(url: String, etag: String, body: String) {
        entries[url] = Entry(etag, body)
    }

    fun clear() = entries.clear()
}

@PublishedApi
internal val conditionalJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * GET con If-None-Match: si el servidor responde 304 se reutiliza el cuerpo guardado (sin
 * descargar ni decodificar de nuevo la red); si responde 2xx con ETag se guarda para la proxima.
 */
suspend inline fun <reified T> HttpClient.getConditional(
    url: String,
    noinline configure: HttpRequestBuilder.() -> Unit = {},
): T {
    val cached = ConditionalCache.get(url)
    val response = get(url) {
        configure()
        if (cached != null) header(HttpHeaders.IfNoneMatch, cached.etag)
    }
    if (response.status == HttpStatusCode.NotModified && cached != null) {
        return conditionalJson.decodeFromString<T>(cached.body)
    }
    val text = response.bodyAsText()
    if (response.status.isSuccess()) {
        response.headers[HttpHeaders.ETag]?.let { ConditionalCache.put(url, it, text) }
    }
    return conditionalJson.decodeFromString<T>(text)
}
