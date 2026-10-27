package com.example.ceris.view.utils

import android.app.DatePickerDialog
import android.content.Context
import com.example.ceris.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Uma unica regra de leitura e selecao de validade para nota e codigo. */
object CalendarioValidade {

    fun mostrar(context: Context, atual: String?, aoEscolher: (String) -> Unit): DatePickerDialog {
        val inicial = Calendar.getInstance().apply {
            atual?.let(::ler)?.let { time = it }
        }

        return DatePickerDialog(
            context,
            R.style.TemaCalendarioCeris,
            { _, ano, mes, dia ->
                val escolhida = Calendar.getInstance().apply {
                    clear()
                    set(ano, mes, dia)
                }
                aoEscolher(formatador(ISO).format(escolhida.time))
            },
            inicial.get(Calendar.YEAR),
            inicial.get(Calendar.MONTH),
            inicial.get(Calendar.DAY_OF_MONTH),
        ).apply { show() }
    }

    fun formatar(valor: String?, formato: String): String? =
        valor?.let(::ler)?.let { formatador(formato).format(it) }

    private fun ler(valor: String): Date? =
        runCatching { formatador(ISO).apply { isLenient = false }.parse(valor) }.getOrNull()

    private fun formatador(formato: String) = SimpleDateFormat(formato, Locale.forLanguageTag("pt-BR"))

    private const val ISO = "yyyy-MM-dd"
}
