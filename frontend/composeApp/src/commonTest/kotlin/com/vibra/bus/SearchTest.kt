package com.vibra.bus

import com.vibra.bus.presentation.components.matchesQuery
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTest {
    @Test fun ignoresCaseAndAccents() {
        assertTrue(matchesQuery("cabecera", "Parada Cabecera del Llano"))
        assertTrue(matchesQuery("CAÑAV", "Cañaveral"))
        assertTrue(matchesQuery("canav", "Cañaveral"))
        assertTrue(matchesQuery("", "cualquier cosa"))
        assertFalse(matchesQuery("zzz", "Parada A", "BT401"))
        assertTrue(matchesQuery("bt4", "Parada A", "BT401"))
    }
}
