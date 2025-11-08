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
import com.google.android.material.snackbar.Snackbar

class PawtyPeopleActivity : AppCompatActivity() {
    private lateinit var ivProfilePreview: ImageView
    private lateinit var ivIdFrontPreview: ImageView
    private lateinit var ivIdBackPreview: ImageView

    private lateinit var zoneUploadProfile: MaterialCardView
    private lateinit var zoneUploadIdFront: MaterialCardView
    private lateinit var zoneUploadIdBack: MaterialCardView

    private val pickProfile =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { ivProfilePreview.setImageURI(it); snack("Profile image selected") }
        }

    private val pickIdFront =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { ivIdFrontPreview.setImageURI(it); snack("Front ID image selected") }
        }

    private val pickIdBack =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { ivIdBackPreview.setImageURI(it); snack("Back ID image selected") }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pawty_people)

        ivProfilePreview = findViewById(R.id.ivProfilePreview)
        ivIdFrontPreview = findViewById(R.id.ivIdFrontPreview)
        ivIdBackPreview  = findViewById(R.id.ivIdBackPreview)

        zoneUploadProfile = findViewById(R.id.zoneUploadProfile)
        zoneUploadIdFront = findViewById(R.id.zoneUploadIdFront)
        zoneUploadIdBack  = findViewById(R.id.zoneUploadIdBack)

        val btnBack: TextView = findViewById(R.id.btnBack)
        val btnNext: TextView = findViewById(R.id.btnNext)

        zoneUploadProfile.setOnClickListener { pickProfile.launch("image/*") }
        zoneUploadIdFront.setOnClickListener { pickIdFront.launch("image/*") }
        zoneUploadIdBack.setOnClickListener { pickIdBack.launch("image/*") }

        btnBack.setOnClickListener { finish() }
        //btnNext.setOnClickListener {
            //startActivity(Intent(this, PawtyPetsActivity::class.java))
        //}
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

    private fun getFileName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
}