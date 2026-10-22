package com.example.ceris.view.fragment

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ProfileAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.aoTerminar
import com.example.ceris.api.bearer
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.model.dto.ProfileUpdateRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Editar perfil, o popup do Figma: nome, idade e restricoes alimentares.
 *
 * Nome e restricoes vao para a Core (PATCH /api/profile). A Core nao guarda
 * idade, entao ela fica no aparelho, por perfil, ate o contrato ter o campo.
 */
class EditarPerfilDialog : DialogFragment() {

    interface Listener {
        fun perfilSalvo(atualizado: ProfileResponse)
    }

    private val restricoes = mutableListOf<String>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val argumentos = requireArguments()
        val conteudo = layoutInflater.inflate(R.layout.dialogo_perfil, null)

        val nome = conteudo.findViewById<EditText>(R.id.campo_nome_perfil)
        val idade = conteudo.findViewById<EditText>(R.id.campo_idade_perfil)
        val grupo = conteudo.findViewById<ChipGroup>(R.id.grupo_restricoes)
        val salvar = conteudo.findViewById<MaterialButton>(R.id.btn_salvar_perfil)

        nome.setText(argumentos.getString(ARG_NOME))
        idade.setText(lerIdade(requireContext()))

        restricoes.clear()
        restricoes.addAll(argumentos.getStringArrayList(ARG_RESTRICOES).orEmpty())
        mostrarRestricoes(grupo)

        conteudo.findViewById<Chip>(R.id.chip_adicionar_restricao).setOnClickListener { pedirRestricao(grupo) }
        conteudo.findViewById<android.view.View>(R.id.btn_cancelar_perfil).setOnClickListener { dismiss() }
        salvar.setOnClickListener { salvar(nome.text.toString(), idade.text.toString(), salvar) }

        return Dialog(requireContext()).apply {
            setContentView(conteudo)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                (resources.displayMetrics.widthPixels * FRACAO_LARGURA).toInt()
                    .coerceAtMost(resources.getDimensionPixelSize(R.dimen.content_max_width)),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    /** As restricoes viram chips removiveis antes do "Adicionar +", que fica sempre por ultimo. */
    private fun mostrarRestricoes(grupo: ChipGroup) {
        (grupo.childCount - 1 downTo 0)
            .map { grupo.getChildAt(it) }
            .filter { it.id != R.id.chip_adicionar_restricao }
            .forEach { grupo.removeView(it) }

        restricoes.forEachIndexed { posicao, texto ->
            val chip = layoutInflater.inflate(R.layout.item_chip_restricao, grupo, false) as Chip
            chip.text = texto
            chip.setOnCloseIconClickListener {
                restricoes.remove(texto)
                mostrarRestricoes(grupo)
            }
            grupo.addView(chip, posicao)
        }
    }

    private fun pedirRestricao(grupo: ChipGroup) {
        val contexto = requireContext()
        val campo = EditText(contexto).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.perfil_restricao_hint)
        }
        val margem = resources.getDimensionPixelSize(R.dimen.dialogo_padding)
        val caixa = FrameLayout(contexto).apply {
            setPadding(margem, 0, margem, 0)
            addView(campo)
        }

        MaterialAlertDialogBuilder(contexto)
            .setTitle(R.string.perfil_restricoes)
            .setView(caixa)
            .setPositiveButton(R.string.perfil_adicionar_confirmar) { _, _ ->
                val texto = campo.text.toString().trim()
                if (texto.isNotEmpty() && restricoes.none { it.equals(texto, ignoreCase = true) }) {
                    restricoes.add(texto)
                    mostrarRestricoes(grupo)
                }
            }
            .setNegativeButton(R.string.lista_nome_cancelar, null)
            .show()
    }

    private fun salvar(nome: String, idade: String, botao: MaterialButton) {
        if (nome.isBlank()) return avisar(getString(R.string.perfil_nome_vazio))
        val contexto = requireContext()
        val token = SessionManager(contexto).bearer() ?: return

        guardarIdade(contexto, idade.trim())

        botao.isEnabled = false
        RetrofitClient.getApiAutenticada(ApiBaseUrl.core, ProfileAPI::class.java)
            .atualizar(token, ProfileUpdateRequest(name = nome.trim(), dietaryRestrictions = restricoes.toList()))
            .aoTerminar(
                onSucesso = { atualizado ->
                    if (atualizado != null) (parentFragment as? Listener)?.perfilSalvo(atualizado)
                    if (isAdded) dismiss()
                },
                onFalha = { mensagem ->
                    botao.isEnabled = true
                    avisar(mensagem)
                }
            )
    }

    private fun avisar(mensagem: String) {
        context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
    }

    companion object {
        const val TAG = "EditarPerfilDialog"

        private const val ARG_NOME = "nome"
        private const val ARG_RESTRICOES = "restricoes"

        private const val FRACAO_LARGURA = 0.9f

        private const val PREFS = "ceris_perfil_local"

        fun novo(nome: String, restricoes: List<String>) = EditarPerfilDialog().apply {
            arguments = Bundle().apply {
                putString(ARG_NOME, nome)
                putStringArrayList(ARG_RESTRICOES, ArrayList(restricoes))
            }
        }

        /** Idade por perfil: duas contas no mesmo aparelho nao dividem o valor. */
        private fun chaveIdade(contexto: Context): String =
            "idade_" + (SessionManager(contexto).snapshot()?.profileId ?: "")

        private fun lerIdade(contexto: Context): String =
            contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(chaveIdade(contexto), "").orEmpty()

        private fun guardarIdade(contexto: Context, idade: String) {
            contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(chaveIdade(contexto), idade)
                .apply()
        }
    }
}
