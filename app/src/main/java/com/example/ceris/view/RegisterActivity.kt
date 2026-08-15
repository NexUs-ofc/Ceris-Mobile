package com.example.ceris.view

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R

class RegisterActivity : AppCompatActivity() {

    private lateinit var familyNameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText

    private lateinit var registerBtn: Button
    private lateinit var enterBtn: Button
    private lateinit var googleIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        familyNameInput = findViewById(R.id.familyNameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)

        registerBtn = findViewById(R.id.registerButton)
        enterBtn = findViewById(R.id.enterButton)
        googleIcon = findViewById(R.id.googleIcon)

        registerBtn.setOnClickListener {
            TODO()
        }

        enterBtn.setOnClickListener {
            TODO()
        }

        googleIcon.setOnClickListener {
            TODO()
        }
    }

}