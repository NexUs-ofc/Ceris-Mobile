package com.example.ceris.view.utils

import android.app.Dialog
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.LinearLayout
import com.example.ceris.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Uma unica regra de leitura e selecao de validade para nota e codigo. */
object CalendarioValidade {

    fun mostrar(context: Context, atual: String?, aoEscolher: (String) -> Unit): Dialog {
        val inicial = Calendar.getInstance().apply {
            atual?.let(::ler)?.let { time = it }
        }
        val tema = ContextThemeWrapper(context, R.style.TemaCalendarioCeris)
        var anoSelecionado = inicial.get(Calendar.YEAR)
        var mesSelecionado = inicial.get(Calendar.MONTH)
        var diaSelecionado = inicial.get(Calendar.DAY_OF_MONTH)

        val seletorData = DatePicker(tema, null, android.R.attr.datePickerStyle).apply {
            init(anoSelecionado, mesSelecionado, diaSelecionado) { _, ano, mes, dia ->
                anoSelecionado = ano
                mesSelecionado = mes
                diaSelecionado = dia
            }
        }

        val rodape = LinearLayout(tema).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(
                0,
                resources.getDimensionPixelSize(R.dimen.spacing_xs),
                resources.getDimensionPixelSize(R.dimen.spacing_s),
                resources.getDimensionPixelSize(R.dimen.spacing_s)
            )
        }
        val cancelar = botaoCalendario(tema, context.getString(R.string.calendario_cancelar))
        val confirmar = botaoCalendario(tema, context.getString(R.string.calendario_confirmar))
        rodape.addView(cancelar)
        rodape.addView(confirmar)

        val conteudo = LinearLayout(tema).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_dialogo)
            addView(
                seletorData,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
            addView(
                rodape,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            )
        }

        val dialogo = Dialog(context, R.style.TemaCalendarioCeris).apply {
            setContentView(conteudo)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setOnShowListener {
                val configuration = context.resources.configuration
                if (configuration.smallestScreenWidthDp >= 600) {
                    // Keep the established tablet width; phones retain the theme's existing width.
                    val widthDp = minOf(360, configuration.screenWidthDp - 32).coerceAtLeast(1)
                    val widthPx = (widthDp * context.resources.displayMetrics.density).toInt()
                    window?.setLayout(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            }
        }
        cancelar.setOnClickListener { dialogo.dismiss() }
        confirmar.setOnClickListener {
            val escolhida = Calendar.getInstance().apply {
                clear()
                set(anoSelecionado, mesSelecionado, diaSelecionado)
            }
            aoEscolher(formatador(ISO).format(escolhida.time))
            dialogo.dismiss()
        }
        dialogo.show()
        return dialogo
    }

    fun formatar(valor: String?, formato: String): String? =
        valor?.let(::ler)?.let { formatador(formato).format(it) }

    private fun botaoCalendario(context: Context, texto: String) =
        Button(context, null, android.R.attr.buttonBarButtonStyle).apply {
            text = texto
            isAllCaps = false
            minWidth = 0
        }

    private fun ler(valor: String): Date? =
        runCatching { formatador(ISO).apply { isLenient = false }.parse(valor) }.getOrNull()

    private fun formatador(formato: String) = SimpleDateFormat(formato, Locale.forLanguageTag("pt-BR"))

    private const val ISO = "yyyy-MM-dd"
}
