package com.example.pawtytime

import android.R.attr.fragment
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
class ProfileView : Fragment() {
    // TODO: Rename and change types of parameters


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile_view, container,false)

        val tabContainer = view.findViewById<FrameLayout>(R.id.profile_tabs_container)

        //define text views
        val usernameText = view.findViewById<TextView>(R.id.profile_view_back_text)
        val nameText = view.findViewById<TextView>(R.id.view_profile_name_text)
        val bioText = view.findViewById<TextView>(R.id.profile_bio_more)

        // define each button
        val backBtn = view.findViewById<ImageButton>(R.id.profile_view_back_Btn)
        val settingsBtn = view.findViewById<ImageButton>(R.id.profile_settings)
        val followersBtn = view.findViewById<Button>(R.id.profile_Followers_btn)
        val followingBtn = view.findViewById<Button>(R.id.profile_Following_btn)

        // define each tab
        val postsTab = view.findViewById<Button>(R.id.view_profile_posts_tab)
        val repostsTab = view.findViewById<Button>(R.id.view_profile_reposts_tab)
        val taggedTab = view.findViewById<Button>(R.id.view_profile_tagged_tab)


        // inflate each tab
        val postsView = layoutInflater.inflate(R.layout.profile_view_posts, tabContainer, false)
        val repostsView = layoutInflater.inflate(R.layout.profile_view_reposts, tabContainer, false)
        val taggedView = layoutInflater.inflate(R.layout.tagged_posts_view, tabContainer, false)

        //default
        tabContainer.removeAllViews()
        tabContainer.addView(postsView)

        val recyclerView = postsView.findViewById<RecyclerView>(R.id.postsRecycler)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        // load data into posts tab
        val postsList = mutableListOf<Post>()
        val adapter = PostAdapter(postsList){
            // put logic for opening up post detail view here
            post ->
            Toast.makeText(requireContext(), "Clicked: ${post.caption}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid


        if(currentUserId != null) {
            FirebaseFirestore.getInstance()
                .collection("posts")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { documents ->
                    for (doc in documents) {
                        val post = doc.toObject(Post::class.java)
                        postsList.add(post)
                    }
                    Toast.makeText(
                        requireContext(),
                        "Loaded ${postsList.size} posts",
                        Toast.LENGTH_SHORT
                    ).show()
                    adapter.notifyDataSetChanged()
                }
        }
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

                }
        }
        //tab switching
        postsTab.setOnClickListener {
            tabContainer.removeAllViews()
            tabContainer.addView(postsView)
        }
        repostsTab.setOnClickListener {
            tabContainer.removeAllViews()
            tabContainer.addView(repostsView)
        }
        taggedTab.setOnClickListener {
            tabContainer.removeAllViews()
            tabContainer.addView(taggedView)
        }


        // profile buttons click listeners

        settingsBtn.setOnClickListener {

        }
        followersBtn.setOnClickListener {

        }
        followingBtn.setOnClickListener {

        }

        // Inflate the layout for this fragment
        return view
    }



}