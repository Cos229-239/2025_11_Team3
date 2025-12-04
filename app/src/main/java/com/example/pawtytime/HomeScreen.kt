package com.example.pawtytime

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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
        val myUid = auth.currentUser?.uid

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

                    // figure out if *this* user liked this post
                    val likedBy = post.likedBy
                    val isLiked = myUid != null && likedBy[myUid] == true

                    posts.add(
                        PostUi(
                            id = if (post.id.isNotBlank()) post.id else doc.id,
                            author = displayName,
                            avatarUrl = headerAvatarUrl,
                            avatarRes = R.drawable.ic_avatar_circle,
                            photoUrl = post.photoUrl,
                            photoRes = R.drawable.sample_dog,
                            caption = post.caption,
                            likeCount = post.likeCount,
                            liked = isLiked,
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
        var likeCount: Long = 0L,
        var liked: Boolean = false,
        var following: Boolean = false,
        val authorUid: String
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

            init {
                v.setOnClickListener { onClick(items[bindingAdapterPosition]) }
            }
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

        private val db = FirebaseFirestore.getInstance()
        private val auth = FirebaseAuth.getInstance()

        // For choosing a pet to comment/reply as
        private data class PetChoice(
            val name: String,
            val photoUrl: String?
        )

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
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

            val btnComment: ImageButton? = v.findViewById(R.id.btnComment)
            val tvCommentCount: TextView? = v.findViewById(R.id.tvCommentCount)

            val rowComments: View? = v.findViewById(R.id.rowComments)
            val tvComments: TextView? = v.findViewById(R.id.tvComments)
            val btnCommentsExpand: ImageButton? = v.findViewById(R.id.btnCommentsExpand)
            val rvComments: RecyclerView? = v.findViewById(R.id.rvComments)

            val commentsAdapter = CommentsAdapter(
                currentUid = auth.currentUser?.uid,
                onToggleLike = { comment -> toggleCommentLike(comment) },
                onReply = { comment -> startReplyFlow(v.context, comment) },
                onDelete = { comment -> deleteComment(v.context, comment) } // 👈 NEW
            )

            var commentsListener: ListenerRegistration? = null
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

            // ----- LIKE / SAVE / SHARE -----
            bindLike(h, item)

            h.btnLike?.setOnClickListener {
                togglePostLike(item, h)
            }
            h.btnShare?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Share tapped", Toast.LENGTH_SHORT).show()
            }
            h.btnSave?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Saved", Toast.LENGTH_SHORT).show()
            }

            //  Long-press to delete your own post
            val currentUid = auth.currentUser?.uid
            h.itemView.setOnLongClickListener {
                if (currentUid != null && currentUid == item.authorUid) {
                    AlertDialog.Builder(h.itemView.context)
                        .setTitle("Delete post?")
                        .setMessage("This can't be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            db.collection("posts")
                                .document(item.id)
                                .delete()
                                .addOnSuccessListener {
                                    val index = h.bindingAdapterPosition
                                    if (index != RecyclerView.NO_POSITION) {
                                        items.removeAt(index)
                                        notifyItemRemoved(index)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        h.itemView.context,
                                        "Failed to delete post: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                } else {
                    false
                }
            }

            // ----- COMMENTS: keep collapsed by default -----
            h.rvComments?.apply {
                if (layoutManager == null) {
                    layoutManager = LinearLayoutManager(context)
                }
                if (adapter == null) {
                    adapter = h.commentsAdapter
                }
                visibility = View.GONE           // collapsed by default
            }
            h.btnCommentsExpand?.setImageResource(R.drawable.ic_expand_more)

            // remove any old listener when view is reused
            h.commentsListener?.remove()
            h.commentsListener = null

            // listen to this post's comments
            if (item.id.isNotEmpty()) {
                h.commentsListener = db.collection("posts")
                    .document(item.id)
                    .collection("comments")
                    .orderBy("createdAt", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null || snapshot == null) return@addSnapshotListener

                        // Build a flat list of Comment with id + postId filled
                        val all = mutableListOf<Comment>()
                        for (doc in snapshot.documents) {
                            val c = doc.toObject(Comment::class.java) ?: continue
                            all.add(
                                c.copy(
                                    id = doc.id,
                                    postId = item.id
                                )
                            )
                        }

                        // Separate top-level comments and replies
                        val topLevel = mutableListOf<Comment>()
                        val repliesMap = mutableMapOf<String, MutableList<Comment>>()

                        for (c in all) {
                            val parentId = c.parentId
                            if (parentId == null) {
                                topLevel.add(c)
                            } else {
                                val list = repliesMap.getOrPut(parentId) { mutableListOf() }
                                list.add(c)
                            }
                        }

                        // Sort top-level comments by createdAt
                        topLevel.sortBy { it.createdAt?.toDate()?.time ?: 0L }

                        // Build ordered list: each parent followed by its replies
                        val ordered = mutableListOf<Comment>()
                        for (parent in topLevel) {
                            ordered.add(parent)

                            val replies = repliesMap[parent.id]
                            if (replies != null) {
                                replies.sortBy { it.createdAt?.toDate()?.time ?: 0L }
                                ordered.addAll(replies)
                            }
                        }

                        // Send to adapter
                        h.commentsAdapter.submitList(ordered)

                        // Text summary and count use total number of comments (including replies)
                        val totalCount = all.size
                        h.tvComments?.text = when (totalCount) {
                            0 -> "No comments yet"
                            1 -> "1 comment"
                            else -> "$totalCount comments"
                        }

                        h.tvCommentCount?.text =
                            if (totalCount == 0) "" else totalCount.toString()
                    }
            } else {
                h.tvComments?.text = "Comments"
                h.tvCommentCount?.text = ""
            }

            // toggle expand/collapse (comments stay collapsed until tapped)
            val toggleComments: (View) -> Unit = {
                val showing = h.rvComments?.visibility == View.VISIBLE
                h.rvComments?.visibility = if (showing) View.GONE else View.VISIBLE
                h.btnCommentsExpand?.setImageResource(R.drawable.ic_expand_more)
            }

            h.rowComments?.setOnClickListener(toggleComments)
            h.btnCommentsExpand?.setOnClickListener(toggleComments)

            // start comment flow: choose pet (if more than one), then type comment
            h.btnComment?.setOnClickListener {
                startAddCommentFlow(h, item)
            }

            h.chipFollow?.setOnClickListener {
                item.following = !item.following
                notifyItemChanged(h.bindingAdapterPosition, PAYLOAD_FOLLOW)
            }
        }

        /* ---------- Comment / Reply flows (pet selection) ---------- */

        // Step 1: choose which pet to comment as, then open text dialog
        private fun startAddCommentFlow(h: VH, post: PostUi) {
            val ctx = h.itemView.context
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(ctx, "You must be signed in to comment", Toast.LENGTH_SHORT).show()
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
                        // No pets – fallback straight to generic name
                        showCommentTextDialog(
                            ctx = ctx,
                            h = h,
                            post = post,
                            displayName = user.displayName
                                ?: user.email?.substringBefore("@")
                                ?: "Pawty Friend",
                            avatarUrl = null
                        )
                        return@addOnSuccessListener
                    }

                    // ALWAYS show a picker if we have at least 1 pet
                    val names = pets.map { it.name }.toTypedArray()
                    AlertDialog.Builder(ctx)
                        .setTitle("Comment as which pet?")
                        .setItems(names) { dialog, which ->
                            val pet = pets[which]
                            showCommentTextDialog(
                                ctx = ctx,
                                h = h,
                                post = post,
                                displayName = pet.name,      // pet name on comment
                                avatarUrl = pet.photoUrl
                            )
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                .addOnFailureListener { e ->
                    // If the pets query fails, still let them comment
                    showCommentTextDialog(
                        ctx = ctx,
                        h = h,
                        post = post,
                        displayName = user.displayName
                            ?: user.email?.substringBefore("@")
                            ?: "Pawty Friend",
                        avatarUrl = null
                    )

                    Toast.makeText(
                        ctx,
                        "Couldn't load pets: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // Step 2: text dialog that actually creates the comment
        private fun showCommentTextDialog(
            ctx: Context,
            h: VH,
            post: PostUi,
            displayName: String,
            avatarUrl: String?
        ) {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(ctx, "You must be signed in to comment", Toast.LENGTH_SHORT).show()
                return
            }

            val input = EditText(ctx).apply {
                hint = "Add a comment..."
                maxLines = 4
            }

            AlertDialog.Builder(ctx)
                .setTitle("New Comment")
                .setView(input)
                .setPositiveButton("Post") { dialog, _ ->
                    val text = input.text.toString().trim()
                    if (text.isEmpty()) {
                        Toast.makeText(ctx, "Comment can't be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val commentData = hashMapOf(
                        "text" to text,
                        "authorUid" to user.uid,
                        "authorName" to displayName,           // pet name or fallback
                        "authorAvatarUrl" to (avatarUrl ?: ""),
                        "createdAt" to Timestamp.now(),
                        "likeCount" to 0L,
                        "likedBy" to emptyMap<String, Boolean>(),
                        "parentId" to null                     // top-level comment
                    )

                    db.collection("posts")
                        .document(post.id)
                        .collection("comments")
                        .add(commentData)
                        .addOnSuccessListener {
                            // Optional: auto-expand comments
                            h.rvComments?.visibility = View.VISIBLE
                            h.btnCommentsExpand?.setImageResource(R.drawable.ic_expand_more)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                ctx,
                                "Failed to post comment: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        // Step 1 (reply): choose which pet to reply as
        private fun startReplyFlow(ctx: Context, parent: Comment) {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(ctx, "You must be signed in to reply", Toast.LENGTH_SHORT).show()
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
                        // No pets – fallback to username
                        showReplyTextDialog(
                            ctx = ctx,
                            parent = parent,
                            displayName = user.displayName
                                ?: user.email?.substringBefore("@")
                                ?: "Pawty Friend",
                            avatarUrl = null
                        )
                        return@addOnSuccessListener
                    }

                    // ALWAYS show a picker if we have at least 1 pet
                    val names = pets.map { it.name }.toTypedArray()
                    AlertDialog.Builder(ctx)
                        .setTitle("Reply as which pet?")
                        .setItems(names) { dialog, which ->
                            val pet = pets[which]
                            showReplyTextDialog(
                                ctx = ctx,
                                parent = parent,
                                displayName = pet.name,
                                avatarUrl = pet.photoUrl
                            )
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
                .addOnFailureListener { e ->
                    // Fallback so reply still works
                    showReplyTextDialog(
                        ctx = ctx,
                        parent = parent,
                        displayName = user.displayName
                            ?: user.email?.substringBefore("@")
                            ?: "Pawty Friend",
                        avatarUrl = null
                    )

                    Toast.makeText(
                        ctx,
                        "Couldn't load pets: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        // Step 2 (reply): text dialog that creates reply linked to parent
        private fun showReplyTextDialog(
            ctx: Context,
            parent: Comment,
            displayName: String,
            avatarUrl: String?
        ) {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(ctx, "You must be signed in to reply", Toast.LENGTH_SHORT).show()
                return
            }

            val input = EditText(ctx).apply {
                hint = "Reply to ${parent.authorName}"
                maxLines = 4
            }

            AlertDialog.Builder(ctx)
                .setTitle("Reply to ${parent.authorName}")
                .setView(input)
                .setPositiveButton("Reply") { dialog, _ ->
                    val text = input.text.toString().trim()
                    if (text.isEmpty()) {
                        Toast.makeText(ctx, "Reply can't be empty", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    val replyData = hashMapOf(
                        "text" to text,
                        "authorUid" to user.uid,
                        "authorName" to displayName,          // pet name or fallback
                        "authorAvatarUrl" to (avatarUrl ?: ""),
                        "createdAt" to Timestamp.now(),
                        "likeCount" to 0L,
                        "likedBy" to emptyMap<String, Boolean>(),
                        "parentId" to (parent.parentId ?: parent.id)               // link to parent comment
                    )

                    db.collection("posts")
                        .document(parent.postId)
                        .collection("comments")
                        .add(replyData)
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                ctx,
                                "Failed to post reply: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        /* ---------------------- Likes ---------------------- */

        // Like / unlike a post and persist to Firestore
        private fun togglePostLike(postUi: PostUi, holder: VH) {
            val uid = auth.currentUser?.uid ?: return
            if (postUi.id.isEmpty()) return

            val docRef = db.collection("posts").document(postUi.id)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)

                // Safely rebuild likedBy as a MutableMap<String, Boolean>
                val likedByAny = snap.get("likedBy") as? Map<*, *>
                val likedBy = mutableMapOf<String, Boolean>()
                if (likedByAny != null) {
                    for ((key, value) in likedByAny) {
                        if (key is String && value is Boolean) {
                            likedBy[key] = value
                        }
                    }
                }

                var likeCount = snap.getLong("likeCount") ?: 0L

                val currentlyLiked = likedBy[uid] == true
                if (currentlyLiked) {
                    likedBy.remove(uid)
                    likeCount -= 1L
                } else {
                    likedBy[uid] = true
                    likeCount += 1L
                }

                tx.update(
                    docRef,
                    mapOf(
                        "likedBy" to likedBy as Map<String, Boolean>,
                        "likeCount" to likeCount
                    )
                )
            }.addOnSuccessListener {
                // Update local UI so it feels instant
                postUi.liked = !postUi.liked
                postUi.likeCount += if (postUi.liked) 1L else -1L
                notifyItemChanged(holder.bindingAdapterPosition, PAYLOAD_LIKE)
            }
        }

        // Like / unlike a specific comment in Firestore
        private fun toggleCommentLike(comment: Comment) {
            val uid = auth.currentUser?.uid ?: return
            if (comment.postId.isEmpty() || comment.id.isEmpty()) return

            val docRef = db.collection("posts")
                .document(comment.postId)
                .collection("comments")
                .document(comment.id)

            db.runTransaction { tx ->
                val snap = tx.get(docRef)

                val likedByAny = snap.get("likedBy") as? Map<*, *>
                val likedBy = mutableMapOf<String, Boolean>()
                if (likedByAny != null) {
                    for ((key, value) in likedByAny) {
                        if (key is String && value is Boolean) {
                            likedBy[key] = value
                        }
                    }
                }

                var likeCount = snap.getLong("likeCount") ?: 0L

                val currentlyLiked = likedBy[uid] == true
                if (currentlyLiked) {
                    likedBy.remove(uid)
                    likeCount -= 1L
                } else {
                    likedBy[uid] = true
                    likeCount += 1L
                }

                tx.update(
                    docRef,
                    mapOf(
                        "likedBy" to likedBy as Map<String, Boolean>,
                        "likeCount" to likeCount
                    )
                )
            }
        }

        // 🔥 Delete a single comment (or reply)
        private fun deleteComment(ctx: Context, comment: Comment) {
            val uid = auth.currentUser?.uid ?: return
            if (comment.postId.isEmpty() || comment.id.isEmpty()) return

            if (uid != comment.authorUid) {
                Toast.makeText(ctx, "You can only delete your own comments", Toast.LENGTH_SHORT).show()
                return
            }

            AlertDialog.Builder(ctx)
                .setTitle("Delete comment?")
                .setMessage("This can't be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    db.collection("posts")
                        .document(comment.postId)
                        .collection("comments")
                        .document(comment.id)
                        .delete()
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                ctx,
                                "Failed to delete comment: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        /* ---------------- RecyclerView plumbing ---------------- */

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

        override fun onViewRecycled(holder: VH) {
            super.onViewRecycled(holder)
            holder.commentsListener?.remove()
            holder.commentsListener = null
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
