package com.example.pawtytime

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class WelcomeActivity : AppCompatActivity() {

    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        callbackManager = CallbackManager.Factory.create()

        val btnCreate: MaterialButton = findViewById(R.id.btnCreateAccount)
        btnCreate.isEnabled = true
        btnCreate.setOnClickListener {
            Snackbar.make(it, "Opening Pawty People…", Snackbar.LENGTH_SHORT).show()
            startActivity(Intent(this, PawtyPeopleActivity::class.java))
            }
        findViewById<MaterialButton>(R.id.btnGoogle).setOnClickListener {
                Snackbar.make(it, "Google sign-in coming next", Snackbar.LENGTH_SHORT).show()
            }
        findViewById<MaterialButton>(R.id.btnEmailLogin).setOnClickListener {
                Snackbar.make(it, "Email login coming next", Snackbar.LENGTH_SHORT).show()
            }
        findViewById<MaterialButton>(R.id.btnFacebook).setOnClickListener {
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("public_profile", "email"))
        }

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    Firebase.auth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            snack("Facebook sign-in ✓")
                            //startActivity(Intent(this@WelcomeActivity, HomeActivity::class.java))
                            //finish()
                        }
                        .addOnFailureListener { e ->
                            snack("Firebase auth failed: ${e.message}")
                        }
                }

                override fun onCancel() {
                    snack("Facebook sign-in cancelled")
                }

                override fun onError(error: FacebookException) {
                    snack("Facebook error: ${error.message}")
                }
            }
        )
    }

    @Deprecated("Facebook SDK uses onActivityResult for login callbacks")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (::callbackManager.isInitialized) {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()
}


