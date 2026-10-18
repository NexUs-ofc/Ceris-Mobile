package com.example.ceris.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.ceris.R
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.notification.PushNotifications
import com.example.ceris.view.LoginActivity
import com.google.android.material.button.MaterialButton

/**
 * Perfil.
 *
 * O conteúdo e a adaptação ao tablet vivem no XML e em dimens/values-sw480dp.
 * A barra de navegação pertence à MainActivity.
 */
class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btn_sair).setOnClickListener { sair() }
    }

    /**
     * Limpa a sessao local e tenta desvincular o push usando a conta antiga.
     * A Auth nao e chamada; a indisponibilidade do Notify nao bloqueia a saida.
     *
     * O refresh continua válido no servidor pelos 30 dias, mas ninguém mais o
     * tem: ele morava só aqui. Sair tem de funcionar com a Auth fora do ar —
     * depender de uma chamada deixaria a pessoa presa numa conta justamente
     * quando o serviço está com problema.
     *
     * Os extras locais de lista saem junto: valor e categoria são por item do
     * dono da sessão e apareceriam na conta seguinte.
     */
    private fun sair() {
        val contexto = requireContext()

        PushNotifications.get(contexto).logout()
        ListaLocalStore(contexto).limpar()

        // CLEAR_TASK porque voltar depois de sair traria Estoque e listas da
        // sessão anterior, já sem token para recarregá-las.
        startActivity(
            Intent(contexto, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        requireActivity().finish()
    }
}
