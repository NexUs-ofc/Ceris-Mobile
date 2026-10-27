package com.example.ceris

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto
import com.example.ceris.view.DetalheProdutoActivity
import com.example.ceris.debug.TelaDeTesteActivity
import com.example.ceris.view.fragment.CodigoBarrasBottomSheet
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Abre as telas de verdade no aparelho, sem passar pelo login, e salva um
 * print de cada uma em Android/data/com.example.ceris/files/telas/.
 */
@RunWith(AndroidJUnit4::class)
class TelasTest {

    private val instrumentacao = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacao.targetContext

    @Before
    fun permitirCamera() {
        instrumentacao.uiAutomation.grantRuntimePermission(contexto.packageName, Manifest.permission.CAMERA)
    }

    private fun print(nome: String) {
        Thread.sleep(800)
        val imagem = instrumentacao.uiAutomation.takeScreenshot()
        val pasta = File(contexto.getExternalFilesDir(null), "telas").apply { mkdirs() }
        File(pasta, "$nome.png").outputStream().use { imagem.compress(Bitmap.CompressFormat.PNG, 100, it) }
        assertTrue(File(pasta, "$nome.png").length() > 0)
    }

    @Test
    fun detalhe_do_produto() {
        val produto = Produto(
            id = "1",
            nome = "Leite Integral Piracanjuba 1L",
            quantidade = 1,
            validade = "25/07",
            situacao = SituacaoProduto.FRESCO,
            unidade = "un",
            categoria = "Laticínios",
            marca = "Piracanjuba"
        )
        ActivityScenario.launch<DetalheProdutoActivity>(DetalheProdutoActivity.intent(contexto, produto)).use {
            // A tabela nutricional vem da internet: espera ela chegar.
            Thread.sleep(6_000)
            print("1_detalhe_produto")
        }
    }

    /** A MainActivity exige sessao; a tela vai numa activity so do debug. */
    private fun tela(nome: String) = ActivityScenario.launch<TelaDeTesteActivity>(
        Intent(contexto, TelaDeTesteActivity::class.java).putExtra(TelaDeTesteActivity.EXTRA_TELA, nome)
    )

    @Test
    fun cards_do_estoque() {
        tela(TelaDeTesteActivity.TELA_ESTOQUE).use { cena ->
            // Sem login a Core nao responde; o adapter recebe os produtos direto.
            Thread.sleep(3_000)
            cena.onActivity { atividade ->
                val lista = atividade.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.lista_produtos)
                atividade.findViewById<android.view.View>(R.id.estado_vazio).visibility = android.view.View.GONE
                lista.visibility = android.view.View.VISIBLE
                (lista.adapter as com.example.ceris.view.adapter.ProdutoAdapter).submitList(
                    listOf(
                        Produto("1", "Leite Integral Piracanjuba 1L", 2, "25/07", SituacaoProduto.FRESCO, unidade = "un"),
                        Produto("2", "Alface crespa", 1, "20/10", SituacaoProduto.VENCENDO, unidade = "un"),
                        Produto("3", "Arroz Branco 5kg", 3, "", SituacaoProduto.SEM_VALIDADE, unidade = "kg")
                    )
                )
            }
            print("0_cards_estoque")
        }
    }

    @Test
    fun camera_com_os_dois_modos_e_cadastro_por_codigo() {
        tela(TelaDeTesteActivity.TELA_NOTA).use { cena ->
            Thread.sleep(3_000)
            print("2_camera_modo_codigo_inicial")

            cena.onActivity { it.findViewById<android.view.View>(R.id.modo_nota).performClick() }
            Thread.sleep(1_500)
            print("3_camera_modo_nota")

            // Mesmo caminho do leitor: um GTIN valido abre o cadastro.
            // Mesmo caminho do leitor: aberto pela tela da camera, que volta a
            // ler quando o cadastro fecha.
            cena.onActivity {
                val camera = it.supportFragmentManager.findFragmentByTag(TelaDeTesteActivity.TELA_NOTA)!!
                CodigoBarrasBottomSheet.novo("7891000100103").show(camera.childFragmentManager, "teste")
            }
            Thread.sleep(6_000)
            print("4_cadastro_codigo_barras")
        }
    }
}
