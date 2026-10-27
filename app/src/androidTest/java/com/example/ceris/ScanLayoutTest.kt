package com.example.ceris

import android.Manifest
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.debug.TelaDeTesteActivity
import com.example.ceris.local.SessionManager
import com.example.ceris.view.MainActivity
import com.example.ceris.view.fragment.EstoqueFragment
import com.example.ceris.view.fragment.NotaFiscalFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanLayoutTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun allowCamera() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)
    }

    @Test
    fun scannerStartsWithBarcodeAndSelectorDoesNotOverlapFlash() {
        val intent = Intent(context, TelaDeTesteActivity::class.java)
            .putExtra(TelaDeTesteActivity.EXTRA_TELA, TelaDeTesteActivity.TELA_NOTA)
        ActivityScenario.launch<TelaDeTesteActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val group = activity.findViewById<MaterialButtonToggleGroup>(R.id.grupo_modo)
                val flash = activity.findViewById<android.view.View>(R.id.btn_flash)
                assertEquals(R.id.modo_codigo, group.checkedButtonId)
                assertTrue(group.width > 0)
                assertTrue("selector must stay below flash", group.top >= flash.bottom)
                assertTrue("selector overflows left", group.left >= 0)
                assertTrue("selector overflows right", group.right <= activity.findViewById<android.view.View>(android.R.id.content).width)
                assertTrue(group.width <= activity.resources.getDimensionPixelSize(R.dimen.camera_modo_max_width))
            }
        }
    }

    @Test
    fun scannerRecreatesMlKitClientsWithItsView() {
        val intent = Intent(context, TelaDeTesteActivity::class.java)
            .putExtra(TelaDeTesteActivity.EXTRA_TELA, TelaDeTesteActivity.TELA_NOTA)
        ActivityScenario.launch<TelaDeTesteActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager
                    .findFragmentByTag(TelaDeTesteActivity.TELA_NOTA) as NotaFiscalFragment
                val barcodeField = NotaFiscalFragment::class.java
                    .getDeclaredField("leitorCodigo").apply { isAccessible = true }
                val textField = NotaFiscalFragment::class.java
                    .getDeclaredField("reconhecedor").apply { isAccessible = true }
                val firstBarcode = barcodeField.get(fragment)
                val firstText = textField.get(fragment)

                activity.supportFragmentManager.beginTransaction().detach(fragment).commitNow()
                activity.supportFragmentManager.beginTransaction().attach(fragment).commitNow()

                assertTrue(firstBarcode !== barcodeField.get(fragment))
                assertTrue(firstText !== textField.get(fragment))
                assertEquals(
                    R.id.modo_codigo,
                    activity.findViewById<MaterialButtonToggleGroup>(R.id.grupo_modo).checkedButtonId,
                )
            }
        }
    }

    @Test
    fun openingScanClearsPreviousTabAndAllowsReturningToIt() {
        SessionManager(context).apply {
            clear()
            startSession("header.payload.signature", "refresh")
        }
        ActivityScenario.launch<MainActivity>(Intent(context, MainActivity::class.java)).use { scenario ->
            scenario.onActivity { activity ->
                val navigation = activity.findViewById<BottomNavigationView>(R.id.nav_destinos)
                assertEquals(R.id.nav_estoque, navigation.selectedItemId)

                activity.findViewById<android.view.View>(R.id.nav_scan).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                assertTrue(activity.supportFragmentManager.fragments.any {
                    it is NotaFiscalFragment && it.isVisible
                })
                assertEquals(R.id.nav_vao_scan, navigation.selectedItemId)
                assertFalse(
                    listOf(R.id.nav_estoque, R.id.nav_locais, R.id.nav_lista, R.id.nav_receitas)
                        .any { navigation.menu.findItem(it).isChecked },
                )

                activity.findViewById<android.view.View>(R.id.modo_nota).performClick()
                navigation.selectedItemId = R.id.nav_estoque
                activity.supportFragmentManager.executePendingTransactions()
                assertTrue(activity.supportFragmentManager.fragments.any {
                    it is EstoqueFragment && it.isVisible
                })

                activity.findViewById<android.view.View>(R.id.nav_scan).performClick()
                activity.supportFragmentManager.executePendingTransactions()
                assertEquals(
                    R.id.modo_codigo,
                    activity.findViewById<MaterialButtonToggleGroup>(R.id.grupo_modo).checkedButtonId,
                )
            }
        }
        SessionManager(context).clear()
    }
}
