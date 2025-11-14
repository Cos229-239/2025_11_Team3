package com.example.pawtytime

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
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

    private var photoUrl: String? = null

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

                // 2) Load FIRST pet for this user
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
                            authorUsername = username.ifBlank { name.ifBlank { user.email ?: "Pawty User" } },
                            authorName = name,
                            authorAvatarUrl = avatarUrl,

                            // NEW: use pet info for the post header
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
            .addOnFailureListener { e ->
                snack("Failed to load profile: ${e.message}")
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
