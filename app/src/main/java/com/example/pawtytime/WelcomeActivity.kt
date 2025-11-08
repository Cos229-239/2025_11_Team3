package com.example.pawtytime

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnCreate: MaterialButton = findViewById(R.id.btnCreateAccount)

        btnCreate.isEnabled = true
        btnCreate.setOnClickListener {
            Snackbar.make(it, "Opening Pawty People…", Snackbar.LENGTH_SHORT).show()
            startActivity(Intent(this, PawtyPeopleActivity::class.java))
            }
        findViewById<MaterialButton>(R.id.btnGoogle).setOnClickListener {
                Snackbar.make(it, "Google sign-in coming next", Snackbar.LENGTH_SHORT).show()
            }
        findViewById<MaterialButton>(R.id.btnFacebook).setOnClickListener {
                Snackbar.make(it, "Facebook sign-in coming next", Snackbar.LENGTH_SHORT).show()
            }
        findViewById<MaterialButton>(R.id.btnEmailLogin).setOnClickListener {
                Snackbar.make(it, "Email login coming next", Snackbar.LENGTH_SHORT).show()
            }

    }
}
