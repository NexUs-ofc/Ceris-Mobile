package com.example.ceris.view.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.ceris.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Obtém o idToken do Google pelo Credential Manager.
 *
 * Vive na camada de View porque precisa de Context e da Activity para exibir
 * a caixa de seleção de conta. A ViewModel recebe apenas a string do token.
 */
object GoogleSignInHelper {

    private const val TAG = "GoogleSignInHelper"
    private const val CLIENT_ID_NAO_CONFIGURADO = "PREENCHER"

    suspend fun obterIdToken(context: Context): Resultado {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.startsWith(CLIENT_ID_NAO_CONFIGURADO)) {
            Log.e(TAG, "GOOGLE_WEB_CLIENT_ID não configurado no local.properties")
            return Resultado.Erro(
                "Login com Google indisponível: configure o GOOGLE_WEB_CLIENT_ID."
            )
        }

        // Este app iniciou a ação por um botão explícito "Google". Usar a
        // opção própria desse botão evita a primeira consulta GetGoogleIdOption,
        // que falha em alguns aparelhos Android 14 (inclusive Xiaomi/MIUI) e
        // deixa uma segunda solicitação ambígua no Credential Manager.
        val opcao = GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        return solicitar(context, opcao)
    }

    private suspend fun solicitar(
        context: Context,
        opcao: CredentialOption
    ): Resultado {
        val requisicao = GetCredentialRequest.Builder()
            .addCredentialOption(opcao)
            .build()

        return try {
            val resposta = CredentialManager.create(context).getCredential(
                context = context,
                request = requisicao
            )

            val credencial = resposta.credential

            if (credencial is CustomCredential &&
                credencial.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credencial.data)
                Resultado.Sucesso(google.idToken)
            } else {
                Log.e(TAG, "Credencial de tipo inesperado: ${credencial.type}")
            Resultado.Erro("Não foi possível acessar sua conta Google. Tente novamente.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Seleção de conta Google cancelada pelo usuário")
            Resultado.Cancelado
        } catch (e: NoCredentialException) {
            Log.e(TAG, "O Credential Manager não ofereceu uma conta Google", e)
            Resultado.Erro(
                "O Android não conseguiu disponibilizar uma conta Google. " +
                    "Confira as contas do aparelho e tente novamente."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Falha ao obter credencial do Google: ${e.type}", e)
            Resultado.Erro("Não foi possível concluir o acesso com o Google. Tente novamente.")
        } catch (e: Exception) {
            // A credencial pode ter sido escolhida com sucesso e ainda assim
            // falhar ao ser convertida (por exemplo, token/claims inesperados).
            // Sem este catch a coroutine morria antes de avisar a tela ou abrir
            // o carregamento, deixando o botão aparentemente travado.
            Log.e(TAG, "Falha ao interpretar a credencial escolhida do Google", e)
            Resultado.Erro("Não foi possível concluir o login com Google. Tente novamente.")
        }
    }

    sealed class Resultado {
        data class Sucesso(val idToken: String) : Resultado()
        data class Erro(val mensagem: String) : Resultado()
        object Cancelado : Resultado()
    }
}
