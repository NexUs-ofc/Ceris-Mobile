package com.example.ceris.view.utils

import android.view.ViewGroup
import android.widget.EditText

fun ViewGroup.getEditTexts(): List<EditText> {
    return (0 until childCount)
        .mapNotNull {
            getChildAt(it) as? EditText
        }
}