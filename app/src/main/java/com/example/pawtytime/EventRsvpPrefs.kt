package com.example.pawtytime

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object EventRsvpPrefs {

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(
        "event_rsvps_" + (FirebaseAuth.getInstance().currentUser?.uid ?: "guest"),
        Context.MODE_PRIVATE
    )

    fun getStatus(context: Context, eventId: String): String {
        return prefs(context).getString(eventId, "none") ?: "none"
    }

    fun setStatus(context: Context, eventId: String, status: String) {
        prefs(context).edit()
            .putString(eventId, status)
            .apply()
    }
}