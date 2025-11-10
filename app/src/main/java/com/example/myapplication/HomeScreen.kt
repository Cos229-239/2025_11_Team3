package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class HomeScreen : Fragment(R.layout.fragment_home_screen) {

    private lateinit var rvFeed: RecyclerView
    private lateinit var rvPeople: RecyclerView
    private val posts = mutableListOf<PostUi>()
    private val people = mutableListOf<PersonUi>()
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var peopleAdapter: PeopleAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Top bar
        val btnProfile = view.findViewById<ImageButton>(R.id.btnProfile)
        val btnNotifications = view.findViewById<ImageButton>(R.id.btnNotifications)
        val btnInbox = view.findViewById<ImageButton>(R.id.btnInbox)
        val btnPeopleMore = view.findViewById<ImageButton>(R.id.btnPeopleMore)

        btnProfile.setOnClickListener { Toast.makeText(requireContext(), "Profile", Toast.LENGTH_SHORT).show() }
        btnNotifications.setOnClickListener { Snackbar.make(view, "Notifications", Snackbar.LENGTH_SHORT).show() }
        btnInbox.setOnClickListener { Snackbar.make(view, "Inbox", Snackbar.LENGTH_SHORT).show() }
        btnPeopleMore.setOnClickListener { Snackbar.make(view, "See more profiles", Snackbar.LENGTH_SHORT).show() }

        // People — horizontal
        rvPeople = view.findViewById(R.id.rvPeople)
        rvPeople.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        rvPeople.setHasFixedSize(true)
        rvPeople.adapter = peopleAdapter

        // Sample people
        people += PersonUi("Rufus", R.drawable.ic_avatar_circle, "0.8 mi")
        people += PersonUi("Ziggy", R.drawable.ic_avatar_circle, "1.3 mi")
        people += PersonUi("Luna", R.drawable.ic_avatar_circle, "2.4 mi")
        people += PersonUi("Milo", R.drawable.ic_avatar_circle, "3.1 mi")
        peopleAdapter.notifyDataSetChanged()

        // Posts — vertical
        rvFeed = view.findViewById(R.id.rvFeed)
        feedAdapter = FeedAdapter(
            items = posts,
            onLike = { p, pos ->
                p.liked = !p.liked
                p.likeCount += if (p.liked) 1 else -1
                feedAdapter.notifyItemChanged(pos, "like")
            },
            onShare = { _, _ -> Snackbar.make(view, "Share tapped", Snackbar.LENGTH_SHORT).show() },
            onFollow = { p, pos ->
                p.following = !p.following
                feedAdapter.notifyItemChanged(pos, "follow")
            },
            onComments = { _, _ -> Snackbar.make(view, "Comments tapped", Snackbar.LENGTH_SHORT).show() },
            onCommentsExpand = { _, _ -> Snackbar.make(view, "Expand comments", Snackbar.LENGTH_SHORT).show() }
        )
        rvFeed.layoutManager = LinearLayoutManager(requireContext())
        rvFeed.adapter = feedAdapter
        rvFeed.isNestedScrollingEnabled = false

        // Sample posts
        posts += PostUi("1","Rufus", R.drawable.ic_avatar_circle, R.drawable.sample_dog, "First fetch of the day!", likeCount = 12)
        posts += PostUi("2","Ziggy", R.drawable.ic_avatar_circle, R.drawable.sample_dog2, "Park meetup at 5?", likeCount = 8)
        feedAdapter.notifyDataSetChanged()
    }
}

/* --- People (recommended) --- */
data class PersonUi(val name: String, @DrawableRes val avatar: Int, val miles: String)

class PeopleAdapter(
    private val items: List<PersonUi>,
    private val onClick: (PersonUi) -> Unit
) : RecyclerView.Adapter<PeopleAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.imgAvatar)
        val name: android.widget.TextView = v.findViewById(R.id.tvName)
        val miles: android.widget.TextView = v.findViewById(R.id.tvMiles)
        init { v.setOnClickListener { onClick(items[bindingAdapterPosition]) } }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
        return VH(v)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.avatar.setImageResource(p.avatar)
        h.name.text = p.name
        h.miles.text = p.miles
    }
}
