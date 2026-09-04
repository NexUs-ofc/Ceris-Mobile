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
 * Chama `POST /api/auth/password/reset` com o `resetId` guardado pelo passo
 * anterior. Em caso de 204 (sucesso) volta para o [LoginActivity] limpando a
 * pilha — a API revoga todas as sessões, então o usuário entra de novo.
 */
class ResetPasswordLoadingActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    companion object {
        const val EXTRA_OTP = "extra_otp"
        const val EXTRA_NEW_PASSWORD = "extra_new_password"
        const val EXTRA_CONFIRM_PASSWORD = "extra_confirm_password"
    }

    private val viewModel: RecoverPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_reset_password_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this
        viewModel.init(SessionManager(this))

        val otp = intent.getStringExtra(EXTRA_OTP)
        val newPassword = intent.getStringExtra(EXTRA_NEW_PASSWORD)
        val confirmPassword = intent.getStringExtra(EXTRA_CONFIRM_PASSWORD)

        viewModel.resetPassword(otp, newPassword, confirmPassword)
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun operationCompleted() {
        Toast.makeText(this, "Senha redefinida! Faça login com a nova senha.", Toast.LENGTH_LONG)
            .show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
