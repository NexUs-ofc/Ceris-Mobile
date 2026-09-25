package com.example.ceris.api

import com.example.ceris.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val retrofitInstances = mutableMapOf<String, Retrofit>()

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(ColdStartInterceptor())
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun getRetrofit(baseUrl: String): Retrofit {
        return retrofitInstances.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
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

}

/**
 * A API roda em função serverless: a JVM leva cerca de 30s para subir e a
 * plataforma desiste antes disso, devolvendo 500 com o header x-vercel-error.
 * Nesse cenário a aplicação nem chegou a receber a requisição, então repetir é
 * seguro. Erro produzido pela própria aplicação vem como JSON, sem esse header,
 * e nunca é repetido — para não duplicar cadastro nem vínculo de conta.
 */
private class ColdStartInterceptor(
    private val maxTentativas: Int = 3,
    private val esperaMillis: Long = 8_000
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var resposta = chain.proceed(chain.request())
        var tentativa = 1

        while (falhaDaPlataforma(resposta) && tentativa < maxTentativas) {
            resposta.close()
            Thread.sleep(esperaMillis)
            tentativa++
            resposta = chain.proceed(chain.request())
        }

        return resposta
    }

    private fun falhaDaPlataforma(resposta: Response): Boolean =
        resposta.code() == 500 && !resposta.header("x-vercel-error").isNullOrBlank()
}
