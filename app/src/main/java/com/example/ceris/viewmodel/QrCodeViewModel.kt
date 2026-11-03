package com.example.ceris.viewmodel

import androidx.lifecycle.ViewModel

class QrCodeViewModel : ViewModel() {

    interface Listener {
        fun makeText(message: String)
        fun operationCompleted()
    }

    lateinit var listener: Listener
}
