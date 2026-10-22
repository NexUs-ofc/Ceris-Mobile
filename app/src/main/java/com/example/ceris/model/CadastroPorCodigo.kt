package com.example.ceris.model

import com.example.ceris.model.dto.FoodRegistrationRequest
import com.example.ceris.model.dto.OffProdutoDetalhe

/** Unidades de embalagem que a Core aceita, com o rotulo que a tela mostra. */
enum class UnidadeEmbalagem(val rotulo: String, val core: String) {
    GRAMA("g", "g"),
    QUILO("kg", "kg"),
    MILILITRO("ml", "ml"),
    LITRO("l", "l"),
    UNIDADE("un", "unit");

    companion object {
        fun rotulos(): List<String> = entries.map { it.rotulo }

        fun porRotulo(rotulo: String): UnidadeEmbalagem? = entries.firstOrNull { it.rotulo == rotulo }

        /** Unidade como o Open Food Facts escreve ("g", "ml", "L", "kg"...). */
        fun daBase(bruta: String?): UnidadeEmbalagem? = when (bruta?.trim()?.lowercase()) {
            "g", "gr", "gramas" -> GRAMA
            "kg" -> QUILO
            "ml" -> MILILITRO
            "l", "lt", "litro", "litros" -> LITRO
            else -> null
        }
    }
}

/** O que da para pre-preencher a partir da base aberta. */
data class ProdutoEncontrado(
    val nome: String?,
    val marca: String?,
    val quantidadeEmbalagem: Double?,
    val unidadeEmbalagem: UnidadeEmbalagem?,
    val imagemUrl: String? = null
)

object CadastroPorCodigo {

    /**
     * GTIN-8, 12, 13 ou 14 com digito verificador certo. A camera as vezes le
     * um codigo pela metade; a Core recusaria do mesmo jeito, so que depois.
     */
    fun gtinValido(codigo: String): Boolean {
        if (codigo.length !in setOf(8, 12, 13, 14) || !codigo.all(Char::isDigit)) return false

        val digitos = codigo.map { it - '0' }
        val soma = digitos.dropLast(1).reversed().mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }.sum()
        return (10 - soma % 10) % 10 == digitos.last()
    }

    fun doOpenFoodFacts(detalhe: OffProdutoDetalhe?): ProdutoEncontrado? {
        detalhe ?: return null

        val nome = detalhe.productName?.trim()?.takeIf { it.isNotEmpty() }
        // "Nestlé, Moça": a primeira marca e a do fabricante.
        val marca = detalhe.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val unidade = UnidadeEmbalagem.daBase(detalhe.productQuantityUnit)
        val quantidade = detalhe.productQuantity?.takeIf { it > 0 && unidade != null }
        val imagem = detalhe.imageFrontUrl?.trim()?.takeIf { it.isNotEmpty() }

        if (nome == null && marca == null && quantidade == null && imagem == null) return null
        return ProdutoEncontrado(nome, marca, quantidade, unidade.takeIf { quantidade != null }, imagem)
    }

    /**
     * Monta o pedido ou devolve o que falta, na ordem em que aparece na tela.
     * As mesmas regras da Core (@NotBlank, @Positive), para a pessoa ver o
     * erro antes de enviar.
     */
    fun montar(
        gtin: String,
        nome: String,
        marca: String,
        quantidadeEmbalagem: String,
        unidade: UnidadeEmbalagem?,
        categoria: CategoriaAlimento?,
        quantidade: Int,
        validade: String?,
        imagemUrl: String? = null
    ): Result<FoodRegistrationRequest> {
        val embalagem = quantidadeEmbalagem.trim().replace(',', '.').toDoubleOrNull()

        val problema = when {
            !gtinValido(gtin) -> "Código de barras inválido. Leia de novo."
            nome.isBlank() -> "Informe o nome do produto."
            embalagem == null || embalagem <= 0 -> "Informe o tamanho da embalagem (ex.: 395)."
            unidade == null -> "Escolha a unidade da embalagem."
            categoria == null -> "Escolha a categoria do produto."
            quantidade < 1 -> "A quantidade precisa ser pelo menos 1."
            else -> null
        }
        if (problema != null) return Result.failure(IllegalArgumentException(problema))

        return Result.success(
            FoodRegistrationRequest(
                gtin = gtin,
                name = nome.trim(),
                productBrand = marca.trim().takeIf { it.isNotEmpty() },
                packageQuantity = embalagem!!,
                unitOfMeasure = unidade!!.core,
                gpcCode = categoria!!.gpc,
                quantity = quantidade,
                expiryDate = validade,
                foodImageUrl = imagemUrl?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }
}
