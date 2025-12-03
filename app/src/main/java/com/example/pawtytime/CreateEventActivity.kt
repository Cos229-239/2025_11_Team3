package com.example.pawtytime

import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

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
    private lateinit var tvHeaderTitle: TextView

    private var selectedImageUri: Uri? = null

    // edit mode flags
    private var isEditMode: Boolean = false
    private var eventId: String? = null
    private var existingImageUrl: String? = null

    private val pickEventImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                ivEventImagePreview.setImageURI(uri)
            }
        }

    // use a sane format
    private val formatter = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        bindViews()
        setupVisibilityToggles()

        // default: creating
        cbPublic.isChecked = true
        cbPrivate.isChecked = false

        val tapToPick = View.OnClickListener {
            pickEventImage.launch("image/*")
        }
        zoneEventImage.setOnClickListener(tapToPick)
        ivEventImagePreview.setOnClickListener(tapToPick)

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveEvent() }

        // check if we are editing an existing event
        eventId = intent.getStringExtra("eventId")
        if (!eventId.isNullOrBlank()) {
            isEditMode = true
            tvHeaderTitle.text = "Edit Event"
            btnSave.text = "Save Changes"
            loadEventForEdit(eventId!!)
        }
    }

    private fun bindViews() {
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

        tvHeaderTitle = findViewById(R.id.tvCreateEventTitle)
    }

    private fun setupVisibilityToggles() {
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
    }

    private fun loadEventForEdit(id: String) {
        db.collection("events")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                val dto = doc.toObject(EventDto::class.java)
                if (dto == null) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val ui = dto.toUi(doc.id)

                // only host can edit
                val currentUid = auth.currentUser?.uid
                if (currentUid == null || currentUid != ui.createdByUid) {
                    Toast.makeText(this, "You can only edit your own events", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                    return@addOnSuccessListener
                }

                existingImageUrl = ui.imageUrl

                etTitle.setText(ui.title)

                val date = ui.dateTime.toDate()
                val formatted = formatter.format(date)        // "12/25/2025 07:30 PM"
                val parts = formatted.split(" ")
                if (parts.size >= 3) {
                    etDate.setText(parts[0])
                    etTime.setText(parts[1])
                    etAmPm.setText(parts[2])
                }

                etVenue.setText(ui.venueName)
                etAddress.setText(ui.addressLine)
                etCity.setText(ui.city)
                etState.setText(ui.state)
                etZip.setText(ui.zip)
                etDescription.setText(ui.description)

                if (ui.isPublic) {
                    cbPublic.isChecked = true
                    cbPrivate.isChecked = false
                } else {
                    cbPublic.isChecked = false
                    cbPrivate.isChecked = true
                }

                if (!ui.imageUrl.isNullOrBlank()) {
                    com.bumptech.glide.Glide.with(this)
                        .load(ui.imageUrl)
                        .centerCrop()
                        .into(ivEventImagePreview)
                } else {
                    ivEventImagePreview.setImageResource(android.R.drawable.ic_menu_camera)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show()
                finish()
            }
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
                        if (isEditMode && !eventId.isNullOrBlank()) {
                            updateEventDocument(
                                eventId = eventId!!,
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
                                visibility = visibility
                            )
                        } else {
                            createEventDocument(
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
                                visibility = visibility
                            )
                        }
                    }
                }
            }
        } else {
            if (isEditMode && !eventId.isNullOrBlank()) {
                updateEventDocument(
                    eventId = eventId!!,
                    imageUrl = existingImageUrl,
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
                    visibility = visibility
                )
            } else {
                createEventDocument(
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
                    visibility = visibility
                )
            }
        }
    }

    // used when creating a brand new event
    private fun createEventDocument(
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
        visibility: String
    ) {
        val createdByUid = auth.currentUser?.uid ?: "anonymous"

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

    // used when editing an existing event document
    private fun updateEventDocument(
        eventId: String,
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
        visibility: String
    ) {
        val updates = hashMapOf<String, Any>(
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
            "visibility" to visibility
        )

        if (imageUrl != null) {
            updates["imageUrl"] = imageUrl
        }

        db.collection("events")
            .document(eventId)
            .update(updates)
            .addOnSuccessListener {
                btnSave.isEnabled = true
                Toast.makeText(this, "Event updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                btnSave.isEnabled = true
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}