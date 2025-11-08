package com.example.pawtytime

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("pawty_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<TextView>(R.id.tagline).bringToFront()

        Handler(Looper.getMainLooper()).postDelayed({
            routeFromSplash()
        }, 3000)
    }

    private fun routeFromSplash() {
        val user = auth.currentUser
        val onboardingComplete = prefs.getBoolean("onboarding_complete", false)

        when {
            user != null && onboardingComplete -> {
                startActivity(Intent(this, MainActivity::class.java))
            }
            else -> {
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
        }
        finish()
    }
}