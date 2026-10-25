package com.example.ceris.repository

import android.content.SharedPreferences
import com.example.ceris.api.StockAPI
import com.example.ceris.api.openfoodfacts.OpenFoodFactsAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.CadastroPorCodigo
import com.example.ceris.model.CategoriaAlimento
import com.example.ceris.model.Produto
import com.example.ceris.model.ProdutoEncontrado
import com.example.ceris.model.dto.FoodRegistrationRequest
import com.example.ceris.model.dto.StockItemResponse
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Repositorio contra um servidor HTTP de verdade, no formato da base aberta e da Core. */
class CodigoBarrasRepositoryTest {

    private lateinit var servidor: HttpServer
    private var corpoRecebido = ""
    private var tokenRecebido: String? = null
    private var camposConsultados: String? = null
    private val foto = "https://img.example/leite.jpg"
    private var respostaCore: Pair<Int, String> =
        201 to "{\"id\":9,\"foodId\":3,\"foodName\":\"Leite Condensado\",\"quantity\":2}"

    @Before
    fun subir() {
        servidor = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servidor.createContext("/api/v2/product/") { troca ->
            camposConsultados = troca.requestURI.query
            if (troca.requestURI.path.endsWith("7891000100103")) {
                responder(
                    troca, 200,
                    "{\"status\":1,\"product\":{\"product_name\":\"Leite Condensado\",\"brands\":\"Nestlé, Moça\"," +
                        "\"product_quantity\":395,\"product_quantity_unit\":\"g\",\"image_front_url\":\"$foto\"}}"
                )
            } else {
                responder(troca, 404, "{\"status\":0,\"status_verbose\":\"product not found\"}")
            }
        }
        servidor.createContext("/api/foods") { troca ->
            tokenRecebido = troca.requestHeaders.getFirst("Authorization")
            corpoRecebido = troca.requestBody.readBytes().decodeToString()
            responder(troca, respostaCore.first, respostaCore.second)
        }
        servidor.createContext("/api/stock/foods") { troca ->
            responder(troca, 200, """[{"id":3,"name":"Leite Condensado","categoryName":"Laticínios","foodImageUrl":"$foto"}]""")
        }
        servidor.createContext("/api/stock") { troca ->
            responder(troca, 200, "[${respostaCore.second}]")
        }
        servidor.start()
    }

    @After
    fun derrubar() = servidor.stop(0)

    private fun responder(troca: HttpExchange, codigo: Int, corpo: String) {
        val bytes = corpo.toByteArray()
        troca.responseHeaders.add("Content-Type", "application/json")
        troca.sendResponseHeaders(codigo, bytes.size.toLong())
        troca.responseBody.use { it.write(bytes) }
    }

    private fun repo(token: String? = "Bearer abc"): CodigoBarrasRepository {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${servidor.address.port}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return CodigoBarrasRepository(
            retrofit.create(OpenFoodFactsAPI::class.java), retrofit.create(StockAPI::class.java)
        ) { token }
    }

    private fun buscar(gtin: String): ProdutoEncontrado? {
        val fim = CountDownLatch(1)
        var achado: ProdutoEncontrado? = null
        repo().buscar(gtin) { achado = it; fim.countDown() }
        assertTrue(fim.await(5, TimeUnit.SECONDS))
        return achado
    }

    @Test
    fun produto_da_base_aberta_vem_preenchido() {
        val achado = buscar("7891000100103")!!
        assertEquals("Leite Condensado", achado.nome)
        assertEquals("Nestlé", achado.marca)
        assertEquals(foto, achado.imagemUrl)
        assertTrue(camposConsultados.orEmpty().contains("image_front_url"))
    }

    @Test
    fun produto_fora_da_base_vem_nulo_sem_erro() {
        assertNull(buscar("7896004000207"))
    }

    private val pedido = FoodRegistrationRequest(
        gtin = "7891000100103",
        name = "Leite Condensado",
        productBrand = "Nestlé",
        packageQuantity = 395.0,
        unitOfMeasure = "g",
        gpcCode = "10000025",
        quantity = 2,
        expiryDate = "2027-01-10"
    )

