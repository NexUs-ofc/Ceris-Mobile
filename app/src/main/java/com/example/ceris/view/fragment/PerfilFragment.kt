package com.example.ceris.view.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ProfileAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.aoTerminar
import com.example.ceris.api.bearer
import com.example.ceris.local.FotoPerfilStore
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.notification.PushNotifications
import com.example.ceris.view.LoginActivity
import com.example.ceris.view.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * Perfil (Figma > Protótipos > V1 > ProfileScreen).
 *
 * Nome e e-mail vêm da Core; a foto escolhida no aparelho tem prioridade sobre
 * a URL da Core. Personalização abre o popup de edição.
 */
class PerfilFragment : Fragment(R.layout.fragment_perfil), EditarPerfilDialog.Listener {

    private val api by lazy { RetrofitClient.getApiAutenticada(ApiBaseUrl.core, ProfileAPI::class.java) }

    private var perfil: ProfileResponse? = null
    private var voltarCallback: OnBackPressedCallback? = null
    private var fotoPendente: Uri? = null
    private val escolherFoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) guardarFoto(uri)
    }
    private val pedirCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permitida ->
        if (permitida) abrirCamera() else avisar(getString(R.string.perfil_camera_sem_permissao))
    }
    private val tirarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { sucesso ->
        val uri = fotoPendente
        fotoPendente = null
        if (sucesso && uri != null) guardarFoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fotoPendente = savedInstanceState?.getString("foto_pendente")?.let(Uri::parse)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("foto_pendente", fotoPendente?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        voltarCallback = object : OnBackPressedCallback(!isHidden) {
            override fun handleOnBackPressed() {
                (activity as? MainActivity)?.voltarAoEstoque()
            }
        }.also { requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it) }

        opcao(view, R.id.opcao_personalizacao, R.drawable.ic_menu_user, R.string.perfil_personalizacao) { editar() }
        opcao(view, R.id.opcao_privacidade, R.drawable.ic_menu_lock, R.string.perfil_privacidade) {
            texto(R.string.perfil_privacidade, R.string.perfil_privacidade_texto)
        }
        opcao(view, R.id.opcao_termos, R.drawable.ic_menu_file_text, R.string.perfil_termos) {
            texto(R.string.perfil_termos, R.string.perfil_termos_texto)
        }
        opcao(view, R.id.opcao_sobre, R.drawable.ic_menu_info, R.string.perfil_sobre) {
            texto(R.string.perfil_sobre, R.string.perfil_sobre_texto)
        }
        opcao(view, R.id.btn_sair, R.drawable.ic_menu_log_out, R.string.perfil_sair) { sair() }

        // Sair e vermelho e sem seta, como no Figma.
        view.findViewById<View>(R.id.btn_sair).apply {
            val vermelho = ContextCompat.getColor(context, R.color.perfil_sair)
            findViewById<TextView>(R.id.txt_opcao).setTextColor(vermelho)
            findViewById<ImageView>(R.id.icone_opcao).setColorFilter(vermelho)
            findViewById<View>(R.id.seta_opcao).visibility = View.GONE
        }

        view.findViewById<View>(R.id.selo_camera).setOnClickListener { escolherOrigemFoto() }
        view.findViewById<View>(R.id.img_perfil).setOnClickListener { escolherOrigemFoto() }
    }

    override fun onResume() {
        super.onResume()
        carregar()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        voltarCallback?.isEnabled = !hidden
        if (!hidden) carregar()
    }

    private fun carregar() {
        val token = SessionManager(requireContext()).bearer() ?: return
        api.buscar(token).aoTerminar(
            onSucesso = { recebido -> recebido?.let(::mostrar) },
            onFalha = ::avisar
        )
    }

    private fun mostrar(recebido: ProfileResponse) {
        perfil = recebido
        val raiz = view ?: return

        raiz.findViewById<TextView>(R.id.txt_nome_perfil).text = recebido.name.orEmpty()
        raiz.findViewById<TextView>(R.id.txt_email_perfil).text = recebido.email.orEmpty()

        mostrarFoto()
    }

    private fun mostrarFoto() {
        val foto = view?.findViewById<ImageView>(R.id.img_perfil) ?: return
        val local = FotoPerfilStore(requireContext()).buscar(
            SessionManager(requireContext()).snapshot()?.profileId
        )
        Glide.with(foto)
            .load(local ?: perfil?.profileImageUrl?.takeIf { it.isNotBlank() })
            .placeholder(R.drawable.ic_menu_user)
            .error(R.drawable.ic_menu_user)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .circleCrop()
            .into(foto)
    }

    private fun escolherOrigemFoto() {
        if (SessionManager(requireContext()).snapshot()?.profileId == null) {
            avisar(getString(R.string.perfil_carregando))
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.perfil_escolher_foto)
            .setItems(arrayOf(getString(R.string.perfil_tirar_foto), getString(R.string.perfil_galeria))) { _, opcao ->
                if (opcao == 0) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        abrirCamera()
                    } else {
                        pedirCamera.launch(Manifest.permission.CAMERA)
                    }
                } else {
                    escolherFoto.launch("image/*")
                }
            }
            .show()
    }

    private fun abrirCamera() {
        val contexto = requireContext()
        val diretorio = File(contexto.cacheDir, "perfil_camera").apply { mkdirs() }
        val arquivo = File(diretorio, "captura.jpg")
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.perfil.fileprovider", arquivo)
        fotoPendente = uri
        tirarFoto.launch(uri)
    }

    private fun guardarFoto(uri: Uri) {
        val contexto = context ?: return
        val profileId = SessionManager(contexto).snapshot()?.profileId ?: return
        Thread {
            val resultado = runCatching { FotoPerfilStore(contexto.applicationContext).salvar(profileId, uri) }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (SessionManager(requireContext()).snapshot()?.profileId != profileId) return@runOnUiThread
                resultado.onSuccess {
                    mostrarFoto()
                    avisar(getString(R.string.perfil_foto_salva))
                }.onFailure { avisar(getString(R.string.perfil_foto_falha)) }
            }
        }.start()
    }

    private fun opcao(raiz: View, id: Int, @DrawableRes icone: Int, @StringRes texto: Int, aoTocar: () -> Unit) {
        val cartao = raiz.findViewById<View>(id)
        cartao.findViewById<ImageView>(R.id.icone_opcao).setImageResource(icone)
        cartao.findViewById<TextView>(R.id.txt_opcao).setText(texto)
        cartao.setOnClickListener { aoTocar() }
    }

    private fun editar() {
        if (childFragmentManager.findFragmentByTag(EditarPerfilDialog.TAG) != null) return
        val nomeAtual = perfil?.name ?: view?.findViewById<TextView>(R.id.txt_nome_perfil)?.text?.toString().orEmpty()
        EditarPerfilDialog.novo(nomeAtual, perfil?.dietaryRestrictions.orEmpty())
            .show(childFragmentManager, EditarPerfilDialog.TAG)
    }

    override fun perfilSalvo(atualizado: ProfileResponse) {
        mostrar(atualizado)
        avisar(getString(R.string.perfil_salvo))
    }

    private fun texto(@StringRes titulo: Int, @StringRes corpo: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titulo)
            .setMessage(corpo)
            .setPositiveButton(R.string.perfil_fechar, null)
            .show()
    }

    private fun avisar(mensagem: String) {
        context?.let { Toast.makeText(it, mensagem, Toast.LENGTH_LONG).show() }
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
