package com.example.ceris

import android.content.Intent
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.debug.FigmaPreviewActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusedLayoutTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun listActionsStayVisibleAndDeleteRemainsAtBottom() {
        preview(R.layout.activity_lista_compras) { activity, root ->
            val adicionar = activity.findViewById<View>(R.id.btn_adicionar_item)
            val excluir = activity.findViewById<View>(R.id.btn_excluir_lista)
            assertInside(root, adicionar)
            assertInside(root, excluir)
            assertTrue("delete must be below add", excluir.top >= adicionar.bottom)
        }
    }

    @Test
    fun recipeDetailUsesTabletWidthAndScaledCards() {
        preview(R.layout.activity_detalhe_receita) { activity, root ->
            val header = activity.findViewById<View>(R.id.cabecalho_receita)
            val content = activity.findViewById<View>(R.id.conteudo_receita).apply {
                visibility = View.VISIBLE
            }
            assertInside(root, header)
            assertInside(root, content)
            if (activity.resources.configuration.smallestScreenWidthDp >= 600) {
                assertTrue("recipe detail remains narrow on tablet", header.width >= root.width * 0.75f)
                assertTrue(
                    "recipe step cards remain phone-sized on tablet",
                    activity.resources.getDimensionPixelSize(R.dimen.receita_passo_card_altura) >=
                        activity.resources.displayMetrics.density * 110,
                )
            }
        }
    }

    private fun preview(layout: Int, check: (FigmaPreviewActivity, View) -> Unit) {
        val intent = Intent(context, FigmaPreviewActivity::class.java)
            .putExtra(FigmaPreviewActivity.EXTRA_LAYOUT, layout)
        ActivityScenario.launch<FigmaPreviewActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(R.id.main)
                check(activity, root)
            }
        }
    }

    private fun assertInside(root: View, child: View) {
        val window = Rect().also(root::getGlobalVisibleRect)
        val bounds = Rect()
        assertTrue(child.getGlobalVisibleRect(bounds))
        assertTrue(bounds.left >= window.left)
        assertTrue(bounds.top >= window.top)
        assertTrue(bounds.right <= window.right)
        assertTrue(bounds.bottom <= window.bottom)
    }
}
