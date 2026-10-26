package com.example.ceris.view.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.ItemNota
import com.example.ceris.model.dto.FoodResponse
import com.example.ceris.view.MainActivity
import com.example.ceris.view.adapter.ItemNotaAdapter
import com.example.ceris.view.utils.MascaraLeituraView
import com.example.ceris.viewmodel.NotaFiscalViewModel
import com.example.ceris.viewmodel.NotaFiscalViewModel.Estado
import com.example.ceris.model.CadastroPorCodigo
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Scanner: camera com moldura, espera e revisao, no mesmo fragment da barra.
 *
 * Dois modos, cada um com a sua moldura. Na nota, a foto sai pelo botao
 * central da barra (a MainActivity repassa para capturar()) ou sozinha quando
 * a nota fica parada e inteira na moldura. No codigo de barras a leitura e
 * automatica e abre o "Confirmar dados do produto".
 */
class NotaFiscalFragment : Fragment(R.layout.fragment_nota_fiscal), NotaFiscalViewModel.Listener,
    CodigoBarrasBottomSheet.Listener {

    private val viewModel: NotaFiscalViewModel by viewModels()

    private val adapter = ItemNotaAdapter()

    private lateinit var camadaCamera: View
    private lateinit var camadaLendo: View
    private lateinit var camadaRevisao: View
    private lateinit var preview: PreviewView
    private lateinit var semPermissao: TextView
    private lateinit var andamento: TextView
    private lateinit var quantos: TextView
    private lateinit var flash: ImageButton

    private var cameraProvider: ProcessCameraProvider? = null
    private var captura: ImageCapture? = null
    private var camera: Camera? = null

    private var lenteTraseira = true
    private var flashLigado = false

    /** Modo da camera: nota fiscal (foto e leitura no ms-ia) ou codigo de barras. */
    @Volatile private var modoCodigo = false

    private lateinit var mascara: MascaraLeituraView
    private lateinit var dica: TextView

    private val reconhecedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Um quadro por vez: o analisador descarta os que chegam enquanto o anterior roda. */
    @Volatile private var analisando = false

    /** Quando a nota passou a estar inteira e centralizada; zero se saiu dela. */
    private var centradaDesde = 0L

    /** Depois do disparo automatico, nao dispara de novo ate a camera ser religada. */
    private var disparoAutomatico = false

    /** So os formatos de produto de mercado: QR e outros nao sao GTIN. */
    private val leitorCodigo = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E
            )
            .build()
    )

    /** Cadastro aberto: a camera para de ler para nao abrir outro por cima. */
    @Volatile private var cadastroAberto = false

    /** JPEG e rotacao fora da thread principal: a foto inteira pesa megas. */
    private lateinit var trabalho: ExecutorService

    /** Foto da galeria: codigo de barras se tiver, senao vai como nota. */
    private val escolherFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::lerDaGaleria)
    }

    private val pedirCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        semPermissao.visibility = if (ok) View.GONE else View.VISIBLE
        if (ok) ligarCamera()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        camadaCamera = view.findViewById(R.id.camada_camera)
        camadaLendo = view.findViewById(R.id.camada_lendo)
        camadaRevisao = view.findViewById(R.id.camada_revisao)
        preview = view.findViewById(R.id.preview)
        semPermissao = view.findViewById(R.id.sem_permissao)
        andamento = view.findViewById(R.id.txt_andamento)
        quantos = view.findViewById(R.id.txt_quantos)
        flash = view.findViewById(R.id.btn_flash)

        view.findViewById<RecyclerView>(R.id.lista_itens_nota).adapter = adapter

        view.findViewById<View>(R.id.btn_adicionar_estoque).setOnClickListener {
            viewModel.salvar(adapter.itens)
        }
        view.findViewById<View>(R.id.btn_descartar).setOnClickListener {
            viewModel.descartar()
        }

        trabalho = Executors.newSingleThreadExecutor()

        mascara = view.findViewById(R.id.mascara)
        dica = view.findViewById(R.id.dica)
        view.findViewById<MaterialButtonToggleGroup>(R.id.grupo_modo)
            .addOnButtonCheckedListener { _, id, marcado ->
                if (marcado) aplicarModo(id == R.id.modo_codigo)
            }

        flash.setOnClickListener { alternarFlash() }
        view.findViewById<View>(R.id.btn_trocar_camera).setOnClickListener {
            lenteTraseira = !lenteTraseira
            flashLigado = false
            ligarCamera()
        }
        view.findViewById<View>(R.id.btn_galeria).setOnClickListener {
            escolherFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        viewModel.listener = this
        viewModel.init(SessionManager(requireContext()))
        estado(viewModel.estado)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        trabalho.shutdown()
        reconhecedor.close()
        leitorCodigo.close()
    }

    /**
     * Os destinos da barra sao escondidos, nao destruidos: sem isto a camera
     * continuaria ligada por tras da tela de estoque.
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (hidden) {
            desligarCamera()
        } else if (viewModel.estado == Estado.CAMERA) {
            prepararCamera()
        }
    }

    /** Chamado pelo botao central da barra quando esta tela ja esta aberta. */
    fun capturar() {
        val alvo = captura ?: return
        // No modo codigo de barras a leitura e automatica; nao ha foto a tirar.
        if (viewModel.estado != Estado.CAMERA || modoCodigo) return

        alvo.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imagem: ImageProxy) {
                    trabalho.execute {
                        val jpeg = imagem.use { paraJpeg(it) }
                        view?.post { viewModel.ler(jpeg) }
                    }
                }

                override fun onError(erro: ImageCaptureException) {
                    Log.e(TAG, "falha ao fotografar", erro)
                    makeText(getString(R.string.nota_falha_foto))
                }
            }
        )
    }

    private fun prepararCamera() {
        val permitido = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (permitido) {
            semPermissao.visibility = View.GONE
            ligarCamera()
        } else {
            pedirCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun ligarCamera() {
        val futuro = ProcessCameraProvider.getInstance(requireContext())

        futuro.addListener({
            if (!isAdded || isHidden || viewModel.estado != Estado.CAMERA) return@addListener

            val provider = futuro.get()
            cameraProvider = provider

            val previa = Preview.Builder().build().also { it.surfaceProvider = preview.surfaceProvider }

            // Qualidade acima de rapidez: o OCR erra mais em foto tremida e
            // comprimida do que a pessoa perde esperando meio segundo.
            val foto = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            captura = foto

            // Os quadros do preview passam pelo reconhecedor: e ele que diz se a
            // nota ja esta inteira e parada na moldura, para tirar a foto sozinho.
            val analise = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(trabalho, ::analisarQuadro) }

            disparoAutomatico = false
            centradaDesde = 0L

            val lente = if (lenteTraseira) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            provider.unbindAll()
            camera = runCatching {
                provider.bindToLifecycle(viewLifecycleOwner, lente, previa, foto, analise)
            }.getOrElse { erro ->
                // Aparelho sem a camera da frente (emulador, tablet antigo): volta para a de tras.
                Log.w(TAG, "camera indisponivel", erro)
                lenteTraseira = true
                provider.bindToLifecycle(viewLifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previa, foto, analise)
            }
            mostrarFlash()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun desligarCamera() {
        cameraProvider?.unbindAll()
        captura = null
        camera = null
        flashLigado = false
    }

    /** A moldura encolhe para o codigo de barras; a dica so faz sentido na nota. */
    private fun aplicarModo(codigo: Boolean) {
        modoCodigo = codigo
        cadastroAberto = false
        disparoAutomatico = false
        centradaDesde = 0L
        mascara.mudarModo(nota = !codigo)
        dica.visibility = if (codigo) View.GONE else View.VISIBLE
    }

    private fun alternarFlash() {
        val atual = camera ?: return
        if (!atual.cameraInfo.hasFlashUnit()) {
            makeText(getString(R.string.camera_sem_flash))
            return
        }
        flashLigado = !flashLigado
        atual.cameraControl.enableTorch(flashLigado)
        mostrarFlash()
    }

    private fun mostrarFlash() {
        flash.setImageResource(if (flashLigado) R.drawable.ic_menu_zap else R.drawable.ic_menu_zap_off)
    }

    /**
     * Foto escolhida na galeria: procura codigo de barras primeiro, que e
     * leitura local e instantanea; sem codigo, manda como nota.
     */
    private fun lerDaGaleria(uri: Uri) {
        if (viewModel.estado != Estado.CAMERA || cadastroAberto) return
        val contexto = context ?: return

        val entrada = runCatching { InputImage.fromFilePath(contexto, uri) }.getOrElse {
            makeText(getString(R.string.nota_falha_foto))
            return
        }

        leitorCodigo.process(entrada)
            .addOnSuccessListener { codigos ->
                val gtin = codigos.mapNotNull { it.rawValue }.firstOrNull(CadastroPorCodigo::gtinValido)
                if (gtin != null) {
                    achouCodigo(listOf(gtin))
                } else {
                    trabalho.execute {
                        val jpeg = runCatching { jpegDaGaleria(contexto, uri) }.getOrNull()
                        view?.post {
                            if (jpeg == null) makeText(getString(R.string.nota_falha_foto)) else viewModel.ler(jpeg)
                        }
                    }
                }
            }
            .addOnFailureListener { makeText(getString(R.string.camera_sem_leitura)) }
    }

    private fun jpegDaGaleria(contexto: Context, uri: Uri): ByteArray {
        val bitmap = contexto.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: error("foto ilegivel")
        return comprimir(bitmap)
    }

    /**
     * Primeiro codigo valido do quadro abre o cadastro. O digito verificador
     * filtra leitura pela metade, que a camera faz com o produto em movimento.
     */
    private fun achouCodigo(codigos: List<String>) {
        if (cadastroAberto || !isAdded) return
        val gtin = codigos.firstOrNull(CadastroPorCodigo::gtinValido) ?: return

        cadastroAberto = true
        CodigoBarrasBottomSheet.novo(gtin).show(childFragmentManager, CodigoBarrasBottomSheet.TAG)
    }

    override fun aoFecharCadastroCodigo(salvou: Boolean) {
        // Volta a ler: da para cadastrar varios produtos em sequencia.
        cadastroAberto = false
    }

    /**
     * Roda em segundo plano, a cada quadro. Fecha o quadro em todo caminho,
     * senao a camera para de entregar.
     */
    // quadro.image e API experimental do CameraX; e o caminho documentado para
    // entregar o quadro ao ML Kit sem copiar a imagem.
    @OptIn(ExperimentalGetImage::class)
    private fun analisarQuadro(quadro: ImageProxy) {
        val midia = quadro.image

        if (midia == null || analisando || viewModel.estado != Estado.CAMERA ||
            (modoCodigo && cadastroAberto) || (!modoCodigo && disparoAutomatico)
        ) {
            quadro.close()
            return
        }

        analisando = true
        val entrada = InputImage.fromMediaImage(midia, quadro.imageInfo.rotationDegrees)

        if (modoCodigo) {
            leitorCodigo.process(entrada)
                .addOnSuccessListener { codigos -> achouCodigo(codigos.mapNotNull { it.rawValue }) }
                .addOnCompleteListener {
                    analisando = false
                    quadro.close()
                }
            return
        }

        reconhecedor.process(entrada)
            .addOnSuccessListener { texto -> conferirEnquadramento(texto, entrada.width, entrada.height) }
            .addOnFailureListener { centradaDesde = 0L }
            .addOnCompleteListener {
                analisando = false
                quadro.close()
            }
    }

    /**
     * Nota inteira e centralizada = o texto ocupa o miolo do quadro, sem
     * encostar na borda, e fica assim por DISPARO_APOS_MS. Qualquer quadro fora
     * disso zera o tempo, para nao disparar com a nota meio fora.
     */
    private fun conferirEnquadramento(texto: Text, largura: Int, altura: Int) {
        val caixas = texto.textBlocks.flatMap { bloco -> bloco.lines }.mapNotNull { it.boundingBox }

        if (caixas.size < MIN_LINHAS_NOTA || largura <= 0 || altura <= 0) {
            centradaDesde = 0L
            return
        }

        val uniao = Rect(caixas.first())
        caixas.drop(1).forEach { uniao.union(it) }

        val margemX = largura * MARGEM_BORDA
        val margemY = altura * MARGEM_BORDA
        val dentro = uniao.left >= margemX && uniao.right <= largura - margemX &&
            uniao.top >= margemY && uniao.bottom <= altura - margemY

        val centroX = (uniao.left + uniao.right) / 2f
        val centroY = (uniao.top + uniao.bottom) / 2f
        val centrada = dentro &&
            kotlin.math.abs(centroX - largura / 2f) <= largura * TOLERANCIA_CENTRO &&
            kotlin.math.abs(centroY - altura / 2f) <= altura * TOLERANCIA_CENTRO

        if (!centrada) {
            centradaDesde = 0L
            return
        }

        val agora = SystemClock.elapsedRealtime()
        if (centradaDesde == 0L) {
            centradaDesde = agora
        } else if (agora - centradaDesde >= DISPARO_APOS_MS) {
            disparoAutomatico = true
            centradaDesde = 0L
            view?.post { capturar() }
        }
    }

    /**
     * Endireita e reduz a foto antes de mandar. A camera entrega o sensor
     * deitado e com muito mais pixel do que o OCR usa: o ms-ia corta em 8 MB e
     * reduz para ~2000 px de qualquer jeito, entao mandar inteiro so gasta
     * rede e tempo, que contam contra os 30s do servidor.
     */
    private fun paraJpeg(imagem: ImageProxy): ByteArray {
        var bitmap = imagem.toBitmap()

        val rotacao = imagem.imageInfo.rotationDegrees
        if (rotacao != 0) {
            val matriz = Matrix().apply { postRotate(rotacao.toFloat()) }
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
        }

        return comprimir(bitmap)
    }

    private fun comprimir(original: Bitmap): ByteArray {
        var bitmap = original
        val maior = maxOf(bitmap.width, bitmap.height)
        if (maior > LADO_MAXIMO) {
            val escala = LADO_MAXIMO.toFloat() / maior
            bitmap = Bitmap.createScaledBitmap(
                bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true
            )
        }

        return ByteArrayOutputStream().use { saida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)
            saida.toByteArray()
        }
    }

    override fun estado(estado: Estado) {
        camadaCamera.visibility = if (estado == Estado.CAMERA) View.VISIBLE else View.GONE
        camadaLendo.visibility = if (estado == Estado.LENDO || estado == Estado.SALVANDO) View.VISIBLE else View.GONE
        camadaRevisao.visibility = if (estado == Estado.REVISAO) View.VISIBLE else View.GONE

        if (estado == Estado.SALVANDO) {
            andamento.setText(R.string.nota_salvando)
        }

        // Camera so fica ligada quando aparece: durante a leitura e a revisao
        // ela so gastaria bateria.
        if (estado == Estado.CAMERA && !isHidden) prepararCamera() else desligarCamera()
    }

    override fun progresso(texto: String) {
        andamento.text = texto
    }

    override fun itensLidos(loja: String?, itens: List<ItemNota>, catalogo: List<FoodResponse>) {
        quantos.text = resources.getQuantityString(R.plurals.nota_itens_lidos, itens.size, itens.size)
        adapter.trocar(itens, catalogo)
    }

    override fun salvo(salvos: Int, falhas: Int) {
        makeText(
            when {
                falhas == 0 -> resources.getQuantityString(R.plurals.nota_salvos, salvos, salvos)
                salvos == 0 -> getString(R.string.nota_nada_salvo)
                else -> getString(R.string.nota_salvos_com_falha, salvos, falhas)
            }
        )

        if (salvos > 0) {
            (activity as? MainActivity)?.voltarAoEstoque()
        }
    }

    override fun makeText(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    private companion object {
        const val TAG = "NotaFiscalFragment"
        const val LADO_MAXIMO = 2000
        const val QUALIDADE_JPEG = 90

        /** Abaixo disso nao e nota: um cartaz ou um rotulo tem poucas linhas. */
        const val MIN_LINHAS_NOTA = 6

        /** Margem minima entre o texto e a borda do quadro, em fracao. */
        const val MARGEM_BORDA = 0.06f

        /** Quanto o centro do texto pode se afastar do centro do quadro, em fracao. */
        const val TOLERANCIA_CENTRO = 0.12f

        /** Tempo parada e centralizada antes de tirar a foto sozinha. */
        const val DISPARO_APOS_MS = 1_200L
    }
}
