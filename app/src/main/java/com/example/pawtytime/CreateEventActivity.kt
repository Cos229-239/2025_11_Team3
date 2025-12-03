package com.example.pawtytime

import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import android.widget.CheckBox

class CreateEventActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val cloudinary by lazy { CloudinaryUploader(this) }

    private lateinit var zoneEventImage: MaterialCardView
    private lateinit var ivEventImagePreview: ImageView

    private lateinit var etTitle: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etTime: TextInputEditText
    private lateinit var etAmPm: TextInputEditText
    private lateinit var etVenue: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etState: TextInputEditText
    private lateinit var etZip: TextInputEditText
    private lateinit var cbPublic: CheckBox
    private lateinit var cbPrivate: CheckBox
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: MaterialButton

    private var selectedImageUri: Uri? = null

    private val pickEventImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                ivEventImagePreview.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        zoneEventImage = findViewById(R.id.zoneEventImage)
        ivEventImagePreview = findViewById(R.id.ivEventImagePreview)

        etTitle = findViewById(R.id.etEventTitle)
        etDate = findViewById(R.id.etEventDate)
        etTime = findViewById(R.id.etEventTime)
        etAmPm = findViewById(R.id.etEventAmPm)
        etVenue = findViewById(R.id.etVenueName)
        etAddress = findViewById(R.id.etStreet)
        etCity = findViewById(R.id.etCity)
        etState = findViewById(R.id.etState)
        etZip = findViewById(R.id.etZip)
        cbPublic = findViewById(R.id.cbPublic)
        cbPrivate = findViewById(R.id.cbPrivate)
        etDescription = findViewById(R.id.etDescription)
        btnSave = findViewById(R.id.btnSaveEvent)
        btnBack = findViewById(R.id.btnCancelEvent)

        val tapToPick = View.OnClickListener {
            pickEventImage.launch("image/*")
        }
        zoneEventImage.setOnClickListener(tapToPick)
        ivEventImagePreview.setOnClickListener(tapToPick)

        cbPublic.isChecked = true
        cbPrivate.isChecked = false

        cbPublic.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbPrivate.isChecked = false
            } else if (!cbPrivate.isChecked) {
                cbPublic.isChecked = true
            }
        }

        cbPrivate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbPublic.isChecked = false
            } else if (!cbPublic.isChecked) {
                cbPrivate.isChecked = true
            }
        }

        btnSave.setOnClickListener { saveEvent() }
        btnBack.setOnClickListener { finish() }
    }

    private fun saveEvent() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        val dateStr = etDate.text?.toString()?.trim().orEmpty()
        val timeStr = etTime.text?.toString()?.trim().orEmpty()
        val ampm = etAmPm.text?.toString()?.trim().orEmpty()
        val venue = etVenue.text?.toString()?.trim().orEmpty()
        val addr = etAddress.text?.toString()?.trim().orEmpty()
        val city = etCity.text?.toString()?.trim().orEmpty()
        val state = etState.text?.toString()?.trim().orEmpty()
        val zip = etZip.text?.toString()?.trim().orEmpty()
        val desc = etDescription.text?.toString()?.trim().orEmpty()

        if (title.isBlank() || dateStr.isBlank() || timeStr.isBlank() || ampm.isBlank() ||
            addr.isBlank() || city.isBlank() || state.isBlank() || zip.isBlank()
        ) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val dateTimeStr = "$dateStr $timeStr $ampm"
        val formatter = java.text.SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())

        val parsedDate = try {
            formatter.parse(dateTimeStr)
        } catch (_: Exception) {
            Toast.makeText(this, "Invalid date or time format", Toast.LENGTH_SHORT).show()
            return
        }

        val timestamp = Timestamp(parsedDate!!)

        val fullAddress = "$addr, $city, $state $zip"
        val geocoder = Geocoder(this, Locale.getDefault())
        val results = try {
            geocoder.getFromLocationName(fullAddress, 1)
        } catch (_: Exception) {
            null
        }

        if (results.isNullOrEmpty()) {
            Toast.makeText(this, "Could not find that location", Toast.LENGTH_SHORT).show()
            return
        }

        val lat = results[0].latitude
        val lng = results[0].longitude
        val currentUid = auth.currentUser?.uid ?: "anonymous"

        if (!cbPublic.isChecked && !cbPrivate.isChecked) {
            Toast.makeText(this, "Please choose Public or Private", Toast.LENGTH_SHORT).show()
            return
        }

        val visibility = if (cbPrivate.isChecked) "private" else "public"

        btnSave.isEnabled = false

        val imageUri = selectedImageUri
        if (imageUri != null) {
            cloudinary.upload(imageUri) { url ->
                runOnUiThread {
                    if (url == null) {
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    } else {
                        saveEventDocument(
                            imageUrl = url,
                            title = title,
                            desc = desc,
                            timestamp = timestamp,
                            venueName = venue,
                            addr = addr,
                            city = city,
                            state = state,
                            zip = zip,
                            lat = lat,
                            lng = lng,
                            createdByUid = currentUid,
                            visibility = visibility
                        )
                    }
                }
            }
        } else {
            saveEventDocument(
                imageUrl = null,
                title = title,
                desc = desc,
                timestamp = timestamp,
                venueName = venue,
                addr = addr,
                city = city,
                state = state,
                zip = zip,
                lat = lat,
                lng = lng,
                createdByUid = currentUid,
                visibility = visibility
            )
        }
    }

    private fun saveEventDocument(
        imageUrl: String?,
        title: String,
        desc: String,
        timestamp: Timestamp,
        venueName: String,
        addr: String,
        city: String,
        state: String,
        zip: String,
        lat: Double,
        lng: Double,
        createdByUid: String,
        visibility: String
    ) {
        val eventData = hashMapOf(
            "title" to title,
            "description" to desc,
            "dateTime" to timestamp,
            "venueName" to venueName,
            "addressLine" to addr,
            "city" to city,
            "state" to state,
            "zip" to zip,
            "lat" to lat,
            "lng" to lng,
            "imageUrl" to imageUrl,
            "createdByUid" to createdByUid,
            "goingCount" to 0L,
            "interestedCount" to 0L,
            "visibility" to visibility
        )

        db.collection("events")
            .add(eventData)
            .addOnSuccessListener {
                btnSave.isEnabled = true
                Toast.makeText(this, "Event created!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
