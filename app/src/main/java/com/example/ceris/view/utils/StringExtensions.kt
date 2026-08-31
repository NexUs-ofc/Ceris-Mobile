package com.example.ceris.view.utils

fun String.onlyNumbers(): String {
    return replace(Regex("[^0-9]"), "")
}
fun String.maskEmail(): String {
    val parts = this.split("@")
    if (parts.size != 2) return this

    val username = parts[0]
    val domain = parts[1]

    if (username.length <= 2) {
        return "${username.first()}****@$domain"
    }

    return "${username.take(2)}****@$domain"
}