package com.example.ceris.view.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.ceris.R

/**
 * Fundo da barra de navegação inferior.
 *
 * Desenha o próprio contorno em vez de montar um drawable: a borda sobe em arco
 * no meio do caminho para envolver o botão de scan, e nem <shape> nem
 * MaterialShapeDrawable descrevem isso de forma confiável.
 *
 * O caminho é um só, percorrido uma vez preenchido e uma vez traçado — é o que
 * garante que o traço acompanhe a saliência em vez de cortá-la.
 *
 * Geometria, toda vinda de dimens:
 *
 *     raio da saliência = metade do botão + entorno
 *     altura da View    = faixa reta + raio da saliência
 *
 * A linha reta do topo fica, portanto, a um raio de distância do topo da View,
 * deixando exatamente o espaço de que a saliência precisa para subir.
 */
class BarraNavegacaoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val preenchimento = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.md_theme_surface)
    }

    private val traco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.nav_stroke_width)
        color = ContextCompat.getColor(context, R.color.nav_traco)
    }

    private val raioSaliencia = resources.getDimension(R.dimen.nav_saliencia_raio)
    private val raioCantos = resources.getDimension(R.dimen.nav_bar_corner_radius)

    private val caminho = Path()

    init {
        // ViewGroup não chama onDraw por padrão.
        setWillNotDraw(false)
    }

    override fun onSizeChanged(largura: Int, altura: Int, antesL: Int, antesA: Int) {
        super.onSizeChanged(largura, altura, antesL, antesA)
        construirCaminho(largura.toFloat(), altura.toFloat())
    }

    /**
     * O traço é desenhado centrado na linha, então metade dele cairia fora da
     * View e seria cortada. Por isso o caminho é recuado meia espessura nas
     * bordas.
     *
     * A linha reta sai da altura da View menos a faixa dos destinos, em vez de
     * ser assumida a partir do raio. Assim, se as medidas saírem de sincronia,
     * a superfície continua alinhada aos itens: a saliência encolhe até caber
     * no espaço que sobrou, em vez de a barra ser desenhada fora de lugar.
     */
    private fun construirCaminho(largura: Float, altura: Float) {
        val recuo = traco.strokeWidth / 2f
        val faixaDestinos = resources.getDimension(R.dimen.nav_bar_height)

        val folga = (altura - faixaDestinos).coerceAtLeast(0f)
        val topo = folga + recuo
        val meio = largura / 2f
        val r = raioSaliencia.coerceAtMost(folga)
        val c = raioCantos.coerceAtMost(largura / 2f - r)

        val esquerda = recuo
        val direita = largura - recuo

        caminho.reset()

        caminho.moveTo(esquerda, altura)
        caminho.lineTo(esquerda, topo + c)
        caminho.arcTo(esquerda, topo, esquerda + 2 * c, topo + 2 * c, 180f, 90f, false)

        if (r > 0f) {
            caminho.lineTo(meio - r, topo)
            // 180 -> 360 passa pelo topo: a borda sobe envolvendo o botão.
            caminho.arcTo(meio - r, topo - r, meio + r, topo + r, 180f, 180f, false)
        }

        caminho.lineTo(direita - c, topo)
        caminho.arcTo(direita - 2 * c, topo, direita, topo + 2 * c, 270f, 90f, false)

        caminho.lineTo(direita, altura)
        caminho.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(caminho, preenchimento)
        canvas.drawPath(caminho, traco)
    }
}
