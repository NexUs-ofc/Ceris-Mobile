package com.example.ceris.local

import com.example.ceris.support.jwt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JwtProfileTest {
    @Test
    fun leSubjectInteiroPositivo() {
        assertEquals(12, JwtProfile.fromAccessToken(jwt()))
    }

    @Test
    fun rejeitaSubjectInvalidoSemDerrubarApp() {
        listOf("0", "-1", "abc", "1.5", "999999999999999999999").forEach {
            assertNull(JwtProfile.fromAccessToken(jwt(it)))
        }
        listOf("", "token", "a.!.c", "a.e30.c").forEach {
            assertNull(JwtProfile.fromAccessToken(it))
        }
    }
}
