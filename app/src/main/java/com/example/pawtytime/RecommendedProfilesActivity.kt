package com.example.pawtytime

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecommendedProfilesActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val people = mutableListOf<PersonProfile>()
    private lateinit var adapter: RecommendedProfilesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommended_profiles)

        val rv = findViewById<RecyclerView>(R.id.recommendedProfilesRecycler)
        adapter = RecommendedProfilesAdapter(people) { person ->
            // TODO: open profileView() / profile details using this PersonProfile
        }
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        loadRecommendedProfiles()
    }

    private fun loadRecommendedProfiles() {
        val myEmail = auth.currentUser?.email

        db.collection("profiles")
            .apply {
                if (myEmail != null) whereNotEqualTo("email", myEmail)
            }
            .limit(20)
            .get()
            .addOnSuccessListener { snap ->
                people.clear()
                people.addAll(snap.toObjects(PersonProfile::class.java))
                adapter.notifyDataSetChanged()
            }
    }
}
