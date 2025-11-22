package com.example.pawtytime

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pawtytime.PostAdapter.PostViewHolder

 class PostAdapter (private val posts: List<Post>,
                            private val click: (Post) -> Unit) :
RecyclerView.Adapter<PostViewHolder>(){

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val imageButton: ImageButton = view.findViewById(R.id.recyclerImageButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.each_post_grid, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        Glide.with(holder.itemView.context)
            .load(post.photoUrl).into(holder.imageButton)

        holder.imageButton.setOnClickListener { click(post) }
        Log.d("PostAdapter", "Binding post: ${post.caption}")
    }



     override fun getItemCount(): Int = posts.size
}