package com.example.ceris.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ceris.R
import com.example.ceris.view.utils.hideNavigationBar

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        setContentView(R.layout.activity_splash_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        animateSplash()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 4000)
    }

    private fun animateSplash() {
        val background = findViewById<android.widget.ImageView>(R.id.imageView)
        val logo = findViewById<android.widget.ImageView>(R.id.logo)
        val title = findViewById<android.widget.TextView>(R.id.title)

        background.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(1800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        logo.animate()
            .alpha(1f)
            .setStartDelay(1000)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        title.animate()
            .alpha(1f)
            .setStartDelay(1200)
            .setDuration(1200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }
}