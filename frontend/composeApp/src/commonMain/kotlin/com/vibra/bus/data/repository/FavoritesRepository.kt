package com.vibra.bus.data.repository

import com.vibra.bus.data.api.FavoriteStopsResponse
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

/** Operaciones remotas de favoritos; la implementacion real es [FavoritesApi]. */
interface FavoritesRemote {
    suspend fun list(): ApiResult<FavoriteStopsResponse>
    suspend fun add(stopId: Int): ApiResult<Unit>
    suspend fun remove(stopId: Int): ApiResult<Unit>
}

/** Persistencia local de favoritos y de los cambios aun no confirmados (ids separados por coma). */
interface FavoritesStorage {
    var ids: String
    var pendingAdds: String
    var pendingRemoves: String
}

class SettingsFavoritesStorage(private val settings: AppSettings) : FavoritesStorage {
    override var ids: String
        get() = settings.favoriteStopsRaw
        set(value) { settings.favoriteStopsRaw = value }
    override var pendingAdds: String
        get() = settings.favoritePendingAddsRaw
        set(value) { settings.favoritePendingAddsRaw = value }
    override var pendingRemoves: String
        get() = settings.favoritePendingRemovesRaw
        set(value) { settings.favoritePendingRemovesRaw = value }
}

/**
 * Paradas favoritas con sincronizacion. La UI siempre lee y escribe en local (respuesta
 * inmediata); la API es el respaldo entre dispositivos.
 *
 * Reglas ante la respuesta del servidor al subir un cambio pendiente:
 * - 2xx: confirmado, deja de estar pendiente.
 * - 401, 405 o sin red: no hay API utilizable ahora; se conserva todo (local y pendientes)
 *   y se reintenta en la siguiente sincronizacion (por ejemplo tras volver a iniciar sesion).
 * - 5xx: error transitorio del servidor; igual, se conserva y se reintenta.
 * - 404 al agregar: la parada no existe, esta inactiva o es de otro tenant (contrato del backend);
 *   es definitivo, se descarta de pendientes y se quita de la lista local en la siguiente sync.
 * - Otro 4xx (validacion, p. ej. 422): rechazo definitivo; se descarta para no reintentarlo siempre.
 * DELETE responde 204 siempre (idempotente), asi que cualquier 2xx confirma.
 */
class FavoritesRepository(
    private val storage: FavoritesStorage,
    private val remote: FavoritesRemote? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    constructor(settings: AppSettings, api: FavoritesApi) : this(
        SettingsFavoritesStorage(settings),
        object : FavoritesRemote {
            override suspend fun list() = api.list()
            override suspend fun add(stopId: Int) = api.add(stopId)
            override suspend fun remove(stopId: Int) = api.remove(stopId)
        },
    )

    private val lock = Mutex()

    private val _ids = MutableStateFlow(parse(storage.ids))
    val ids: StateFlow<Set<Int>> = _ids.asStateFlow()

    private var pendingAdds = parse(storage.pendingAdds).toMutableSet()
    private var pendingRemoves = parse(storage.pendingRemoves).toMutableSet()

    fun toggle(stopId: Int) {
        val nowFavorite = stopId !in _ids.value
        val next = _ids.value.toMutableSet().apply { if (nowFavorite) add(stopId) else remove(stopId) }
        _ids.value = next
        storage.ids = serialize(next)
        if (nowFavorite) { pendingRemoves.remove(stopId); pendingAdds.add(stopId) }
        else { pendingAdds.remove(stopId); pendingRemoves.add(stopId) }
        persistPending()
        syncAsync()
    }

    fun clear() {
        _ids.value = emptySet()
        storage.ids = ""
        pendingAdds.clear()
        pendingRemoves.clear()
        persistPending()
    }

    /** Sincroniza sin bloquear a quien llama (arranque de Home, tras cada cambio). */
    fun syncAsync() {
        if (remote == null) return
        scope.launch { sync() }
    }

    private enum class Outcome { Done, Discard, Retry }

    private fun classify(result: ApiResult<Unit>): Outcome = when (result) {
        is ApiResult.Success -> Outcome.Done
        is ApiResult.NetworkError -> Outcome.Retry
        is ApiResult.HttpError -> when {
            result.code == 401 || result.code == 405 -> Outcome.Retry
            result.code >= 500 -> Outcome.Retry
            result.code in 400..499 -> Outcome.Discard
            else -> Outcome.Retry
        }
    }

    suspend fun sync() {
        val remote = remote ?: return
        lock.withLock {
            // 1. Subir cambios pendientes. Con un resultado "reintentar" se corta todo: no se
            // adopta la lista del servidor (que aun no refleja los cambios) ni se pierde nada.
            for (id in pendingAdds.toList()) {
                when (classify(remote.add(id))) {
                    Outcome.Done, Outcome.Discard -> pendingAdds.remove(id)
                    Outcome.Retry -> { persistPending(); return }
                }
            }
            for (id in pendingRemoves.toList()) {
                when (classify(remote.remove(id))) {
                    Outcome.Done, Outcome.Discard -> pendingRemoves.remove(id)
                    Outcome.Retry -> { persistPending(); return }
                }
            }
            persistPending()
            // 2. Con todo subido, la lista del servidor es la verdad.
            val r = remote.list()
            if (r is ApiResult.Success) {
                val remoteIds = r.data.data?.map { it.id }?.toSet() ?: return
                _ids.value = remoteIds
                storage.ids = serialize(remoteIds)
            }
        }
    }

    private fun persistPending() {
        storage.pendingAdds = serialize(pendingAdds)
        storage.pendingRemoves = serialize(pendingRemoves)
    }

    private fun serialize(ids: Set<Int>) = ids.joinToString(",")

    private fun parse(raw: String): Set<Int> =
        raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
}
