package com.example.ceris

import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.RetrofitClient
import com.example.ceris.local.SessionManager
import com.example.ceris.model.Produto
import com.example.ceris.model.SituacaoProduto
import com.example.ceris.view.DetalheProdutoActivity
import com.example.ceris.view.fragment.EstoqueMinimoDialog
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class EstoqueMinimoDialogTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val writes = CopyOnWriteArrayList<Int>()
    private lateinit var cache: MutableMap<String, Retrofit>
    private var previous: Retrofit? = null

    @Volatile private var minimum: Int? = 3
    @Volatile private var failSave = false

    @Before
    fun mockCore() {
        SessionManager(context).startSession("e30.eyJzdWIiOiIxMiJ9.signature", "qa-refresh")
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val path = request.url().encodedPath()
            var code = 200
            val body = when {
                request.method() == "GET" && path == "/api/stock/settings" ->
                    minimum?.let { """[{"foodId":99,"minimumQuantity":20},{"foodId":42,"minimumQuantity":$it}]""" } ?: "[]"
                request.method() == "PUT" && path == "/api/stock/settings/42" -> {
                    val buffer = Buffer()
                    request.body()!!.writeTo(buffer)
                    val value = JsonParser().parse(buffer.readUtf8()).asJsonObject.get("minimumQuantity").asInt
                    writes.add(value)
                    if (failSave) code = 503 else minimum = value.takeIf { it > 0 }
                    """{"foodId":42,"minimumQuantity":$value}"""
                }
                else -> { code = 404; "{}" }
            }
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(code).message("QA").body(ResponseBody.create(MediaType.get("application/json"), body)).build()
        }.build()
        val retrofit = Retrofit.Builder().baseUrl(ApiBaseUrl.core).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        // Substitui apenas o transporte no teste; exercita a API e o popup reais.
        val field = RetrofitClient::class.java.getDeclaredField("retrofitAutenticados").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        cache = field.get(RetrofitClient) as MutableMap<String, Retrofit>
        previous = cache.put(ApiBaseUrl.core, retrofit)
    }

    @After
    fun restoreCore() {
        if (previous == null) cache.remove(ApiBaseUrl.core) else cache[ApiBaseUrl.core] = previous!!
        SessionManager(context).clear()
    }

    private fun launch(): ActivityScenario<DetalheProdutoActivity> {
        // Nome vazio evita a busca nutricional externa durante este teste.
        val produto = Produto("900", "", 1, "", SituacaoProduto.SEM_VALIDADE, foodId = 42)
        return ActivityScenario.launch(DetalheProdutoActivity.intent(context, produto))
    }

    private fun popup(activity: DetalheProdutoActivity) =
        activity.supportFragmentManager.findFragmentByTag(EstoqueMinimoDialog.TAG) as? EstoqueMinimoDialog

    private fun await(scenario: ActivityScenario<DetalheProdutoActivity>, condition: (DetalheProdutoActivity) -> Boolean) {
        repeat(100) {
            var ready = false
            scenario.onActivity { ready = condition(it) }
            if (ready) return
            Thread.sleep(50)
        }
        throw AssertionError("O popup não chegou ao estado esperado")
    }

    private fun open(scenario: ActivityScenario<DetalheProdutoActivity>, expected: String) {
        scenario.onActivity { it.findViewById<View>(R.id.btn_detalhe_configuracao).performClick() }
        await(scenario) { activity ->
            val root = popup(activity)?.view
            root?.findViewById<View>(R.id.btn_mais)?.isEnabled == true &&
                root.findViewById<TextView>(R.id.txt_valor).text.toString() == expected
        }
    }

    @Test
    fun carregaMinimoDoAlimentoPermiteZeroEMantemPopupSeSalvarFalhar() {
        launch().use { scenario ->
            open(scenario, "3")
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            File(context.getExternalFilesDir(null), "qa-estoque-minimo.png").outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            failSave = true
            scenario.onActivity { activity ->
                val root = popup(activity)!!.requireView()
                repeat(4) { root.findViewById<View>(R.id.btn_menos).performClick() }
                assertEquals("0", root.findViewById<TextView>(R.id.txt_valor).text.toString())
                root.findViewById<View>(R.id.btn_salvar_dialogo).performClick()
            }
            await(scenario) { activity ->
                val root = popup(activity)?.view
                writes.size == 1 && root?.findViewById<View>(R.id.btn_salvar_dialogo)?.isEnabled == true
            }
            failSave = false
            scenario.onActivity { popup(it)!!.requireView().findViewById<View>(R.id.btn_salvar_dialogo).performClick() }
            await(scenario) { popup(it) == null }
            assertEquals(listOf(0, 0), writes.toList())
            open(scenario, "1")
        }
    }

    @Test
    fun semConfiguracaoIniciaEmUmPreservaEdicaoAoRecriarECancelarNaoGrava() {
        minimum = null
        launch().use { scenario ->
            open(scenario, "1")
            scenario.onActivity { popup(it)!!.requireView().findViewById<View>(R.id.btn_mais).performClick() }
            scenario.recreate()
            await(scenario) { activity ->
                val root = popup(activity)?.view
                root?.findViewById<View>(R.id.btn_mais)?.isEnabled == true &&
                    root.findViewById<TextView>(R.id.txt_valor).text.toString() == "2"
            }
            scenario.onActivity { popup(it)!!.requireView().findViewById<View>(R.id.btn_cancelar_dialogo).performClick() }
            await(scenario) { popup(it) == null }
            assertTrue(writes.isEmpty())
        }
    }
}
