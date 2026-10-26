package com.example.ceris.viewmodel

import com.example.ceris.model.Local
import com.example.ceris.repository.AddressProblem
import com.example.ceris.repository.LocaisResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaisViewModelTest {
    private var generation: String? = "first"
    private val callbacks = mutableListOf<(LocaisResult) -> Unit>()
    private var cancelled = 0
    private val model =
        LocaisViewModel({ generation }) { deliver ->
            val cancelAction = {
                cancelled++
                Unit
            }
            callbacks += deliver
            cancelAction
        }
    private val local = Local("45", "Mercado", "Rua B", "450 m")
    private val content = LocaisResult.Loaded("Rua A", listOf(local))

    @Test
    fun carregaUmaVezAoCriarRetomarOuTrocarDeAba() {
        model.onVisible()
        assertEquals(LocaisStatus.LOADING, model.state.value.status)
        model.onVisible()
        assertEquals(1, callbacks.size)
        callbacks.single()(content)
        repeat(3) { model.onVisible() }
        assertEquals(1, callbacks.size)
        assertEquals(LocaisUiState(LocaisStatus.CONTENT, "Rua A", listOf(local)), model.state.value)
    }

    @Test
    fun falhaPermiteRetryEVazioNaoMostraCards() {
        model.onVisible()
        callbacks[0](LocaisResult.Failure(503))
        assertEquals(LocaisStatus.ERROR, model.state.value.status)
        model.refresh()
        assertEquals(LocaisStatus.LOADING, model.state.value.status)
        callbacks[1](LocaisResult.Empty("Rua A"))
        assertEquals(LocaisStatus.EMPTY, model.state.value.status)
        assertEquals("Rua A", model.state.value.address)
        assertTrue(model.state.value.locais.isEmpty())
    }

    @Test
    fun respostaMaisRecentePrevaleceMesmoSeCanceladaAindaResponder() {
        model.onVisible()
        model.refresh()
        assertEquals(1, cancelled)
        callbacks[1](LocaisResult.Empty("Novo endereco"))
        callbacks[0](content)
        assertEquals(LocaisUiState(LocaisStatus.EMPTY, "Novo endereco"), model.state.value)
    }

    @Test
    fun enderecoSalvoAtualizaDadosMasRetornoSemSalvarMantemCache() {
        model.onVisible()
        callbacks[0](content)
        model.onVisible()
        assertEquals(1, callbacks.size)
        model.refresh()
        assertEquals("Rua A", model.state.value.address)
        assertTrue(model.state.value.locais.isEmpty())
        callbacks[1](LocaisResult.Loaded("Endereco salvo", listOf(local.copy(id = "99"))))
        assertEquals("Endereco salvo", model.state.value.address)
        assertEquals("99", model.state.value.locais.single().id)
    }

    @Test
    fun distingueEnderecoAusenteNaoGeocodificadoESessaoInvalida() {
        mapOf(
            LocaisResult.AddressRequired(AddressProblem.MISSING) to LocaisStatus.ADDRESS_MISSING,
            LocaisResult.AddressRequired(AddressProblem.NOT_GEOCODED, "Rua A") to LocaisStatus.NOT_GEOCODED,
            LocaisResult.AuthenticationRequired to LocaisStatus.AUTH_REQUIRED,
            LocaisResult.SessionChanged to LocaisStatus.AUTH_REQUIRED,
        ).forEach { (result, status) ->
            model.refresh()
            callbacks.last()(result)
            assertEquals(status, model.state.value.status)
            assertTrue(model.state.value.locais.isEmpty())
        }
    }

    @Test
    fun logoutLimpaEnderecoEListaSemConsultarNovamente() {
        model.onVisible()
        callbacks[0](content)
        generation = null
        model.onVisible()
        assertEquals(LocaisUiState(LocaisStatus.AUTH_REQUIRED), model.state.value)
        callbacks[0](content)
        assertEquals(LocaisStatus.AUTH_REQUIRED, model.state.value.status)
        assertEquals(1, callbacks.size)
    }

    @Test
    fun trocaDeContaDescartaCacheERespostaDaContaAnterior() {
        model.onVisible()
        callbacks[0](content)
        generation = "second"
        model.onVisible()
        assertEquals(LocaisUiState(LocaisStatus.LOADING), model.state.value)
        callbacks[0](content)
        assertEquals(LocaisStatus.LOADING, model.state.value.status)
        callbacks[1](LocaisResult.Empty("Outra conta"))
        assertEquals("Outra conta", model.state.value.address)
    }

    @Test
    fun trocaDeSessaoDuranteRespostaNuncaEntregaSucessoAntigo() {
        model.onVisible()
        generation = "second"
        callbacks[0](content)
        assertEquals(2, callbacks.size)
        assertEquals(LocaisUiState(LocaisStatus.LOADING), model.state.value)
        callbacks[1](LocaisResult.Empty("Nova origem"))
        assertEquals("Nova origem", model.state.value.address)
    }

    @Test
    fun respostaSincronaNaoFicaPresaEmCarregamento() {
        val immediate =
            LocaisViewModel({ "current" }) { callback ->
                val noOp = { Unit }
                callback(content)
                noOp
            }
        immediate.onVisible()
        immediate.onVisible()
        assertEquals(LocaisStatus.CONTENT, immediate.state.value.status)
    }

    @Test
    fun sairDuranteCapturaCancelaERetomarIniciaUmaNovaConsulta() {
        model.onVisible()
        model.onNotVisible()
        assertEquals(1, cancelled)
        assertEquals(LocaisStatus.IDLE, model.state.value.status)
        callbacks[0](content)
        assertEquals(LocaisStatus.IDLE, model.state.value.status)
        model.onVisible()
        assertEquals(2, callbacks.size)
    }

    @Test
    fun permissaoELocalizacaoIndisponivelTemEstadosProprios() {
        mapOf(
            LocaisResult.LocationPermissionRequired to LocaisStatus.LOCATION_PERMISSION,
            LocaisResult.LocationUnavailable to LocaisStatus.LOCATION_UNAVAILABLE,
        ).forEach { (result, status) ->
            model.refresh()
            callbacks.last()(result)
            assertEquals(status, model.state.value.status)
            assertTrue(model.state.value.locais.isEmpty())
        }
    }

    @Test
    fun sairDepoisDoSucessoParaGpsERetornarReiniciaCaptura() {
        model.onVisible()
        callbacks[0](content)
        model.onNotVisible()
        assertEquals(1, cancelled)
        callbacks[0](LocaisResult.Empty("Resposta atrasada"))
        assertEquals("Rua A", model.state.value.address)
        model.onVisible()
        assertEquals(2, callbacks.size)
        callbacks[1](LocaisResult.Empty("Nova captura"))
        assertEquals("Nova captura", model.state.value.address)
    }

    @Test
    fun distingueOrigemFallbackEApagaCardsQuandoOrigemMuda() {
        model.onVisible()
        callbacks[0](LocaisResult.QueryStarted(true))
        callbacks[0](LocaisResult.Loaded("", listOf(local)))
        assertTrue(model.state.value.usesProfileAddress)
        callbacks[0](LocaisResult.QueryStarted(false))
        assertEquals(LocaisStatus.LOADING, model.state.value.status)
        assertTrue(model.state.value.locais.isEmpty())
        callbacks[0](LocaisResult.Empty(""))
        assertEquals(LocaisUiState(LocaisStatus.EMPTY), model.state.value)
    }

    @Test
    fun alterarOrigemCancelaBuscaAnteriorEConsultaEnderecoSalvoSemGps() {
        val device = mutableListOf<(LocaisResult) -> Unit>()
        val saved = mutableListOf<(LocaisResult) -> Unit>()
        var canceled = 0
        val choosing = LocaisViewModel(
            generation = { "current" },
            load = { callback ->
                val cancel = { canceled++; Unit }
                device += callback
                cancel
            },
            loadSavedAddress = { callback ->
                val cancel = { canceled++; Unit }
                saved += callback
                cancel
            },
        )

        choosing.onVisible()
        choosing.selectSavedAddress()
        assertEquals(1, canceled)
        assertEquals(1, saved.size)
        assertTrue(choosing.state.value.usesProfileAddress)
        saved[0](LocaisResult.Loaded("Rua salva, 123", listOf(local)))
        device[0](LocaisResult.Empty("resposta antiga"))
        assertEquals("Rua salva, 123", choosing.state.value.address)

        choosing.selectDeviceLocation()
        assertEquals(2, device.size)
        assertEquals(2, canceled)
        assertTrue(!choosing.state.value.usesProfileAddress)
        device[1](LocaisResult.Empty(""))
        assertEquals(LocaisStatus.EMPTY, choosing.state.value.status)
    }
}
