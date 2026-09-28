package com.example.ceris.local

import android.content.Context
import androidx.core.content.edit
import com.example.ceris.model.CategoriaItem

// Preco e categoria nao existem no contrato da Core, e a tela precisa dos dois.
// Ficam aqui, por id do item, ate o servidor passar a guardá-los.
class ListaLocalStore(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        ARQUIVO,
        Context.MODE_PRIVATE
    )

    fun salvar(itemId: String, valor: Double, categoria: CategoriaItem) {
        preferences.edit {
            putFloat(chaveValor(itemId), valor.toFloat())
            putString(chaveCategoria(itemId), categoria.name)
        }
    }

    fun valorDe(itemId: String): Double =
        preferences.getFloat(chaveValor(itemId), 0f).toDouble()

    fun categoriaDe(itemId: String): CategoriaItem =
        CategoriaItem.porNome(preferences.getString(chaveCategoria(itemId), null))

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
