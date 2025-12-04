package com.example.pawtytime

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CommentsAdapter(
    private val currentUid: String?,
    private val onToggleLike: (Comment) -> Unit,
    private val onReply: (Comment) -> Unit,
    private val onDelete: (Comment) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentVH>() {

    private val items = mutableListOf<Comment>()

    fun submitList(newItems: List<Comment>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentVH(v)
    }

    override fun onBindViewHolder(holder: CommentVH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class CommentVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvAuthor = itemView.findViewById<TextView>(R.id.tvCommentAuthor)
        private val tvText = itemView.findViewById<TextView>(R.id.tvCommentText)
        private val btnLike = itemView.findViewById<ImageButton>(R.id.btnLikeComment)
        private val tvLikeCount = itemView.findViewById<TextView>(R.id.tvLikeCountComment)
        private val btnReply = itemView.findViewById<TextView>(R.id.btnReply)

        fun bind(c: Comment) {
            val isReply = c.parentId != null

            // Show a little arrow for replies so they look nested
            tvAuthor.text = if (isReply) {
                "↳ ${c.authorName}"
            } else {
                c.authorName
            }

            tvText.text = c.text
            tvLikeCount.text = c.likeCount.toString()

            // Indent replies a bit to the right
            val density = itemView.resources.displayMetrics.density
            val leftPadding = if (isReply) (24 * density).toInt() else 0
            itemView.setPadding(
                leftPadding,
                itemView.paddingTop,
                itemView.paddingRight,
                itemView.paddingBottom
            )

            // Is this comment liked by the current user?
            val liked = currentUid != null && c.likedBy[currentUid] == true

            val iconRes = if (liked) R.drawable.ic_like_filled else R.drawable.ic_like
            btnLike.setImageResource(iconRes)

            val ctx = itemView.context
            val likedColor = Color.parseColor("#0B4365")
            val normalColor = ContextCompat.getColor(ctx, R.color.liked_color)
            btnLike.imageTintList =
                ColorStateList.valueOf(if (liked) likedColor else normalColor)

            btnLike.setOnClickListener { onToggleLike(c) }
            btnReply.setOnClickListener { onReply(c) }

            // Long-press to delete *your own* comment/reply
            itemView.setOnLongClickListener {
                if (currentUid != null && currentUid == c.authorUid) {
                    AlertDialog.Builder(itemView.context)
                        .setTitle("Delete comment?")
                        .setMessage("This can't be undone.")
                        .setPositiveButton("Delete") { _, _ ->
                            onDelete(c)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                } else {
                    false
                }
            }
        }
    }
}
