package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel

/**
 * Locais.
 *
 * Camada de apresentacao da tela. Segue a convencao do LoginViewModel: a
 * Activity implementa Listener e apenas reage a eventos, sem conhecer
 * repositorio nem cliente HTTP.
 *
 * LocaisRepository fornece os dados da Core. A conexao com os estados da tela
 * sera feita na entrega da listagem (PR 2).
 */
class LocaisViewModel : ViewModel() {
    interface Listener {
        fun makeText(message: String)

        fun operationCompleted()
    }

    lateinit var listener: Listener
}
