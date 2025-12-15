package com.example.pawtytime

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import coil.load
import coil.size.Scale
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class petProfileView : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private val db by lazy { FirebaseFirestore.getInstance() }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var petViewSpinner: Spinner

    fun snack(msg: String) =

        view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_SHORT) }?.show()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout for this fragment
        val view = inflater.inflate(R.layout.fragment_pet_profile_view, container, false)

        petViewSpinner = view.findViewById<Spinner>(R.id.pet_profile_view_filter)

        // text views
        val _petName = view.findViewById<TextView>(R.id.pet_view_pet_name)
        val _petBreed = view.findViewById<TextView>(R.id.pet_view_breed)
        val _petBirthdate = view.findViewById<TextView>(R.id.pet_view_birthdate)
        val _petGender = view.findViewById<TextView>(R.id.pet_view_gender)
        val _petSpecies = view.findViewById<TextView>(R.id.pet_view_species)
        val _petWeight = view.findViewById<TextView>(R.id.pet_view_weight)
        val _petDislikes = view.findViewById<TextView>(R.id.pet_view_pet_dislikes)
        val _petLikes = view.findViewById<TextView>(R.id.pet_view_pet_likes)
        val _petMedicalConditions = view.findViewById<TextView>(R.id.pet_view_medical_conditions)
        val _petMedications = view.findViewById<TextView>(R.id.pet_view_medications)
        val _petVaccinations = view.findViewById<TextView>(R.id.pet_view_vaccinations)
        val _petSpayedNeutered = view.findViewById<TextView>(R.id.pet_view_spayed_neutered)


        // Buttons
        val viewPetPhotos = view.findViewById<Button>(R.id.pet_view_photosBtn)

        // pet prof image
        var petProfPhotoUrl: String
        val _petProfImage = view.findViewById<ImageView>(R.id.pet_view_prof_pic)


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
            petViewSpinner.adapter = listAdapter
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

        // method to update the pets information
        fun updatePetInfo(petName: String) {
            if(currentUserId != null){
                db.collection("users").document(currentUserId)
                    .collection("pets")
                    .whereEqualTo("name", petName)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot.documents){
                            _petName.text = document.getString("name")
                            _petBreed.text = document.getString("breed")
                            _petBirthdate.text = document.getString("birthdate")
                            _petGender.text = document.getString("sex")
                            _petSpecies.text = document.getString("species")
                            _petWeight.text = document.getString("weightLbs")
                            _petDislikes.text = document.getString("dislikes")
                            _petLikes.text = document.getString("likes")
                            _petMedicalConditions.text = document.getString("medicalConditions")
                            _petMedications.text = document.getString("medications")
                            if(document.getString("vaccinations").isNullOrEmpty()){
                                _petVaccinations.text = "N/A"
                            } else {
                                _petVaccinations.text = document.getString("vaccinations")
                            }
                            _petSpayedNeutered.text = document.getString("spayedNeutered")


                            petProfPhotoUrl = document.getString("photoUrl").toString()
                            if(petProfPhotoUrl.isNotBlank()) {
                                _petProfImage.load(petProfPhotoUrl) {
                                    placeholder(R.drawable.ic_profile)
                                    error(R.drawable.ic_profile)
                                    transformations(CircleCropTransformation())
                                    size(_petProfImage.width, _petProfImage.height)
                                }
                            }

                        }
                    }
            }
        }

        petViewSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (position == 0) return
                val selectedPetName = parent.getItemAtPosition(position) as String
                updatePetInfo(selectedPetName)

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        return view
    }

    //  Hook up the Schedule Playdate button
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSchedulePlaydate =
            view.findViewById<MaterialButton>(R.id.btnSchedulePlaydate)

        // TEMP VALUES just so it compiles & runs.
        // Replace later with real data from Firebase / pet object.
        val petId: String? = null
        val petName: String = param1 ?: "Your Pet"
        val petAge: Int = param2?.toIntOrNull() ?: -1

        btnSchedulePlaydate.setOnClickListener {
            val ctx = requireContext()
            val intent = Intent(ctx, SchedulePlaydateActivity::class.java).apply {
                putExtra(SchedulePlaydateActivity.EXTRA_PET_ID, petId)
                putExtra(SchedulePlaydateActivity.EXTRA_PET_NAME, petName)
                putExtra(SchedulePlaydateActivity.EXTRA_PET_AGE, petAge)
            }
            startActivity(intent)
        }
    }


}
