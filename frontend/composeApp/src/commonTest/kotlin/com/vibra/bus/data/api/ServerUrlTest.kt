package com.vibra.bus.data.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerUrlTest {

    private fun valid(input: String, cleartext: Boolean = true): String {
        val r = normalizeServerUrl(input, cleartext)
        assertTrue(r is ServerUrlResult.Valid, "esperaba válida: $input -> $r")
        return (r as ServerUrlResult.Valid).url
    }

    private fun invalid(input: String, cleartext: Boolean = true) {
        assertTrue(normalizeServerUrl(input, cleartext) is ServerUrlResult.Invalid, "esperaba inválida: $input")
    }

    @Test fun domainWithoutSchemeGetsHttpsAndSuffix() =
        assertEquals("https://api.ejemplo.com/api/v1", valid("api.ejemplo.com"))

    @Test fun localHostsDefaultToHttp() {
        assertEquals("http://192.168.1.20:8000/api/v1", valid("192.168.1.20:8000"))
        assertEquals("http://localhost:8000/api/v1", valid("localhost:8000"))
        assertEquals("http://10.0.2.2:8000/api/v1", valid("10.0.2.2:8000"))
    }

    @Test fun suffixIsNotDuplicatedAndSlashesTrimmed() {
        assertEquals("https://a.com/api/v1", valid("https://a.com/api/v1/"))
        assertEquals("https://a.com/api/v1", valid("HTTPS://A.com/api"))
        assertEquals("https://a.com/base/api/v1", valid("a.com/base"))
    }

    @Test fun releaseRejectsCleartext() {
        invalid("http://a.com", cleartext = false)
        invalid("192.168.1.20:8000", cleartext = false)
        assertEquals("https://a.com/api/v1", valid("https://a.com", cleartext = false))
    }

    @Test fun rejectsGarbage() {
        invalid("")
        invalid("   ")
        invalid("ftp://a.com")
        invalid("a b.com")
        invalid("a.com:99999")
        invalid("a.com:abc")
        invalid("user@a.com")
        invalid("-a.com")
    }
}
