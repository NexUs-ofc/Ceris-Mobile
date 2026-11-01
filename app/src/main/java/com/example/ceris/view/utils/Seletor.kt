package com.example.ceris.view.utils

import android.view.View
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.example.ceris.R

/**
 * Liga o layout view_stepper: "-" e "+" mudam o valor entre minimo e maximo,
 * e o texto do meio mostra o valor do jeito que a tela pedir ("3 un", "1").
 */
class Seletor(
    raiz: View,
    inicial: Int = 1,
    val minimo: Int = 1,
    val maximo: Int = Int.MAX_VALUE,
    private val formato: (Int) -> String = { it.toString() },
    private val aoMudar: (Int) -> Unit = {},
    private val aoValidar: (Boolean) -> Unit = {}
) {
    private val texto: EditText = raiz.findViewById(R.id.txt_valor)

    private var atualizandoTexto = false

    var valor: Int = inicial.coerceIn(minimo, maximo)
        set(novo) {
            field = novo.coerceIn(minimo, maximo)
            mostrarValor()
        }

    init {
        mostrarValor()
        texto.setOnFocusChangeListener { _, focado ->
            if (!focado) {
                if (!confirmarEntrada()) {
                    texto.error = mensagemParaEntradaInvalida()
                    aoValidar(false)
                }
            } else {
                texto.error = null
                definirTexto(valor.toString())
            }
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (atualizandoTexto || !texto.hasFocus()) return
                val novo = lerTexto()
                if (novo != null && novo in minimo..maximo) {
                    if (novo != valor) {
                        valor = novo
                        aoMudar(valor)
                    }
                    aoValidar(true)
                } else {
                    aoValidar(false)
                }
            }
        }
        (texto.getTag(R.id.tag_seletor_watcher) as? TextWatcher)?.let(texto::removeTextChangedListener)
        texto.setTag(R.id.tag_seletor_watcher, watcher)
        texto.addTextChangedListener(watcher)
        raiz.findViewById<View>(R.id.btn_menos).setOnClickListener { mudar(-1) }
        raiz.findViewById<View>(R.id.btn_mais).setOnClickListener { mudar(+1) }
    }

    private fun mostrarValor() = definirTexto(if (texto.hasFocus()) valor.toString() else formato(valor))

    private fun definirTexto(conteudo: String) {
        if (texto.text.toString() == conteudo) return
        atualizandoTexto = true
        texto.setText(conteudo)
        texto.setSelection(texto.text.length)
        atualizandoTexto = false
    }

    fun valorDigitado(): Int? = texto.text.toString().toIntOrNull()

    private fun lerTexto(): Int? = valorDigitado()

    fun confirmarEntrada(): Boolean {
        val digitado = lerTexto() ?: return false
        if (digitado !in minimo..maximo) return false
        valor = digitado
        texto.error = null
        aoMudar(valor)
        aoValidar(true)
        return true
    }

    fun mostrarErro(mensagem: String) {
        texto.error = mensagem
        texto.requestFocus()
    }

    private fun mensagemParaEntradaInvalida(): String =
        if (valorDigitado()?.let { it > maximo } == true && maximo < Int.MAX_VALUE) {
            texto.context.getString(R.string.quantidade_acima_estoque, maximo)
        } else {
            texto.context.getString(R.string.quantidade_positiva_inteira)
        }

    private fun mudar(passo: Int) {
        val antes = valor
        valor = antes + passo
        texto.error = null
        aoValidar(true)
        if (valor != antes) aoMudar(valor)
    }
}
