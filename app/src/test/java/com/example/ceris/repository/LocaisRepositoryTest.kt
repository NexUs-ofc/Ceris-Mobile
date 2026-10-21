package com.example.ceris.repository

import com.example.ceris.api.LocaisAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ProfileAddressResponse
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.StoreResponse
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import com.example.ceris.support.testApi
import okhttp3.Request
import okhttp3.ResponseBody
import okio.Timeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy

class LocaisRepositoryTest {
    private val sessions = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val address = ProfileAddressResponse("Rua A", "10", "Centro", "01001000", "Cidade", "SP")
    private val profile = ProfileResponse(12, address)
    private val store = StoreResponse(45, "Mercado", "Rua B", "20", null, "Cidade", "SP", null, null, distanceKm = 0.45)

    private fun api(
        profileCall: Call<ProfileResponse> = TestCall { Response.success(profile) },
        nearbyCall: Call<List<StoreResponse>> = TestCall { Response.success(listOf(store)) },
    ): LocaisAPI = testApi { method -> if (method == "profile") profileCall else nearbyCall }

    private fun load(api: LocaisAPI): LocaisResult? {
        var result: LocaisResult? = null
        LocaisRepository(api, sessions).load { result = it }
        return result
    }

    @Test
    fun retornaOrigemELocaisMapeadosOuVazioExplicito() {
        val result = load(api()) as LocaisResult.Loaded
        assertEquals("Rua A, 10 - Centro - Cidade / SP", result.address)
        assertEquals("45", result.locais.single().id)
        assertEquals("450 m", result.locais.single().distancia)
        assertEquals(LocaisResult.Empty(result.address), load(api(nearbyCall = TestCall { Response.success(emptyList()) })))
    }

    @Test
    fun perfilSemEnderecoNaoConsultaLojas() {
        val result =
            load(
                api(
                    profileCall = TestCall { Response.success(profile.copy(address = null)) },
                    nearbyCall = TestCall { error("Nao consultar") },
                ),
            )
        assertEquals(LocaisResult.AddressRequired(AddressProblem.MISSING), result)
    }

    @Test
    fun naoConsultaSemSessaoOuSemPerfilReconhecido() {
        val api = testApi<LocaisAPI> { error("Nao consultar") }
        sessions.clear()
        assertEquals(LocaisResult.AuthenticationRequired, load(api))
        sessions.startSession("token-sem-perfil", "refresh")
        assertEquals(LocaisResult.AuthenticationRequired, load(api))
    }

    @Test
    fun diferenciaErrosDeEnderecoSemTransformarQualquer400EmEnderecoAusente() {
        mapOf(
            "Perfil autenticado nao possui endereco" to LocaisResult.AddressRequired(AddressProblem.MISSING, addressText()),
            "Endereco do perfil nao possui latitude e longitude" to
                LocaisResult.AddressRequired(AddressProblem.NOT_GEOCODED, addressText()),
            "Latitude e longitude devem ser informadas juntas" to LocaisResult.Failure(400),
            "Raio deve ser maior que zero e menor ou igual a 100 km" to LocaisResult.Failure(400),
        ).forEach { (message, expected) ->
            assertEquals(expected, load(api(nearbyCall = TestCall { errorResponse(400, message) })))
        }
        assertEquals(
            LocaisResult.Failure(400),
            load(
                api(
                    nearbyCall =
                        TestCall {
                            Response.error(400, ResponseBody.create(null, "<html>erro</html>"))
                        },
                ),
            ),
        )
        // As mensagens de endereco so sao reconhecidas no endpoint nearby.
        assertEquals(
            LocaisResult.Failure(400),
            load(api(profileCall = TestCall { errorResponse(400, "Perfil autenticado nao possui endereco") })),
        )
    }

    @Test
    fun diferenciaAutenticacaoEFalhasHttpEmAmbasEtapas() {
        listOf(401, 403, 404, 429, 500, 503).forEach { code ->
            val expected = if (code == 401) LocaisResult.AuthenticationRequired else LocaisResult.Failure(code)
            assertEquals(expected, load(api(profileCall = TestCall { errorResponse(code) })))
            assertEquals(expected, load(api(nearbyCall = TestCall { errorResponse(code) })))
        }
    }

    @Test
    fun rejeitaPerfilDiferenteIdAusenteECorpoVazio() {
        assertEquals(LocaisResult.Failure(), load(api(profileCall = TestCall { Response.success(profile.copy(id = 99)) })))
        assertEquals(LocaisResult.Failure(), load(api(profileCall = TestCall { Response.success(null) })))
        assertEquals(LocaisResult.Failure(), load(api(nearbyCall = TestCall { Response.success(null) })))
        assertEquals(LocaisResult.Failure(), load(api(nearbyCall = TestCall { Response.success(listOf(store.copy(id = null))) })))
    }

