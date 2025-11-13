package com.example.pawtytime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecommendedProfilesAdapter(
    private val items: MutableList<PersonProfile>,
    private val onProfileClicked: (PersonProfile) -> Unit = {}
) : RecyclerView.Adapter<RecommendedProfilesAdapter.PersonVH>() {

    class PersonVH(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgAvatar)
        val tvName: TextView     = view.findViewById(R.id.tvName)
        val tvMiles: TextView    = view.findViewById(R.id.tvMiles)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_person, parent, false)
        return PersonVH(v)
    }

    override fun onBindViewHolder(holder: PersonVH, position: Int) {
        val profile = items[position]

        // Name pill → username or First Last
        holder.tvName.text = profile.username.ifBlank {
            listOf(profile.firstName, profile.lastName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
        }

        holder.tvMiles.text = profile.location ?: ""

        // NEW: load avatar from profileUrl if present
        val url = profile.profileUrl
        if (!url.isNullOrBlank()) {
            Glide.with(holder.itemView)
                .load(url)
                .placeholder(R.drawable.ic_avatar_circle)
                .circleCrop()
                .into(holder.imgAvatar)
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_avatar_circle)
        }

        holder.itemView.setOnClickListener {
            onProfileClicked(profile)
        }
    }

    override fun getItemCount(): Int = items.size
}

