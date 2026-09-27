package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.model.dto.GoogleRegistrationRequiredResponse
import com.example.ceris.view.utils.hideKeyboard
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.view.utils.iniciarLoginGoogle
import com.example.ceris.viewmodel.GoogleAuthViewModel
import com.example.ceris.viewmodel.RegisterViewModel

class RegisterActivity :
    AppCompatActivity(),
    RegisterViewModel.Listener,
    GoogleAuthViewModel.Listener {

    private lateinit var familyNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText

    private lateinit var continueButton: Button
    private lateinit var enterButton: TextView
    private val viewModel: RegisterViewModel by viewModels()
    private val googleViewModel: GoogleAuthViewModel by viewModels()
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

        continueButton = findViewById(R.id.registerBtn)
        enterButton = findViewById(R.id.enterButton)

        googleViewModel.listener = this
        googleViewModel.init(sessionManager)

        googleButton = findViewById(R.id.googleIcon)
        googleButton.setOnClickListener {
            iniciarLoginGoogle(
                aoObterToken = { token -> googleViewModel.authenticateWithGoogle(token) },
                aoFalhar = { mensagem -> makeText(mensagem) }
            )
        }

        enterButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        continueButton.setOnClickListener {

            it.hideKeyboard()

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

    override fun makeText(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
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

    override fun googleSignInStarted() {
        googleButton.isEnabled = false
    }

    override fun googleSignInFinished() {
        googleButton.isEnabled = true
    }

    override fun loggedIn() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * A conta Google e valida, mas ainda nao existe perfil: o cadastro segue na
     * tela de endereco, levando o que o backend ja sabe.
     */
    override fun registrationRequired(dados: GoogleRegistrationRequiredResponse) {
        val intent = Intent(this, SetAddressActivity::class.java).apply {
            putExtra(SetAddressActivity.EXTRA_EMAIL, dados.email)
            putExtra(SetAddressActivity.EXTRA_FAMILY_NAME, dados.name)
            putExtra(SetAddressActivity.EXTRA_DO_GOOGLE, true)
        }
        startActivity(intent)
    }
}
