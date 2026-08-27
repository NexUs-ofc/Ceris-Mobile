package com.example.ceris.view.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class OtpTextWatcher(
    private val nextInput: EditText?
) : TextWatcher {

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) {}

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) {
        if (s?.length == 1) {
            nextInput?.requestFocus()
        }
    }

    override fun afterTextChanged(s: Editable?) {}
}