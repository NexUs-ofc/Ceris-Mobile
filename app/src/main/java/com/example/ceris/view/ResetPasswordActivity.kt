package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.utils.getEditTexts
import com.example.ceris.view.utils.hideKeyboard
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.view.utils.maskEmail
import com.example.ceris.view.utils.setupOtpInputs
import com.example.ceris.viewmodel.RecoverPasswordViewModel

/**
 * Segunda etapa da recuperação de senha (API 7.5): o usuário digita o OTP de 6
 * dígitos e a nova senha. A validação local roda aqui; a chamada a
 * `POST /api/auth/password/reset` acontece na [ResetPasswordLoadingActivity].
 */
class ResetPasswordActivity : AppCompatActivity(), RecoverPasswordViewModel.Listener {

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }

    private lateinit var subtitleText: TextView
    private lateinit var otpInputsRow: LinearLayout
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var resetPasswordBtn: Button
    private lateinit var resendButton: TextView
    private lateinit var voltarBtn: ImageView

    private lateinit var otpInputs: List<EditText>
    private var email: String? = null

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

        subtitleText = findViewById(R.id.subtitleText)
        otpInputsRow = findViewById(R.id.otpInputsRow)
        newPasswordInput = findViewById(R.id.newPasswordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        resetPasswordBtn = findViewById(R.id.resetPasswordBtn)
        resendButton = findViewById(R.id.resendButton)
        voltarBtn = findViewById(R.id.voltarBtn)

        otpInputs = otpInputsRow.getEditTexts()
        otpInputs.setupOtpInputs()

        email = intent.getStringExtra(EXTRA_EMAIL)
        subtitleText.text = getString(R.string.otp_subtitle, email?.maskEmail().orEmpty())

        resetPasswordBtn.setOnClickListener {
            it.hideKeyboard()
            viewModel.verifyResetData(
                otp = collectOtpCode(),
                newPassword = newPasswordInput.text.toString(),
                confirmPassword = confirmPasswordInput.text.toString()
            )
        }

        resendButton.setOnClickListener {
            val target = email ?: return@setOnClickListener
            Toast.makeText(this, "Enviando um novo código...", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ForgotPasswordLoadingActivity::class.java)
            intent.putExtra(ForgotPasswordLoadingActivity.EXTRA_EMAIL, target)
            startActivity(intent)
            finish()
        }

        voltarBtn.setOnClickListener { finish() }
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun operationCompleted() {
        val intent = Intent(this, ResetPasswordLoadingActivity::class.java)
        intent.apply {
            putExtra(ResetPasswordLoadingActivity.EXTRA_OTP, collectOtpCode())
            putExtra(ResetPasswordLoadingActivity.EXTRA_NEW_PASSWORD, newPasswordInput.text.toString())
            putExtra(
                ResetPasswordLoadingActivity.EXTRA_CONFIRM_PASSWORD,
                confirmPasswordInput.text.toString()
            )
        }
        startActivity(intent)
    }

    private fun collectOtpCode(): String = otpInputs.joinToString("") { it.text.toString() }
}
