package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import com.example.ceris.view.utils.mostrarErro
import com.google.android.material.textfield.TextInputLayout
import com.example.ceris.viewmodel.RecoverPasswordViewModel


class ResetPasswordActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var resetPasswordBtn: Button
    private lateinit var voltarBtn: ImageView

    private val viewModel: RecoverPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_reset_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this
        viewModel.init(SessionManager(this))

        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn)
        voltarBtn = findViewById(R.id.voltarBtn)

        resetPasswordBtn.setOnClickListener {
            it.hideKeyboard()
            viewModel.verifyNewPassword(
                newPassword = newPasswordInput.text.toString(),
                confirmPassword = confirmPasswordInput.text.toString()
            )
        }

        voltarBtn.setOnClickListener { finish() }
    }

    /**
     * "As senhas nao conferem" e sobre a confirmacao; o resto e sobre a senha
     * nova. Marcar o campo errado faria a pessoa mexer no que estava certo.
     */
    override fun makeText(message: String) {
        val campo =
            if (message.contains("conferem", ignoreCase = true)) {
                R.id.etl_confirm_password
            } else {
                R.id.etl_new_password
            }

        findViewById<TextInputLayout>(R.id.etl_new_password).mostrarErro(null)
        findViewById<TextInputLayout>(R.id.etl_confirm_password).mostrarErro(null)
        findViewById<TextInputLayout>(campo).mostrarErro(message)
    }

    override fun operationCompleted() {
        val intent = Intent(this, ResetPasswordLoadingActivity::class.java)
        intent.apply {
            putExtra(ResetPasswordLoadingActivity.EXTRA_NEW_PASSWORD, newPasswordInput.text.toString())
            putExtra(
                ResetPasswordLoadingActivity.EXTRA_CONFIRM_PASSWORD,
                confirmPasswordInput.text.toString()
            )
        }
        redefinir.launch(intent)
    }

    /** A troca acontece na tela de espera; a falha volta para ca, no campo. */
    private val redefinir = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        resultado.data
            ?.getStringExtra(ResetPasswordLoadingActivity.EXTRA_ERRO)
            ?.let { makeText(it) }
    }
}
