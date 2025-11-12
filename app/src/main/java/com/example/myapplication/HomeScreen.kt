package com.example.myapplication

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

class HomeScreen : Fragment(R.layout.fragment_home_screen) {

    private lateinit var rvPeople: RecyclerView
    private lateinit var rvFeed: RecyclerView

    private val people = mutableListOf<PersonUi>()
    private val posts = mutableListOf<PostUi>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Top actions
        view.findViewById<ImageButton?>(R.id.btnPeopleMore)?.setOnClickListener {
            Toast.makeText(requireContext(), "See more profiles", Toast.LENGTH_SHORT).show()
        }

        // Recommended (HORIZONTAL)
        rvPeople = view.findViewById(R.id.rvPeople)
        rvPeople.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val peopleAdapter = PeopleAdapter(people) { p ->
            Toast.makeText(requireContext(), "Open ${p.name}", Toast.LENGTH_SHORT).show()
        }
        rvPeople.adapter = peopleAdapter

        val peopleStart = people.size
        people += listOf(
            PersonUi("Rufus", R.drawable.ic_avatar_circle, "0.8 mi"),
            PersonUi("Ziggy", R.drawable.ic_avatar_circle, "1.3 mi"),
            PersonUi("Luna",  R.drawable.ic_avatar_circle, "2.4 mi")
        )
        peopleAdapter.notifyItemRangeInserted(peopleStart, 3)

        // Feed (VERTICAL)
        rvFeed = view.findViewById(R.id.rvFeed)
        rvFeed.layoutManager = LinearLayoutManager(requireContext())
        val feedAdapter = FeedAdapter(posts)
        rvFeed.adapter = feedAdapter
        rvFeed.isNestedScrollingEnabled = false

        val postStart = posts.size
        posts += listOf(
            PostUi(
                id = "1",
                author = "Rufus",
                avatarRes = R.drawable.ic_avatar_circle,
                photoRes = R.drawable.sample_dog,
                caption = "First fetch of the day!",
                likeCount = 12,
                liked = false,
                following = false
            ),
            PostUi(
                id = "2",
                author = "Ziggy",
                avatarRes = R.drawable.ic_avatar_circle,
                photoRes = R.drawable.sample_dog2,
                caption = "Park meetup at 5?",
                likeCount = 8,
                liked = true,
                following = true
            )
        )
        feedAdapter.notifyItemRangeInserted(postStart, 2)
    }

    /* --------------------- Models ---------------------- */

    data class PersonUi(
        val name: String,
        @field:DrawableRes val avatarRes: Int,
        val miles: String
    )

    data class PostUi(
        val id: String,
        val author: String,
        @field:DrawableRes val avatarRes: Int,
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
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_person, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = items[pos]
            h.avatar.setImageResource(p.avatarRes)
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
            // Header
            val imgAvatar: ImageView? = v.findViewById(R.id.imgAvatar)
            val tvAuthor: TextView? = v.findViewById(R.id.tvAuthor)
            val chipFollow: com.google.android.material.chip.Chip? = v.findViewById(R.id.chipFollow)

            // Main
            val imgPhoto: ImageView? = v.findViewById(R.id.imgPhoto)
            val tvCaption: TextView? = v.findViewById(R.id.tvCaption)

            // Actions
            val btnLike: ImageButton? = v.findViewById(R.id.btnLike)
            val tvLikeCount: TextView? = v.findViewById(R.id.tvLikeCount) // may not exist; null-safe
            val btnShare: ImageButton? = v.findViewById(R.id.btnShare)
            val btnSave: ImageButton? = v.findViewById(R.id.btnSave)

            // Comments row
            val rowComments: View? = v.findViewById(R.id.rowComments)
            val btnCommentsExpand: ImageButton? = v.findViewById(R.id.btnCommentsExpand)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = items[pos]

            // header
            h.imgAvatar?.setImageResource(item.avatarRes)
            h.tvAuthor?.text = item.author
            bindFollowChip(h, item)

            // photo & caption
            h.imgPhoto?.setImageResource(item.photoRes)
            h.tvCaption?.text = item.caption

            // like/share/save
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

            // comments
            h.rowComments?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Comments tapped", Toast.LENGTH_SHORT).show()
            }
            h.btnCommentsExpand?.setOnClickListener {
                Toast.makeText(h.itemView.context, "Expand comments", Toast.LENGTH_SHORT).show()
            }

            // follow toggle
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
            // count (only if TextView exists)
            h.tvLikeCount?.text = item.likeCount.toString()

            // icon swap
            val ctx = h.itemView.context
            val iconRes = if (item.liked) R.drawable.ic_like_filled else R.drawable.ic_like
            h.btnLike?.setImageResource(iconRes)

            // optional tint for liked
            val likedColor = Color.parseColor("#0B4365")
            val normalColor = ContextCompat.getColor(ctx, R.color.liked_color)
            h.btnLike?.imageTintList = ColorStateList.valueOf(if (item.liked) likedColor else normalColor)

            // a11y content description always available
            val cd = if (item.liked) "Unlike. ${item.likeCount} likes" else "Like. ${item.likeCount} likes"
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
