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
import com.example.ceris.R
import com.example.ceris.api.ApiBaseUrl
import com.example.ceris.api.ProfileAPI
import com.example.ceris.api.RetrofitClient
import com.example.ceris.api.aoTerminar
import com.example.ceris.api.bearer
import com.example.ceris.local.ListaLocalStore
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.ProfileResponse
import com.example.ceris.notification.PushNotifications
import com.example.ceris.util.FotoPerfilUpload
import com.example.ceris.view.LoginActivity
import com.example.ceris.view.MainActivity
import com.example.ceris.view.utils.PopupCeris
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import java.io.File
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

/**
 * Perfil (Figma > Protótipos > V1 > ProfileScreen).
 *
 * Nome, e-mail e foto vêm da Core. Personalização abre o popup de edição.
 */
class PerfilFragment : Fragment(R.layout.fragment_perfil), EditarPerfilDialog.Listener {

    private val api by lazy { RetrofitClient.getApiAutenticada(ApiBaseUrl.core, ProfileAPI::class.java) }

    private var perfil: ProfileResponse? = null
    private var voltarCallback: OnBackPressedCallback? = null
    private var fotoPendente: Uri? = null
    private var enviandoFoto = false
    private var versaoFoto = 0
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
        view.findViewById<View>(R.id.btn_voltar_perfil).setOnClickListener {
            (activity as? MainActivity)?.voltarAoEstoque()
        }

        opcao(view, R.id.opcao_personalizacao, R.drawable.ic_menu_user, R.string.perfil_personalizacao) { editar() }
        opcao(view, R.id.opcao_privacidade, R.drawable.ic_menu_lock, R.string.perfil_privacidade) {
            texto(R.string.perfil_privacidade, R.string.perfil_privacidade_texto, R.drawable.ic_menu_lock)
        }
        opcao(view, R.id.opcao_termos, R.drawable.ic_menu_file_text, R.string.perfil_termos) {
            texto(R.string.perfil_termos, R.string.perfil_termos_texto, R.drawable.ic_menu_file_text)
        }
        opcao(view, R.id.opcao_sobre, R.drawable.ic_menu_info, R.string.perfil_sobre) {
            texto(R.string.perfil_sobre, R.string.perfil_sobre_texto, R.drawable.ic_menu_info)
        }
        opcao(view, R.id.btn_sair, R.drawable.ic_menu_log_out, R.string.perfil_sair) { confirmarSaida() }

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
        val sessao = SessionManager(requireContext())
        val atual = sessao.snapshot() ?: return
        val token = sessao.bearer() ?: return
        val versao = versaoFoto
        api.buscar(token).aoTerminar(
            onSucesso = { recebido ->
                if (sessao.isCurrent(atual) && versao == versaoFoto && !enviandoFoto) recebido?.let(::mostrar)
            },
            onFalha = { mensagem -> if (sessao.isCurrent(atual)) avisar(mensagem) }
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
        Glide.with(foto)
            .load(perfil?.profileImageUrl?.takeIf { it.isNotBlank() })
            .placeholder(R.drawable.ic_menu_user)
            .error(R.drawable.ic_menu_user)
            .circleCrop()
            .into(foto)
    }

    private fun escolherOrigemFoto() {
        if (enviandoFoto) return
        if (SessionManager(requireContext()).snapshot()?.profileId == null) {
            avisar(getString(R.string.perfil_carregando))
            return
        }
        val conteudo = layoutInflater.inflate(R.layout.dialogo_origem_foto, null)
        val dialogo = MaterialAlertDialogBuilder(requireContext())
            .setView(conteudo)
            .create()
        conteudo.findViewById<MaterialButton>(R.id.btn_foto_camera).setOnClickListener {
            dialogo.dismiss()
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamera()
            } else {
                pedirCamera.launch(Manifest.permission.CAMERA)
            }
        }
        conteudo.findViewById<MaterialButton>(R.id.btn_foto_galeria).setOnClickListener {
            dialogo.dismiss()
            escolherFoto.launch("image/*")
        }
        conteudo.findViewById<View>(R.id.btn_cancelar_foto).setOnClickListener { dialogo.dismiss() }
        dialogo.show()
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
        val sessao = SessionManager(contexto)
        val atual = sessao.snapshot() ?: return
        val profileId = atual.profileId ?: return
        if (enviandoFoto) return
        enviandoFoto = true
        versaoFoto++
        Thread {
            val resultado = runCatching {
                val jpeg = FotoPerfilUpload.preparar(contexto.applicationContext, uri)
                val corpo = RequestBody.create(MediaType.parse("image/jpeg"), jpeg)
                val parte = MultipartBody.Part.createFormData("file", "perfil.jpg", corpo)
                val resposta = api.enviarFoto("Bearer ${atual.accessToken}", parte).execute()
                val imagem = resposta.body()
                check(resposta.isSuccessful && imagem?.profileId == profileId && !imagem?.profileImageUrl.isNullOrBlank()) {
                    "Falha no upload da foto"
                }
                imagem?.profileImageUrl ?: error("Foto sem URL")
            }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                enviandoFoto = false
                if (!sessao.isCurrent(atual)) return@runOnUiThread
                resultado.onSuccess { url ->
                    perfil = (perfil ?: ProfileResponse(id = profileId)).copy(profileImageUrl = url)
                    mostrarFoto()
                    avisar(getString(R.string.perfil_foto_salva))
                }.onFailure {
                    avisar(getString(R.string.perfil_foto_falha))
                    carregar()
                }
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

    private fun texto(@StringRes titulo: Int, @StringRes corpo: Int, @DrawableRes icone: Int) {
        PopupCeris.mostrar(
            contexto = requireContext(),
            titulo = getString(titulo),
            mensagem = getString(corpo),
            icone = icone,
            confirmar = getString(R.string.perfil_fechar)
        )
    }

    private fun confirmarSaida() {
        PopupCeris.mostrar(
            contexto = requireContext(),
            titulo = getString(R.string.perfil_sair_confirmar_titulo),
            mensagem = getString(R.string.perfil_sair_confirmar_mensagem),
            icone = R.drawable.ic_menu_log_out,
            confirmar = getString(R.string.perfil_sair_confirmar_botao),
            cancelar = getString(R.string.lista_nome_cancelar),
            destrutivo = true,
            aoConfirmar = ::sair
        )
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
