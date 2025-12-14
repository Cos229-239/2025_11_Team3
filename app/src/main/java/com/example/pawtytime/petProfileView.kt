package com.example.pawtytime

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class petProfileView : Fragment(R.layout.fragment_pet_profile_view) {

    companion object {
        const val ARG_OWNER_UID = "arg_owner_uid"
        const val ARG_PET_ID = "arg_pet_id"
    }

    private val db by lazy { FirebaseFirestore.getInstance() }

    // Loaded pet values (used for Schedule button)
    private var loadedPetId: String = ""
    private var loadedPetName: String = "Your Pet"
    private var loadedPetAge: Int = -1
    private var loadedOwnerUid: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSchedulePlaydate = view.findViewById<MaterialButton>(R.id.btnSchedulePlaydate)

        val ownerUid = arguments?.getString(ARG_OWNER_UID).orEmpty()
        val petId = arguments?.getString(ARG_PET_ID).orEmpty()

        loadedOwnerUid = ownerUid
        loadedPetId = petId

        if (ownerUid.isBlank() || petId.isBlank()) {
            Toast.makeText(requireContext(), "Missing pet info", Toast.LENGTH_SHORT).show()
        } else {
            // Load the pet doc
            db.collection("users").document(ownerUid)
                .collection("pets").document(petId)
                .get()
                .addOnSuccessListener { snap ->
                    if (!snap.exists()) {
                        Toast.makeText(requireContext(), "Pet not found", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    loadedPetName = snap.getString("name")?.takeIf { it.isNotBlank() } ?: "Your Pet"

                    // age might be stored as Long, String, or missing
                    loadedPetAge = when (val ageAny = snap.get("age")) {
                        is Long -> ageAny.toInt()
                        is Int -> ageAny
                        is String -> ageAny.toIntOrNull() ?: -1
                        else -> -1
                    }

                    // TODO: bind UI here (TextViews/ImageViews in fragment_pet_profile_view.xml)
                    // view.findViewById<TextView>(R.id.tvPetName)?.text = loadedPetName
                    // Glide.with(view).load(snap.getString("photoUrl")).into(...)
                    val tvName = view.findViewById<TextView>(R.id.pet_view_pet_name)
                    val tvAge = view.findViewById<TextView>(R.id.pet_view_pet_age)
                    val img = view.findViewById<ImageView>(R.id.pet_view_prof_pic)

                    tvName.text = loadedPetName
                    tvAge.text = if (loadedPetAge > -1) " (${loadedPetAge} Yrs)" else ""

                    val photoUrl = snap.getString("photoUrl")
                    if (!photoUrl.isNullOrBlank()) {
                        Glide.with(this@petProfileView)
                            .load(photoUrl)
                            .into(img)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to load pet: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnSchedulePlaydate.setOnClickListener {
            val ctx = requireContext()
            val intent = Intent(ctx, SchedulePlaydateActivity::class.java).apply {
                putExtra(SchedulePlaydateActivity.EXTRA_PET_ID, loadedPetId)
                putExtra(SchedulePlaydateActivity.EXTRA_PET_NAME, loadedPetName)
                putExtra(SchedulePlaydateActivity.EXTRA_PET_AGE, loadedPetAge)

                // If SchedulePlaydate later needs ownerUid too, add:
                // putExtra("extra_owner_uid", loadedOwnerUid)
            }
            startActivity(intent)
        }
    }
}