    @Test
    fun grava_na_core_com_o_token_e_o_json_do_contrato() {
        val fim = CountDownLatch(1)
        var salvo: StockItemResponse? = null
        repo().registrar(pedido, { salvo = it; fim.countDown() }, { _, _ -> fim.countDown() }, { fim.countDown() })

        assertTrue(fim.await(5, TimeUnit.SECONDS))
        assertEquals("Leite Condensado", salvo?.foodName)
        assertEquals("Bearer abc", tokenRecebido)
        assertTrue(corpoRecebido, corpoRecebido.contains("\"gtin\":\"7891000100103\""))
        assertTrue(corpoRecebido, corpoRecebido.contains("\"unitOfMeasure\":\"g\""))
        assertTrue(corpoRecebido, corpoRecebido.contains("\"expiryDate\":\"2027-01-10\""))
    }

    @Test
    fun imagemDaConsultaVaiParaCoreEVoltaNoCardAoListar() {
        val encontrado = buscar("7891000100103")!!
        val cadastro = CadastroPorCodigo.montar(
            "7891000100103", encontrado.nome!!, encontrado.marca.orEmpty(), "395",
            encontrado.unidadeEmbalagem, CategoriaAlimento.LATICINIOS, 2, null,
            imagemUrl = encontrado.imagemUrl,
        ).getOrThrow()
        respostaCore = 201 to """{"id":9,"foodId":3,"foodName":"Leite Condensado","quantity":2,"foodImageUrl":"$foto"}"""
        val cadastroFinalizado = CountDownLatch(1)
        var salvo: StockItemResponse? = null
        repo().registrar(cadastro, { salvo = it; cadastroFinalizado.countDown() }, { _, _ -> cadastroFinalizado.countDown() }, { cadastroFinalizado.countDown() })
        assertTrue(cadastroFinalizado.await(5, TimeUnit.SECONDS))
        assertEquals(foto, Gson().fromJson(corpoRecebido, JsonObject::class.java)["foodImageUrl"].asString)
        assertEquals(foto, salvo?.foodImageUrl)

        assertEquals(foto, listarCards().single().imagemUrl)
    }

    @Test
    fun listagemDeEstoqueAntigoSemFotoContinuaFuncionando() {
        val produto = listarCards().single()
        assertEquals("Leite Condensado", produto.nome)
        assertNull(produto.imagemUrl)
    }

    private fun listarCards(): List<Produto> {
        val preferencias = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, _ ->
            if (method.name == "getString") "abc" else error("Preferencia inesperada: ${method.name}")
        } as SharedPreferences
        val retrofit = Retrofit.Builder()
            .baseUrl("http://127.0.0.1:${servidor.address.port}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val fim = CountDownLatch(1)
        var produtos: List<Produto>? = null
        StockRepository(retrofit.create(StockAPI::class.java), SessionManager(preferencias)).listar(
            { produtos = it; fim.countDown() },
            { _, _ -> fim.countDown() },
            { fim.countDown() },
        )
        assertTrue(fim.await(5, TimeUnit.SECONDS))
        return requireNotNull(produtos)
    }

    @Test
    fun recusa_da_core_chega_com_codigo_e_corpo() {
        respostaCore = 400 to "{\"message\":\"Informe um código GPC alimentar conhecido para cadastrar um novo produto\"}"
        val fim = CountDownLatch(1)
        var erro: Pair<Int, String?>? = null
        repo().registrar(pedido, { fim.countDown() }, { c, b -> erro = c to b; fim.countDown() }, { fim.countDown() })

        assertTrue(fim.await(5, TimeUnit.SECONDS))
        assertEquals(400, erro?.first)
        assertTrue(erro?.second.orEmpty().contains("GPC"))
    }

    @Test
    fun sem_sessao_nao_chama_a_core() {
        val fim = CountDownLatch(1)
        var falha: Throwable? = null
        repo(token = null).registrar(pedido, { fim.countDown() }, { _, _ -> fim.countDown() }, { falha = it; fim.countDown() })

        assertTrue(fim.await(5, TimeUnit.SECONDS))
        assertTrue(falha is ShoppingListRepository.SemSessao)
        assertEquals("", corpoRecebido)
    }
}
