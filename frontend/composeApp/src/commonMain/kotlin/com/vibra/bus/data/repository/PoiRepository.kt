package com.vibra.bus.data.repository

import com.vibra.bus.data.api.PoiApi
import com.vibra.bus.data.model.PoisResponse
import com.vibra.bus.util.ApiResult

class PoiRepository(private val api: PoiApi) {
    suspend fun getPois(lat: Double, lng: Double, category: String? = null): ApiResult<PoisResponse> =
        api.getPois(lat, lng, category)
}
