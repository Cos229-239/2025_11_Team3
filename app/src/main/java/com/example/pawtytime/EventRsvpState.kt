package com.example.pawtytime

import android.content.Context

object EventRsvpState {
    val interestedIds = mutableSetOf<String>()
    val goingIds = mutableSetOf<String>()

    fun loadForEvent(context: Context, eventId: String) {
        val status = EventRsvpPrefs.getStatus(context, eventId)

        interestedIds.remove(eventId)
        goingIds.remove(eventId)

        when (status) {
            "interested" -> interestedIds.add(eventId)
            "going" -> goingIds.add(eventId)
        }
    }

    fun saveForEvent(context: Context, eventId: String) {
        val status = when {
            goingIds.contains(eventId) -> "going"
            interestedIds.contains(eventId) -> "interested"
            else -> "none"
        }
        EventRsvpPrefs.setStatus(context, eventId, status)
    }
}