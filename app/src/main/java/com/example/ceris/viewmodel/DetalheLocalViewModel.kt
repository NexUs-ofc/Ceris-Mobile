package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel

class DetalheLocalViewModel : ViewModel() {

    interface Listener {
        fun makeText(message: String)
        fun operationCompleted()
    }

    lateinit var listener: Listener
}
