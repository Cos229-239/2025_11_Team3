package com.example.pawtytime

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale
import android.widget.CheckBox
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth

class EventsAdapter(
    private val items: MutableList<EventUi>,
    private val onEventClicked: (EventUi) -> Unit
) : RecyclerView.Adapter<EventsAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("MM/dd/yyyy h:mm a", Locale.getDefault())
    private val auth = FirebaseAuth.getInstance()

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardEvent)
        val imgEvent: ImageView = itemView.findViewById(R.id.imgEvent)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvEventDateTime)
        val tvTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        val tvLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        val tvSocialMessage: TextView = itemView.findViewById(R.id.tvSocialMessage)
        val ivInterestedIcon: ImageView = itemView.findViewById(R.id.ivInterestedIcon)
        val cbGoing: CheckBox = itemView.findViewById(R.id.cbGoing)
        val btnEditEvent: TextView? = itemView.findViewById(R.id.btnEditEvent)
    }

    private fun bindStarIcon(view: ImageView, isInterested: Boolean) {
        val resId = if (isInterested) {
            R.drawable.ic_star_filled
        } else {
            R.drawable.ic_star
        }
        view.setImageResource(resId)
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

        holder.tvSocialMessage.text = "Comet is interested"

        val currentUid = auth.currentUser?.uid
        val isHost = currentUid != null && currentUid == item.createdByUid

        holder.btnEditEvent?.apply {
            if (isHost) {
                visibility = View.VISIBLE
                setOnClickListener {
                    val ctx = holder.itemView.context
                    val intent = Intent(ctx, CreateEventActivity::class.java).apply {
                        putExtra("eventId", item.id)
                    }
                    ctx.startActivity(intent)
                }
            } else {
                visibility = View.GONE
                setOnClickListener(null)
            }
        }

        EventRsvpState.loadForEvent(holder.itemView.context, item.id)

        val isInterested = EventRsvpState.interestedIds.contains(item.id)
        val isGoing = EventRsvpState.goingIds.contains(item.id)

        bindStarIcon(holder.ivInterestedIcon, isInterested)

        holder.cbGoing.setOnCheckedChangeListener(null)
        holder.cbGoing.isChecked = isGoing

        holder.ivInterestedIcon.setOnClickListener {
            val currentlyInterested = EventRsvpState.interestedIds.contains(item.id)
            val newInterested = !currentlyInterested

            if (newInterested) {
                EventRsvpState.interestedIds.add(item.id)
                EventRsvpState.goingIds.remove(item.id)
                holder.cbGoing.isChecked = false
            } else {
                EventRsvpState.interestedIds.remove(item.id)
            }

            bindStarIcon(holder.ivInterestedIcon, newInterested)

            EventRsvpState.saveForEvent(holder.itemView.context, item.id)
        }

        holder.cbGoing.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                EventRsvpState.goingIds.add(item.id)
                EventRsvpState.interestedIds.remove(item.id)
                bindStarIcon(holder.ivInterestedIcon, false)
            } else {
                EventRsvpState.goingIds.remove(item.id)
            }

            EventRsvpState.saveForEvent(holder.itemView.context, item.id)
        }

        holder.itemView.setOnClickListener {
            onEventClicked(item)
        }
    }
}
