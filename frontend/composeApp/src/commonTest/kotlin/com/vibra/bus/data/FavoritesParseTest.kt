package com.vibra.bus.data

import com.vibra.bus.data.api.FavoriteStopsResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FavoritesParseTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test fun parsesBackendPayloadWithExtraFields() {
        val r = json.decodeFromString(
            FavoriteStopsResponse.serializer(),
            """{"success":true,"message":"ok","data":[{"id":7,"name":"A","latitude":7.1,"longitude":-73.1},{"id":9}]}""",
        )
        assertEquals(listOf(7, 9), r.data?.map { it.id })
    }

    @Test fun missingDataIsNull() {
        val r = json.decodeFromString(FavoriteStopsResponse.serializer(), """{"success":true}""")
        assertNull(r.data)
    }
}
