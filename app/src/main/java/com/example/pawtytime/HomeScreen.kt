package com.example.pawtytime

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.location.Geocoder
import android.location.Location
import java.util.Locale

class HomeScreen : Fragment(R.layout.fragment_home_screen) {

    private lateinit var rvPeople: RecyclerView
    private lateinit var rvFeed: RecyclerView

    private val people = mutableListOf<PersonUi>()
    private val posts = mutableListOf<PostUi>()

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private lateinit var peopleAdapter: PeopleAdapter
    private lateinit var feedAdapter: FeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "See more profiles" button
        view.findViewById<ImageButton?>(R.id.btnPeopleMore)?.setOnClickListener {
            val intent = Intent(requireContext(), RecommendedProfilesActivity::class.java)
            startActivity(intent)
        }

        // Floating "Create Post" button
        val btnCreatePost = view.findViewById<View>(R.id.btnCreatePost)
        btnCreatePost?.setOnClickListener {
            val intent = Intent(requireContext(), CreatePostActivity::class.java)
            startActivity(intent)
        }

        // ---------------- Recommended PROFILES (HORIZONTAL) ----------------
        rvPeople = view.findViewById(R.id.rvPeople)
        rvPeople.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        peopleAdapter = PeopleAdapter(people) { p ->
            Toast.makeText(requireContext(), "Open ${p.name}", Toast.LENGTH_SHORT).show()
        }
        rvPeople.adapter = peopleAdapter

        loadRecommendedProfiles()

        // ---------------- FEED (VERTICAL) ----------------
        rvFeed = view.findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(requireContext())
        feedAdapter = FeedAdapter(posts)
        rvFeed.adapter = feedAdapter
        rvFeed.isNestedScrollingEnabled = false

