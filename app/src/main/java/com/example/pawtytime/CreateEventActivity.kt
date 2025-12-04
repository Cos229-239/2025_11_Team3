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
    private lateinit var cbAllAges: CheckBox
    private lateinit var cb18Plus: CheckBox
    private lateinit var cb21Plus: CheckBox
    private lateinit var cbIndoor: CheckBox
    private lateinit var cbOutdoor: CheckBox
    private lateinit var cbFood: CheckBox
    private lateinit var cbBeverage: CheckBox
    private lateinit var cbPetTreats: CheckBox
    private lateinit var cbGames: CheckBox
    private lateinit var cbLiveMusic: CheckBox

    private var selectedImageUri: Uri? = null
    private var isEditMode: Boolean = false
    private var eventId: String? = null
    private var existingImageUrl: String? = null
    private var isUpdatingAgeGroup = false

    private val pickEventImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                ivEventImagePreview.setImageURI(uri)
            }
        }

    private val formatter = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        bindViews()
        setupVisibilityToggles()
        setupAgeGroupToggles()

        cbPublic.isChecked = true
        cbPrivate.isChecked = false
        cbAllAges.isChecked = true

        val tapToPick = View.OnClickListener {
            pickEventImage.launch("image/*")
        }
        zoneEventImage.setOnClickListener(tapToPick)
        ivEventImagePreview.setOnClickListener(tapToPick)

        btnBack.setOnClickListener { finish() }
        btnSave.setOnClickListener { saveEvent() }

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
        cbAllAges = findViewById(R.id.cbAllAges)
        cb18Plus = findViewById(R.id.cb18Plus)
        cb21Plus = findViewById(R.id.cb21Plus)
        cbIndoor = findViewById(R.id.cbIndoor)
        cbOutdoor = findViewById(R.id.cbOutdoor)
        cbFood = findViewById(R.id.cbFood)
        cbBeverage = findViewById(R.id.cbBeverage)
        cbPetTreats = findViewById(R.id.cbPetTreats)
        cbGames = findViewById(R.id.cbGames)
        cbLiveMusic = findViewById(R.id.cbLiveMusic)
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
                val formatted = formatter.format(date)
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

    private fun setupAgeGroupToggles() {
        cbAllAges.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAgeGroup) return@setOnCheckedChangeListener
            isUpdatingAgeGroup = true
            if (isChecked) {
                cb18Plus.isChecked = false
                cb21Plus.isChecked = false
            } else if (!cb18Plus.isChecked && !cb21Plus.isChecked) {
                cbAllAges.isChecked = true
            }
            isUpdatingAgeGroup = false
        }

        cb18Plus.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAgeGroup) return@setOnCheckedChangeListener
            isUpdatingAgeGroup = true
            if (isChecked) {
                cbAllAges.isChecked = false
                cb21Plus.isChecked = false
            } else if (!cbAllAges.isChecked && !cb21Plus.isChecked) {
                cb18Plus.isChecked = true
            }
            isUpdatingAgeGroup = false
        }

        cb21Plus.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingAgeGroup) return@setOnCheckedChangeListener
            isUpdatingAgeGroup = true
            if (isChecked) {
                cbAllAges.isChecked = false
                cb18Plus.isChecked = false
            } else if (!cbAllAges.isChecked && !cb18Plus.isChecked) {
                cb21Plus.isChecked = true
            }
            isUpdatingAgeGroup = false
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

        val ageGroup = when {
            cbAllAges.isChecked -> "all_ages"
            cb18Plus.isChecked -> "18_plus"
            cb21Plus.isChecked -> "21_plus"
            else -> "all_ages"
        }

        val isIndoor = cbIndoor.isChecked
        val isOutdoor = cbOutdoor.isChecked

        val hasFood = cbFood.isChecked
        val hasBeverage = cbBeverage.isChecked
        val hasPetTreats = cbPetTreats.isChecked
        val hasGames = cbGames.isChecked
        val hasLiveMusic = cbLiveMusic.isChecked

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
                                visibility = visibility,
                                ageGroup = ageGroup,
                                isIndoor = isIndoor,
                                isOutdoor = isOutdoor,
                                hasFood = hasFood,
                                hasBeverage = hasBeverage,
                                hasPetTreats = hasPetTreats,
                                hasGames = hasGames,
                                hasLiveMusic = hasLiveMusic
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
                                visibility = visibility,
                                ageGroup = ageGroup,
                                isIndoor = isIndoor,
                                isOutdoor = isOutdoor,
                                hasFood = hasFood,
                                hasBeverage = hasBeverage,
                                hasPetTreats = hasPetTreats,
                                hasGames = hasGames,
                                hasLiveMusic = hasLiveMusic
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
                    visibility = visibility,
                    ageGroup = ageGroup,
                    isIndoor = isIndoor,
                    isOutdoor = isOutdoor,
                    hasFood = hasFood,
                    hasBeverage = hasBeverage,
                    hasPetTreats = hasPetTreats,
                    hasGames = hasGames,
                    hasLiveMusic = hasLiveMusic
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
                    visibility = visibility,
                    ageGroup = ageGroup,
                    isIndoor = isIndoor,
                    isOutdoor = isOutdoor,
                    hasFood = hasFood,
                    hasBeverage = hasBeverage,
                    hasPetTreats = hasPetTreats,
                    hasGames = hasGames,
                    hasLiveMusic = hasLiveMusic
                )
            }
        }
    }

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
        visibility: String,
        ageGroup: String,
        isIndoor: Boolean,
        isOutdoor: Boolean,
        hasFood: Boolean,
        hasBeverage: Boolean,
        hasPetTreats: Boolean,
        hasGames: Boolean,
        hasLiveMusic: Boolean
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
            "visibility" to visibility,
            "ageGroup" to ageGroup,
            "indoor" to isIndoor,
            "outdoor" to isOutdoor,
            "hasFood" to hasFood,
            "hasBeverage" to hasBeverage,
            "hasPetTreats" to hasPetTreats,
            "hasGames" to hasGames,
            "hasLiveMusic" to hasLiveMusic
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
        visibility: String,
        ageGroup: String,
        isIndoor: Boolean,
        isOutdoor: Boolean,
        hasFood: Boolean,
        hasBeverage: Boolean,
        hasPetTreats: Boolean,
        hasGames: Boolean,
        hasLiveMusic: Boolean
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
            "visibility" to visibility,
            "ageGroup" to ageGroup,
            "indoor" to isIndoor,
            "outdoor" to isOutdoor,
            "hasFood" to hasFood,
            "hasBeverage" to hasBeverage,
            "hasPetTreats" to hasPetTreats,
            "hasGames" to hasGames,
            "hasLiveMusic" to hasLiveMusic
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