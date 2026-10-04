package com.vibra.bus.data.repository

import com.vibra.bus.util.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Paradas favoritas del usuario, guardadas en local (AppSettings). Hoy no hay API de favoritos;
 * cuando exista se sincroniza desde aqui sin tocar la UI.
 */
class FavoritesRepository(private val settings: AppSettings) {
    private val _ids = MutableStateFlow(parse(settings.favoriteStopsRaw))
    val ids: StateFlow<Set<Int>> = _ids.asStateFlow()

    fun toggle(stopId: Int) {
        val next = _ids.value.toMutableSet().apply { if (!add(stopId)) remove(stopId) }
        _ids.value = next
        settings.favoriteStopsRaw = next.joinToString(",")
    }

    fun clear() {
        _ids.value = emptySet()
        settings.favoriteStopsRaw = ""
    }

    private fun parse(raw: String): Set<Int> =
        raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
}
