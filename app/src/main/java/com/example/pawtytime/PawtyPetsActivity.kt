package com.example.pawtytime

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText


data class Pet(
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
    private lateinit var btnSave: Button

    private val pickPetPhoto =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                ivPetPhotoPreview.setImageURI(it)
                snack("Pet photo selected")
            }
        }

    private val pickVax =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                ivVaxPreview.setImageURI(it)
                snack("Vaccine record selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pawty_pets)

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
        btnSave = findViewById(R.id.btnSave)
    }

    private fun wireInteractions() {
        findViewById<View>(R.id.tilBirthdate)?.setOnClickListener {
            snack("Enter birthdate (e.g., MM/DD/YYYY)")
        }
        etBirthdate.setOnClickListener {

        }

        zoneUploadPetPhoto.setOnClickListener {
            pickPetPhoto.launch("image/*")
        }
        zoneUploadVax.setOnClickListener {
            pickVax.launch("*/*")
        }

        btnAddAnother.setOnClickListener {
            val pet = collectPet()
            snack("Saved ${pet.name}. Add another")
            clearForm()
        }

        btnSave.setOnClickListener {
            val pet = collectPet()
            snack("Saved ${pet.name}.")
            finish()
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
            spayedNeutered = etSpayNeuter.readTrim()
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

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

    private fun TextInputEditText?.readTrim(): String =
        this?.text?.toString()?.trim().orEmpty()
}
