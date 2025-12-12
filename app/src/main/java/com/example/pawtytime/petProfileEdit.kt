package com.example.pawtytime

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore



class PetProfileEdit : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val db by lazy { FirebaseFirestore.getInstance() }

   private val uploader by lazy { CloudinaryUploader(requireContext()) }

    private lateinit var editPetPhoto: ImageButton

    private lateinit var editPetVaccinations: ImageButton
    private var petPhotoUrl: String? = null

    private var petVaccinationsUrl: String? = null

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var petEditSpinner: Spinner

    fun snack(msg: String) =

        view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_SHORT) }?.show()


    private val editPetImage = registerForActivityResult(ActivityResultContracts.GetContent()){
            uri: Uri? ->
        uri ?: return@registerForActivityResult
        editPetPhoto.setImageURI(uri)
        snack("Image is uploading...")

        uploader.upload(uri){
                url ->
            petPhotoUrl = url
            snack(if (url != null) "Photo uploaded!" else "Upload has failed")
        }
    }



    private val editPetVax = registerForActivityResult(ActivityResultContracts.GetContent()){
            uri: Uri? ->
        uri ?: return@registerForActivityResult
        editPetVaccinations.setImageURI(uri)
        snack("Image is uploading...")

        uploader.upload(uri){
                url ->
            petVaccinationsUrl = url
            snack(if (url != null) "Photo uploaded!" else "Upload has failed")
        }
    }




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_pet_profile_edit, container, false)

        // spinner for pet filter
        petEditSpinner = view.findViewById<Spinner>(R.id.pet_profile_edit_filter_pet)

        // editable fields
        val petNameField = view.findViewById<EditText>(R.id.pet_profile_edit_petname)
        val petBreed = view.findViewById<EditText>(R.id.pet_profile_edit_pet_breed)
        val petBirthdate = view.findViewById<EditText>(R.id.pet_profile_edit_birthdate)
        val petGender = view.findViewById<EditText>(R.id.pet_profile_edit_sex)
        val petSpecies = view.findViewById<EditText>(R.id.pet_profile_edit_species)
        val petWeight = view.findViewById<EditText>(R.id.pet_profile_edit_weight)
        val petDislikes = view.findViewById<EditText>(R.id.pet_profile_edit_dislikes)
        val petLikes = view.findViewById<EditText>(R.id.pet_profile_edit_likes)
        val petMedicalConditions = view.findViewById<EditText>(R.id.pet_profile_edit_medical_conditions)
        val petMedications = view.findViewById<EditText>(R.id.pet_profile_edit_medications)
        val petVaccinations = view.findViewById<EditText>(R.id.pet_profile_edit_vaccinations)
        val petSpayedNeutered = view.findViewById<EditText>(R.id.pet_profile_edit_spayed_neutered)

        // Buttons
        val addMorePets = view.findViewById<Button>(R.id.pet_profile_edit_add_pet)
        val backBtn = view.findViewById<Button>(R.id.btn_edit_pet_profile_back)
        val saveChanges = view.findViewById<Button>(R.id.edit_pet_profile_save_changes)

        // Change images buttons
        editPetPhoto = view.findViewById(R.id.pet_profile_edit_pic_btn)
        editPetVaccinations = view.findViewById(R.id.pet_profile_edit_vax_upload)



        // this function populates the spinner for the pet filter:
        fun populateSpinner(list: List<String>){
            val listAdapter = object : ArrayAdapter<String>(
                requireContext(),
                R.layout.pets_default_spinner, list)
            {

                override fun isEnabled(position: Int): Boolean {
                    return position != 0
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup):View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    if(position == 0){
                        view.setTextColor(Color.GRAY)
                    } else {
                        view.setTextColor(Color.BLACK)
                    }
                    return view
                }
            }
            listAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            petEditSpinner.adapter = listAdapter
        }


        // populate the spinner:
        currentUserId?.let{
                uid ->
            db.collection("users")
                .document(uid)
                .collection("pets")
                .get()
                .addOnSuccessListener {
                        querySnapshot ->
                    val petList = mutableListOf("Filter Pet")
                    for(document in querySnapshot.documents){
                        document.getString("name")?.let{
                                petName ->
                            petList.add(petName)
                        }
                    }
                    populateSpinner(petList)
                }

        }


        // This method will show the information for a specific pet:
        fun updatePetsInfo(petName: String){
            db.collection("users").document(currentUserId ?: "")
                .collection("pets")
                .whereEqualTo("name",petName)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        petNameField.setText(document.getString("name"))
                        petBreed.setText(document.getString("breed"))
                        petBirthdate.setText(document.getString("birthdate"))
                        petGender.setText(document.getString("sex"))
                        petSpecies.setText(document.getString("species"))
                        petWeight.setText(document.getString("weightLbs"))
                        petDislikes.setText(document.getString("dislikes"))
                        petLikes.setText(document.getString("likes"))
                        petMedicalConditions.setText(document.getString("medicalConditions"))
                        petMedications.setText(document.getString("medications"))
                        petVaccinations.setText(document.getString("vaccinations"))
                        petSpayedNeutered.setText(document.getString("spayedNeutered"))

                        petVaccinationsUrl = document.getString("vaxRecordUrl")
                        petPhotoUrl = document.getString("photoUrl")

                        if(!petPhotoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(petPhotoUrl)
                                .into(editPetPhoto)
                        }

                        if(!petVaccinationsUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(petVaccinationsUrl)
                                .into(editPetVaccinations)
                        }
                    }
                }
        }

        // this method will save the changes for each pet
        fun savePetChanges(petName: String){
            val newPetName = petNameField.text.toString()
            val newPetBreed = petBreed.text.toString()
            val newPetBirthdate = petBirthdate.text.toString()
            val newPetGender = petGender.text.toString()
            val newPetSpecies = petSpecies.text.toString()
            val newPetWeight = petWeight.text.toString()
            val newPetDislikes = petDislikes.text.toString()
            val newPetLikes = petLikes.text.toString()
            val newPetMedicalConditions = petMedicalConditions.text.toString()
            val newPetMedications = petMedications.text.toString()
            val newPetSpayedNeutered = petSpayedNeutered.text.toString()
            val newPetVaccinations = petVaccinations.text.toString()


            val petProfUpdates = mutableMapOf<String, Any> (
                "name" to newPetName,
                "breed" to newPetBreed,
                "birthdate" to newPetBirthdate,
                "sex" to newPetGender,
                "species" to newPetSpecies,
                "weightLbs" to newPetWeight,
                "dislikes" to newPetDislikes,
                "likes" to newPetLikes,
                "medicalConditions" to newPetMedicalConditions,
                "medications" to newPetMedications,
                "spayedNeutered" to newPetSpayedNeutered,
                "vaccinations" to newPetVaccinations
            )

            petPhotoUrl?.let{ petProfUpdates["photoUrl"] = it }
            petVaccinationsUrl?.let { petProfUpdates["vaxRecordUrl"] = it }

            db.collection("users").document(currentUserId ?: "")
                .collection("pets")
                .whereEqualTo("name", petName)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        document.reference.update(petProfUpdates)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "${petName} has been updated!", Toast.LENGTH_SHORT).show()
                                }
                            .addOnFailureListener { Toast.makeText(requireContext(), "Update has Failed!", Toast.LENGTH_SHORT).show() }
                            }
                    }
        }

        petEditSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (position == 0) return
                val selectedPetName = parent.getItemAtPosition(position) as String
                updatePetsInfo(selectedPetName)

                editPetPhoto.setOnClickListener {
                    editPetImage.launch("image/*")
                }
                editPetVaccinations.setOnClickListener {
                    editPetVax.launch("image/*")
                }


                saveChanges.setOnClickListener {
                    savePetChanges(selectedPetName)
                }
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        addMorePets.setOnClickListener { (activity as? MainActivity)?.loadFragment(Pet_edit_add_pets()) }


        return view
    }


}