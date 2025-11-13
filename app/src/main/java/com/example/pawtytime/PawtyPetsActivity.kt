package com.example.pawtytime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.Timestamp

data class Pet(
    // Hide own pet in recommended
    val ownerUid: String? = null,

    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val sex: String = "",
    val birthdate: String = "",
    val weightLbs: String = "",
    val likes: String = "",
    val dislikes: String = "",
    val medicalConditions: String = "",
    val medications: String = "",
    val vaccinations: String = "",
    val spayedNeutered: String = "",
    val photoUrl: String? = null,
    val vaxRecordUrl: String? = null
)

class PawtyPetsActivity : AppCompatActivity() {
    private lateinit var person: PersonProfile

    private lateinit var etPetName: TextInputEditText
    private lateinit var etSpecies: TextInputEditText
    private lateinit var etBreed: TextInputEditText
    private lateinit var etBirthdate: TextInputEditText
    private lateinit var etWeight: TextInputEditText

    private var etSex: TextInputEditText? = null
    private var etLikes: TextInputEditText? = null
    private var etDislikes: TextInputEditText? = null
    private var etMedical: TextInputEditText? = null
    private var etMeds: TextInputEditText? = null
    private var etSpayNeuter: TextInputEditText? = null

    private lateinit var zoneUploadPetPhoto: MaterialCardView
    private lateinit var zoneUploadVax: MaterialCardView
    private lateinit var ivPetPhotoPreview: ImageView
    private lateinit var ivVaxPreview: ImageView

    private lateinit var btnAddAnother: Button
    private lateinit var btnDone: Button

    private var petPhotoUrl: String? = null
    private var vaxRecordUrl: String? = null

    private val pets = mutableListOf<Pet>()

    private val uploader by lazy { CloudinaryUploader(this) }

