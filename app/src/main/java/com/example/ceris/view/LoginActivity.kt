package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.GoogleRegistrationRequiredResponse
import com.example.ceris.view.utils.GoogleSignInHelper
import com.example.ceris.view.utils.hideKeyboard
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.GoogleAuthViewModel
import com.example.ceris.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity(), LoginViewModel.Listener, GoogleAuthViewModel.Listener {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: TextView
    private lateinit var forgotPasswordButton: TextView
    private lateinit var googleIcon: ImageView
    private val viewModel: LoginViewModel by viewModels()
    private val googleViewModel: GoogleAuthViewModel by viewModels()

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
        googleViewModel.listener = this

        val sessionManager = SessionManager(this)
        viewModel.init(sessionManager)
        googleViewModel.init(sessionManager)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginBtn)
        registerButton = findViewById(R.id.registerButton)
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton)
        googleIcon = findViewById(R.id.googleIcon)

        googleIcon.setOnClickListener {
            it.hideKeyboard()
            entrarComGoogle()
        }

        loginButton.setOnClickListener {

            it.hideKeyboard()

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
     * O Credential Manager exige Context, então a obtenção do idToken acontece
     * aqui na View. A ViewModel recebe apenas a string do token.
     */
    private fun entrarComGoogle() {
        lifecycleScope.launch {
            when (val resultado = GoogleSignInHelper.obterIdToken(this@LoginActivity)) {
                is GoogleSignInHelper.Resultado.Sucesso ->
                    googleViewModel.authenticateWithGoogle(resultado.idToken)

                is GoogleSignInHelper.Resultado.Erro ->
                    makeText(resultado.mensagem)

                GoogleSignInHelper.Resultado.Cancelado -> Unit
            }
        }
    }

    override fun makeText(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun operationCompleted() {
        val intent = Intent(this, LoginLoadingActivity::class.java)
        intent.apply {
            putExtra(LoginLoadingActivity.EXTRA_EMAIL, emailInput.text.toString())
            putExtra(LoginLoadingActivity.EXTRA_PASSWORD, passwordInput.text.toString())
        }
        startActivity(intent)
    }

    override fun googleSignInStarted() {
        googleIcon.isEnabled = false
        googleIcon.alpha = 0.5f
    }

    override fun googleSignInFinished() {
        googleIcon.isEnabled = true
        googleIcon.alpha = 1f
    }

    override fun loggedIn() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun registrationRequired(dados: GoogleRegistrationRequiredResponse) {
        val intent = Intent(this, SetAddressActivity::class.java)
        intent.apply {
            putExtra(EXTRA_GOOGLE_EMAIL, dados.email)
            putExtra(EXTRA_GOOGLE_NAME, dados.name)
            putExtra(EXTRA_FROM_GOOGLE, true)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_FROM_GOOGLE = "from_google"
        const val EXTRA_GOOGLE_EMAIL = "google_email"
        const val EXTRA_GOOGLE_NAME = "google_name"
    }
}
