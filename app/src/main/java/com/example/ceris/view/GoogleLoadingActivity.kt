package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.GoogleRegistrationRequiredResponse
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.GoogleAuthViewModel

/**
 * Espera a resposta do login com Google.
 *
 * Antes disto, a troca do idToken pela sessao acontecia na propria tela de
 * login: a pessoa escolhia a conta, voltava para o formulario e ficava
 * olhando para ele, com o icone do Google apagado como unico sinal de que
 * algo estava acontecendo. Em rede ruim sao varios segundos parecendo travado.
 *
 * O caminho e o mesmo do login por senha, que ja passa pela LoginLoadingActivity:
 * a tela de origem so entrega o token e sai da frente.
 *
 * Reaproveita o layout do login por senha porque e a mesma espera, com a mesma
 * frase; um XML identico ao lado nao se manteria igual por muito tempo.
 */
class GoogleLoadingActivity :
    AppCompatActivity(),
    GoogleAuthViewModel.Listener {

    companion object {
        const val EXTRA_ID_TOKEN = "extra_id_token"
    }

    private val viewModel: GoogleAuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_login_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this

        val sessionManager = SessionManager(this)
        viewModel.init(sessionManager)

        val idToken = intent.getStringExtra(EXTRA_ID_TOKEN) ?: run {
            finish()
            return
        }

        // Só na primeira criação. Girar a tela recria a Activity, e sem isto a
        // troca iria de novo com um idToken que o servidor já consumiu — a
        // segunda tentativa voltaria como credencial inválida. O ViewModel
        // sobrevive à rotação e entrega a resposta para a Activity nova.
        if (savedInstanceState == null) {
            viewModel.authenticateWithGoogle(idToken)
        }
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    // A tela inteira é o indicador de progresso: não há botão para apagar.
    override fun googleSignInStarted() = Unit

    override fun googleSignInFinished() = Unit

    override fun loggedIn() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * A conta Google e valida, mas ainda nao existe perfil: o cadastro segue na
     * tela de endereco, levando o que o backend ja sabe.
     *
     * Encerra junto: voltar da tela de endereco tem de cair no login, nao nesta
     * espera, que ja cumpriu o papel dela.
     */
    override fun registrationRequired(dados: GoogleRegistrationRequiredResponse) {
        val intent = Intent(this, SetAddressActivity::class.java).apply {
            putExtra(SetAddressActivity.EXTRA_EMAIL, dados.email)
            putExtra(SetAddressActivity.EXTRA_FAMILY_NAME, dados.name)
            putExtra(SetAddressActivity.EXTRA_DO_GOOGLE, true)
        }
        startActivity(intent)
        finish()
    }
}
