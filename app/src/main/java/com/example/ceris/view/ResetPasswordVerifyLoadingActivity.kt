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


class ResetPasswordVerifyLoadingActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    companion object {
        const val EXTRA_OTP = "extra_otp"
    }

    private val viewModel: RecoverPasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_reset_password_verify_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this
        viewModel.init(SessionManager(this))

        val otp = intent.getStringExtra(EXTRA_OTP)
        viewModel.verifyPasswordReset(otp)
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun operationCompleted() {
        val intent = Intent(this, ResetPasswordActivity::class.java)
        startActivity(intent)
        finish()
    }
}
