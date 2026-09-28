package com.example.ceris.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.ceris.R
import com.example.ceris.view.MainActivity

/**
 * Tela Inicial - estoque.
 *
 * O conteúdo e a adaptação ao tablet vivem no XML e em dimens/values-sw480dp.
 * A barra de navegação pertence à MainActivity.
 */
class EstoqueFragment : Fragment(R.layout.fragment_estoque) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // O perfil exibe a barra sem ser um destino dela, então quem troca o
        // conteúdo é a Activity: o fragment não mexe no FragmentManager dela.
        view.findViewById<ImageButton>(R.id.btn_perfil).setOnClickListener {
            (activity as? MainActivity)?.abrirPerfil()
        }
    }
}
