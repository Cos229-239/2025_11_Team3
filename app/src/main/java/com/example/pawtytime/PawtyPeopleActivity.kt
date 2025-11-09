package com.example.pawtytime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.StorageReference

class PawtyPeopleActivity : AppCompatActivity() {
    private lateinit var ivProfilePreview: ImageView
    private lateinit var ivIdFrontPreview: ImageView
    private lateinit var ivIdBackPreview: ImageView

    private lateinit var zoneUploadProfile: MaterialCardView
    private lateinit var zoneUploadIdFront: MaterialCardView
    private lateinit var zoneUploadIdBack: MaterialCardView

    private lateinit var storageRef: StorageReference

    private val pickProfile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                ivProfilePreview.setImageURI(it)
                snack("Uploading profile image...")
                uploadImageToStorage(it, "profile_${System.currentTimeMillis()}.jpg")
            }
        }

    private val pickIdFront =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                ivIdFrontPreview.setImageURI(it)
                snack("Uploading front ID...")
                uploadImageToStorage(it, "id_front_${System.currentTimeMillis()}.jpg")
            }
        }

    private val pickIdBack =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                ivIdBackPreview.setImageURI(it)
                snack("Uploading back ID...")
                uploadImageToStorage(it, "id_back_${System.currentTimeMillis()}.jpg")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pawty_people)
        storageRef = Firebase.storage.reference

        val btnBack: MaterialButton = findViewById(R.id.btnBack)
        val btnNext: MaterialButton = findViewById(R.id.btnNext)

        ivProfilePreview = findViewById(R.id.ivProfilePreview)
        ivIdFrontPreview = findViewById(R.id.ivIdFrontPreview)
        ivIdBackPreview  = findViewById(R.id.ivIdBackPreview)
        zoneUploadProfile = findViewById(R.id.zoneUploadProfile)
        zoneUploadIdFront = findViewById(R.id.zoneUploadIdFront)
        zoneUploadIdBack  = findViewById(R.id.zoneUploadIdBack)

        zoneUploadProfile.setOnClickListener { pickProfile.launch("image/*") }
        zoneUploadIdFront.setOnClickListener { pickIdFront.launch("image/*") }
        zoneUploadIdBack.setOnClickListener { pickIdBack.launch("image/*") }

        btnBack.setOnClickListener { finish() }

        btnNext.setOnClickListener {
            startActivity(Intent(this@PawtyPeopleActivity, PawtyPetsActivity::class.java))
        }
    }
    private fun uploadImageToStorage(uri: Uri, fileName: String) {
        val fileRef = storageRef.child("users/uploads/$fileName")
        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    snack("Upload complete")
                }
            }
            .addOnFailureListener { e ->
                snack("Upload failed: ${e.message}")
            }
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

    private fun getFileName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
}