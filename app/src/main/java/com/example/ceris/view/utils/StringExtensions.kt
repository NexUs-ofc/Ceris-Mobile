package com.example.ceris.view.utils

fun String.onlyNumbers(): String {
    return replace(Regex("[^0-9]"), "")
}