    private val pickPetPhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?: return@registerForActivityResult
                ivPetPhotoPreview.setImageURI(uri)
                snack("Uploading pet photo…")
            uploader.upload(uri) { url ->
                petPhotoUrl = url
                snack(if (url != null) "Pet photo uploaded ✓" else "Upload failed")
                }
            }


    private val pickVax =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?: return@registerForActivityResult
                ivVaxPreview.setImageURI(uri)
                snack("Uploading vaccine record…")
            uploader.upload(uri) { url ->
                vaxRecordUrl = url
                snack(if (url != null) "Vaccine record uploaded ✓" else "Upload failed")
                }
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pawty_pets)

        person = intent.getSerializableExtra("person") as? PersonProfile
            ?: run {
                snack("Missing profile info. Please start again.")
                finish()
                return
            }

        bindViews()
        wireInteractions()
    }

    private fun bindViews() {
        etPetName = findViewById(R.id.etPetName)
        etSpecies = findViewById(R.id.etSpecies)
        etBreed = findViewById(R.id.etBreed)
        etBirthdate = findViewById(R.id.etBirthdate)
        etWeight = findViewById(R.id.etWeight)
        etSex = findViewById(R.id.etSex)
        etLikes = findViewById(R.id.etLikes)
        etDislikes = findViewById(R.id.etDislikes)
        etMedical = findViewById(R.id.etMedical)
        etMeds = findViewById(R.id.etMeds)
        etSpayNeuter = findViewById(R.id.etSpayedNeutered)

        zoneUploadPetPhoto = findViewById(R.id.zoneUploadPetPhoto)
        zoneUploadVax = findViewById(R.id.zoneUploadVax)
        ivPetPhotoPreview = findViewById(R.id.ivPetPhotoPreview)
        ivVaxPreview = findViewById(R.id.ivVaxPreview)

        btnAddAnother = findViewById(R.id.btnAddAnother)
        btnDone = findViewById(R.id.btnDone)
    }

    private fun wireInteractions() {
        findViewById<View>(R.id.tilBirthdate)?.setOnClickListener {
            snack("Enter birthdate (e.g., MM/DD/YYYY)")
        }

        zoneUploadPetPhoto.setOnClickListener {
            pickPetPhoto.launch("image/*")
        }
        zoneUploadVax.setOnClickListener {
            pickVax.launch("*/*")
        }

        btnAddAnother.setOnClickListener {
            val pet = collectPet()
            if (pet.name.isBlank()) {
                snack("Please enter at least a pet name")
                return@setOnClickListener
            }
            pets += pet
            snack("Saved ${pet.name}. Add another")
            clearForm()
            petPhotoUrl = null
            vaxRecordUrl = null
        }

        btnDone.setOnClickListener {
            val current = collectPet()
            if (current.name.isNotBlank() || current.species.isNotBlank() || current.breed.isNotBlank()) {
                pets += current
            }
            createAccountAndSave(person, pets)
        }
    }

    private fun collectPet(): Pet {
        return Pet(
            name = etPetName.text?.toString()?.trim().orEmpty(),
            species = etSpecies.text?.toString()?.trim().orEmpty(),
            breed = etBreed.text?.toString()?.trim().orEmpty(),
            sex = etSex.readTrim(),
            birthdate = etBirthdate.text?.toString()?.trim().orEmpty(),
            weightLbs = etWeight.text?.toString()?.trim().orEmpty(),
            likes = etLikes.readTrim(),
            dislikes = etDislikes.readTrim(),
            medicalConditions = etMedical.readTrim(),
            medications = etMeds.readTrim(),
            spayedNeutered = etSpayNeuter.readTrim(),
            photoUrl = petPhotoUrl,
            vaxRecordUrl = vaxRecordUrl
        )
    }

    private fun clearForm() {
        listOf(
            etPetName, etSpecies, etBreed, etBirthdate, etWeight,
            etSex, etLikes, etDislikes, etMedical, etMeds, etSpayNeuter
        ).forEach { it?.setText("") }

        ivPetPhotoPreview.setImageResource(android.R.drawable.ic_menu_camera)
        ivVaxPreview.setImageResource(android.R.drawable.ic_menu_camera)
    }

    private fun createAccountAndSave(person: PersonProfile, pets: List<Pet>) {
        val auth = Firebase.auth
        val db = Firebase.firestore

        auth.createUserWithEmailAndPassword(person.email, person.password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: run {
                    snack("No user ID returned.")
                    return@addOnSuccessListener
                }

                val userRef = db.collection("users").document(uid)
                val verificationRef = userRef.collection("verification").document("identity")
                val petsRef = userRef.collection("pets")

                val userDoc = mapOf(
                    "uid" to uid,
                    "email" to person.email,
                    "username" to person.username,
                    "firstName" to person.firstName,
                    "lastName" to person.lastName,
                    "phone" to person.phone,
                    "location" to person.location,
                    "profileUrl" to person.profileUrl,
                    "createdAt" to Timestamp.now()
                )

                userRef.set(userDoc)
                    .continueWithTask {
                        val verificationDoc = mapOf(
                            "idFrontUrl" to person.idFrontUrl,
                            "idBackUrl" to person.idBackUrl,
                            "updatedAt" to Timestamp.now()
                        )
                        verificationRef.set(verificationDoc)
                    }
                    .continueWithTask {
                        val batch = db.batch()
                        pets.forEach { pet ->
                            val doc = petsRef.document()

                            // attach ownerUid to each pet
                            val petWithOwner = pet.copy(ownerUid = uid)

                            batch.set(doc, petWithOwner)
                        }
                        batch.commit()
                    }
                    .addOnSuccessListener {
                        getSharedPreferences("pawty_prefs", MODE_PRIVATE)
                            .edit()
                            .putBoolean("onboarding_complete", true)
                            .apply()

                        snack("Account created and data saved ✓")
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        snack("Save failed: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                snack("Sign-up failed: ${e.message}")
            }
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

    private fun TextInputEditText?.readTrim(): String =
        this?.text?.toString()?.trim().orEmpty()
}
