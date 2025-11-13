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
import android.util.Patterns
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.ktx.firestore

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
                    saveUserProfile()
                    snack("Google sign-in ✓")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
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

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        val btnCreate: MaterialButton = findViewById(R.id.btnCreateAccount)
        btnCreate.isEnabled = true
        btnCreate.setOnClickListener {
            snack("Opening Pawty People…")
            startActivity(Intent(this, PawtyPeopleActivity::class.java))
            }
        findViewById<MaterialButton>(R.id.btnGoogle).setOnClickListener {
            googleSignInLauncher.launch(googleClient.signInIntent)
            }

        findViewById<MaterialButton>(R.id.btnFacebook).setOnClickListener {
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("public_profile", "email"))
        }

        findViewById<MaterialButton>(R.id.btnEmailLogin).setOnClickListener {
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val password = etPassword.text?.toString()?.trim().orEmpty()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                snack("Enter a valid email")
                etEmail.requestFocus()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                snack("Enter your password")
                etPassword.requestFocus()
                return@setOnClickListener
            }

            Firebase.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    saveUserProfile()
                    snack("Signed in ✓")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }.addOnFailureListener { e ->
                    val msg = when (e) {
                        is FirebaseAuthInvalidUserException -> "No account found for that email."
                        is FirebaseAuthInvalidCredentialsException -> "Incorrect password."
                        else -> e.message ?: "Sign-in failed."
                    }
                    snack(msg)
                }
        }

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                    Firebase.auth.signInWithCredential(credential)
                        .addOnSuccessListener {
                            saveUserProfile()
                            snack("Facebook sign-in ✓")
                            startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                            finish()
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

    private fun saveUserProfile() {
        val user = Firebase.auth.currentUser ?: return
        val db = Firebase.firestore

        val userData = mapOf(
            "uid" to user.uid,
            "email" to user.email,
            "name" to (user.displayName ?: "Pawty User"),
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(user.uid)
            .set(userData)
            .addOnSuccessListener {
                snack("User profile saved to Firestore ✓")
            }
            .addOnFailureListener { e ->
                snack("Failed to save profile: ${e.message}")
            }
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()
}


