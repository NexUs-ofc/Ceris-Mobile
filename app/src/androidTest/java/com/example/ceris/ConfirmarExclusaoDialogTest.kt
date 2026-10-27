package com.example.ceris

import android.graphics.Bitmap
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.view.LoginActivity
import com.example.ceris.view.fragment.ConfirmarExclusaoDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class ConfirmarExclusaoDialogTest {
    @Test
    fun cancelarNaoPublicaExclusaoEConfirmarPreservaIdAposRecriar() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            var results = 0
            scenario.onActivity { activity ->
                activity.supportFragmentManager.setFragmentResultListener("qa_excluir", activity) { _, _ -> results++ }
                ConfirmarExclusaoDialog.novo("Excluir item?", "Arroz QA", "2 unidades", "qa_excluir", "qa-id")
                    .showNow(activity.supportFragmentManager, ConfirmarExclusaoDialog.TAG)
                val dialog = activity.supportFragmentManager.findFragmentByTag(ConfirmarExclusaoDialog.TAG) as ConfirmarExclusaoDialog
                assertEquals("Arroz QA", dialog.requireDialog().findViewById<TextView>(R.id.nome_exclusao).text.toString())
                dialog.requireDialog().findViewById<TextView>(R.id.btn_cancelar_exclusao).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                assertEquals(0, results)
                ConfirmarExclusaoDialog.novo("Excluir lista?", "Lista QA", "A lista e seus itens serão excluídos.", "qa_excluir", "qa-id")
                    .showNow(activity.supportFragmentManager, ConfirmarExclusaoDialog.TAG)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.setFragmentResultListener("qa_excluir", activity) { _, result ->
                    assertEquals("qa-id", result.getString(ConfirmarExclusaoDialog.ID))
                    results++
                }
                val dialog = activity.supportFragmentManager.findFragmentByTag(ConfirmarExclusaoDialog.TAG) as ConfirmarExclusaoDialog
                assertNotNull(dialog.dialog)
            }
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.waitForIdleSync()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            assertNotNull(screenshot)
            val output = File(instrumentation.targetContext.getExternalFilesDir(null), "qa-exclusao.png")
            output.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
            scenario.onActivity { activity ->
                val dialog = activity.supportFragmentManager.findFragmentByTag(ConfirmarExclusaoDialog.TAG) as ConfirmarExclusaoDialog
                dialog.requireDialog().findViewById<android.view.View>(R.id.btn_confirmar_exclusao).performClick()
                assertEquals(1, results)
            }
        }
    }
}
