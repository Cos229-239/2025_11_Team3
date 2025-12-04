package com.example.pawtytime

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pawtytime.HomeScreen.PostUi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [profileView.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileView : Fragment(R.layout.fragment_profile_view) {
   private lateinit var recyclerView: RecyclerView

    private val auth by lazy { FirebaseAuth.getInstance() }

    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var adapter: HomeScreen.FeedAdapter

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var postsList = mutableListOf<HomeScreen.PostUi>()

    private lateinit var postPetSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_view, container,false)


        //define text views
        val usernameText = view.findViewById<TextView>(R.id.profile_view_back_text)
        val nameText = view.findViewById<TextView>(R.id.view_profile_name_text)
        val bioText = view.findViewById<TextView>(R.id.profile_bio_more)
        val profPhoto = view.findViewById<ImageView>(R.id.profile_view_picture)

        // define each button
        val backBtn = view.findViewById<ImageButton>(R.id.profile_view_back_Btn)
        val settingsBtn = view.findViewById<ImageButton>(R.id.profile_settings)
        val followersBtn = view.findViewById<Button>(R.id.profile_Followers_btn)
        val followingBtn = view.findViewById<Button>(R.id.profile_Following_btn)


        // I want these to only show up if its another profile:
        settingsBtn.visibility = View.GONE
        backBtn.visibility = View.GONE


        // define each tab
        val postsTab = view.findViewById<Button>(R.id.view_profile_posts_tab)
        postPetSpinner = view.findViewById<Spinner>(R.id.pet_posts_spinner)
        val taggedTab = view.findViewById<Button>(R.id.view_profile_tagged_tab)

        // --- filling in profile information ---
        if(currentUserId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener{
                        doc ->
                    val first = doc.getString("firstName") ?: ""
                    val last = doc.getString("lastName") ?: ""
                    val username = doc.getString("username") ?: ""
                    val profileUrl = doc.getString("profileUrl")
                    val bio = doc.getString("bio")

                    val fullName = "$first $last".trim()

                    usernameText.text = username
                    nameText.text = fullName
                    bioText.text = bio

                    Glide.with(this)
                        .load(profileUrl)
                        .into(profPhoto)
                }
        }


        // separate method to populate spinner with names
        fun populateSpinner(List: List<String>){
            val listAdapter = ArrayAdapter(requireContext(), R.layout.pets_default_spinner, List)

            listAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            postPetSpinner.adapter = listAdapter
        }


        // populate the pets spinner
        currentUserId?.let{
            uid ->
            db.collection("users")
                .document(uid)
                .collection("pets")
                .get()
                .addOnSuccessListener {
                    querySnapshot ->
                    val petList = mutableListOf<String>()
                    for(document in querySnapshot.documents){
                        document.getString("name")?.let{
                            petName ->
                            petList.add(petName)
                        }
                    }
                    populateSpinner(petList)
                }

        }



        recyclerView = view.findViewById(R.id.postsRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // load data into posts tab

        adapter = HomeScreen.FeedAdapter(postsList)

        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = false

        // loading the users posts into view - should make it default
        //loadProfilePosts(adapter)





        //tab switching
        postsTab.setOnClickListener {
            loadProfilePosts(adapter)
        }
        postPetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {

                val selectedPetName = parent.getItemAtPosition(position) as String
                loadPetPosts(adapter, selectedPetName)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        taggedTab.setOnClickListener {

        }


        // profile buttons click listeners

        settingsBtn.setOnClickListener {


        }
        followersBtn.setOnClickListener {

        }
        followingBtn.setOnClickListener {

        }

        backBtn.setOnClickListener(){

        }


        // Inflate the layout for this fragment
        return view
    }


    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            loadProfilePosts(adapter)
        }
    }

private fun loadProfilePosts(adapter: HomeScreen.FeedAdapter) {
    FirebaseFirestore.getInstance()
        .collection("posts")
        .whereEqualTo("authorUid", currentUserId)
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { snap ->
            postsList.clear()

            snap.documents.forEach { doc ->
                val post = doc.toObject(Post::class.java) ?: return@forEach

                // Prefer the pet name; fall back to user info if missing
                val displayName = when {
                    post.petName.isNotBlank() -> post.petName
                    post.authorUsername.isNotBlank() -> post.authorUsername
                    post.authorName.isNotBlank() -> post.authorName
                    else -> "Pawty Friend"
                }

                // Prefer the pet’s photo; fall back to the user’s profile avatar
                val headerAvatarUrl = post.petPhotoUrl ?: post.authorAvatarUrl

                postsList.add(
                    PostUi(
                        id = post.id,
                        author = displayName,
                        avatarUrl = headerAvatarUrl,
                        avatarRes = R.drawable.ic_avatar_circle,
                        photoUrl = post.photoUrl,
                        photoRes = R.drawable.sample_dog,
                        caption = post.caption,
                        likeCount = post.likeCount,
                        liked = false,
                        following = false,
                        authorUid = post.authorUid
                    )
                )
            }


            adapter.notifyDataSetChanged()

        }
        .addOnFailureListener { e ->
            Toast.makeText(
                requireContext(),
                "Failed to load posts: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
}
    private fun loadPetPosts(adapter: HomeScreen.FeedAdapter, petName: String){
        FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("authorUid", currentUserId)
            .whereEqualTo("petName", petName)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener{
                snap ->
                postsList.clear()
                snap.documents.forEach {
                        doc ->
                    val post = doc.toObject(Post::class.java) ?: return@forEach
                    val displayName = when {
                        post.petName.isNotBlank() -> post.petName
                        post.authorUsername.isNotBlank() -> post.authorUsername
                        post.authorName.isNotBlank() -> post.authorName
                        else -> "Pawty Friend"
                    }
                    val headerAvatarUrl = post.petPhotoUrl ?: post.authorAvatarUrl

                    postsList.add(
                        PostUi(
                            id = post.id,
                            author = displayName,
                            avatarUrl = headerAvatarUrl,
                            avatarRes = R.drawable.ic_avatar_circle,
                            photoUrl = post.photoUrl,
                            photoRes = R.drawable.sample_dog,
                            caption = post.caption,
                            likeCount = post.likeCount,
                            liked = false,
                            following = false,
                            authorUid = post.authorUid
                        )
                    )
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                e ->
                Toast.makeText(requireContext(), "Failed to load posts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }






}