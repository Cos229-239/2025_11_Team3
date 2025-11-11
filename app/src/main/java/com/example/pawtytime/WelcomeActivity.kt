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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class WelcomeActivity : AppCompatActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var googleClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(Exception::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrEmpty()) {
                snack("Google sign-in failed: empty token")
                return@registerForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            Firebase.auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    snack("Google sign-in ✓")
                    // startActivity(Intent(this, HomeActivity::class.java))
                    // finish()
                }
                .addOnFailureListener { e ->
                    snack("Firebase auth failed: ${e.message}")
                }
        } catch (e: Exception) {
            snack("Google sign-in error: ${e.message}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleClient = GoogleSignIn.getClient(this, gso)

        callbackManager = CallbackManager.Factory.create()

        val btnCreate: MaterialButton = findViewById(R.id.btnCreateAccount)
        btnCreate.isEnabled = true
        btnCreate.setOnClickListener {
            Snackbar.make(it, "Opening Pawty People…", Snackbar.LENGTH_SHORT).show()
            startActivity(Intent(this, PawtyPeopleActivity::class.java))
            }
        findViewById<MaterialButton>(R.id.btnGoogle).setOnClickListener {
            googleSignInLauncher.launch(googleClient.signInIntent)
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


