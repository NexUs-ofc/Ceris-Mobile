package com.example.ceris

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.debug.FigmaPreviewActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FigmaResponsiveTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun telasEstaticasDoFigmaCabemNaJanela() {
        val previews = listOf(
            "login" to R.layout.activity_login,
            "cadastro" to R.layout.activity_register,
            "esqueci-senha" to R.layout.activity_forgot_password,
            "redefinir-senha" to R.layout.activity_reset_password,
            "endereco" to R.layout.activity_set_address,
            "salvando-endereco" to R.layout.activity_address_loading,
            "carregando-login" to R.layout.activity_login_loading,
            "carregando-otp" to R.layout.activity_otp_loading,
            "erro" to R.layout.activity_erro,
            "otp-email" to R.layout.activity_email_verification,
            "otp-senha" to R.layout.activity_reset_password_otp,
            "splash" to R.layout.activity_splash_screen,
            "estoque" to R.layout.fragment_estoque,
            "locais" to R.layout.fragment_locais,
            "listas" to R.layout.fragment_listas,
            "receitas" to R.layout.fragment_receitas,
            "perfil" to R.layout.fragment_perfil,
            "chat" to R.layout.activity_chatbot,
            "detalhe-lista" to R.layout.activity_lista_compras,
            "detalhe-receita" to R.layout.activity_detalhe_receita,
            "scanner" to R.layout.fragment_nota_fiscal,
        )

        previews.forEach { (name, layout) ->
            val intent = Intent(context, FigmaPreviewActivity::class.java)
                .putExtra(FigmaPreviewActivity.EXTRA_LAYOUT, layout)
            ActivityScenario.launch<FigmaPreviewActivity>(intent).use { scenario ->
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val root = activity.findViewById<View>(R.id.main)
                        ?: activity.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
                    assertTrue("$name: raiz sem largura", root.width > 0)
                    val window = Rect()
                    root.getGlobalVisibleRect(window)
                    idsCriticos(layout).forEach { id ->
                        val view = activity.findViewById<View>(id) ?: return@forEach
                        if (view.visibility != View.VISIBLE) return@forEach
                        view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
                        val rect = Rect()
                        assertTrue("$name: view $id não está visível", view.getGlobalVisibleRect(rect))
                        assertTrue("$name: view $id ultrapassa a esquerda", rect.left >= window.left)
                        assertTrue("$name: view $id ultrapassa a direita", rect.right <= window.right)
                        assertTrue("$name: view $id ultrapassa o topo", rect.top >= window.top)
                        assertTrue("$name: view $id ultrapassa a base", rect.bottom <= window.bottom)
                    }
                }
                screenshot("figma-$name")
            }
        }
    }

    private fun idsCriticos(layout: Int): IntArray = when (layout) {
        R.layout.activity_login -> intArrayOf(R.id.etl_email, R.id.etl_senha, R.id.loginBtn)
        R.layout.activity_register -> intArrayOf(R.id.etl_nome, R.id.etl_email, R.id.etl_senha, R.id.registerBtn)
        R.layout.activity_forgot_password -> intArrayOf(R.id.etl_email, R.id.sendCodeBtn)
        R.layout.activity_reset_password -> intArrayOf(
            R.id.etl_new_password, R.id.etl_confirm_password, R.id.resetPasswordBtn,
        )
        R.layout.activity_set_address -> intArrayOf(
            R.id.headerLayout, R.id.etl_cep, R.id.etl_rua, R.id.etl_numero,
            R.id.etl_bairro, R.id.etl_cidade, R.id.etl_estado, R.id.registerBtn,
        )
        R.layout.activity_email_verification,
        R.layout.activity_reset_password_otp -> intArrayOf(R.id.titleText, R.id.subtitleText, R.id.otpInputsRow, R.id.confirmCodeBtn)
        R.layout.activity_address_loading -> intArrayOf(R.id.titleText, R.id.spinnerContainer)
        R.layout.activity_login_loading,
        R.layout.activity_otp_loading -> intArrayOf(R.id.titleText, R.id.spinnerContainer)
        R.layout.activity_erro -> intArrayOf(R.id.conteudo, R.id.btn_tentar, R.id.btn_inicio)
        R.layout.activity_splash_screen -> intArrayOf(R.id.logo, R.id.title, R.id.legends)
        R.layout.fragment_estoque -> intArrayOf(R.id.titulo, R.id.campo_busca, R.id.filtros)
        R.layout.fragment_locais -> intArrayOf(R.id.cabecalho, R.id.titulo_proximos, R.id.lista_locais)
        R.layout.fragment_listas -> intArrayOf(R.id.titulo_listas, R.id.lista_listas, R.id.btn_criar_lista)
        R.layout.fragment_receitas -> intArrayOf(R.id.titulo_receitas, R.id.lista_receitas)
        R.layout.fragment_perfil -> intArrayOf(R.id.img_perfil, R.id.txt_nome_perfil, R.id.opcoes_perfil)
        R.layout.activity_chatbot -> intArrayOf(R.id.cabecalho, R.id.lista_mensagens, R.id.barra_envio)
        R.layout.activity_lista_compras -> intArrayOf(
            R.id.txt_titulo, R.id.painel_progresso, R.id.lista_itens, R.id.btn_adicionar_item,
        )
        R.layout.activity_detalhe_receita -> intArrayOf(
            R.id.cabecalho_receita, R.id.conteudo_receita,
        )
        R.layout.fragment_nota_fiscal -> intArrayOf(
            R.id.grupo_modo, R.id.btn_flash, R.id.controles_camera,
        )
        else -> intArrayOf()
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val directory = File(context.getExternalFilesDir(null), "figma-tests").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
