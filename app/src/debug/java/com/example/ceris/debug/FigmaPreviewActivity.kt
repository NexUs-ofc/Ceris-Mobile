package com.example.ceris.debug

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.view.utils.hideNavigationBar

/** Hospeda layouts estáticos nos testes visuais sem disparar API ou cadastro. */
class FigmaPreviewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        val layout = intent.getIntExtra(EXTRA_LAYOUT, 0)
        require(layout != 0) { "Layout de preview não informado" }
        setContentView(layout)
        val root = findViewById<android.view.View>(R.id.main)
            ?: findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_LAYOUT = "layout"
    }
}
