package com.example.pawtytime

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileEdit : Fragment() {

    private lateinit var editProfImage: ImageButton

    private lateinit var cbGoer: CheckBox
    private lateinit var cbHost: CheckBox
    private lateinit var cbService: CheckBox
    private lateinit var cbShop: CheckBox

    private lateinit var ivIdFrontPreview: ImageButton
    private lateinit var ivIdBackPreview: ImageButton

    private var idFrontUrl: String? = null
    private var idBackUrl: String? = null

    private val uploader by lazy { CloudinaryUploader(requireContext()) }

    private var photoUrl: String? = null
    fun snack(msg: String) =
        view?.let { Snackbar.make(it, msg, Snackbar.LENGTH_SHORT) }?.show()


    private val editImage = registerForActivityResult(ActivityResultContracts.GetContent()){
        uri: Uri? ->
        uri ?: return@registerForActivityResult
        editProfImage.setImageURI(uri)
        snack("Image is uploading...")

        uploader.upload(uri){
            url ->
            photoUrl = url
            snack(if (url != null) "Photo uploaded!" else "Upload has failed")
        }
    }

    private val pickIdFront = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        ivIdFrontPreview.setImageURI(uri)
        snack("Uploading front ID...")
        uploader.upload(uri) { url ->
            idFrontUrl = url
            snack(if (url != null) "Front ID uploaded" else "Front ID upload failed")
        }
    }

    private val pickIdBack = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult
        ivIdBackPreview.setImageURI(uri)
        snack("Uploading back ID...")
        uploader.upload(uri) { url ->
            idBackUrl = url
            snack(if (url != null) "Back ID uploaded" else "Back ID upload failed")
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
        val bioField = view.findViewById<EditText>(R.id.profile_edit_bio)
        editProfImage = view.findViewById(R.id.profile_edit_pic_btn)
        val btnBack = view.findViewById<Button>(R.id.btn_edit_profile_back)
        val saveChanges = view.findViewById<Button>(R.id.edit_profile_save_changes)

        val petEditBtn = view.findViewById<Button>(R.id.go_to_pet_edit_from_profile)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        cbGoer = view.findViewById(R.id.cbEditGoer)
        cbHost = view.findViewById(R.id.cbEditHost)
        cbService = view.findViewById(R.id.cbEditService)
        cbShop = view.findViewById(R.id.cbEditShop)

        ivIdFrontPreview = view.findViewById(R.id.btnEditIdFront)
        ivIdBackPreview = view.findViewById(R.id.btnEditIdBack)

        ivIdFrontPreview.setOnClickListener {
            pickIdFront.launch("image/*")
        }

        ivIdBackPreview.setOnClickListener {
            pickIdBack.launch("image/*")
        }


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
                bioField.setText(document.getString("bio"))


                val photoUrlPic = document.getString("profileUrl")

                if(!photoUrlPic.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(photoUrlPic)
                        .into(editProfImage)
                }

                val rawTypes = document.get("profileTypes") as? List<*>
                val types = rawTypes?.filterIsInstance<String>() ?: emptyList()

                cbGoer.isChecked    = "Pawty Goer" in types
                cbHost.isChecked    = "Pawty Host" in types
                cbService.isChecked = "Pawty Service Provider" in types
                cbShop.isChecked    = "Pawty Shop Owner" in types


                idFrontUrl = document.getString("idFrontUrl")
                idBackUrl  = document.getString("idBackUrl")

                if (!idFrontUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(idFrontUrl)
                        .into(ivIdFrontPreview)
                }

                if (!idBackUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(idBackUrl)
                        .into(ivIdBackPreview)
                }
            }

        editProfImage.setOnClickListener {
            editImage.launch("image/*")
        }

        //back button
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(HomeScreen())
        }
        //save button
        saveChanges.setOnClickListener {
            val newUsername = usernameField.text.toString()
            val newFirstName = firstNameField.text.toString()
            val newLastName = lastNameField.text.toString()
            val newEmailField = emailField.text.toString()
            val newPhone = phoneField.text.toString()
            val newLocation = locationField.text.toString()
            val newBio = bioField.text.toString()

            val profileTypes = mutableListOf<String>()
            if (cbGoer.isChecked)    profileTypes.add("Pawty Goer")
            if (cbHost.isChecked)    profileTypes.add("Pawty Host")
            if (cbService.isChecked) profileTypes.add("Pawty Service Provider")
            if (cbShop.isChecked)    profileTypes.add("Pawty Shop Owner")

            val requiresVerification =
                cbHost.isChecked || cbService.isChecked || cbShop.isChecked

            if (requiresVerification) {
                if (idFrontUrl.isNullOrEmpty() || idBackUrl.isNullOrEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Verification ID (front and back) is required for Hosts, Service Providers, or Shop Owners.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }

            val profUpdates = mutableMapOf<String, Any>(
                "username"  to newUsername,
                "firstName" to newFirstName,
                "lastName"  to newLastName,
                "email"     to newEmailField,
                "phone"     to newPhone,
                "location"  to newLocation,
                "bio"       to newBio,
            )

            photoUrl?.let { url ->
                profUpdates["profileUrl"] = url
            }

            profUpdates["profileTypes"] = profileTypes
            idFrontUrl?.let { profUpdates["idFrontUrl"] = it }
            idBackUrl?.let { profUpdates["idBackUrl"] = it }

            db.collection("users").document(userId ?: "")
                .update(profUpdates)
                .addOnSuccessListener{
                    Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                    (activity as? MainActivity)?.loadFragment(HomeScreen())
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

        petEditBtn.setOnClickListener{
            (activity as? MainActivity)?.loadFragment(petProfileEdit())
        }
        return view
    }
}