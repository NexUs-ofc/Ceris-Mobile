package com.example.ceris.api.openfoodfacts

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Cliente proprio, sem o token do app: a API externa nao deve receber a sessao. */
object OpenFoodFactsClient {

    val api: OpenFoodFactsAPI by lazy {
        Retrofit.Builder()
            .baseUrl(OpenFoodFactsAPI.BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .callTimeout(20, TimeUnit.SECONDS)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsAPI::class.java)
    }
}
