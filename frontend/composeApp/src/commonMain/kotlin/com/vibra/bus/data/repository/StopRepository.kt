package com.vibra.bus.data.repository

import com.vibra.bus.data.api.StopApi
import com.vibra.bus.data.model.StopsResponse
import com.vibra.bus.util.ApiResult

class StopRepository(private val api: StopApi) {
    suspend fun getStops(): ApiResult<StopsResponse> = api.getStops()
}
