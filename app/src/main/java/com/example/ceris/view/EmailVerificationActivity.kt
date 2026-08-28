package com.example.ceris.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
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
import com.example.ceris.viewmodel.RegisterViewModel
import kotlin.getValue

class EmailVerificationActivity : AppCompatActivity(), RegisterViewModel.Listener {
    companion object {
        const val EXTRA_EMAIL = "extra_email"
        private const val RESEND_TIMEOUT_MS = 30_000L
        private const val RESEND_INTERVAL_MS = 1_000L
    }
    private lateinit var subtitleText: TextView
    private lateinit var otpInputsRow: LinearLayout
    private lateinit var confirmCodeBtn: Button
    private lateinit var resendButton: TextView
    private lateinit var resendTimerText: TextView

    private lateinit var otpInputs: List<EditText>
    private var resendTimer: CountDownTimer? = null

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_email_verification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        viewModel.listener = this

        val ssessionManager = SessionManager(this)
        viewModel.init(ssessionManager)

        subtitleText = findViewById(R.id.subtitleText)
        confirmCodeBtn = findViewById(R.id.confirmCodeBtn)
        resendButton = findViewById(R.id.resendButton)
        resendTimerText = findViewById(R.id.resendTimerText)

        otpInputsRow = findViewById(R.id.otpInputsRow)

        otpInputs = otpInputsRow.getEditTexts()
        otpInputs.setupOtpInputs()

        val extraEmail = intent.getStringExtra(EXTRA_EMAIL)
        subtitleText.text = getString(
            R.string.otp_subtitle,
            extraEmail?.maskEmail()
        )

        confirmCodeBtn.setOnClickListener {

           it.hideKeyboard()

            val otpCode = collectOtpCode()
            viewModel.verifyRegistration(otpCode)
        }

        resendButton.setOnClickListener {
            resendButton.isEnabled = false
            resendTimerText.visibility = TextView.VISIBLE
            startResendTimer()
        }

        startResendTimer()
    }

    private fun collectOtpCode(): String {
        return otpInputs.joinToString("") { it.text.toString() }
    }

    private fun startResendTimer() {
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(RESEND_TIMEOUT_MS, RESEND_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                val minutes = secondsRemaining / 60
                val seconds = secondsRemaining % 60
                resendTimerText.text = getString(
                    R.string.otp_timer,
                    String.format("%02d:%02d", minutes, seconds)
                )
            }

            override fun onFinish() {
                resendButton.isEnabled = true
                resendTimerText.visibility = TextView.GONE
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }

    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .show()
    }

    override fun operationCompleted() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
