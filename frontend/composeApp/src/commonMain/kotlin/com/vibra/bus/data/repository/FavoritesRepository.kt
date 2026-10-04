package com.vibra.bus.data.repository

import com.vibra.bus.data.api.FavoritesApi
import com.vibra.bus.util.ApiResult
import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Paradas favoritas con sincronizacion. La UI siempre lee y escribe en local (respuesta
 * inmediata); la API es el respaldo entre dispositivos:
 * - toggle: cambia en local y lo envia; si falla (sin red o endpoint ausente) queda pendiente.
 * - sync: sube los pendientes y, si el servidor responde, adopta su lista como verdad.
 * Si la API no existe (404/405) o la sesion no es valida, todo sigue funcionando solo en local.
 */
class FavoritesRepository(
    private val settings: AppSettings,
    private val api: FavoritesApi? = null,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    private val _ids = MutableStateFlow(parse(settings.favoriteStopsRaw))
    val ids: StateFlow<Set<Int>> = _ids.asStateFlow()

    private var pendingAdds = parse(settings.favoritePendingAddsRaw).toMutableSet()
    private var pendingRemoves = parse(settings.favoritePendingRemovesRaw).toMutableSet()

    fun toggle(stopId: Int) {
        val nowFavorite = stopId !in _ids.value
        val next = _ids.value.toMutableSet().apply { if (nowFavorite) add(stopId) else remove(stopId) }
        _ids.value = next
        settings.favoriteStopsRaw = serialize(next)
        if (nowFavorite) { pendingRemoves.remove(stopId); pendingAdds.add(stopId) }
        else { pendingAdds.remove(stopId); pendingRemoves.add(stopId) }
        persistPending()
        syncAsync()
    }

    fun clear() {
        _ids.value = emptySet()
        settings.favoriteStopsRaw = ""
        pendingAdds.clear()
        pendingRemoves.clear()
        persistPending()
    }

    /** Sincroniza sin bloquear a quien llama (arranque de Home, tras cada cambio). */
    fun syncAsync() {
        if (api == null) return
        scope.launch { sync() }
    }

    suspend fun sync() {
        val api = api ?: return
        lock.withLock {
            // 1. Subir cambios pendientes; los que el servidor acepta dejan de estar pendientes.
            for (id in pendingAdds.toList()) {
                when (val r = api.add(id)) {
                    is ApiResult.Success -> pendingAdds.remove(id)
                    is ApiResult.HttpError -> if (unavailable(r.code)) return
                    is ApiResult.NetworkError -> return
                }
            }
            for (id in pendingRemoves.toList()) {
                when (val r = api.remove(id)) {
                    is ApiResult.Success -> pendingRemoves.remove(id)
                    is ApiResult.HttpError -> if (unavailable(r.code)) return
                    is ApiResult.NetworkError -> return
                }
            }
            persistPending()
            // 2. Adoptar la lista del servidor (mas lo que aun este pendiente de subir).
            when (val r = api.list()) {
                is ApiResult.Success -> {
                    val remote = r.data.data?.map { it.id }?.toSet() ?: return
                    val merged = (remote + pendingAdds) - pendingRemoves
                    _ids.value = merged
                    settings.favoriteStopsRaw = serialize(merged)
                }
                else -> Unit
            }
        }
    }

    /** 401, 404 y 405: no hay API utilizable (sesion caduca o backend sin el endpoint): seguir en local. */
    private fun unavailable(code: Int) = code == 401 || code == 404 || code == 405

    private fun persistPending() {
        settings.favoritePendingAddsRaw = serialize(pendingAdds)
        settings.favoritePendingRemovesRaw = serialize(pendingRemoves)
    }

    private fun serialize(ids: Set<Int>) = ids.joinToString(",")

    private fun parse(raw: String): Set<Int> =
        raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
}
