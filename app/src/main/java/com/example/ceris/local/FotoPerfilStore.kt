package com.example.ceris.local

import android.content.Context
import android.net.Uri
import java.io.File

/** Foto escolhida no aparelho, isolada pelo identificador da conta. */
class FotoPerfilStore(private val context: Context) {
    private fun arquivo(profileId: Int): File = File(context.filesDir, "perfil/foto_$profileId")

    fun buscar(profileId: Int?): File? = profileId?.let(::arquivo)?.takeIf { it.isFile }

    fun salvar(profileId: Int, origem: Uri): File {
        val destino = arquivo(profileId)
        destino.parentFile?.mkdirs()
        val temporario = File(destino.parentFile, "foto_${profileId}.tmp")
        try {
            context.contentResolver.openInputStream(origem)?.use { entrada ->
                temporario.outputStream().use { saida -> entrada.copyTo(saida) }
            } ?: error("Não foi possível abrir a imagem")
            if (temporario.length() == 0L) error("Imagem vazia")
            if (destino.exists() && !destino.delete()) error("Não foi possível trocar a foto")
            if (!temporario.renameTo(destino)) error("Não foi possível guardar a foto")
            return destino
        } finally {
            temporario.delete()
        }
    }
}
