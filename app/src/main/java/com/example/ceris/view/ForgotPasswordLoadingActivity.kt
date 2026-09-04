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
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.viewmodel.RecoverPasswordViewModel

/**
 * Chama `POST /api/auth/password/forgot`. Sempre responde 202 com um `resetId`
 * (exista ou não a conta — anti-enumeração da API). Em caso de sucesso segue para
 * a [ResetPasswordActivity].
 */
class ForgotPasswordLoadingActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
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

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun operationCompleted() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        intent.putExtra(ResetPasswordActivity.EXTRA_EMAIL, email)
        startActivity(intent)
        finish()
    }
}
