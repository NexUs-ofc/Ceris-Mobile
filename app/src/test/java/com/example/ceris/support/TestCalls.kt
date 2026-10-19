package com.example.ceris.support

import okhttp3.Request
import okio.ByteString
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.reflect.Proxy

fun jwt(
    profileId: String = "12",
    signature: String = "signature",
): String = "header.${ByteString.encodeUtf8("{\"sub\":\"$profileId\"}").base64Url()}.$signature"

class TestCall<T>(private val response: () -> Response<T>) : Call<T> {
    private var executed = false
    private var cancelled = false

    override fun execute(): Response<T> {
        executed = true
        return response()
    }

    override fun enqueue(callback: Callback<T>) {
        callback.onResponse(this, execute())
    }

    override fun isExecuted(): Boolean = executed

    override fun isCanceled(): Boolean = cancelled

    override fun cancel() {
        cancelled = true
    }

    override fun clone(): Call<T> = TestCall(response)

    override fun request(): Request = Request.Builder().url("https://notify.example/").build()

    override fun timeout(): Timeout = Timeout()
}

inline fun <reified T> testApi(crossinline respond: (String) -> Any): T =
    Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ ->
        respond(method.name)
    } as T
