package com.example.ceris.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream

/** Converte fotos da camera/galeria em JPEG dentro dos limites aceitos pela Core. */
object FotoPerfilUpload {
    private const val LADO_MAXIMO = 2048
    private const val BYTES_MAXIMOS = 5 * 1024 * 1024

    fun preparar(context: Context, uri: Uri): ByteArray {
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val entrada = resolver.openInputStream(uri) ?: error("Nao foi possivel abrir a imagem")
        entrada.use { BitmapFactory.decodeStream(it, null, limites) }
        require(limites.outWidth > 0 && limites.outHeight > 0) { "Imagem invalida" }

        var amostra = 1
        while (limites.outWidth / amostra > LADO_MAXIMO || limites.outHeight / amostra > LADO_MAXIMO) {
            amostra *= 2
        }
        val orientacao = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
        val opcoes = BitmapFactory.Options().apply { inSampleSize = amostra }
        val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opcoes) }
            ?: error("Imagem invalida")
        val ajustada = corrigirOrientacao(original, orientacao)
        try {
            val saida = ByteArrayOutputStream()
            check(ajustada.compress(Bitmap.CompressFormat.JPEG, 85, saida)) { "Imagem invalida" }
            val bytes = saida.toByteArray()
            require(bytes.isNotEmpty() && bytes.size <= BYTES_MAXIMOS) { "Imagem muito grande" }
            return bytes
        } finally {
            if (ajustada !== original) ajustada.recycle()
            original.recycle()
        }
    }

    private fun corrigirOrientacao(bitmap: Bitmap, orientacao: Int): Bitmap {
        val matriz = Matrix()
        when (orientacao) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matriz.postRotate(90f)
                matriz.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matriz.postRotate(270f)
                matriz.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }
}
