package com.example.ceris.repository

import android.util.Log
import com.example.ceris.api.ViaCEPAPI
import com.example.ceris.model.dto.ViaCEPResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViaCEPRepository(
    private val api: ViaCEPAPI
) {
    companion object {
        private const val TAG = "ViaCEPRepository"
    }

    fun getAddressByCEP(
        cep: String,
        onSuccess: (ViaCEPResponse) -> Unit,
        onError: (statusCode: Int) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        Log.d(TAG, "Consultando CEP: $cep")
        api.getAddressByCEP(cep)
            .enqueue(object : Callback<ViaCEPResponse> {
                override fun onResponse(
                    call: Call<ViaCEPResponse>,
                    response: Response<ViaCEPResponse>
                ) {
                    val body = response.body()
                    Log.d(TAG, "Resposta ViaCEP (${response.code()}): $body")
                    if (response.isSuccessful && body != null && body.isValid()) {
                        onSuccess(body)
                    } else {
                        onError(response.code())
                    }
                }

                override fun onFailure(call: Call<ViaCEPResponse>, t: Throwable) {
                    Log.e(TAG, "Falha ao consultar CEP", t)
                    onFailure(Exception(t.message))
                }
            })
    }
}
