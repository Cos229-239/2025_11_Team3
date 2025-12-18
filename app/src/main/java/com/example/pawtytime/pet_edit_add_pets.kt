package com.example.pawtytime

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import org.w3c.dom.Text


class Pet_edit_add_pets : Fragment() {

    private lateinit var petNameAdd: TextInputEditText
    private lateinit var petSpeciesAdd: TextInputEditText
    private lateinit var petBreedAdd: TextInputEditText
    private lateinit var petBirthdateAdd: TextInputEditText
    private lateinit var petWeightAdd: TextInputEditText

    private var petLikesAdd: TextInputEditText? = null
    private var petDislikesAdd: TextInputEditText? = null
    private var petMedicalAdd: TextInputEditText? = null
    private var petGenderAdd: TextInputEditText? = null
    private var petMedicationsAdd: TextInputEditText? = null
    private var petSpayNeuterAdd: TextInputEditText? = null

    private lateinit var petPhotoUpload: MaterialCardView
    private lateinit var petVaxUpload: MaterialCardView
    private lateinit var petPhotoPreview: ImageView
    private lateinit var petVaxPreview: ImageView

    private lateinit var addAnotherPet: Button
    private lateinit var backButton: Button
    private lateinit var doneBtn: Button

    private var petPhotoUrl: String? = null
    private var vaxRecordUrl: String? = null

    private val pets = mutableListOf<Pet>()
    private val uploader by lazy { CloudinaryUploader(requireContext()) }


    private val petPhotoPick =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            petPhotoPreview.setImageURI(uri)
            snack("Uploading Pet Photo...")
            uploader.upload(uri){ url ->
                petPhotoUrl = url
                snack(if(url != null)"Pet Photo Uploaded" else "Photo failed to upload")
            }

        }


    private val petUploadVax =
        registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
            uri ?: return@registerForActivityResult
            petVaxPreview.setImageURI(uri)
            snack("Uploading Vaccination Records...")
            uploader.upload(uri){ url ->
                vaxRecordUrl = url
                snack(if (url != null) "Vaccination Records uploaded!" else "Vaccination Records failed to upload")


            }
        }


    private fun bindVarViews (view: View){
        petNameAdd = view.findViewById(R.id.petEditName)
        petSpeciesAdd = view.findViewById(R.id.petEditSpecies)
        petBreedAdd = view.findViewById(R.id.petEditBreed)
        petBirthdateAdd = view.findViewById(R.id.petEditBirthdate)
        petWeightAdd = view.findViewById(R.id.petEditWeight)
        petGenderAdd = view.findViewById(R.id.petEditSex)
        petLikesAdd = view.findViewById(R.id.petEditLikes)
        petDislikesAdd = view.findViewById(R.id.petEditDislikes)
        petMedicalAdd = view.findViewById(R.id.petEditMedical)
        petMedicationsAdd = view.findViewById(R.id.petEditSpayedNeutered)

        petPhotoUpload = view.findViewById(R.id.petEditUploadPetPhoto)
        petVaxUpload = view.findViewById(R.id.petEditUploadVax)
        petPhotoPreview = view.findViewById(R.id.petEditPhotoPreview)
        petVaxPreview = view.findViewById(R.id.petEditVaxPreview)

        addAnotherPet = view.findViewById(R.id.petEditAddAnother)
        backButton = view.findViewById(R.id.pet_edit_back_Btn)
        doneBtn = view.findViewById(R.id.petEditDone)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pet_edit_add_pets, container, false)

        val backBtn = view.findViewById<ImageButton>(R.id.pet_edit_back_Btn)


        backBtn.setOnClickListener { (activity as? MainActivity)?.loadFragment(PetProfileEdit()) }
        return view
    }

    private fun wireInteractions() {
        petPhotoUpload.setOnClickListener { petPhotoPick.launch("image/*") }
        petVaxUpload.setOnClickListener { petUploadVax.launch("*/*") }

        addAnotherPet.setOnClickListener {
            val pet = collectPet()
            if (pet.name.isBlank()) {
                snack("Please enter at least a pet name")
                return@setOnClickListener
            }
            savePet(pet)
            pets += pet
            snack("Saved ${pet.name}. Add another")
            clearForm()
            petPhotoUrl = null
            vaxRecordUrl = null
        }

        backButton.setOnClickListener {
            val current = collectPet()
            if (current.name.isNotBlank() || current.species.isNotBlank() || current.breed.isNotBlank()) {
                savePet(current)
                pets += current
            }
            (activity as? MainActivity)?.loadFragment(PetProfileEdit())
        }
    }

    private fun collectPet(): Pet {
        return Pet(
            name = petNameAdd.text?.toString()?.trim().orEmpty(),
            species = petSpeciesAdd.text?.toString()?.trim().orEmpty(),
            breed = petBreedAdd.text?.toString()?.trim().orEmpty(),
            sex = petGenderAdd.readTrim(),
            birthdate = petBirthdateAdd.text?.toString()?.trim().orEmpty(),
            weightLbs = petWeightAdd.text?.toString()?.trim().orEmpty(),
            likes = petLikesAdd.readTrim(),
            dislikes = petDislikesAdd.readTrim(),
            medicalConditions = petMedicalAdd.readTrim(),
            medications = petMedicationsAdd.readTrim(),
            spayedNeutered = petSpayNeuterAdd.readTrim(),
            photoUrl = petPhotoUrl,
            vaxRecordUrl = vaxRecordUrl
        )

    }
    private fun clearForm() {
        listOf(
            petNameAdd, petSpeciesAdd, petBreedAdd, petBirthdateAdd, petWeightAdd,
            petGenderAdd, petLikesAdd, petDislikesAdd, petMedicalAdd, petMedicationsAdd, petSpayNeuterAdd
        ).forEach { it?.setText("") }
        petPhotoPreview.setImageResource(android.R.drawable.ic_menu_camera)
        petVaxPreview.setImageResource(android.R.drawable.ic_menu_camera)
    }

    private fun savePet(pet: Pet) {
        val uid = Firebase.auth.currentUser?.uid ?: return snack("No logged-in user")
        val db = Firebase.firestore
        val petsRef = db.collection("users").document(uid).collection("pets")
        val doc = petsRef.document()
        val petWithOwner = pet.copy(ownerUid = uid)
        doc.set(petWithOwner)
            .addOnSuccessListener { snack("Pet saved ✓") }
            .addOnFailureListener { e -> snack("Save failed: ${e.message}") }
    }

    private fun snack(msg: String) =
        Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show()

    private fun TextInputEditText?.readTrim(): String =
        this?.text?.toString()?.trim().orEmpty()

}