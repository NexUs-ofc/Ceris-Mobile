package com.example.ceris.view.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

fun ViewGroup.getEditTexts(): List<EditText> {
    return (0 until childCount)
        .mapNotNull {
            getChildAt(it) as? EditText
        }
}

fun View.hideKeyboard() {
    val imm = this.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    imm.hideSoftInputFromWindow(rootView.windowToken, 0)
}