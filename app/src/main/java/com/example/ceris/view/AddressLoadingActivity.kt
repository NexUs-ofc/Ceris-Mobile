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
import com.example.ceris.model.Address
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.GoogleAuthViewModel
import com.example.ceris.viewmodel.RegisterViewModel

class AddressLoadingActivity : AppCompatActivity(), RegisterViewModel.Listener {

    companion object {
        const val EXTRA_FAMILY_NAME = "extra_family_name"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_DO_GOOGLE = "extra_do_google"
        const val EXTRA_CEP = "extra_cep"
        const val EXTRA_STREET = "extra_street"
        const val EXTRA_NUMBER = "extra_number"
        const val EXTRA_NEIGHBORHOOD = "extra_neighborhood"
        const val EXTRA_CITY = "extra_city"
        const val EXTRA_STATE = "extra_state"
    }

    private lateinit var email: String
    private var doGoogle: Boolean = false

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_address_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this

        val sessionManager = SessionManager(this)
        viewModel.init(sessionManager)

        doGoogle = intent.getBooleanExtra(EXTRA_DO_GOOGLE, false)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: run {
            finish()
            return
        }

        val isValid = viewModel.setUserRegistrationData(
            familyName = intent.getStringExtra(EXTRA_FAMILY_NAME),
            email = email,
            password = intent.getStringExtra(EXTRA_PASSWORD),
            neighborhood = intent.getStringExtra(EXTRA_NEIGHBORHOOD),
            street = intent.getStringExtra(EXTRA_STREET),
            number = intent.getStringExtra(EXTRA_NUMBER),
            cep = intent.getStringExtra(EXTRA_CEP),
            city = intent.getStringExtra(EXTRA_CITY),
            state = intent.getStringExtra(EXTRA_STATE)
        )
        if (!isValid) {
            finish()
            return
        }

        if (doGoogle) {
            completarCadastroGoogle(sessionManager)
        } else {
            viewModel.startRegistrationWithPassword()
        }
    }

    private fun completarCadastroGoogle(sessionManager: SessionManager) {
        val googleViewModel = GoogleAuthViewModel()
        googleViewModel.init(sessionManager)
        googleViewModel.listener = object : GoogleAuthViewModel.Listener {
            override fun makeText(message: String) {
                Toast.makeText(this@AddressLoadingActivity, message, Toast.LENGTH_SHORT)
                    .show()
                finish()
            }

            override fun googleSignInStarted() = Unit

            override fun googleSignInFinished() = Unit

            override fun loggedIn() {
                val intent = Intent(this@AddressLoadingActivity, com.example.ceris.view.MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }

            override fun registrationRequired(dados: com.example.ceris.model.dto.GoogleRegistrationRequiredResponse) {
                makeText("Cadastro ja esta em andamento.")
            }
        }

        val address = Address(
            neighborhood = intent.getStringExtra(EXTRA_NEIGHBORHOOD) ?: "",
            street = intent.getStringExtra(EXTRA_STREET) ?: "",
            number = intent.getStringExtra(EXTRA_NUMBER) ?: "",
            cep = intent.getStringExtra(EXTRA_CEP) ?: "",
            city = intent.getStringExtra(EXTRA_CITY) ?: "",
            state = intent.getStringExtra(EXTRA_STATE) ?: ""
        )
        googleViewModel.completeRegistration(address = address)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .show()
        finish()
    }

    override fun operationCompleted() {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, email)
        startActivity(intent)
        finish()
    }
}
