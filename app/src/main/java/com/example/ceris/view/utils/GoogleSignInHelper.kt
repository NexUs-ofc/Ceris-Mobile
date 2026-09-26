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
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
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

    suspend fun obterIdToken(
        context: Context,
        filtrarPorContasAutorizadas: Boolean = false
    ): Resultado {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.startsWith(CLIENT_ID_NAO_CONFIGURADO)) {
            Log.e(TAG, "GOOGLE_WEB_CLIENT_ID não configurado no local.properties")
            return Resultado.Erro(
                "Login com Google indisponível: configure o GOOGLE_WEB_CLIENT_ID."
            )
        }

        val porGoogleId = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filtrarPorContasAutorizadas)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        solicitar(context, porGoogleId)?.let { return it }

        Log.i(TAG, "GetGoogleIdOption não ofereceu conta, tentando Sign in with Google")

        val porBotaoGoogle = GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        solicitar(context, porBotaoGoogle)?.let { return it }

        return Resultado.Erro(
            "Nenhuma conta Google disponível para este app. " +
                "Verifique se a assinatura do app está registrada no Google Cloud."
        )
    }

    /**
     * Devolve null quando o Credential Manager não oferece nenhuma conta para
     * esta opção, sinalizando ao chamador que vale tentar a próxima estratégia.
     */
    private suspend fun solicitar(
        context: Context,
        opcao: CredentialOption
    ): Resultado? {
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
                Resultado.Erro("Não foi possível ler a credencial do Google.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Usuário cancelou a seleção de conta", e)
            Resultado.Cancelado
        } catch (e: NoCredentialException) {
            Log.w(TAG, "Nenhuma credencial oferecida para esta opção", e)
            null
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Falha ao obter credencial do Google: ${e.type}", e)
            Resultado.Erro("Não foi possível entrar com o Google: ${e.type}")
        }
    }

    sealed class Resultado {
        data class Sucesso(val idToken: String) : Resultado()
        data class Erro(val mensagem: String) : Resultado()
        object Cancelado : Resultado()
    }
}
