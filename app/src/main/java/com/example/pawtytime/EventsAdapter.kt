package com.example.pawtytime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.CheckBox

class EventsAdapter(
    private val items: MutableList<EventUi>
) : RecyclerView.Adapter<EventsAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("EEE MMM d · h:mm a", Locale.getDefault())

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgEvent: ImageView = itemView.findViewById(R.id.imgEvent)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvEventDateTime)
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        val tvSocialMessage: TextView = itemView.findViewById(R.id.tvSocialMessage)

        val ivInterestedIcon: ImageView = itemView.findViewById(R.id.ivInterestedIcon)
        val cbGoing: CheckBox = itemView.findViewById(R.id.cbGoing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        if (!item.imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.imgEvent)
        } else {
            holder.imgEvent.setImageResource(R.drawable.sample_dog)
        }

        holder.tvDateTime.text = dateFormat.format(item.dateTime.toDate())

        holder.tvTitle.text = item.title
        holder.tvLocation.text =
            "${item.addressLine}, ${item.city}, ${item.state} ${item.zip}"

        holder.tvSocialMessage.text = item.description.ifBlank {
            "Comet is interested"
        }

        holder.ivInterestedIcon.isSelected = false
        holder.cbGoing.isChecked = false
    }

    fun replaceAll(newItems: List<EventUi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}