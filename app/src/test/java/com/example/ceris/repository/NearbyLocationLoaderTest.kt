package com.example.ceris.repository

import com.example.ceris.location.DeviceCoordinates
import com.example.ceris.location.DeviceLocationResult
import com.example.ceris.location.LocationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NearbyLocationLoaderTest {
    private var owner: String? = "first"
    private lateinit var locationCallback: (DeviceLocationResult) -> Unit
    private lateinit var httpCallback: (LocaisResult) -> Unit
    private val points = mutableListOf<DeviceCoordinates>()
    private var locationCanceled = 0
    private var httpCanceled = 0
    private val source =
        LocationSource { callback ->
            locationCallback = callback
            val cancel = {
                locationCanceled++
                Unit
            }
            cancel
        }
    private val loader =
        NearbyLocationLoader(source, { owner }) { point, callback ->
            points += point
            httpCallback = callback
            val cancel = {
                httpCanceled++
                Unit
            }
            cancel
        }
    private val point = DeviceCoordinates(-23.5, -46.6)

    @Test
    fun aguardaLocalizacaoEEncaminhaAsDuasCoordenadasSemAlterarAPrecisao() {
        var received: LocaisResult? = null
        loader.load { received = it }
        assertEquals(0, points.size)
        assertNull(received)
        locationCallback(DeviceLocationResult.Available(point))
        assertEquals(listOf(point), points)
        httpCallback(LocaisResult.Empty(""))
        assertEquals(LocaisResult.Empty(""), received)
    }

    @Test
    fun permissaoNegadaOuLocalizacaoIndisponivelNaoDisparaHttp() {
        mapOf(
            DeviceLocationResult.PermissionRequired to LocaisResult.LocationPermissionRequired,
            DeviceLocationResult.Unavailable to LocaisResult.LocationUnavailable,
        ).forEach { (location, expected) ->
            var received: LocaisResult? = null
            loader.load { received = it }
            locationCallback(location)
            assertEquals(expected, received)
            assertEquals(0, points.size)
        }
    }

    @Test
    fun cancelaCapturaSemIniciarHttpMesmoSeCallbackAtrasadoChegar() {
        var received: LocaisResult? = null
        val cancel = loader.load { received = it }
        cancel()
        locationCallback(DeviceLocationResult.Available(point))
        assertEquals(1, locationCanceled)
        assertEquals(0, points.size)
        assertNull(received)
    }

    @Test
    fun cancelaHttpEDescartaRespostaDepoisDeSair() {
        var received: LocaisResult? = null
        val cancel = loader.load { received = it }
        locationCallback(DeviceLocationResult.Available(point))
        received = null
        cancel()
        httpCallback(LocaisResult.Empty(""))
        assertEquals(1, httpCanceled)
        assertNull(received)
    }

    @Test
    fun trocaDeContaDuranteCapturaNaoConsultaComLocalizacaoDaSessaoAnterior() {
        var received: LocaisResult? = null
        loader.load { received = it }
        owner = "second"
        locationCallback(DeviceLocationResult.Available(point))
        assertEquals(LocaisResult.SessionChanged, received)
        assertEquals(0, points.size)
    }

    @Test
    fun localizacaoDuplicadaNaoDuplicaHttp() {
        loader.load {}
        repeat(2) { locationCallback(DeviceLocationResult.Available(point)) }
        assertEquals(1, points.size)
    }

    @Test
    fun fallbackSomenteDepoisDaTentativaSemPosicaoERetornaAoGpsQuandoDisponivel() {
        val events = mutableListOf<LocaisResult>()
        var fallbackCalls = 0
        var profileResponse: ((LocaisResult) -> Unit)? = null
        val streaming =
            NearbyLocationLoader(
                source,
                { owner },
                fallback = { callback ->
                    fallbackCalls++
                    val cancellation = { Unit }
                    profileResponse = callback
                    cancellation
                },
            ) { point, callback ->
                points += point
                val cancellation = { Unit }
                httpCallback = callback
                cancellation
            }
        streaming.load(events::add)
        assertEquals(0, fallbackCalls)
        locationCallback(DeviceLocationResult.Unavailable)
        assertEquals(1, fallbackCalls)
        assertEquals(LocaisResult.QueryStarted(true), events.last())
        profileResponse!!(LocaisResult.Empty(""))
        repeat(3) { locationCallback(DeviceLocationResult.Unavailable) }
        assertEquals(1, fallbackCalls)
        locationCallback(DeviceLocationResult.Available(point))
        assertEquals(listOf(point), points)
        assertEquals(LocaisResult.QueryStarted(false), events.last())
        profileResponse!!(LocaisResult.Failure(400))
        assertEquals(LocaisResult.QueryStarted(false), events.last())
        httpCallback(LocaisResult.Empty(""))
        assertEquals(LocaisResult.Empty(""), events.last())
    }

    @Test
    fun pedePermissaoAntesDePermitirFallbackAposRecusa() {
        var allowed = false
        var calls = 0
        var received: LocaisResult? = null
        val streaming =
            NearbyLocationLoader(
                source,
                { owner },
                fallback = {
                    calls++;
                    { Unit }
                },
                permissionFallbackAllowed = { allowed },
            ) { _, _ -> error("Sem coordenadas") }
        streaming.load { received = it }
        locationCallback(DeviceLocationResult.PermissionRequired)
        assertEquals(LocaisResult.LocationPermissionRequired, received)
        assertEquals(0, calls)
        allowed = true
        locationCallback(DeviceLocationResult.PermissionRequired)
        assertEquals(1, calls)
        assertEquals(LocaisResult.QueryStarted(true), received)
    }

    @Test
    fun movimentoRenovaConsultaEDescartaRespostaDaOrigemAnterior() {
        var time = 0L
        var received: LocaisResult? = null
        val streaming =
            NearbyLocationLoader(source, { owner }, nowMs = { time }) { point, callback ->
                points += point
                val cancellation = {
                    httpCanceled++
                    Unit
                }
                httpCallback = callback
                cancellation
            }
        streaming.load { received = it }
        locationCallback(DeviceLocationResult.Available(point))
        val oldReply = httpCallback
        time = 16_000
        locationCallback(DeviceLocationResult.Available(point.copy(latitude = -23.502)))
        assertEquals(2, points.size)
        assertEquals(1, httpCanceled)
        received = null
        oldReply(LocaisResult.Failure())
        assertNull(received)
        httpCallback(LocaisResult.Empty(""))
        assertEquals(LocaisResult.Empty(""), received)
    }

    @Test
    fun fallbackRenovaNoMaximoUmaVezPorMinutoECancelamentoImpedeRespostas() {
        var time = 0L
        var calls = 0
        var received: LocaisResult? = null
        val streaming =
            NearbyLocationLoader(
                source,
                { owner },
                nowMs = { time },
                fallback = { callback ->
                    calls++
                    val cancellation = {
                        httpCanceled++
                        Unit
                    }
                    httpCallback = callback
                    cancellation
                },
            ) { _, _ -> error("Sem coordenadas") }
        val cancel = streaming.load { received = it }
        locationCallback(DeviceLocationResult.Unavailable)
        time = 59_999
        locationCallback(DeviceLocationResult.Unavailable)
        assertEquals(1, calls)
        time = 60_000
        locationCallback(DeviceLocationResult.Unavailable)
        assertEquals(2, calls)
        cancel()
        received = null
        httpCallback(LocaisResult.Empty(""))
        locationCallback(DeviceLocationResult.Unavailable)
        assertNull(received)
        assertEquals(2, calls)
        assertEquals(1, locationCanceled)
    }
}
