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
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.utils.hideKeyboard
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.RecoverPasswordViewModel

/**
 * Primeira etapa da recuperação de senha (API 7.5): o usuário informa o e-mail da
 * conta. A validação local roda aqui; a chamada a `POST /api/auth/password/forgot`
 * acontece na [ForgotPasswordLoadingActivity].
 */
class ForgotPasswordActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    private lateinit var emailInput: EditText
    private lateinit var sendCodeBtn: Button
    private lateinit var backToLoginButton: TextView
    private lateinit var voltarBtn: ImageView

    private val viewModel: RecoverPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this
        viewModel.init(SessionManager(this))

        emailInput = findViewById(R.id.emailInput)
        sendCodeBtn = findViewById(R.id.sendCodeBtn)
        backToLoginButton = findViewById(R.id.backToLoginButton)
        voltarBtn = findViewById(R.id.voltarBtn)

        sendCodeBtn.setOnClickListener {
            it.hideKeyboard()
            viewModel.verifyEmail(emailInput.text.toString())
        }

        emailInput.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                sendCodeBtn.performClick()
                true
            } else {
                false
            }
        }

        backToLoginButton.setOnClickListener { finish() }
        voltarBtn.setOnClickListener { finish() }
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun operationCompleted() {
        val intent = Intent(this, ForgotPasswordLoadingActivity::class.java)
        intent.putExtra(ForgotPasswordLoadingActivity.EXTRA_EMAIL, emailInput.text.toString())
        startActivity(intent)
    }
}
