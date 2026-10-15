package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.utils.hideKeyboard
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.view.utils.iniciarLoginGoogle
import com.example.ceris.view.utils.limparErros
import com.example.ceris.view.utils.mostrarErro
import com.google.android.material.textfield.TextInputLayout
import com.example.ceris.viewmodel.LoginViewModel

class LoginActivity :
    AppCompatActivity(),
    LoginViewModel.Listener {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var loginButton: Button
    private lateinit var registerButton: TextView
    private lateinit var forgotPasswordButton: TextView
    private val viewModel: LoginViewModel by viewModels()

    /**
     * A chamada de login acontece na tela de espera, que nao tem onde escrever
     * e se encerra sozinha. O resultado traz o motivo da falha de volta para
     * ca, onde ele vira mensagem embaixo do campo.
     */
    private val entrarComSenha = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        resultado.data
            ?.getStringExtra(LoginLoadingActivity.EXTRA_ERRO)
            ?.let { makeText(it) }
    }
    private lateinit var googleButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this

        val sessionManager = SessionManager(this)
        viewModel.init(sessionManager)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailLayout = findViewById(R.id.etl_email)
        passwordLayout = findViewById(R.id.etl_senha)
        loginButton = findViewById(R.id.loginBtn)
        registerButton = findViewById(R.id.registerButton)
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton)

        googleButton = findViewById(R.id.googleIcon)
        googleButton.setOnClickListener {
            // Desligado ja no toque, e nao quando a resposta chega: a escolha da
            // conta demora o bastante para caber um segundo toque, e cada um
            // abriria o seu fluxo.
            googleButton.isEnabled = false

            iniciarLoginGoogle(
                aoObterToken = { token -> abrirCarregamentoDoGoogle(token) },
                aoFalhar = { mensagem -> makeText(mensagem) }
            )
        }

        loginButton.setOnClickListener {

            it.hideKeyboard()

            limparErros(emailLayout, passwordLayout)

            viewModel.verifyCredentials(
                email = emailInput.text.toString(),
                password = passwordInput.text.toString()
            )
        }

        passwordInput.setOnEditorActionListener { _, id, _ ->

            if (id == EditorInfo.IME_ACTION_DONE) {
                loginButton.performClick()
                true
            } else {
                false
            }
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        forgotPasswordButton.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    /**
     * A mensagem aparece embaixo do campo, nao mais como Toast.
     *
     * Qual campo marcar sai do proprio texto: e o unico sinal que o ViewModel
     * manda, e trocar o contrato dele por causa da tela seria pior. Quando a
     * frase cita os dois — "E-mail ou senha invalidos" — os dois ficam
     * vermelhos e o texto fica embaixo do ultimo, como no desenho.
     */
    override fun makeText(message: String) {
        val citaEmail = message.contains("mail", ignoreCase = true)
        val citaSenha = message.contains("senha", ignoreCase = true)

        limparErros(emailLayout, passwordLayout)

        when {
            citaEmail && !citaSenha -> emailLayout.mostrarErro(message)

            citaSenha && !citaEmail -> passwordLayout.mostrarErro(message)

            // Inclui o que nao fala de campo nenhum, como falha de rede: a
            // pessoa precisa ler o motivo, e este e o unico lugar da tela que
            // sabe mostrar texto.
            else -> {
                emailLayout.mostrarErro(" ")
                passwordLayout.mostrarErro(message)
            }
        }
    }

    override fun operationCompleted() {
        val intent = Intent(this, LoginLoadingActivity::class.java)
        intent.apply {
            putExtra(LoginLoadingActivity.EXTRA_EMAIL, emailInput.text.toString())
            putExtra(LoginLoadingActivity.EXTRA_PASSWORD, passwordInput.text.toString())
        }
        entrarComSenha.launch(intent)
    }

    /**
     * A troca do idToken pela sessao acontece na GoogleLoadingActivity, que
     * mostra a espera. Esta tela so entrega o token.
     */
    private fun abrirCarregamentoDoGoogle(idToken: String) {
        val intent = Intent(this, GoogleLoadingActivity::class.java)
        intent.putExtra(GoogleLoadingActivity.EXTRA_ID_TOKEN, idToken)
        startActivity(intent)
    }

    // Cobre os tres jeitos de voltar para ca: cancelar a escolha da conta,
    // falhar, ou voltar da tela de espera.
    override fun onResume() {
        super.onResume()
        googleButton.isEnabled = true
    }
}
