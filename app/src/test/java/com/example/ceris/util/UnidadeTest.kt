package com.example.ceris.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unidade do estoque: a Core manda o enum, a tela mostra a abreviacao. */
class UnidadeTest {

    @Test
    fun unidade_da_core_vira_abreviacao_da_tela() {
        assertEquals("un", unidadeNaTela("unit"))
        assertEquals("un", unidadeNaTela(" Unidades "))
        assertEquals("kg", unidadeNaTela("KG"))
        assertEquals("ml", unidadeNaTela("ml"))
        assertNull(unidadeNaTela(""))
        assertNull(unidadeNaTela(null))
    }
}
