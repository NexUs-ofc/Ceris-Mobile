package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        startActivity(intent)
    }
}
