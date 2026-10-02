package com.vibra.bus.data.repository

import com.vibra.bus.data.api.BrandApi
import com.vibra.bus.data.api.DEFAULT_ORG_SLUG
import com.vibra.bus.domain.brand.BrandConfig
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Fuente única de la marca activa. Orden de resolución:
 * API -> último BrandConfig cacheado (offline) -> tema neutro embebido.
 */
class BrandRepository(
    private val api: BrandApi,
    private val settings: AppSettings,
    /** Solo desarrollo: resuelve el slug "demo" localmente sin backend (API real ya disponible). */
    private val useMockForDemo: Boolean = false,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        if (settings.orgSlug.isBlank() && DEFAULT_ORG_SLUG.isNotBlank()) settings.orgSlug = DEFAULT_ORG_SLUG
    }

    private val _brand = MutableStateFlow(loadCached() ?: BrandConfig.Neutral)
    val brand: StateFlow<BrandConfig> = _brand.asStateFlow()

    val orgSlug: String get() = settings.orgSlug
    val hasOrganization: Boolean get() = settings.orgSlug.isNotBlank()

    private fun loadCached(): BrandConfig? {
        val raw = settings.brandJson
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString(BrandConfig.serializer(), raw) }.getOrNull()
    }

    /** Valida el código de organización contra la API y, si existe, lo fija como activo. */
    suspend fun selectOrganization(rawCode: String, serverUrl: String? = null): ApiResult<BrandConfig> {
        val slug = normalizeSlug(rawCode)
        if (slug.isEmpty()) return ApiResult.HttpError(422, "Ingresa el código de tu organización")
        // Servidor de prueba: se aplica solo mientras valida y se revierte si no responde.
        val previousServer = settings.serverUrl
        if (serverUrl != null) settings.serverUrl = serverUrl
        val outcome = selectOrganizationInternal(slug)
        if (serverUrl != null && outcome !is ApiResult.Success) settings.serverUrl = previousServer
        return outcome
    }

    private suspend fun selectOrganizationInternal(slug: String): ApiResult<BrandConfig> {
        return when (val result = fetch(slug)) {
            is ApiResult.Success -> {
                settings.orgSlug = slug
                apply(result.data)
                result
            }
            is ApiResult.HttpError -> when (result.code) {
                404 -> ApiResult.HttpError(404, "No encontramos esa organización")
                429 -> ApiResult.HttpError(429, "Demasiados intentos. Espera un momento.")
                else -> result
            }
            is ApiResult.NetworkError -> result
        }
    }

    /** Refresca la marca de la organización ya elegida; ante fallo conserva la cacheada. */
    suspend fun refresh() {
        val slug = settings.orgSlug
        if (slug.isBlank()) return
        val result = fetch(slug)
        if (result is ApiResult.Success && result.data.version != _brand.value.version) apply(result.data)
    }

    /**
     * El backend es la fuente de verdad del tenant del usuario: si el slug de /auth/... difiere
     * del activo, se adopta y se recarga la marca.
     */
    suspend fun adoptOrganization(slug: String?) {
        val clean = slug?.let { normalizeSlug(it) }.orEmpty()
        if (clean.isEmpty() || clean == settings.orgSlug) return
        settings.orgSlug = clean
        val result = fetch(clean)
        if (result is ApiResult.Success) apply(result.data)
    }

    /** Servidor guardado por el usuario (vacío = el del build). */
    val serverUrl: String get() = settings.serverUrl

    fun clearOrganization() {
        settings.orgSlug = ""
        settings.brandJson = ""
        _brand.value = BrandConfig.Neutral
    }

    private fun apply(config: BrandConfig) {
        settings.brandJson = json.encodeToString(BrandConfig.serializer(), config)
        _brand.value = config
    }

    private suspend fun fetch(slug: String): ApiResult<BrandConfig> {
        if (useMockForDemo && slug == BrandConfig.Demo.slug) return ApiResult.Success(BrandConfig.Demo)
        return when (val r = api.getBranding(slug)) {
            is ApiResult.Success -> r.data.data?.let { ApiResult.Success(it) }
                ?: ApiResult.HttpError(
                    if (r.data.status in 400..599) r.data.status else 404,
                    "No se pudo cargar la organización",
                )
            is ApiResult.HttpError -> r
            is ApiResult.NetworkError -> r
        }
    }

    private fun normalizeSlug(raw: String): String =
        raw.trim().lowercase().filter { (it in 'a'..'z') || (it in 'A'..'Z') || (it in '0'..'9') || it == '-' || it == '_' }.take(60)
}
