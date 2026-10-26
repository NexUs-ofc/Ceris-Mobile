package com.example.ceris

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.debug.ReceitasTestActivity
import com.example.ceris.model.PassoReceita
import com.example.ceris.model.Receita
import com.example.ceris.model.RecipeDifficulty
import com.example.ceris.view.ChatbotActivity
import com.example.ceris.view.DetalheReceitaActivity
import com.example.ceris.view.fragment.ReceitasFragment
import com.example.ceris.viewmodel.DetalheReceitaViewModel
import com.example.ceris.viewmodel.ReceitasViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceitasTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val receita =
        Receita(
            id = "507f1f77bcf86cd799439011",
            nome = "Frango ao molho",
            porcoes = 4,
            ingredientes =
                listOf(
                    "500g de peito de frango",
                    "1 cebola picada",
                    "2 dentes de alho",
                    "200ml de creme de leite",
                    "Sal e pimenta a gosto",
                ),
            passos =
                listOf(
                    PassoReceita(
                        "Cortar e temperar o frango",
                        "Em uma tábua, corte o frango em cubos.\nTempere com sal e pimenta e misture para distribuir bem o tempero.",
                    ),
                    PassoReceita("Refogar a cebola e o alho", "Em uma panela, refogue a cebola e o alho no azeite."),
                ),
        )

    @Test
    fun vazioMostraMascoteMensagemExataELinkParaChatbot() {
        ActivityScenario.launch(ReceitasTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fragment(activity).estadoAtualizado(ReceitasViewModel.Estado.Conteudo(emptyList()))
                assertNotNull(activity.findViewById<ImageView>(R.id.img_receitas_vazia).drawable)
            }
            onView(withText("Você ainda não tem receitas salvas! Peça para mim que crio uma receita para você!"))
                .check(matches(isDisplayed()))
            onView(withId(R.id.lista_receitas)).check(matches(not(isDisplayed())))
            screenshot("receitas-vazio")
            navigate(ChatbotActivity::class.java) {
                onView(withText("Gerar receita com a Cerita")).perform(click())
            }
        }
    }

    @Test
    fun falhaNaoApareceComoListaVaziaEOfereceNovaTentativa() {
        ActivityScenario.launch(ReceitasTestActivity::class.java).use { scenario ->
            scenario.onActivity { fragment(it).estadoAtualizado(ReceitasViewModel.Estado.Erro("Sem conexão")) }
            onView(withText("Sem conexão")).check(matches(isDisplayed()))
            onView(withId(R.id.btn_tentar_receitas)).check(matches(isDisplayed()))
            onView(withId(R.id.estado_receitas_vazio)).check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun tocarReceitaAbreDetalheComIdSelecionado() {
        ActivityScenario.launch(ReceitasTestActivity::class.java).use { scenario ->
            val receitas =
                listOf(
                    receita.copy(tempoPreparo = "45 min"),
                    receita.copy(id = "507f1f77bcf86cd799439012", nome = "Bolo de cenoura", tempoPreparo = "45 min"),
                    receita.copy(id = "507f1f77bcf86cd799439013", nome = "Strogonoff de frango", tempoPreparo = "30 min"),
                    receita.copy(id = "507f1f77bcf86cd799439014", nome = "Lasanha à bolonhesa", tempoPreparo = "1h 20min"),
                )
            scenario.onActivity { fragment(it).estadoAtualizado(ReceitasViewModel.Estado.Conteudo(receitas)) }
            onView(withText("Frango ao molho")).check(matches(isDisplayed()))
            onView(withText("1h 20min")).check(matches(isDisplayed()))
            onView(isAssignableFrom(ShapeableImageView::class.java)).check(doesNotExist())
            screenshot("receitas-lista")
            navigate(DetalheReceitaActivity::class.java, { activity ->
                assertEquals(receita.id, activity.intent.getStringExtra(DetalheReceitaActivity.EXTRA_RECEITA_ID))
            }) {
                onView(withText("Frango ao molho")).perform(click())
            }
        }
    }

    @Test
    fun detalheMostraIngredientesPassosEEscondeMetadadosAusentes() {
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                activity.estadoAtualizado(DetalheReceitaViewModel.Estado.Conteudo(receita))
                assertEquals(5, activity.findViewById<LinearLayout>(R.id.ingredientes_receita).childCount)
                val passos = activity.findViewById<LinearLayout>(R.id.passos_receita)
                assertEquals(2, passos.childCount)
                assertEquals("1", passos.getChildAt(0).findViewById<TextView>(R.id.numero_passo).text.toString())
                assertPasso(activity, 0, false)
                assertPasso(activity, 1, false)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.grupo_tempo_receita).visibility)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.grupo_dificuldade_receita).visibility)
                assertEquals(R.id.card_resumo_receita, activity.findViewById<ViewGroup>(R.id.conteudo).getChildAt(0).id)
            }
            onView(withText("Frango ao molho")).check(matches(isDisplayed()))
            onView(withText("Serve 4")).check(matches(isDisplayed()))
            onView(withText("500g de peito de frango")).check(matches(isDisplayed()))
            onView(isAssignableFrom(ShapeableImageView::class.java)).check(doesNotExist())
            screenshot("receitas-detalhe-sem-metadados")
        }
    }

    @Test
    fun detalheMostraTempoPorcoesEDificuldadeEmPortuguesSemFoto() {
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).use { scenario ->
            RecipeDifficulty.entries.forEach { dificuldade ->
                scenario.onActivity { activity ->
                    activity.estadoAtualizado(
                        DetalheReceitaViewModel.Estado.Conteudo(receita.copy(tempoPreparo = "40 min", dificuldade = dificuldade)),
                    )
                }
                onView(withText("40 min")).check(matches(isDisplayed()))
                onView(withText("Serve 4")).check(matches(isDisplayed()))
                onView(withText(dificuldade.rotulo)).check(matches(isDisplayed()))
                onView(isAssignableFrom(ShapeableImageView::class.java)).check(doesNotExist())
                screenshot("receitas-detalhe-${dificuldade.name.lowercase()}")
            }
        }
    }

    @Test
    fun detalheSemMetadadosEscondeResumoInteiro() {
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                activity.estadoAtualizado(DetalheReceitaViewModel.Estado.Conteudo(receita.copy(porcoes = null)))
            }
            onView(withId(R.id.card_resumo_receita)).check(matches(not(isDisplayed())))
            onView(withText("500g de peito de frango")).check(matches(isDisplayed()))
            screenshot("receitas-detalhe-sem-resumo")
        }
    }

    @Test
    fun tempoExtensoCabeNoResumoSemCortarTexto() {
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                activity.estadoAtualizado(
                    DetalheReceitaViewModel.Estado.Conteudo(
                        receita.copy(tempoPreparo = "1 hora e 30 minutos", dificuldade = RecipeDifficulty.MEDIUM),
                    ),
                )
            }
            onView(withText("1 hora e 30 minutos")).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                val tempo = activity.findViewById<TextView>(R.id.txt_detalhe_tempo)
                val resumo = activity.findViewById<View>(R.id.card_resumo_receita)
                assertTrue(tempo.height >= tempo.layout.height)
                assertTrue(tempo.right <= (tempo.parent as View).width)
                assertTrue(resumo.height >= tempo.height)
            }
            screenshot("receitas-detalhe-tempo-extenso")
        }
    }

    @Test
    fun detalhePermiteRolarReceitaExtensaAteUltimoPasso() {
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                activity.estadoAtualizado(
                    DetalheReceitaViewModel.Estado.Conteudo(
                        receita.copy(passos = (1..30).map { PassoReceita("Etapa de preparo $it", "Detalhes da etapa $it.") }),
                    ),
                )
            }
            onView(withText("Etapa de preparo 30")).perform(scrollTo(), click())
            onView(withText("Detalhes da etapa 30.")).perform(scrollTo()).check(matches(isDisplayed()))
        }
    }

    @Test
    fun etapasIniciamFechadasEAlternamEntreDetalhesERetornoAoPadrao() {
        abrirDetalhe().use { scenario ->
            scenario.onActivity { activity ->
                assertPasso(activity, 0, false)
                assertPasso(activity, 1, false)
            }
            screenshot("receitas-etapas-recolhidas")
            onView(withText(receita.passos[0].titulo)).perform(click())
            onView(withText(receita.passos[0].descricao)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                assertPasso(activity, 0, true)
                assertPasso(activity, 1, false)
            }
            screenshot("receitas-etapa-expandida")
            onView(withText(receita.passos[1].titulo)).perform(click())
            onView(withText(receita.passos[0].descricao)).check(matches(not(isDisplayed())))
            onView(withText(receita.passos[1].descricao)).check(matches(isDisplayed()))
            scenario.onActivity { activity ->
                assertPasso(activity, 0, false)
                assertPasso(activity, 1, true)
            }
            onView(withText(receita.passos[1].titulo)).perform(click())
            scenario.onActivity { activity ->
                assertPasso(activity, 0, false)
                assertPasso(activity, 1, false)
            }
        }
    }

    @Test
    fun reabrirReceitaComecaComTodasAsEtapasRecolhidas() {
        abrirDetalhe().use {
            onView(withText(receita.passos[0].titulo)).perform(click())
            onView(withText(receita.passos[0].descricao)).check(matches(isDisplayed()))
        }
        abrirDetalhe().use { scenario ->
            scenario.onActivity { activity ->
                assertPasso(activity, 0, false)
                assertPasso(activity, 1, false)
            }
        }
    }

    @Test
    fun etapaAbertaSobreviveRecriacaoSemAbrirAsOutras() {
        abrirDetalhe().use { scenario ->
            onView(withText(receita.passos[0].titulo)).perform(click())
            scenario.recreate()
            scenario.onActivity { activity ->
                activity.estadoAtualizado(DetalheReceitaViewModel.Estado.Conteudo(receita))
                assertPasso(activity, 0, true)
                assertPasso(activity, 1, false)
            }
        }
    }

    @Test
    fun acoesDeAcessibilidadeExpandemERecolhemComNomeEEstado() {
        abrirDetalhe().use { scenario ->
            scenario.onActivity { activity ->
                val passos = activity.findViewById<LinearLayout>(R.id.passos_receita)
                val cabecalho = passos.getChildAt(0).findViewById<View>(R.id.cabecalho_passo)
                assertEquals("Etapa 1: ${receita.passos[0].titulo}", cabecalho.contentDescription)
                assertEquals("android.widget.Button", cabecalho.createAccessibilityNodeInfo().className)
                assertTrue(cabecalho.performAccessibilityAction(AccessibilityNodeInfo.ACTION_EXPAND, null))
                assertPasso(activity, 0, true)
                assertTrue(cabecalho.performAccessibilityAction(AccessibilityNodeInfo.ACTION_COLLAPSE, null))
                assertPasso(activity, 0, false)
            }
        }
    }

    @Test
    fun receitaSemEtapasMostraMensagemSemCardsOuIndicacaoDeToque() {
        abrirDetalhe(receita.copy(passos = emptyList())).use { scenario ->
            onView(withText(R.string.receita_sem_preparo)).check(matches(isDisplayed()))
            onView(withText(R.string.receita_preparo_descricao)).check(matches(not(isDisplayed())))
            scenario.onActivity { activity ->
                assertEquals(0, activity.findViewById<LinearLayout>(R.id.passos_receita).childCount)
            }
        }
    }

    private fun abrirDetalhe(dados: Receita = receita.copy(tempoPreparo = "40 min", dificuldade = RecipeDifficulty.EASY)) =
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).also { scenario ->
            scenario.onActivity { it.estadoAtualizado(DetalheReceitaViewModel.Estado.Conteudo(dados)) }
        }

    private fun assertPasso(
        activity: DetalheReceitaActivity,
        indice: Int,
        expandido: Boolean,
    ) {
        val card = activity.findViewById<LinearLayout>(R.id.passos_receita).getChildAt(indice) as MaterialCardView
        val cabecalho = card.findViewById<View>(R.id.cabecalho_passo)
        val numero = card.findViewById<TextView>(R.id.numero_passo)
        assertEquals((indice + 1).toString(), numero.text.toString())
        assertEquals(activity.getColor(R.color.md_theme_primary), numero.currentTextColor)
        assertTrue(card.findViewById<TextView>(R.id.titulo_passo).typeface.isBold)
        assertEquals(
            activity.getColor(if (expandido) R.color.md_theme_primary else R.color.ceris_white),
            card.cardBackgroundColor.defaultColor,
        )
        assertEquals(if (expandido) View.VISIBLE else View.GONE, card.findViewById<View>(R.id.texto_passo).visibility)
        assertEquals(
            activity.getColor(if (expandido) R.color.ceris_white else R.color.card_titulo),
            card.findViewById<TextView>(R.id.titulo_passo).currentTextColor,
        )
        assertEquals(
            activity.getString(if (expandido) R.string.receita_passo_detalhes else R.string.receita_passo_abrir),
            card.findViewById<TextView>(R.id.dica_passo).text,
        )
        assertEquals(
            activity.getString(if (expandido) R.string.receita_passo_expandido else R.string.receita_passo_recolhido),
            ViewCompat.getStateDescription(cabecalho),
        )
        assertEquals(if (expandido) -90f else 90f, card.findViewById<ImageView>(R.id.icone_passo).rotation, 0f)
    }

    private fun fragment(activity: ReceitasTestActivity) = activity.supportFragmentManager.findFragmentByTag("receitas") as ReceitasFragment

    private fun navigate(
        type: Class<out Activity>,
        verify: (Activity) -> Unit = {},
        action: () -> Unit,
    ) {
        val monitor = instrumentation.addMonitor(type.name, null, false)
        try {
            action()
            val activity = instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
            assertNotNull(activity)
            instrumentation.runOnMainSync {
                verify(requireNotNull(activity))
                activity.finish()
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun screenshot(name: String) {
        onView(isRoot()).perform(
            object : ViewAction {
                override fun getConstraints(): Matcher<View> = isRoot()

                override fun getDescription() = "Aguardar o estado visual final dos cards"

                override fun perform(
                    uiController: UiController,
                    view: View,
                ) {
                    uiController.loopMainThreadForAtLeast(ViewConfiguration.getPressedStateDuration().toLong())
                    view.jumpDrawablesToCurrentState()
                    val doisFrames = (2_000f / (view.display?.refreshRate ?: 60f)).toLong()
                    uiController.loopMainThreadForAtLeast(doisFrames)
                }
            },
        )
        instrumentation.waitForIdleSync()
        val directory = File(context.getExternalFilesDir(null), "receitas-tests").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
