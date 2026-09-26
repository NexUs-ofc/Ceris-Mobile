package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel

/**
 * Minhas Listas.
 *
 * Camada de apresentacao da tela. Segue a convencao do LoginViewModel: a
 * Activity implementa Listener e apenas reage a eventos, sem conhecer
 * repositorio nem cliente HTTP.
 *
 * Ainda sem fonte de dados - a AuthAPI nao expoe contrato para esta tela.
 * Quando existir, o repositorio entra por init(), como nas demais.
 */
class ListasViewModel : ViewModel() {

    interface Listener {
        fun makeText(message: String)
        fun operationCompleted()
    }

    lateinit var listener: Listener
}
