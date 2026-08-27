package com.example.ceris.view

import android.os.Bundle
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
import com.example.ceris.view.utils.hideNavigationBar
import com.example.ceris.view.utils.maskEmail
import com.example.ceris.view.utils.setupOtpInputs
import com.example.ceris.viewmodel.RegisterViewModel
import kotlin.getValue

class EmailVerificationActivity : AppCompatActivity(), RegisterViewModel.Listener {
    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
    private lateinit var subtitleText: TextView
    private lateinit var otpInputsRow: LinearLayout
    private lateinit var confirmCodeBtn: Button
    private lateinit var resendButton: TextView

    private lateinit var otpInputs: List<EditText>

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

        otpInputsRow = findViewById(R.id.otpInputsRow)

        otpInputs = otpInputsRow.getEditTexts()
        otpInputs.setupOtpInputs()

        val extraEmail = intent.getStringExtra(EXTRA_EMAIL)
        subtitleText.text = getString(
            R.string.otp_subtitle,
            extraEmail?.maskEmail()
        )


    }


    override fun makeText(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT)
            .show()
    }

    override fun operationCompleted() {
        Toast.makeText(this, "VOCÊ FOI FINALMENTE CADASTRADO UHUUUUUL", Toast.LENGTH_SHORT)
            .show()
    }
}
