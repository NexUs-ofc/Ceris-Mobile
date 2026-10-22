package com.example.ceris.repository

import com.example.ceris.api.StoreAPI
import com.example.ceris.api.aoTerminar
import com.example.ceris.model.Local
import com.example.ceris.model.dto.AddressDto
import com.example.ceris.model.dto.StoreResponse
import java.util.Locale

/** Mercados proximos da Core, traduzidos para o card e o detalhe. */
class LocaisRepository(private val api: StoreAPI) {

    fun proximos(token: String, onSucesso: (List<Local>) -> Unit, onFalha: (String) -> Unit) {
        api.proximos(token).aoTerminar(
            onSucesso = { lista -> onSucesso(lista.orEmpty().sortedBy { it.distanceKm ?: Double.MAX_VALUE }.map(::paraLocal)) },
            onFalha = onFalha
        )
    }

    companion object {
        fun paraLocal(loja: StoreResponse) = Local(
            id = loja.id.toString(),
            nome = loja.name.orEmpty(),
            endereco = listOfNotNull(loja.street, loja.number).filter { it.isNotBlank() }.joinToString(", "),
            distancia = loja.distanceKm?.let(::distancia).orEmpty(),
            imagemUrl = loja.storeImageUrl?.takeIf { it.isNotBlank() },
            enderecoCompleto = enderecoCompleto(loja),
            horario = horario(loja.openingTime, loja.closingTime),
            latitude = loja.latitude,
            longitude = loja.longitude
        )

        /** 0.8 -> "0.8"; abaixo de 10 km uma casa, acima sem casa, como no Figma. */
        fun distancia(km: Double): String =
            if (km < 10) String.format(Locale.US, "%.1f", km) else km.toLong().toString()

        /** "07:00" e "22:00" viram "7h às 22h"; "08:30" vira "8h30". */
        fun horario(abre: String?, fecha: String?): String? {
            val a = hora(abre) ?: return null
            val f = hora(fecha) ?: return null
            return "$a às $f"
        }

        private fun hora(texto: String?): String? {
            val partes = texto?.split(":") ?: return null
            val h = partes.getOrNull(0)?.toIntOrNull() ?: return null
            val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
            return if (m == 0) "${h}h" else "${h}h" + m.toString().padStart(2, '0')
        }

        private fun enderecoCompleto(loja: StoreResponse): String {
            val rua = listOfNotNull(loja.street, loja.number).filter { it.isNotBlank() }.joinToString(", ")
            val cidade = listOfNotNull(loja.city, loja.state).filter { it.isNotBlank() }.joinToString(" - ")
            return listOf(rua, loja.neighborhood.orEmpty(), cidade).filter { it.isNotBlank() }.joinToString(" - ")
        }

        /** "Rua das Flores, 123 - Centro", como o cabecalho de Locais mostra. */
        fun enderecoCurto(endereco: AddressDto?): String? {
            endereco ?: return null
            val rua = listOfNotNull(endereco.street, endereco.number).filter { it.isNotBlank() }.joinToString(", ")
            return listOf(rua, endereco.neighborhood.orEmpty()).filter { it.isNotBlank() }.joinToString(" - ")
                .ifBlank { null }
        }
    }
}
