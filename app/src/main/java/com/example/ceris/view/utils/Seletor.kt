package com.example.ceris.view.utils

import android.view.View
import android.widget.TextView
import com.example.ceris.R

/**
 * Liga o layout view_stepper: "-" e "+" mudam o valor entre minimo e maximo,
 * e o texto do meio mostra o valor do jeito que a tela pedir ("3 un", "1").
 */
class Seletor(
    raiz: View,
    inicial: Int = 1,
    private val minimo: Int = 1,
    private val maximo: Int = Int.MAX_VALUE,
    private val formato: (Int) -> String = { it.toString() },
    private val aoMudar: (Int) -> Unit = {}
) {
    private val texto: TextView = raiz.findViewById(R.id.txt_valor)

    var valor: Int = inicial.coerceIn(minimo, maximo)
        set(novo) {
            field = novo.coerceIn(minimo, maximo)
            texto.text = formato(field)
        }

    init {
        texto.text = formato(valor)
        raiz.findViewById<View>(R.id.btn_menos).setOnClickListener { mudar(-1) }
        raiz.findViewById<View>(R.id.btn_mais).setOnClickListener { mudar(+1) }
    }

    private fun mudar(passo: Int) {
        val antes = valor
        valor = antes + passo
        if (valor != antes) aoMudar(valor)
    }
}
