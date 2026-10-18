package com.example.ceris

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FirebaseConfigurationTest {
    @Test
    fun configuracaoAndroidPertenceAoProjetoConferidoNoNotify() {
        val file = File("google-services.json").takeIf { it.isFile } ?: File("app/google-services.json")
        val config = Gson().fromJson(file.readText(), JsonObject::class.java)
        assertEquals("ceris-bfbfe", config.getAsJsonObject("project_info").get("project_id").asString)
        assertTrue(
            config.getAsJsonArray("client").any {
                it.asJsonObject.getAsJsonObject("client_info").getAsJsonObject("android_client_info")
                    .get("package_name").asString == BuildConfig.APPLICATION_ID
            },
        )
    }
}
