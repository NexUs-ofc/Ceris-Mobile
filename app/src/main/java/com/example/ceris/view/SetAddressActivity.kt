package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.utils.MaskTextWatcher
import com.example.ceris.view.utils.Masks
import com.example.ceris.view.utils.hideKeyboard
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.view.utils.onlyNumbers
import com.example.ceris.viewmodel.RegisterViewModel
import com.google.android.material.textfield.TextInputEditText

class SetAddressActivity : AppCompatActivity(), RegisterViewModel.Listener {
    private lateinit var cepInput: TextInputEditText
    private lateinit var ruaInput: TextInputEditText
    private lateinit var numeroInput: TextInputEditText
    private lateinit var bairroInput: TextInputEditText
    private lateinit var estadoInput: TextInputEditText
    private lateinit var cidadeInput: TextInputEditText
    private lateinit var registerBtn: Button
    private lateinit var voltarBtn: ImageView
    private lateinit var email: String
    private val viewModel: RegisterViewModel by viewModels()

    companion object {
        const val EXTRA_FAMILY_NAME = "extra_family_name"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_PASSWORD = "extra_password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_set_address)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this

        val sessionManager = SessionManager(this)
        viewModel.init(sessionManager)

        val familyName = intent.getStringExtra(EXTRA_FAMILY_NAME)
        email = intent.getStringExtra(EXTRA_EMAIL) ?: run {
            finish()
            return
        }
        val password = intent.getStringExtra(EXTRA_PASSWORD)

        cepInput = findViewById(R.id.cepInput)
        ruaInput = findViewById(R.id.ruaInput)
        numeroInput = findViewById(R.id.numeroInput)
        bairroInput = findViewById(R.id.bairroInput)
        estadoInput = findViewById(R.id.estadoInput)
        cidadeInput = findViewById(R.id.cidadeInput)
        registerBtn = findViewById(R.id.registerBtn)
        voltarBtn = findViewById(R.id.voltarBtn)

        cepInput.addTextChangedListener(
            MaskTextWatcher(Masks.CEP)
        )

        cepInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: android.text.Editable?) {
                val cep = s.toString().onlyNumbers()
                if (cep.length == 8) {
                    fetchAddressByCEP(cep)
                }
            }
        })

        estadoInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                registerBtn.performClick()
                true
            } else {
                false
            }
        }

        registerBtn.setOnClickListener {

            it.hideKeyboard()

            val isValid = viewModel.setUserRegistrationData(
                familyName = familyName,
                email = email,
                password = password,
                cep = cepInput.text.toString().onlyNumbers(),
                street = ruaInput.text.toString(),
                number = numeroInput.text.toString(),
                neighborhood = bairroInput.text.toString(),
                state = estadoInput.text.toString(),
                city = cidadeInput.text.toString()
            )
            if (!isValid) return@setOnClickListener

            viewModel.startRegistrationWithPassword()
        }

        voltarBtn.setOnClickListener {
            finish()
        }

    }

    private fun fetchAddressByCEP(cep: String) {
        viewModel.fetchAddressByCEP(
            cep = cep,
            onSuccess = { response ->
                ruaInput.setText(response.logradouro)
                bairroInput.setText(response.bairro)
                cidadeInput.setText(response.localidade)
                estadoInput.setText(response.uf)
            },
            onError = { throwable ->
                Toast.makeText(this, throwable.message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun makeText(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun operationCompleted() {
        val intent = Intent(this, EmailVerificationActivity::class.java)
        intent.putExtra(EmailVerificationActivity.EXTRA_EMAIL, email )
        startActivity(intent)
    }
}