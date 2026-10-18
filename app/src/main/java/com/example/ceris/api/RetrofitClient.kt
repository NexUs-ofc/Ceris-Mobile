package com.example.ceris.api

import com.example.ceris.BuildConfig
import com.example.ceris.local.SessionManager
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // O proxy do DisCloud corta em 30s e devolve uma pagina HTML de erro com
    // status 200. Passar disso nao adianta: so atrasa o inevitavel.
    private const val LEITURA_DEMORADA_SEGUNDOS = 45L

    private val retrofitInstances = mutableMapOf<String, Retrofit>()

    private val retrofitAutenticados = mutableMapOf<String, Retrofit>()

    @Volatile
    private var renovador: Authenticator? = null

    private var sessaoInterceptor: SessionRequestInterceptor? = null

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Liga a renovação automática de sessão. Chamada uma vez, na abertura do
     * app, porque precisa de um Context para o SessionManager.
     *
     * O cliente que renova é o comum, sem Authenticator: se o próprio
     * /token/refresh respondesse 401, um cliente com renovação tentaria
     * renovar a renovação, em círculo.
     */
    @Synchronized
    fun configurarRenovacao(sessionManager: SessionManager) {
        renovador = RenovadorDeSessao(
            sessionManager = sessionManager,
            api = getApi(ApiBaseUrl.auth, AuthAPI::class.java)
        )
        sessaoInterceptor = SessionRequestInterceptor(sessionManager)

        // Quem foi criado antes disto ficou sem renovação; refazer o cache
        // evita depender da ordem em que as telas pedem sua API.
        retrofitAutenticados.clear()
    }

    @Synchronized
    private fun getRetrofit(baseUrl: String): Retrofit {
        return retrofitInstances.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    /**
     * Cliente do chat. A Cerita passa por varios nos e demora mais que as
     * outras rotas, mas quem manda no teto e o DisCloud, que corta em 30s.
     */
    private val httpClientDemorado: OkHttpClient by lazy {
        httpClient.newBuilder()
            .readTimeout(LEITURA_DEMORADA_SEGUNDOS, TimeUnit.SECONDS)
            .callTimeout(LEITURA_DEMORADA_SEGUNDOS, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    private fun getRetrofitAutenticado(baseUrl: String): Retrofit {
        val autenticador = renovador ?: return getRetrofit(baseUrl)

        return retrofitAutenticados.getOrPut(baseUrl) {
            val interceptor = checkNotNull(sessaoInterceptor)
            val client =
                (if (baseUrl == ApiBaseUrl.ia) httpClientDemorado else httpClient)
                    .newBuilder()
                    .addInterceptor(interceptor)
                    .authenticator(autenticador)
                    .build()
            Retrofit.Builder()
                .baseUrl(baseUrl)
                // O interceptor roda depois da fila. Vincular antes dela evita
                // rejeitar chamadas de listas/chat quando outra chamada renova.
                .callFactory(interceptor.vincularChamadas(client))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    fun <T> getApi(
        baseUrl: String,
        api: Class<T>
    ): T {
        return getRetrofit(baseUrl).create(api)
    }

    /**
     * Para as APIs que exigem Bearer: um 401 renova a sessão e repete a
     * chamada sozinho, sem a tela precisar saber.
     */
    fun <T> getApiAutenticada(
        baseUrl: String,
        api: Class<T>
    ): T {
        return getRetrofitAutenticado(baseUrl).create(api)
    }
}
