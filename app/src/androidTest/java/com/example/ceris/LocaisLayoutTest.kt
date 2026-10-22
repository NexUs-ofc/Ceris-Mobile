package com.example.ceris

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ceris.model.Local
import com.example.ceris.view.adapter.LocalAdapter
import com.example.ceris.view.renderLocais
import com.example.ceris.viewmodel.LocaisStatus
import com.example.ceris.viewmodel.LocaisUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Fixtures apenas no teste; nenhuma conta, servidor ou cadastro real e usado. */
@RunWith(AndroidJUnit4::class)
class LocaisLayoutTest {
    @Test
    fun estadosNoCelularETablet() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val folder = File(context.getExternalFilesDir(null), "locais-pr2").apply { mkdirs() }
        instrumentation.runOnMainSync {
            listOf(360 to 800, 1280 to 800).forEach { (width, height) ->
                val configuration =
                    Configuration(context.resources.configuration).apply {
                        densityDpi = 160
                        screenWidthDp = width
                        screenHeightDp = height
                        smallestScreenWidthDp = minOf(width, height)
                        fontScale = 1f
                    }
                val themed = ContextThemeWrapper(context.createConfigurationContext(configuration), R.style.AppTheme)
                val root = LayoutInflater.from(themed).inflate(R.layout.fragment_locais, null)
                val list = root.findViewById<RecyclerView>(R.id.lista_locais)
                val adapter = LocalAdapter()
                list.adapter = adapter
                val states =
                    listOf(
                        LocaisUiState(LocaisStatus.LOADING),
                        LocaisUiState(
                            LocaisStatus.CONTENT,
                            "Rua de teste, 123 - Centro",
                            listOf(
                                Local("1", "Mercado de teste", "Rua de teste, 456", "450 m"),
                                Local("2", "Outro mercado de teste", "Avenida de teste, 789", "1,2 km"),
                            ),
                        ),
                        LocaisUiState(LocaisStatus.EMPTY, "Rua de teste, 123 - Centro"),
                        LocaisUiState(LocaisStatus.EMPTY, usesProfileAddress = true),
                        LocaisUiState(LocaisStatus.ERROR),
                        LocaisUiState(LocaisStatus.ADDRESS_MISSING),
                        LocaisUiState(LocaisStatus.NOT_GEOCODED, "Rua de teste, 123"),
                        LocaisUiState(LocaisStatus.AUTH_REQUIRED),
                        LocaisUiState(LocaisStatus.LOCATION_PERMISSION),
                        LocaisUiState(LocaisStatus.LOCATION_UNAVAILABLE),
                    )
                states.forEach { state ->
                    renderLocais(root, adapter, state)
                    if (state.usesProfileAddress) {
                        assertEquals(
                            themed.getString(R.string.locais_origem_perfil),
                            root.findViewById<TextView>(R.id.txt_endereco_atual).text.toString(),
                        )
                        assertEquals(
                            themed.getString(R.string.locais_vazio_perfil),
                            root.findViewById<TextView>(R.id.mensagem_locais).text.toString(),
                        )
                    }
                    root.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
                    )
                    root.layout(0, 0, width, height)
                    assertEquals(if (state.status == LocaisStatus.CONTENT) View.VISIBLE else View.GONE, list.visibility)
                    if (state.status == LocaisStatus.CONTENT) {
                        val card = requireNotNull(list.findViewHolderForAdapterPosition(0)).itemView
                        assertEquals("450 m", card.findViewById<TextView>(R.id.selo_distancia).text.toString())
                        assertTrue(list.width <= themed.resources.getDimensionPixelSize(R.dimen.content_max_width))
                    } else {
                        assertEquals(0, adapter.itemCount)
                    }
                    val retry = root.findViewById<View>(R.id.btn_tentar_locais)
                    val retryable =
                        state.status in
                            setOf(
                                LocaisStatus.ERROR,
                                LocaisStatus.AUTH_REQUIRED,
                                LocaisStatus.LOCATION_PERMISSION,
                                LocaisStatus.LOCATION_UNAVAILABLE,
                            )
                    assertEquals(
                        if (retryable) View.VISIBLE else View.GONE,
                        retry.visibility,
                    )
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    root.draw(Canvas(bitmap))
                    val origin = if (state.usesProfileAddress) "-perfil" else ""
                    File(folder, "$width-${state.status}$origin.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
            }
        }
    }
}
