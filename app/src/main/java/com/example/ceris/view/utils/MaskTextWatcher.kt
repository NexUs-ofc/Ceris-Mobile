package com.example.ceris.view.utils

import android.text.Editable
import android.text.TextWatcher

class MaskTextWatcher(
    private val mask: String
) : TextWatcher {

    private var isUpdating = false

    override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
    ) = Unit

    override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
    ) = Unit

    override fun afterTextChanged(s: Editable?) {
        if (isUpdating || s == null) return

        isUpdating = true

        val digits = s
            .toString()
            .replace(Regex("[^0-9]"), "")

        var digitIndex = 0

        val formatted = buildString {
            for (character in mask) {

                if (character == '#') {
                    if (digitIndex >= digits.length) break

                    append(digits[digitIndex])
                    digitIndex++
                } else {
                    if (digitIndex < digits.length) {
                        append(character)
                    }
                }
            }
        }

        s.replace(0, s.length, formatted)

        isUpdating = false
    }
}