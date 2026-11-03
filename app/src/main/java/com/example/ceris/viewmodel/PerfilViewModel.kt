package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel

class PerfilViewModel : ViewModel() {

    interface Listener {
        fun makeText(message: String)
        fun operationCompleted()
    }

    lateinit var listener: Listener
}
