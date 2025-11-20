package com.example.pawtytime

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [profileEdit.newInstance] factory method to
 * create an instance of this fragment.
 */
class profileEdit : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var editProfImage: ImageButton
    private val uploader by lazy { CloudinaryUploader(requireContext()) }


    fun snack(msg: String) =
        view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_SHORT) }?.show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private val editImage = registerForActivityResult(ActivityResultContracts.GetContent()){
        uri: Uri? ->
        uri ?: return@registerForActivityResult
        editProfImage.setImageURI(uri)
        snack("Image is uploading...")

        uploader.upload(uri){
            url ->
            if(url != null){
                updateProfImageUrl(url)
            } else {
                snack("Upload failed! Please try again")
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile_edit, container, false)

        // editable fields
        val usernameField = view.findViewById<EditText>(R.id.profile_edit_username)
        val firstNameField = view.findViewById<EditText>(R.id.profile_edit_firstname)
        val lastNameField = view.findViewById<EditText>(R.id.profile_edit_lastname)
        val emailField = view.findViewById<EditText>(R.id.profile_edit_email)
        // val passwordField = view.findViewById<EditText>(R.id.profile_edit_password) (need to do this separately)
        val phoneField = view.findViewById<EditText>(R.id.profile_edit_phone_number)
        val locationField = view.findViewById<EditText>(R.id.profile_edit_location)
        editProfImage = view.findViewById<ImageButton>(R.id.profile_edit_pic_btn)
        val saveChanges = view.findViewById<Button>(R.id.edit_profile_save_changes)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()


        // loading current user (which should be the only one that will be edited)
        db.collection("users").document(userId ?: "")
            .get()
            .addOnSuccessListener{
                document ->
                usernameField.setText(document.getString("username"))
                firstNameField.setText(document.getString("firstName"))
                lastNameField.setText(document.getString("lastName"))
                emailField.setText(document.getString("email"))
                phoneField.setText(document.getString("phone"))
                locationField.setText(document.getString("location"))

            }
        editProfImage.setOnClickListener {
            editImage.launch("image/*")
        }
        //save button
        saveChanges.setOnClickListener {
            val newUsername = usernameField.text.toString()
            val newFirstName = firstNameField.text.toString()
            val newLastName = lastNameField.text.toString()
            val newEmailField = emailField.text.toString()
            val newPhone = phoneField.text.toString()
            val newLocation = locationField.text.toString()

            val profUpdates = mapOf(
                "username" to newUsername,
                "firstName" to newFirstName,
                "lastName" to newLastName,
                "email" to newEmailField,
                "phone" to newPhone,
                "location" to newLocation
            )

            db.collection("users").document(userId ?: "")
                .update(profUpdates)
                .addOnSuccessListener{
                    Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener{
                    Toast.makeText(requireContext(), "Update failed, try again later", Toast.LENGTH_SHORT).show()
                }
        }


        val changePassBtn = view.findViewById<Button>(R.id.profile_edit_password)
        val passFrag = ChangePasswordScreen()

        changePassBtn.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(passFrag)
        }
        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment profileEdit.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            profileEdit().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }


    private fun updateProfImageUrl(url: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .update("photoUrl", url)
            .addOnSuccessListener {
                Glide.with(this)
                    .load(url)
                    .into(editProfImage)
            }
            .addOnFailureListener {
                snack("Failed to update profile image in Firestore")
            }
    }
}