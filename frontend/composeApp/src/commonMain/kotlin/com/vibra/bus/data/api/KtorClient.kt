package com.vibra.bus.data.api

import com.vibra.bus.util.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createHttpClient(): HttpClient

fun createKtorClient(settings: AppSettings): HttpClient {
    // Read the token fresh on every request — avoids the Ktor bearer-plugin
    // internal cache sticking to a stale/null token across logout + re-login.
    val bearerPlugin = createClientPlugin("FreshBearer") {
        onRequest { request, _ ->
            val token = settings.token
            val path = request.url.pathSegments
            if (token.isNotBlank() &&
                !path.contains("login") &&
                !path.contains("google")
            ) {
                request.headers["Authorization"] = "Bearer $token"
            }
        }
    }

    return createHttpClient().config {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(bearerPlugin)
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.BODY
        }
    }
}
