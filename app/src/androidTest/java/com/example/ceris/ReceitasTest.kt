package com.example.ceris

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.debug.ReceitasTestActivity
import com.example.ceris.model.Receita
import com.example.ceris.view.ChatbotActivity
import com.example.ceris.view.DetalheReceitaActivity
import com.example.ceris.view.fragment.ReceitasFragment
import com.example.ceris.viewmodel.DetalheReceitaViewModel
import com.example.ceris.viewmodel.ReceitasViewModel
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                    "Corte o frango em cubos e tempere com sal e pimenta",
                    "Em uma panela, refogue a cebola e o alho no azeite",
                    "Adicione o frango e deixe dourar por 5 minutos",
                    "Acrescente o creme de leite e misture bem",
                    "Cozinhe em fogo baixo por 15 minutos",
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
            scenario.onActivity { fragment(it).estadoAtualizado(ReceitasViewModel.Estado.Conteudo(listOf(receita))) }
            onView(withText("Frango ao molho")).check(matches(isDisplayed()))
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
                assertEquals(5, passos.childCount)
                assertEquals("1", passos.getChildAt(0).findViewById<TextView>(R.id.numero_passo).text.toString())
                assertEquals(View.GONE, activity.findViewById<View>(R.id.img_detalhe_receita).visibility)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.grupo_tempo_receita).visibility)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.grupo_dificuldade_receita).visibility)
            }
            onView(withText("Frango ao molho")).check(matches(isDisplayed()))
            onView(withText("Serve 4")).check(matches(isDisplayed()))
            onView(withText("500g de peito de frango")).check(matches(isDisplayed()))
            screenshot("receitas-detalhe")
        }
    }

    @Test
    fun detalhePermiteRolarReceitaExtensaAteUltimoPasso() {
        ActivityScenario.launch<DetalheReceitaActivity>(DetalheReceitaActivity.intent(context, "")).use { scenario ->
            scenario.onActivity { activity ->
                activity.estadoAtualizado(
                    DetalheReceitaViewModel.Estado.Conteudo(receita.copy(passos = (1..30).map { "Etapa de preparo $it" })),
                )
            }
            onView(withText("Etapa de preparo 30")).perform(scrollTo()).check(matches(isDisplayed()))
        }
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
        instrumentation.waitForIdleSync()
        val directory = File(context.getExternalFilesDir(null), "receitas-tests").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