    @Test
    fun descartaRespostaDeAmbasEtapasDepoisDeLogoutOuTrocaDeConta() {
        listOf(false, true).forEach { atNearby ->
            listOf(false, true).forEach { switchAccount ->
                sessions.startSession(jwt(), "refresh")
                val action = { if (switchAccount) sessions.startSession(jwt("99"), "other") else sessions.clear() }
                val oldProfile =
                    TestCall {
                        action()
                        Response.success(profile)
                    }
                val oldStores =
                    TestCall {
                        action()
                        Response.success(listOf(store))
                    }
                assertEquals(
                    LocaisResult.SessionChanged,
                    load(if (atNearby) api(nearbyCall = oldStores) else api(profileCall = oldProfile)),
                )
            }
        }
    }

    @Test
    fun renovacaoDaMesmaSessaoPermiteResposta() {
        val call =
            TestCall {
                assertTrue(sessions.renew(requireNotNull(sessions.snapshot()), jwt(signature = "renewed"), "new-refresh"))
                Response.success(listOf(store))
            }
        assertTrue(load(api(nearbyCall = call)) is LocaisResult.Loaded)
    }

    @Test
    fun falhaDeTransporteNaoViraSucessoNemOcultaTrocaDeSessao() {
        val call = DeferredCall<List<StoreResponse>>()
        var result: LocaisResult? = null
        LocaisRepository(api(nearbyCall = call), sessions).load { result = it }
        assertNull(result)
        call.fail()
        assertEquals(LocaisResult.Failure(), result)
        val old = DeferredCall<ProfileResponse>()
        LocaisRepository(api(profileCall = old), sessions).load { result = it }
        sessions.clear()
        old.fail()
        assertEquals(LocaisResult.SessionChanged, result)
    }

    @Test
    fun cancelamentoAlcancaCadaEtapaESuprimeEntrega() {
        val first = DeferredCall<ProfileResponse>()
        var delivered = false
        val request = LocaisRepository(api(profileCall = first), sessions).load { delivered = true }
        request.cancel()
        first.respond(profile)
        assertTrue(first.isCanceled)
        assertFalse(delivered)
        val next = DeferredCall<List<StoreResponse>>()
        val second = LocaisRepository(api(nearbyCall = next), sessions).load { delivered = true }
        second.cancel()
        next.respond(listOf(store))
        assertTrue(next.isCanceled)
        assertFalse(delivered)
    }

    @Test
    fun enviaBearerSnapshotERaioSemCoordenadas() {
        listOf(null, 0.1, 5.0, 100.0).forEach { radius ->
            val session = requireNotNull(sessions.snapshot())
            val methods = mutableListOf<String>()
            val api =
                Proxy.newProxyInstance(LocaisAPI::class.java.classLoader, arrayOf(LocaisAPI::class.java)) { _, method, args ->
                    methods += method.name
                    assertEquals("Bearer ${session.accessToken}", args[0])
                    assertEquals(session, args[1])
                    when (method.name) {
                        "profile" -> TestCall { Response.success(profile) }
                        "nearby" -> {
                            assertNull(args[2])
                            assertNull(args[3])
                            assertEquals(radius, args[4])
                            TestCall { Response.success(listOf(store)) }
                        }
                        else -> error("Endpoint inesperado")
                    }
                } as LocaisAPI
            var result: LocaisResult? = null
            LocaisRepository(api, sessions).load(radius) { result = it }
            assertEquals(listOf("profile", "nearby"), methods)
            assertTrue(result is LocaisResult.Loaded)
        }
    }

    @Test
    fun rejeitaRaiosInvalidosAntesDeConsultar() {
        val api = testApi<LocaisAPI> { error("Nao consultar") }
        listOf(0.0, -1.0, 100.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { radius ->
            assertTrue(runCatching { LocaisRepository(api, sessions).load(radius) {} }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    private fun addressText() = "Rua A, 10 - Centro - Cidade / SP"

    private fun <T> errorResponse(
        code: Int,
        message: String = "Erro",
    ): Response<T> = Response.error(code, ResponseBody.create(null, """{"status":$code,"message":"$message"}"""))
}

private class DeferredCall<T> : Call<T> {
    private var callback: Callback<T>? = null
    private var canceled = false

    fun respond(value: T) = requireNotNull(callback).onResponse(this, Response.success(value))

    fun fail() = requireNotNull(callback).onFailure(this, IOException("offline"))

    override fun enqueue(callback: Callback<T>) {
        this.callback = callback
    }

    override fun execute(): Response<T> = error("Apenas async")

    override fun cancel() {
        canceled = true
    }

    override fun isCanceled() = canceled

    override fun isExecuted() = callback != null

    override fun clone(): Call<T> = DeferredCall()

    override fun request(): Request = Request.Builder().url("https://core.example/").build()

    override fun timeout() = Timeout()
}
