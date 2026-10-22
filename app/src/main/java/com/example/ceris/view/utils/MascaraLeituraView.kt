package com.example.ceris.view.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.ceris.R

/**
 * Camada por cima da camera: escurece tudo fora da moldura de leitura e
 * desenha so os quatro cantos da moldura, em roxo, como no Figma.
 *
 * Dois formatos: a nota fiscal e alta e estreita, o codigo de barras e baixo e
 * largo (o do Figma). Trocar de modo anima a moldura de um para o outro.
 *
 * A moldura sai do tamanho da View, nao de dp fixo: ocupa a mesma fracao da
 * tela em qualquer celular e para de crescer no tablet.
 */
class MascaraLeituraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.mascara_camera)
    }

    private val cantos = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.moldura_traco)
        strokeCap = Paint.Cap.SQUARE
        color = ContextCompat.getColor(context, R.color.md_theme_primary)
    }

    private val larguraMaxima = resources.getDimension(R.dimen.moldura_largura_max)
    private val tamanhoCanto = resources.getDimension(R.dimen.moldura_canto)
    private val raio = resources.getDimension(R.dimen.moldura_raio)

    /** Espaco em cima (seletor de modo) e embaixo (dica) que a moldura da nota nao invade. */
    private val espacoTopo = resources.getDimension(R.dimen.moldura_espaco_topo)
    private val espacoDica = resources.getDimension(R.dimen.moldura_espaco_dica)

    /** Moldura desenhada agora; durante a animacao fica entre as duas. */
    val moldura = RectF()

    private val molduraNota = RectF()
    private val molduraCodigo = RectF()

    private val buraco = Path()
    private val tracos = Path()

    private var animacao: ValueAnimator? = null

    var modoNota: Boolean = true
        private set

    /** Troca o formato, animando de um para o outro. */
    fun mudarModo(nota: Boolean) {
        if (nota == modoNota) return
        modoNota = nota
        animacao?.cancel()

        val de = RectF(moldura)
        val para = if (nota) molduraNota else molduraCodigo
        if (width == 0 || de.isEmpty) {
            definir(para)
            return
        }

        animacao = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACAO_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                definir(
                    RectF(
                        de.left + (para.left - de.left) * t,
                        de.top + (para.top - de.top) * t,
                        de.right + (para.right - de.right) * t,
                        de.bottom + (para.bottom - de.bottom) * t
                    )
                )
            }
            start()
        }
    }

    override fun onSizeChanged(largura: Int, altura: Int, antesL: Int, antesA: Int) {
        super.onSizeChanged(largura, altura, antesL, antesA)

        // Codigo de barras: 293 x 180 de 440 no Figma, um pouco acima do meio.
        val wCodigo = (largura * FRACAO_CODIGO).coerceAtMost(larguraMaxima)
        val hCodigo = (wCodigo * PROPORCAO_CODIGO).coerceAtMost(altura * 0.5f)
        val xCodigo = (largura - wCodigo) / 2f
        val yCodigo = (altura - hCodigo) * POSICAO_CODIGO
        molduraCodigo.set(xCodigo, yCodigo, xCodigo + wCodigo, yCodigo + hCodigo)

        // Nota: alta, no formato de um cupom, entre o seletor de modo e a dica.
        val alturaUtil = (altura - espacoTopo - espacoDica).coerceAtLeast(altura * 0.4f)
        var wNota = (largura * FRACAO_NOTA).coerceAtMost(larguraMaxima)
        var hNota = wNota * PROPORCAO_NOTA
        if (hNota > alturaUtil) {
            hNota = alturaUtil
            wNota = hNota / PROPORCAO_NOTA
        }
        val xNota = (largura - wNota) / 2f
        val yNota = espacoTopo + (alturaUtil - hNota) / 2f
        molduraNota.set(xNota, yNota, xNota + wNota, yNota + hNota)

        animacao?.cancel()
        animacao = null
        definir(if (modoNota) molduraNota else molduraCodigo)
    }

    private fun definir(nova: RectF) {
        moldura.set(nova)

        buraco.reset()
        buraco.fillType = Path.FillType.EVEN_ODD
        buraco.addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
        buraco.addRoundRect(moldura, raio, raio, Path.Direction.CW)

        val c = tamanhoCanto.coerceAtMost(minOf(moldura.width(), moldura.height()) / 3f)
        tracos.reset()
        with(moldura) {
            canto(left, top + c, left, top, left + c, top)
            canto(right - c, top, right, top, right, top + c)
            canto(right, bottom - c, right, bottom, right - c, bottom)
            canto(left + c, bottom, left, bottom, left, bottom - c)
        }
        invalidate()
    }

    /** Um canto em L: da ponta de um braco, passando pelo vertice, ate a ponta do outro. */
    private fun canto(x1: Float, y1: Float, xv: Float, yv: Float, x2: Float, y2: Float) {
        tracos.moveTo(x1, y1)
        tracos.lineTo(xv, yv)
        tracos.lineTo(x2, y2)
    }

    override fun onDetachedFromWindow() {
        animacao?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(buraco, sombra)
        canvas.drawPath(tracos, cantos)
    }

    private companion object {
        const val FRACAO_CODIGO = 0.67f
        const val PROPORCAO_CODIGO = 0.62f

        /** 0.5 centraliza; menos sobe a moldura. */
        const val POSICAO_CODIGO = 0.42f

        const val FRACAO_NOTA = 0.78f

        /** Cupom fiscal: bem mais alto que largo. */
        const val PROPORCAO_NOTA = 1.45f

        const val DURACAO_MS = 250L
    }
}
