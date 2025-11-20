package com.example.pawtytime

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class CreatePostActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val uploader by lazy { CloudinaryUploader(this) }

    private lateinit var ivPostImagePreview: ImageView
    private lateinit var zonePostImage: MaterialCardView
    private lateinit var etPostCaption: TextInputEditText
    private lateinit var btnPost: MaterialButton

    // NEW: "Posting as" UI
    private lateinit var rowPostingAs: View
    private lateinit var tvPostingAsValue: TextView

    private var photoUrl: String? = null

    // NEW: which pet we're posting as
    private data class PetChoice(
        val name: String,
        val photoUrl: String?
    )

    private var selectedPet: PetChoice? = null

    private val pickPostImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            ivPostImagePreview.setImageURI(uri)
            snack("Uploading photo...")
            uploader.upload(uri) { url ->
                photoUrl = url
                snack(if (url != null) "Photo uploaded ✓" else "Upload failed")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        ivPostImagePreview = findViewById(R.id.ivPostImagePreview)
        zonePostImage = findViewById(R.id.zonePostImage)
        etPostCaption = findViewById(R.id.etPostCaption)
        btnPost = findViewById(R.id.btnPost)

        // NEW: hook up "Posting as" row
        rowPostingAs = findViewById(R.id.rowPostingAs)
        tvPostingAsValue = findViewById(R.id.tvPostingAsValue)

        rowPostingAs.setOnClickListener {
            openPetPicker()
        }
        tvPostingAsValue.setOnClickListener {
            openPetPicker()
        }

        zonePostImage.setOnClickListener {
            pickPostImage.launch("image/*")
        }

        btnPost.setOnClickListener {
            createPost()
        }
    }

    private fun createPost() {
        val user = auth.currentUser
        if (user == null) {
            snack("You must be signed in to post.")
            return
        }

        val caption = etPostCaption.text?.toString()?.trim().orEmpty()
        if (caption.isEmpty()) {
            snack("Please enter a caption.")
            etPostCaption.requestFocus()
            return
        }

        val photo = photoUrl
        if (photo.isNullOrBlank()) {
            snack("Please select a photo for your post.")
            return
        }

        val uid = user.uid

        // 1) Load basic user info
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val username = userDoc.getString("username").orEmpty()
                val name = userDoc.getString("name").orEmpty()
                val avatarUrl = userDoc.getString("profileUrl")

                val authorUsername = username.ifBlank {
                    name.ifBlank { user.email ?: "Pawty User" }
                }

                val chosenPet = selectedPet

                // If user already picked a pet, use that and skip extra query
                if (chosenPet != null) {
                    val postRef = db.collection("posts").document()
                    val post = Post(
                        id = postRef.id,
                        authorUid = uid,
                        authorUsername = authorUsername,
                        authorName = name,
                        authorAvatarUrl = avatarUrl,

                        // use chosen pet
                        petName = chosenPet.name,
                        petPhotoUrl = chosenPet.photoUrl,

                        photoUrl = photo,
                        caption = caption,
                        likeCount = 0,
                        createdAt = System.currentTimeMillis()
                    )

                    postRef.set(post, SetOptions.merge())
                        .addOnSuccessListener {
                            snack("Posted ✓")
                            setResult(RESULT_OK)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            snack("Failed to post: ${e.message}")
                        }
                } else {
                    // Otherwise: fallback to FIRST pet in /users/{uid}/pets like before
                    db.collection("users")
                        .document(uid)
                        .collection("pets")
                        .limit(1)
                        .get()
                        .addOnSuccessListener { petSnap ->
                            val petDoc = petSnap.documents.firstOrNull()
                            val pet = petDoc?.toObject(Pet::class.java)

                            val postRef = db.collection("posts").document()
                            val post = Post(
                                id = postRef.id,
                                authorUid = uid,
                                authorUsername = authorUsername,
                                authorName = name,
                                authorAvatarUrl = avatarUrl,

                                // fallback pet info (may be empty if no pets)
                                petName = pet?.name.orEmpty(),
                                petPhotoUrl = pet?.photoUrl,

                                photoUrl = photo,
                                caption = caption,
                                likeCount = 0,
                                createdAt = System.currentTimeMillis()
                            )

                            postRef.set(post, SetOptions.merge())
                                .addOnSuccessListener {
                                    snack("Posted ✓")
                                    setResult(RESULT_OK)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    snack("Failed to post: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            snack("Failed to load pet: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                snack("Failed to load profile: ${e.message}")
            }
    }

    // NEW: picker to choose pet for the post
    private fun openPetPicker() {
        val user = auth.currentUser
        if (user == null) {
            snack("You must be signed in to choose a pet.")
            return
        }

        db.collection("users")
            .document(user.uid)
            .collection("pets")
            .get()
            .addOnSuccessListener { petsSnap ->
                val pets = petsSnap.documents.map { doc ->
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: "Pawty Pet"
                    val photoUrl = doc.getString("photoUrl")
                    PetChoice(name, photoUrl)
                }

                if (pets.isEmpty()) {
                    snack("No pets found. Add a pet first!")
                    return@addOnSuccessListener
                }

                val names = pets.map { it.name }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Post as which pet?")
                    .setItems(names) { dialog, which ->
                        val pet = pets[which]
                        selectedPet = pet
                        tvPostingAsValue.text = pet.name
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                snack("Couldn't load pets: ${e.message}")
            }
    }

    private fun snack(msg: String) =
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()

    @Suppress("UNUSED")
    private fun getFileName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }
}
