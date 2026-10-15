package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.RecoverPasswordViewModel


class ForgotPasswordLoadingActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    companion object {
        const val EXTRA_EMAIL = "extra_email"

        /** Motivo da falha, devolvido para a tela anterior mostrar no campo. */
        const val EXTRA_ERRO = "extra_erro"
    }

    private val viewModel: RecoverPasswordViewModel by viewModels()
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_forgot_password_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this
        viewModel.init(SessionManager(this))

        email = intent.getStringExtra(EXTRA_EMAIL) ?: run {
            finish()
            return
        }

        viewModel.forgotPassword(email)
    }

    /**
     * A tela de espera nao tem onde escrever e sai de cena assim que a resposta
     * chega: devolve o motivo para quem a abriu mostrar embaixo do campo.
     */
    override fun makeText(message: String) {
        setResult(RESULT_OK, Intent().putExtra(EXTRA_ERRO, message))
        finish()
    }

    override fun operationCompleted() {
        val intent = Intent(this, ResetPasswordOtpActivity::class.java)
        intent.putExtra(ResetPasswordOtpActivity.EXTRA_EMAIL, email)
        startActivity(intent)
        finish()
    }
}
