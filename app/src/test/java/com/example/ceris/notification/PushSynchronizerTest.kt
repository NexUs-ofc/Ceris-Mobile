package com.example.ceris.notification

import com.example.ceris.api.NotifyAPI
import com.example.ceris.local.SessionManager
import com.example.ceris.local.SessionSnapshot
import com.example.ceris.model.dto.PushInstallationResponse
import com.example.ceris.model.dto.RegisterPushInstallationRequest
import com.example.ceris.repository.PushInstallationRepository
import com.example.ceris.support.MemoryPreferences
import com.example.ceris.support.TestCall
import com.example.ceris.support.jwt
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy

class PushSynchronizerTest {
    private val manager = SessionManager(MemoryPreferences()).apply { startSession(jwt(), "refresh") }
    private val state = PushState(MemoryPreferences()).apply { registered("fid") }
    private var permitted = true
    private var puts = 0
    private val revocations = mutableListOf<Pair<String, String>>()
    private var respond: () -> Response<PushInstallationResponse> = { Response.success(PushInstallationResponse("mongo-id", "ACTIVE")) }
    private val api =
        Proxy.newProxyInstance(NotifyAPI::class.java.classLoader, arrayOf(NotifyAPI::class.java)) { _, method, args ->
            when (method.name) {
                "registrarInstalacao" ->
                    TestCall {
                        puts++
                        assertEquals(state.installationId, (args!![1] as RegisterPushInstallationRequest).installationId)
                        assertNotNull(args[2] as SessionSnapshot?)
                        respond()
                    }
                "desvincularInstalacao" ->
                    TestCall<Void> {
                        revocations += (args!![0] as String) to (args[1] as String)
                        Response.success<Void>(204, null)
                    }
                else -> error("Metodo inesperado: ${method.name}")
            }
        } as NotifyAPI
    private val synchronizer = PushSynchronizer(PushInstallationRepository(api, manager, true, api), manager, state, { permitted }, {})

    @Test
    fun guardaIdInternoEFidSeparadamente() {
        assertFalse(synchronizer.synchronize())
        assertEquals("mongo-id", state.binding()?.id)
        assertEquals("fid", state.binding()?.fid)
        assertEquals(manager.snapshot()?.generation, state.binding()?.generation)
        assertTrue(revocations.isEmpty())
    }

    @Test
    fun naoCadastraSemPermissaoOuSessao() {
        permitted = false
        assertFalse(synchronizer.synchronize())
        permitted = true
        manager.clear()
        assertFalse(synchronizer.synchronize())
        assertEquals(0, puts)
    }

    @Test
    fun falhasTemporariasPedemRetrySemApagarSessao() {
        listOf(408, 429, 500, 503).forEach { code ->
            respond = { Response.error(code, ResponseBody.create(MediaType.parse("application/json"), "{}")) }
            assertTrue(synchronizer.synchronize())
            assertNotNull(manager.snapshot())
        }
        respond = { throw IOException("offline") }
        assertTrue(synchronizer.synchronize())
        assertNull(state.binding())
    }

    @Test
    fun errosPermanentesNaoGeramLoopDeRetry() {
        listOf(400, 401, 403, 404).forEach { code ->
            respond = { Response.error(code, ResponseBody.create(MediaType.parse("application/json"), "{}")) }
            assertFalse(synchronizer.synchronize())
        }
        assertNull(state.binding())
    }

    @Test
    fun logoutDurantePutDesvinculaRespostaTardiaComCredencialAntiga() {
        val old = manager.snapshot()!!
        respond = {
            manager.clear()
            Response.success(PushInstallationResponse("mongo-id", "ACTIVE"))
        }
        assertFalse(synchronizer.synchronize())
        assertNull(state.binding())
        assertEquals(listOf("Bearer ${old.accessToken}" to "mongo-id"), revocations)
    }

    @Test
    fun trocaDeContaDurantePutNaoSalvaVinculoNaNovaConta() {
        val old = manager.snapshot()!!
        respond = {
            manager.startSession(jwt("99"), "refresh-99")
            Response.success(PushInstallationResponse("mongo-id", "ACTIVE"))
        }
        assertTrue(synchronizer.synchronize())
        assertEquals(99, manager.snapshot()?.profileId)
        assertNull(state.binding())
        assertEquals("Bearer ${old.accessToken}", revocations.single().first)
    }

    @Test
    fun permissaoRevogadaDurantePutNaoMantemCadastro() {
        respond = {
            permitted = false
            Response.success(PushInstallationResponse("mongo-id", "ACTIVE"))
        }
        assertFalse(synchronizer.synchronize())
        assertNull(state.binding())
        assertEquals("mongo-id", revocations.single().second)
    }

    @Test
    fun trocaDeFidRemoveCadastroAnteriorDepoisDeCadastrarONovo() {
        state.bind(manager.snapshot()!!, "old-fid", "old-mongo-id")
        assertFalse(synchronizer.synchronize())
        assertEquals("fid", state.binding()?.fid)
        assertEquals("old-mongo-id", revocations.single().second)
    }

    @Test
    fun desvinculacaoNaoUsaCredencialDaContaNova() {
        val old = manager.snapshot()!!
        val binding = PushState.Binding("mongo-id", "fid", old.generation, 12)
        manager.clear()
        manager.startSession(jwt("99"), "refresh-99")
        synchronizer.revoke(binding, old)
        assertEquals("Bearer ${old.accessToken}" to "mongo-id", revocations.single())
        assertEquals(99, manager.snapshot()?.profileId)
    }

    @Test
    fun limpezaAntigaNaoDesvinculaDocumentoReutilizadoPorNovoLoginNaMesmaConta() {
        val old = manager.snapshot()!!
        val binding = PushState.Binding("mongo-id", "fid", old.generation, 12)
        manager.startSession(jwt(), "new-refresh")
        state.bind(manager.snapshot()!!, "fid", "mongo-id")
        synchronizer.revoke(binding, old)
        assertTrue(revocations.isEmpty())
        assertEquals(manager.snapshot()?.generation, state.binding()?.generation)
    }

    @Test
    fun corpoAusenteOuStatusInesperadoNaoCriaVinculo() {
        respond = { Response.success(null) }
        assertFalse(synchronizer.synchronize())
        respond = { Response.success(PushInstallationResponse("mongo-id", "INVALID")) }
        assertFalse(synchronizer.synchronize())
        assertNull(state.binding())
    }
}
