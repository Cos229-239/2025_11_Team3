package com.example.pawtytime

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SchedulePlaydateActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PET_ID = "extra_pet_id"
        const val EXTRA_PET_NAME = "extra_pet_name"
        const val EXTRA_PET_AGE = "extra_pet_age"
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Pet info to reuse later
    private var petId: String? = null
    private var petName: String? = null
    private var petAgeYears: Int = -1

    // Top info
    private lateinit var ivPetPhoto: ImageView
    private lateinit var tvPetNameAge: TextView
    private lateinit var tvScheduleWith: TextView

    // Privacy
    private lateinit var cardPrivacyStatus: MaterialCardView
    private lateinit var tvPrivacyStatus: TextView
    private lateinit var ivPrivacyCheckbox: ImageView
    private var selectedPrivacy: String? = null
    private val privacyOptions = arrayOf("Public", "Friends", "Private")

    // Availability (date + time + days)
    private lateinit var btnCalendar: MaterialButton
    private lateinit var btnEveryDay: MaterialButton
    private lateinit var btnCustomDays: MaterialButton
    private lateinit var layoutDaysRow: View
    private lateinit var btnTimeRange: MaterialButton

    private lateinit var dayButtons: List<MaterialButton>

    // Time state
    private var startHour = 10
    private var startMinute = 0
    private var endHour = 22
    private var endMinute = 0

    // Date state
    private var selectedDateDisplay: String? = null

    // Location & invite
    private lateinit var btnChooseOnMap: MaterialButton
    private lateinit var btnSearchProfiles: MaterialButton

    // Activities
    private lateinit var btnActivityWalk: MaterialButton
    private lateinit var btnActivityFetch: MaterialButton
    private lateinit var btnActivityOther: MaterialButton
    private lateinit var layoutActivityOther: View
    private lateinit var etActivityOther: TextInputEditText

    // Final action
    private lateinit var btnCreatePlaydate: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule_playdate)

        // --- Firebase init ---
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- Get pet info from Intent ---
        petId = intent.getStringExtra(EXTRA_PET_ID)
        petName = intent.getStringExtra(EXTRA_PET_NAME)
        petAgeYears = intent.getIntExtra(EXTRA_PET_AGE, -1)

        // --- Bind views ---
        ivPetPhoto = findViewById(R.id.ivPetPhoto)
        tvPetNameAge = findViewById(R.id.tvPetNameAge)
        tvScheduleWith = findViewById(R.id.tvScheduleWith)

        cardPrivacyStatus = findViewById(R.id.cardPrivacyStatus)
        tvPrivacyStatus = findViewById(R.id.tvPrivacyStatus)
        ivPrivacyCheckbox = findViewById(R.id.ivPrivacyCheckbox)

        btnCalendar = findViewById(R.id.btnCalendar)
        btnEveryDay = findViewById(R.id.btnEveryDay)
        btnCustomDays = findViewById(R.id.btnCustomDays)
        layoutDaysRow = findViewById(R.id.layoutDaysRow)
        btnTimeRange = findViewById(R.id.btnTimeRange)

        val btnDaySun = findViewById<MaterialButton>(R.id.btnDaySun)
        val btnDayMon = findViewById<MaterialButton>(R.id.btnDayMon)
        val btnDayTue = findViewById<MaterialButton>(R.id.btnDayTue)
        val btnDayWed = findViewById<MaterialButton>(R.id.btnDayWed)
        val btnDayThu = findViewById<MaterialButton>(R.id.btnDayThu)
        val btnDayFri = findViewById<MaterialButton>(R.id.btnDayFri)
        val btnDaySat = findViewById<MaterialButton>(R.id.btnDaySat)

        dayButtons = listOf(
            btnDaySun, btnDayMon, btnDayTue, btnDayWed,
            btnDayThu, btnDayFri, btnDaySat
        )

        btnChooseOnMap = findViewById(R.id.btnChooseOnMap)
        btnSearchProfiles = findViewById(R.id.btnSearchProfiles)

        btnActivityWalk = findViewById(R.id.btnActivityWalk)
        btnActivityFetch = findViewById(R.id.btnActivityFetch)
        btnActivityOther = findViewById(R.id.btnActivityOther)
        layoutActivityOther = findViewById(R.id.layoutActivityOther)
        etActivityOther = findViewById(R.id.etActivityOther)

        btnCreatePlaydate = findViewById(R.id.btnCreatePlaydate)

        // --- Fill top section with pet info ---
        val initialName = petName ?: "Loading..."
        tvPetNameAge.text = initialName
        tvScheduleWith.text = "Schedule with $initialName"

        // Try to load  actual pet
        if (petId != null) {
            // We were given a specific petId
            loadPetDetailsFromFirestore(petId!!)
        } else {
            // Fallback
            loadFirstPetForCurrentUser()
        }

        // --- Privacy ---
        updatePrivacyViews()
        cardPrivacyStatus.setOnClickListener {
            showPrivacyDropdown()
        }

        // --- Calendar & time ---
        btnCalendar.setOnClickListener { showDatePicker() }
        setupAvailabilityModeButtons()
        setupDayButtons()
        btnTimeRange.setOnClickListener { showTimeRangePicker() }
        updateTimeRangeText()

        // --- Location & invite (placeholders for now) ---
        btnChooseOnMap.setOnClickListener {
            Toast.makeText(this, "TODO: Open map location picker", Toast.LENGTH_SHORT).show()
        }

        btnSearchProfiles.setOnClickListener {
            Toast.makeText(this, "TODO: Search & invite other pets", Toast.LENGTH_SHORT).show()
        }

        // --- Activities ---
        setupActivityButtons()

        // --- Create Playdate ---
        btnCreatePlaydate.setOnClickListener {
            savePlaydate(petId, petName ?: "Your Pet")
        }
    }

    // ---------- PRIVACY ----------

    private fun showPrivacyDropdown() {
        AlertDialog.Builder(this)
            .setTitle("Privacy Status")
            .setItems(privacyOptions) { _, which ->
                selectedPrivacy = privacyOptions[which]
                updatePrivacyViews()
            }
            .show()
    }

    private fun updatePrivacyViews() {
        if (selectedPrivacy == null) {
            tvPrivacyStatus.text = "Select privacy"
            ivPrivacyCheckbox.setImageDrawable(
                AppCompatResources.getDrawable(this, R.drawable.ic_checkbox_empty)
            )
        } else {
            tvPrivacyStatus.text = selectedPrivacy
            ivPrivacyCheckbox.setImageDrawable(
                AppCompatResources.getDrawable(this, R.drawable.ic_checkbox_checked)
            )
        }
    }

    // ---------- AVAILABILITY: EVERY DAY / CUSTOM DAYS ----------

    private fun setupAvailabilityModeButtons() {
        val checkedIcon = AppCompatResources.getDrawable(this, R.drawable.ic_checkbox_checked)
        val emptyIcon = AppCompatResources.getDrawable(this, R.drawable.ic_checkbox_empty)

        btnEveryDay.isCheckable = true
        btnCustomDays.isCheckable = true

        val selectedBg = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.pawty_dark_blue)
        )
        val unselectedBg = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.pawty_pill_blue)
        )
        val selectedTextColor = ContextCompat.getColor(this, R.color.pawty_cream)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.pawty_dark_blue)

        btnEveryDay.setOnClickListener {
            btnEveryDay.isChecked = true
            btnCustomDays.isChecked = false
            btnEveryDay.icon = checkedIcon
            btnCustomDays.icon = emptyIcon

            layoutDaysRow.visibility = View.GONE

            // Mark all days selected visually + in tag
            //
            dayButtons.forEach { dayBtn ->
                dayBtn.tag = true
                dayBtn.backgroundTintList = selectedBg
                dayBtn.setTextColor(selectedTextColor)
            }
        }

        btnCustomDays.setOnClickListener {
            btnCustomDays.isChecked = true
            btnEveryDay.isChecked = false
            btnCustomDays.icon = checkedIcon
            btnEveryDay.icon = emptyIcon

            layoutDaysRow.visibility = View.VISIBLE

            // Clear selection so user chooses manually
            dayButtons.forEachIndexed { index, dayBtn ->
                dayBtn.tag = false
                dayBtn.backgroundTintList = unselectedBg
                dayBtn.setTextColor(unselectedTextColor)

                // Reset text to single letter
                val baseLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                dayBtn.text = baseLabels.getOrNull(index) ?: dayBtn.text.toString()
            }
        }

        // Default: Every Day
        btnEveryDay.performClick()
    }

    private fun setupDayButtons() {
        // Define the base labels explicitly so we don't fight whatever text is there
        val baseLabels = listOf("S", "M", "T", "W", "T", "F", "S")

        val selectedBg = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.pawty_dark_blue)
        )
        val unselectedBg = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.pawty_pill_blue)
        )
        val selectedTextColor = ContextCompat.getColor(this, R.color.pawty_cream)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.pawty_dark_blue)

        dayButtons.forEachIndexed { index, btn ->
            val baseLabel = baseLabels.getOrNull(index) ?: btn.text.toString()

            // Start unselected
            btn.tag = false          // store our own selected flag
            btn.text = baseLabel
            btn.backgroundTintList = unselectedBg
            btn.setTextColor(unselectedTextColor)

            btn.setOnClickListener {
                val currentlySelected = btn.tag as? Boolean ?: false
                val nowSelected = !currentlySelected
                btn.tag = nowSelected

                if (nowSelected) {
                    btn.backgroundTintList = selectedBg
                    btn.setTextColor(selectedTextColor)
                } else {
                    btn.backgroundTintList = unselectedBg
                    btn.setTextColor(unselectedTextColor)
                }
            }
        }
    }

    // ---------- DATE & TIME ----------

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, y, m, d ->
            val display = "${m + 1}/$d/$y"
            selectedDateDisplay = display
            btnCalendar.text = display
        }, year, month, day).show()
    }

    private fun showTimeRangePicker() {
        val startPicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                startHour = hourOfDay
                startMinute = minute

                // after picking start, pick end
                val endPicker = TimePickerDialog(
                    this,
                    { _, endHourOfDay, endMinuteOfDay ->
                        endHour = endHourOfDay
                        endMinute = endMinuteOfDay
                        updateTimeRangeText()
                    },
                    endHour,
                    endMinute,
                    false
                )
                endPicker.show()
            },
            startHour,
            startMinute,
            false
        )
        startPicker.show()
    }

    private fun updateTimeRangeText() {
        btnTimeRange.text =
            "${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}"
    }

    private fun formatTime(h: Int, m: Int): String {
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        val amPm = if (h >= 12) "PM" else "AM"
        val minuteStr = if (m < 10) "0$m" else "$m"
        return "$hour12:$minuteStr $amPm"
    }

    // ---------- ACTIVITIES ----------

    private fun setupActivityButtons() {
        val selectedBg = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.pawty_dark_blue)
        )
        val unselectedBg = ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.pawty_pill_blue)
        )
        val selectedTextColor = ContextCompat.getColor(this, R.color.pawty_cream)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.pawty_dark_blue)

        fun wireActivity(
            button: MaterialButton,
            baseLabel: String,
            onToggle: ((Boolean) -> Unit)? = null
        ) {
            button.text = baseLabel
            button.tag = false
            button.backgroundTintList = unselectedBg
            button.setTextColor(unselectedTextColor)

            button.setOnClickListener {
                val currentlySelected = button.tag as? Boolean ?: false
                val nowSelected = !currentlySelected
                button.tag = nowSelected

                if (nowSelected) {
                    button.backgroundTintList = selectedBg
                    button.setTextColor(selectedTextColor)
                } else {
                    button.backgroundTintList = unselectedBg
                    button.setTextColor(unselectedTextColor)
                }

                onToggle?.invoke(nowSelected)
            }
        }

        wireActivity(btnActivityWalk, "Walking")
        wireActivity(btnActivityFetch, "Fetch")
        wireActivity(btnActivityOther, "Other") { isNowChecked ->
            layoutActivityOther.visibility = if (isNowChecked) View.VISIBLE else View.GONE
            if (!isNowChecked) {
                etActivityOther.setText("")
            }
        }

        // Initial state
        layoutActivityOther.visibility = View.GONE
    }

    // ---------- LOAD PET FROM FIRESTORE ----------

    private fun loadPetDetailsFromFirestore(petId: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in, can't load pet", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .collection("pets")
            .document(petId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val nameFromDb = doc.getString("name") ?: petName ?: "Your Pet"
                    val birthdate = doc.getString("birthdate") // stored like "10/15/20"
                    val photoUrl = doc.getString("photoUrl")   // matches your Firestore field

                    petName = nameFromDb
                    petAgeYears = calculateAgeYears(birthdate)

                    // Update UI with real name + age
                    if (petAgeYears > 0) {
                        tvPetNameAge.text = "$nameFromDb ($petAgeYears yrs)"
                    } else {
                        tvPetNameAge.text = nameFromDb
                    }
                    tvScheduleWith.text = "Schedule with $nameFromDb"

                    // Load photo via Glide if available
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .into(ivPetPhoto)
                    }
                } else {
                    Toast.makeText(this, "Pet not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load pet: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadFirstPetForCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in, can't load pets", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .collection("pets")
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc != null) {
                    val id = doc.id
                    petId = id

                    val nameFromDb = doc.getString("name") ?: "Your Pet"
                    val birthdate = doc.getString("birthdate")
                    val photoUrl = doc.getString("photoUrl")   // <-- matches your field

                    petName = nameFromDb
                    petAgeYears = calculateAgeYears(birthdate)

                    if (petAgeYears > 0) {
                        tvPetNameAge.text = "$nameFromDb ($petAgeYears yrs)"
                    } else {
                        tvPetNameAge.text = nameFromDb
                    }
                    tvScheduleWith.text = "Schedule with $nameFromDb"

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .into(ivPetPhoto)
                    }
                } else {
                    Toast.makeText(this, "No pets found for this user", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load pets: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun calculateAgeYears(birthdateStr: String?): Int {
        if (birthdateStr.isNullOrBlank()) return -1

        return try {
            // Firestore value is like "10/15/20"
            val sdf = SimpleDateFormat("MM/dd/yy", Locale.US)
            val birthDate = sdf.parse(birthdateStr) ?: return -1

            val now = Calendar.getInstance()
            val dob = Calendar.getInstance().apply { time = birthDate }

            var age = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (now.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            -1
        }
    }

    // ---------- SAVE / SUMMARY ----------

    private fun savePlaydate(petId: String?, petName: String) {
        // Collect days (0 = Sun, 1 = Mon, etc.) using the tag flag
        val selectedDays = mutableListOf<Int>()
        dayButtons.forEachIndexed { index, button ->
            val isSelected = button.tag as? Boolean ?: false
            if (isSelected) {
                selectedDays.add(index)
            }
        }

        // Collect activities
        val activities = mutableListOf<String>()

        val walkSelected = btnActivityWalk.tag as? Boolean ?: false
        val fetchSelected = btnActivityFetch.tag as? Boolean ?: false
        val otherSelected = btnActivityOther.tag as? Boolean ?: false

        if (walkSelected) activities.add("Walking")
        if (fetchSelected) activities.add("Fetch")
        if (otherSelected) {
            val custom = etActivityOther.text?.toString()?.trim()
            if (!custom.isNullOrEmpty()) {
                activities.add(custom)
            } else {
                activities.add("Other")
            }
        }

        val timeRange = btnTimeRange.text.toString()
        val dateStr = selectedDateDisplay ?: "No date chosen"
        val privacyStr = selectedPrivacy ?: "Not set"

        val summary = """
            Pet: $petName
            Pet ID: $petId
            Privacy: $privacyStr
            Date: $dateStr
            Time: $timeRange
            Days indices: $selectedDays
            Activities: $activities
        """.trimIndent()

        Toast.makeText(this, summary, Toast.LENGTH_LONG).show()

        // Later: save this to Firestore, then finish()
        // finish()
    }
}
