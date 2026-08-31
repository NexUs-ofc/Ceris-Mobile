package com.example.ceris.view.utils

import android.view.KeyEvent
import android.widget.EditText

fun List<EditText>.setupOtpInputs() {
    forEachIndexed { index, editText ->

        val nextInput = getOrNull(index + 1)

        editText.addTextChangedListener(
            OtpTextWatcher(nextInput)
        )


        editText.setOnKeyListener { _, keyCode, event ->

            if (
                keyCode == KeyEvent.KEYCODE_DEL &&
                event.action == KeyEvent.ACTION_DOWN &&
                editText.text.isEmpty() &&
                index > 0
            ) {
                this[index - 1].requestFocus()
                this[index - 1].text.clear()
                this[index - 1]

                true
            } else {
                false
            }
        }
    }
}