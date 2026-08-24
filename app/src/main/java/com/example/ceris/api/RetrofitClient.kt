package com.example.ceris.api

import com.example.ceris.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val retrofitInstances = mutableMapOf<String, Retrofit>()

    private fun getRetrofit(baseUrl: String): Retrofit {
        return retrofitInstances.getOrPut(baseUrl) {
            Retrofit.Builder()
                .baseUrl(baseUrl)
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