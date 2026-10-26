package com.example.ceris.local

import android.content.Context
import androidx.core.content.edit
import com.example.ceris.model.CategoriaItem

// Preco fica local por item; categorias antigas sao lidas apenas para compatibilidade.
// Novos itens nao recebem uma categoria manual no aparelho.
class ListaLocalStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        ARQUIVO,
        Context.MODE_PRIVATE
    )

    fun salvarValor(itemId: String, valor: Double) {
        preferences.edit {
            putFloat(chaveValor(itemId), valor.toFloat())
        }
    }

    fun valorDe(itemId: String?): Double =
        if (itemId.isNullOrBlank()) 0.0 else preferences.getFloat(chaveValor(itemId), 0f).toDouble()

    fun categoriaDe(itemId: String?): CategoriaItem =
        if (itemId.isNullOrBlank()) CategoriaItem.PADRAO
        else CategoriaItem.porNome(preferences.getString(chaveCategoria(itemId), null))

    fun esquecer(itemId: String) {
        preferences.edit {
            remove(chaveValor(itemId))
            remove(chaveCategoria(itemId))
        }
    }

    fun esquecerTodos(itemIds: Collection<String>) {
        preferences.edit {
            itemIds.forEach { id ->
                remove(chaveValor(id))
                remove(chaveCategoria(id))
            }
        }
    }

    fun limpar() {
        preferences.edit { clear() }
    }

    private fun chaveValor(itemId: String) = "$itemId$SUFIXO_VALOR"

    private fun chaveCategoria(itemId: String) = "$itemId$SUFIXO_CATEGORIA"

    private companion object {
        const val ARQUIVO = "ceris_lista_extras"
        const val SUFIXO_VALOR = ".valor"
        const val SUFIXO_CATEGORIA = ".categoria"
    }
}
