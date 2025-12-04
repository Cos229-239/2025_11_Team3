package com.example.pawtytime

import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation

class EventDetailActivity : AppCompatActivity() {

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var ivHeader: ImageView
    private lateinit var tvDateTime: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvVenueName: TextView
    private lateinit var tvVenueAddress: TextView
    private lateinit var tvInterested: TextView
    private lateinit var tvGoing: TextView
    private lateinit var tvDetailPrivacy: TextView
    private lateinit var tvPawtyDetailsHeader: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var ivHostAvatar: ImageView
    private lateinit var tvHostName: TextView
    private lateinit var rbHostRating: RatingBar
    private lateinit var ivInterestedIcon: ImageView
    private lateinit var cbGoing: CheckBox
    private lateinit var tvDetailAgeGroup: TextView
    private lateinit var tvDetailVenueType: TextView
    private lateinit var tvDetailOfferings: TextView

    private var currentEvent: EventUi? = null

    private val dateFormat =
        SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        ivHeader = findViewById(R.id.ivEventHeader)
        tvDateTime = findViewById(R.id.tvDetailDateTime)
        tvTitle = findViewById(R.id.tvDetailTitle)
        tvVenueName = findViewById(R.id.tvDetailVenueName)
        tvVenueAddress = findViewById(R.id.tvDetailVenueAddress)
        tvInterested = findViewById(R.id.tvDetailInterested)
        tvGoing = findViewById(R.id.tvDetailGoing)
        tvDetailPrivacy = findViewById(R.id.tvDetailPrivacy)
        tvPawtyDetailsHeader = findViewById(R.id.tvPawtyDetailsHeader)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        ivHostAvatar = findViewById(R.id.ivHostAvatar)
        tvHostName = findViewById(R.id.tvHostName)
        rbHostRating = findViewById(R.id.rbHostRating)
        tvDetailAgeGroup = findViewById(R.id.tvDetailAgeGroup)
        tvDetailVenueType = findViewById(R.id.tvDetailVenueType)
        tvDetailOfferings = findViewById(R.id.tvDetailOfferings)

        ivInterestedIcon = findViewById(R.id.ivInterestedIcon)
        cbGoing = findViewById(R.id.cbGoing)

