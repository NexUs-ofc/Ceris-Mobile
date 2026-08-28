package com.example.ceris.repository

import android.util.Log
import com.example.ceris.api.ViaCEPAPI
import com.example.ceris.model.ViaCEPResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViaCEPRepository(
    private val api: ViaCEPAPI
) {
    fun getAddressByCEP(
        cep: String,
        onSuccess: (ViaCEPResponse) -> Unit,
        onError: (statusCode: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        Log.d("debug", cep)
        api.getAddressByCEP(cep)
            .enqueue(object : Callback<ViaCEPResponse> {
                override fun onResponse(
                    call: Call<ViaCEPResponse>,
                    response: Response<ViaCEPResponse>
                ) {
                    val body = response.body()
                    Log.d("debug", body.toString())
                    if (response.isSuccessful && body != null) {
                        onSuccess(body)
                    } else {
                        onError(response.code())
                    }
                }

                override fun onFailure(call: Call<ViaCEPResponse>, t: Throwable) {
                    onFailure(Exception(t.message))
                }
            })
    }
}
