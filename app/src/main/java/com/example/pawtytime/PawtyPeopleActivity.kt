package com.example.pawtytime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.android.material.textfield.TextInputEditText


class PawtyPeopleActivity : AppCompatActivity() {
    private lateinit var ivProfilePreview: ImageView
    private lateinit var ivIdFrontPreview: ImageView
    private lateinit var ivIdBackPreview: ImageView

    private lateinit var zoneUploadProfile: MaterialCardView
    private lateinit var zoneUploadIdFront: MaterialCardView
    private lateinit var zoneUploadIdBack: MaterialCardView

    private var profileUrl: String? = null
    private var idFrontUrl: String? = null
    private var idBackUrl: String? = null

    private val uploader by lazy { CloudinaryUploader(this) }

    private val pickProfile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?: return@registerForActivityResult
                ivProfilePreview.setImageURI(uri)
                snack("Uploading profile image...")
                uploader.upload(uri) { url ->
                    profileUrl = url
                    snack(if (url != null) "Profile image uploaded ✓" else "Upload failed")
                }
            }

    private val pickIdFront =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?: return@registerForActivityResult
                ivIdFrontPreview.setImageURI(uri)
                snack("Uploading front ID...")
            uploader.upload(uri) { url ->
                idFrontUrl = url
                snack(if (url != null) "Front ID uploaded ✓" else "Upload failed")
                }
            }


    private val pickIdBack =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?: return@registerForActivityResult
                ivIdBackPreview.setImageURI(uri)
                snack("Uploading back ID...")
            uploader.upload(uri) { url ->
                idBackUrl = url
                snack(if (url != null) "Back ID uploaded ✓" else "Upload failed")
                }
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pawty_people)

        val btnBack: MaterialButton = findViewById(R.id.btnBack)
        val btnNext: MaterialButton = findViewById(R.id.btnNext)

        ivProfilePreview = findViewById(R.id.ivProfilePreview)
        ivIdFrontPreview = findViewById(R.id.ivIdFrontPreview)
        ivIdBackPreview = findViewById(R.id.ivIdBackPreview)

        zoneUploadProfile = findViewById(R.id.zoneUploadProfile)
        zoneUploadIdFront = findViewById(R.id.zoneUploadIdFront)
        zoneUploadIdBack = findViewById(R.id.zoneUploadIdBack)

        zoneUploadProfile.setOnClickListener { pickProfile.launch("image/*") }
        zoneUploadIdFront.setOnClickListener { pickIdFront.launch("image/*") }
        zoneUploadIdBack.setOnClickListener { pickIdBack.launch("image/*") }

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            val person = collectPersonOrShowErrors() ?: return@setOnClickListener
            val intent = Intent(this@PawtyPeopleActivity, PawtyPetsActivity::class.java)
            intent.putExtra("person", person)
            startActivity(intent)
        }
    }

    private fun collectPersonOrShowErrors(): PersonProfile? {
        val firstName = findViewById<TextInputEditText>(R.id.etFirstName)?.text?.toString()?.trim().orEmpty()
        val lastName  = findViewById<TextInputEditText>(R.id.etLastName)?.text?.toString()?.trim().orEmpty()
        val username  = findViewById<TextInputEditText>(R.id.etUsername)?.text?.toString()?.trim().orEmpty()
        val email     = findViewById<TextInputEditText>(R.id.etEmail)?.text?.toString()?.trim().orEmpty()
        val password  = findViewById<TextInputEditText>(R.id.etPassword)?.text?.toString()?.trim().orEmpty()
        val phone     = findViewById<TextInputEditText>(R.id.etPhone)?.text?.toString()?.trim()
        val location  = findViewById<TextInputEditText>(R.id.etLocation)?.text?.toString()?.trim()

        if (firstName.isEmpty() || lastName.isEmpty() || username.isEmpty() ||
            email.isEmpty() || password.isEmpty()) {
            snack("Please complete all required fields (*)")
            return null
        }

        return PersonProfile(
            firstName = firstName,
            lastName = lastName,
            username = username,
            email = email,
            password = password,
            phone = phone,
            location = location,
            profileUrl = profileUrl,
            idFrontUrl = idFrontUrl,
            idBackUrl = idBackUrl
        )
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

    private fun getFileName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
}