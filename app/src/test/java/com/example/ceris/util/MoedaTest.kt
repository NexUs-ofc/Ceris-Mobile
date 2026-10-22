package com.example.ceris.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Valor digitado no item da lista de compras e o total exibido em reais. */
class MoedaTest {

    @Test
    fun interpreta_virgula_ponto_milhar_e_campo_vazio() {
        assertEquals(12.90, Moeda.interpretar("12,90"), 0.0)
        assertEquals(12.90, Moeda.interpretar("12.90"), 0.0)
        assertEquals(1234.56, Moeda.interpretar("1.234,56"), 0.0)
        assertEquals(5.0, Moeda.interpretar("R$ 5"), 0.0)
        assertEquals(0.0, Moeda.interpretar(""), 0.0)
        assertEquals(0.0, Moeda.interpretar(null), 0.0)
    }

    @Test
    fun formata_em_reais_com_espaco_normal() {
        // Espaco comum, nao o fino do ICU, para nao quebrar comparacao e layout.
        assertEquals("R$ 65,29", Moeda.formatar(65.29))
        assertEquals("R$ 1.234,50", Moeda.formatar(1234.5))
    }
}
