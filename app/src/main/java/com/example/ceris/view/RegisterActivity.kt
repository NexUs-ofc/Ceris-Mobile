package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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
import com.example.ceris.viewmodel.RegisterViewModel

class RegisterActivity :
    AppCompatActivity(),
    RegisterViewModel.Listener {

    private lateinit var familyNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var nomeLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var senhaLayout: TextInputLayout

    private lateinit var continueButton: Button
    private lateinit var enterButton: TextView
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var googleButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this

        val sessionManager = SessionManager(this)
        viewModel.init(sessionManager)

        familyNameInput = findViewById(R.id.familyNameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        nomeLayout = findViewById(R.id.etl_nome)
        emailLayout = findViewById(R.id.etl_email)
        senhaLayout = findViewById(R.id.etl_senha)

        continueButton = findViewById(R.id.registerBtn)
        enterButton = findViewById(R.id.enterButton)

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

        enterButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        continueButton.setOnClickListener {

            it.hideKeyboard()

            limparErros(nomeLayout, emailLayout, senhaLayout)

            viewModel.verifyPersonalData(
                familyName = familyNameInput.text.toString(),
                email = emailInput.text.toString(),
                password = passwordInput.text.toString()
            )

        }

        passwordInput.setOnEditorActionListener { _, id, _ ->

            if (id == EditorInfo.IME_ACTION_DONE) {
                continueButton.performClick()
                true
            } else {
                false
            }
        }
    }

    /**
     * A mensagem aparece embaixo do campo, nao mais como Toast. Mesma regra do
     * login: o campo sai do proprio texto, que e o unico sinal que o ViewModel
     * manda. "Preencha todos os campos" acende os tres de uma vez.
     */
    override fun makeText(message: String) {
        val citaEmail = message.contains("mail", ignoreCase = true)
        val citaSenha = message.contains("senha", ignoreCase = true)
        val todosVazios = message.contains("todos os campos", ignoreCase = true)

        limparErros(nomeLayout, emailLayout, senhaLayout)

        when {
            todosVazios -> {
                nomeLayout.mostrarErro(" ")
                emailLayout.mostrarErro(" ")
                senhaLayout.mostrarErro(message)
            }

            citaEmail && !citaSenha -> emailLayout.mostrarErro(message)

            citaSenha && !citaEmail -> senhaLayout.mostrarErro(message)

            // Sem campo declarado — erro do servidor ou de rede. A pessoa
            // precisa ler o motivo, e o ultimo campo e o lugar que a tela tem.
            else -> senhaLayout.mostrarErro(message)
        }
    }

    override fun operationCompleted() {
        val intent = Intent(this, SetAddressActivity::class.java)
        intent.apply {
            putExtra(SetAddressActivity.EXTRA_FAMILY_NAME, familyNameInput.text.toString())
            putExtra(SetAddressActivity.EXTRA_EMAIL, emailInput.text.toString())
            putExtra(SetAddressActivity.EXTRA_PASSWORD, passwordInput.text.toString())
        }
        startActivity(intent)
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