        val eventId = intent.getStringExtra("eventId")
        if (eventId.isNullOrBlank()) {
            Toast.makeText(this, "Missing event id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadEvent(eventId)
    }

    private fun loadEvent(eventId: String) {
        db.collection("events")
            .document(eventId)
            .get()
            .addOnSuccessListener { doc ->
                val dto = doc.toObject(EventDto::class.java)
                if (dto == null) {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                val ui = dto.toUi(doc.id)

                tvTitle.text = ui.title
                tvDateTime.text = dateFormat.format(ui.dateTime.toDate())
                tvVenueName.text = ui.venueName.ifBlank { "" }
                tvVenueAddress.text =
                    "${ui.addressLine}, ${ui.city}, ${ui.state} ${ui.zip}"

                tvInterested.text = "Interested: ${ui.interestedCount}"
                tvGoing.text = "Going: ${ui.goingCount}"
                tvDetailPrivacy.text = if (ui.isPublic) "Privacy: Public" else "Privacy: Private"
                val ageLabel = when (ui.ageGroup) {
                    "18_plus" -> "18+"
                    "21_plus" -> "21+"
                    else -> "All Ages"
                }
                tvDetailAgeGroup.text = "Age Group: $ageLabel"

                val typeParts = mutableListOf<String>()
                if (ui.indoor) typeParts.add("Indoor")
                if (ui.outdoor) typeParts.add("Outdoor")

                tvDetailVenueType.text = if (typeParts.isEmpty()) {
                    "Venue Type: Not specified"
                } else {
                    "Venue Type: " + typeParts.joinToString(" · ")
                }

                val offeringParts = mutableListOf<String>()
                if (ui.hasFood) offeringParts.add("Food")
                if (ui.hasBeverage) offeringParts.add("Beverage")
                if (ui.hasPetTreats) offeringParts.add("Pet treats")
                if (ui.hasGames) offeringParts.add("Games")
                if (ui.hasLiveMusic) offeringParts.add("Live music")

                tvDetailOfferings.text = if (offeringParts.isEmpty()) {
                    "Offerings: None listed"
                } else {
                    "Offerings: " + offeringParts.joinToString(" · ")
                }
                tvDetailDescription.text = ui.description

                if (!ui.imageUrl.isNullOrBlank()) {
                    Glide.with(this)
                        .load(ui.imageUrl)
                        .centerCrop()
                        .into(ivHeader)
                } else {
                    ivHeader.setImageResource(R.drawable.sample_dog)
                }

                EventRsvpState.loadForEvent(this, ui.id)


                val isInterested = EventRsvpState.interestedIds.contains(ui.id)
                val isGoing = EventRsvpState.goingIds.contains(ui.id)

                bindStarIcon(ivInterestedIcon, isInterested)

                cbGoing.setOnCheckedChangeListener(null)
                cbGoing.isChecked = isGoing

                ivInterestedIcon.setOnClickListener {
                    val currentlyInterested =
                        EventRsvpState.interestedIds.contains(ui.id)
                    val newInterested = !currentlyInterested

                    if (newInterested) {
                        EventRsvpState.interestedIds.add(ui.id)
                        EventRsvpState.goingIds.remove(ui.id)
                        cbGoing.isChecked = false
                    } else {
                        EventRsvpState.interestedIds.remove(ui.id)
                    }

                    bindStarIcon(ivInterestedIcon, newInterested)

                    EventRsvpState.saveForEvent(this, ui.id)
                }

                cbGoing.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        EventRsvpState.goingIds.add(ui.id)
                        EventRsvpState.interestedIds.remove(ui.id)
                        bindStarIcon(ivInterestedIcon, false)
                    } else {
                        EventRsvpState.goingIds.remove(ui.id)
                    }
                }

                loadHost(ui.createdByUid)

                EventRsvpState.saveForEvent(this, ui.id)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadHost(hostUid: String?) {
        if (hostUid.isNullOrBlank()) {
            tvHostName.text = "Host"
            rbHostRating.rating = 0f
            ivHostAvatar.setImageResource(R.drawable.ic_profile)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(hostUid)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    tvHostName.text = "Host"
                    rbHostRating.rating = 0f
                    ivHostAvatar.setImageResource(R.drawable.ic_profile)
                    return@addOnSuccessListener
                }

                val first = doc.getString("firstName") ?: ""
                val last = doc.getString("lastName") ?: ""
                val fullFromParts = "$first $last".trim()

                val username = doc.getString("username") ?: ""
                val displayName = when {
                    fullFromParts.isNotBlank() -> fullFromParts
                    !username.isBlank() -> username
                    !doc.getString("displayName").isNullOrBlank() -> doc.getString("displayName")
                    !doc.getString("name").isNullOrBlank() -> doc.getString("name")
                    !doc.getString("fullName").isNullOrBlank() -> doc.getString("fullName")
                    else -> "Host"
                }

                val avatarUrl = doc.getString("profileUrl")
                val rating = doc.getDouble("rating") ?: 0.0

                tvHostName.text = displayName
                rbHostRating.rating = rating.toFloat()

                if (!avatarUrl.isNullOrBlank()) {
                    ivHostAvatar.load(avatarUrl) {
                        placeholder(R.drawable.ic_profile)
                        error(R.drawable.ic_profile)
                        crossfade(true)
                        transformations(CircleCropTransformation())
                        scale(Scale.FILL)
                    }
                } else {
                    ivHostAvatar.setImageResource(R.drawable.ic_profile)
                }
            }
    }

    private fun bindStarIcon(view: ImageView, isInterested: Boolean) {
        val resId = if (isInterested) {
            R.drawable.ic_star_filled
        } else {
            R.drawable.ic_star
        }
        view.setImageResource(resId)
    }
}