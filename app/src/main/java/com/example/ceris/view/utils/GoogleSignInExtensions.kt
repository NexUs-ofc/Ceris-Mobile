package com.example.ceris.view.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Dispara a seleção de conta Google e devolve o idToken.
 *
 * Existe para o login e o cadastro compartilharem o mesmo fluxo: as duas telas
 * só dizem o que fazer com o token e com o erro.
 *
 * Roda em lifecycleScope, então a corrotina é cancelada se a tela for destruída
 * enquanto a caixa de seleção estiver aberta - sem isso, o retorno chegaria a
 * uma Activity morta.
 *
 * O cancelamento pelo usuário não chama nenhum dos dois: fechar a caixa de
 * propósito não é erro e não merece aviso.
 */
fun AppCompatActivity.iniciarLoginGoogle(
    aoObterToken: (String) -> Unit,
    aoFalhar: (String) -> Unit
) {
    lifecycleScope.launch {
        when (val resultado = GoogleSignInHelper.obterIdToken(this@iniciarLoginGoogle)) {
            is GoogleSignInHelper.Resultado.Sucesso -> aoObterToken(resultado.idToken)
            is GoogleSignInHelper.Resultado.Erro -> aoFalhar(resultado.mensagem)
            GoogleSignInHelper.Resultado.Cancelado -> Unit
        }
    }
}