        loadFeedPosts(feedAdapter)
    }

    override fun onResume() {
        super.onResume()
        if (::feedAdapter.isInitialized) {
            loadFeedPosts(feedAdapter)
        }
    }

    /* --------------------- Firestore loaders ---------------------- */

    private fun loadRecommendedProfiles() {
        val myUid = auth.currentUser?.uid

        if (myUid == null) {
            // No signed-in user – just show names with no distance
            db.collectionGroup("pets")
                .limit(20)
                .get()
                .addOnSuccessListener { snap ->
                    people.clear()
                    snap.documents.forEach { doc ->
                        val pet = doc.toObject(Pet::class.java) ?: return@forEach
                        val displayName = pet.name.ifBlank { "Pawty Pup" }

                        people.add(
                            PersonUi(
                                name = displayName,
                                avatarUrl = pet.photoUrl,
                                avatarRes = R.drawable.ic_avatar_circle,
                                miles = ""
                            )
                        )
                    }
                    peopleAdapter.notifyDataSetChanged()
                }
            return
        }

        // 1) Get *your* location from users/{myUid}
        db.collection("users").document(myUid)
            .get()
            .addOnSuccessListener { meSnap ->
                val myLocation = meSnap.getString("location") ?: ""

                db.collectionGroup("pets")
                    .limit(20)
                    .get()
                    .addOnSuccessListener { snap ->
                        val petDocs = snap.documents
                        if (petDocs.isEmpty()) {
                            people.clear()
                            peopleAdapter.notifyDataSetChanged()
                            return@addOnSuccessListener
                        }

                        // temp list that we will sort
                        val temp = mutableListOf<PersonUi>()
                        var remaining = petDocs.size

                        fun maybeFinish() {
                            if (remaining == 0) {
                                // sort by parsed miles: closest first, blanks last
                                val sorted = temp.sortedBy { parseMiles(it.miles) }
                                people.clear()
                                people.addAll(sorted)
                                peopleAdapter.notifyDataSetChanged()
                            }
                        }

                        petDocs.forEach { doc ->
                            val pet = doc.toObject(Pet::class.java) ?: run {
                                remaining--
                                maybeFinish()
                                return@forEach
                            }

                            val ownerUid = doc.getString("ownerUid")
                            // skip my own pets
                            if (ownerUid == null || ownerUid == myUid) {
                                remaining--
                                maybeFinish()
                                return@forEach
                            }

                            val displayName = pet.name.ifBlank { "Pawty Pup" }

                            // 2) For each owner, fetch their location
                            db.collection("users").document(ownerUid)
                                .get()
                                .addOnSuccessListener { ownerSnap ->
                                    val ownerLocation = ownerSnap.getString("location")

                                    val milesText =
                                        if (myLocation.isNotBlank() && !ownerLocation.isNullOrBlank()) {
                                            distanceMilesBetweenLocations(myLocation, ownerLocation)
                                        } else {
                                            ""
                                        }

                                    temp.add(
                                        PersonUi(
                                            name = displayName,
                                            avatarUrl = pet.photoUrl,
                                            avatarRes = R.drawable.ic_avatar_circle,
                                            miles = milesText
                                        )
                                    )

                                    remaining--
                                    maybeFinish()
                                }
                                .addOnFailureListener {
                                    remaining--
                                    maybeFinish()
                                }
                        }
                    }
            }
    }


    private fun loadFeedPosts(adapter: FeedAdapter) {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { snap ->
                posts.clear()

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

                    posts.add(
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
                            following = false
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

    private fun distanceMilesBetweenLocations(
        fromLocation: String,
        toLocation: String
    ): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())

            val fromList = geocoder.getFromLocationName(fromLocation, 1)
            val toList = geocoder.getFromLocationName(toLocation, 1)

            if (fromList.isNullOrEmpty() || toList.isNullOrEmpty()) {
                ""
            } else {
                val from = fromList[0]
                val to = toList[0]

                val result = FloatArray(1)
                Location.distanceBetween(
                    from.latitude, from.longitude,
                    to.latitude, to.longitude,
                    result
                )

                val miles = result[0] / 1609.34f
                String.format(Locale.getDefault(), "%.1f mi away", miles)
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseMiles(milesText: String): Double {
        val numberPart = milesText.substringBefore(" ").trim()
        return numberPart.toDoubleOrNull() ?: Double.MAX_VALUE
    }

    /* --------------------- Models ---------------------- */

    data class PersonUi(
        val name: String,
        val avatarUrl: String?,
        @field:DrawableRes val avatarRes: Int,
        val miles: String
    )

    data class PostUi(
        val id: String,
        val author: String,
        val avatarUrl: String? = null,
        @field:DrawableRes val avatarRes: Int,
        val photoUrl: String? = null,
        @field:DrawableRes val photoRes: Int,
        val caption: String,
        var likeCount: Int = 0,
        var liked: Boolean = false,
        var following: Boolean = false
    )

    /* ----------------- People Adapter ------------------ */

    class PeopleAdapter(
        private val items: List<PersonUi>,
        private val onClick: (PersonUi) -> Unit
    ) : RecyclerView.Adapter<PeopleAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val avatar: ImageView = v.findViewById(R.id.imgAvatar)
            val name: TextView = v.findViewById(R.id.tvName)
            val miles: TextView = v.findViewById(R.id.tvMiles)
            init { v.setOnClickListener { onClick(items[bindingAdapterPosition]) } }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_person, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = items[pos]

            if (!p.avatarUrl.isNullOrBlank()) {
                Glide.with(h.itemView)
                    .load(p.avatarUrl)
                    .placeholder(p.avatarRes)
                    .circleCrop()
                    .into(h.avatar)
            } else {
                h.avatar.setImageResource(p.avatarRes)
            }

            h.name.text = p.name
            h.miles.text = p.miles
        }

        override fun getItemCount() = items.size
    }

    /* ------------------ Feed Adapter ------------------- */

    class FeedAdapter(
        private val items: MutableList<PostUi>
    ) : RecyclerView.Adapter<FeedAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val imgAvatar: ImageView? = v.findViewById(R.id.imgAvatar)
            val tvAuthor: TextView? = v.findViewById(R.id.tvAuthor)
            val chipFollow: com.google.android.material.chip.Chip? =
                v.findViewById(R.id.chipFollow)

            val imgPhoto: ImageView? = v.findViewById(R.id.imgPhoto)
            val tvCaption: TextView? = v.findViewById(R.id.tvCaption)

            val btnLike: ImageButton? = v.findViewById(R.id.btnLike)
            val tvLikeCount: TextView? = v.findViewById(R.id.tvLikeCount)
            val btnShare: ImageButton? = v.findViewById(R.id.btnShare)
            val btnSave: ImageButton? = v.findViewById(R.id.btnSave)

            val rowComments: View? = v.findViewById(R.id.rowComments)
            val btnCommentsExpand: ImageButton? = v.findViewById(R.id.btnCommentsExpand)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_post, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]

            // ----- HEADER -----
            h.tvAuthor?.text = item.author

            h.imgAvatar?.let { avatarView ->
                if (!item.avatarUrl.isNullOrBlank()) {
                    Glide.with(avatarView)
                        .load(item.avatarUrl)
                        .placeholder(item.avatarRes)
                        .circleCrop()
                        .into(avatarView)
                } else {
                    avatarView.setImageResource(item.avatarRes)
                }
            }

            bindFollowChip(h, item)

            // ----- PHOTO + CAPTION -----
            if (!item.photoUrl.isNullOrBlank()) {
                h.imgPhoto?.let { photoView ->
                    Glide.with(photoView)
                        .load(item.photoUrl)
                        .placeholder(item.photoRes)
                        .centerCrop()
                        .into(photoView)
                }
            } else {
                h.imgPhoto?.setImageResource(item.photoRes)
            }

            h.tvCaption?.text = item.caption

            // ----- ACTIONS -----
            bindLike(h, item)

            h.btnLike?.setOnClickListener {
                item.liked = !item.liked
                item.likeCount += if (item.liked) 1 else -1
                notifyItemChanged(h.bindingAdapterPosition, PAYLOAD_LIKE)
            }
            h.btnShare?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Share tapped", Toast.LENGTH_SHORT).show()
            }
            h.btnSave?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Saved", Toast.LENGTH_SHORT).show()
            }

            h.rowComments?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Comments tapped", Toast.LENGTH_SHORT).show()
            }
            h.btnCommentsExpand?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Expand comments", Toast.LENGTH_SHORT).show()
            }

            h.chipFollow?.setOnClickListener {
                item.following = !item.following
                notifyItemChanged(h.bindingAdapterPosition, PAYLOAD_FOLLOW)
            }
        }

        override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
            if (payloads.contains(PAYLOAD_LIKE)) {
                bindLike(holder, items[position])
                return
            }
            if (payloads.contains(PAYLOAD_FOLLOW)) {
                bindFollowChip(holder, items[position])
                return
            }
            super.onBindViewHolder(holder, position, payloads)
        }

        private fun bindLike(h: VH, item: PostUi) {
            h.tvLikeCount?.text = item.likeCount.toString()

            val ctx = h.itemView.context
            val iconRes = if (item.liked) R.drawable.ic_like_filled else R.drawable.ic_like
            h.btnLike?.setImageResource(iconRes)

            val likedColor = Color.parseColor("#0B4365")
            val normalColor = ContextCompat.getColor(ctx, R.color.liked_color)
            h.btnLike?.imageTintList =
                ColorStateList.valueOf(if (item.liked) likedColor else normalColor)

            val cd = if (item.liked) {
                "Unlike. ${item.likeCount} likes"
            } else {
                "Like. ${item.likeCount} likes"
            }
            h.btnLike?.contentDescription = cd
        }

        private fun bindFollowChip(h: VH, item: PostUi) {
            val chip = h.chipFollow ?: return
            if (item.following) {
                chip.text = "Following"
                chip.isChecked = true
            } else {
                chip.text = "Follow"
                chip.isChecked = false
            }
        }

        companion object {
            private const val PAYLOAD_LIKE = "like"
            private const val PAYLOAD_FOLLOW = "follow"
        }
    }
}
