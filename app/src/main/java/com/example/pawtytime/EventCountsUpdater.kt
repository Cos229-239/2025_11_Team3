package com.example.pawtytime

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object EventCountsUpdater {

    private val db by lazy { FirebaseFirestore.getInstance() }

    fun updateCounts(
        eventId: String,
        oldStatus: String,
        newStatus: String,
        onLocalDelta: ((deltaInterested: Long, deltaGoing: Long) -> Unit)? = null
    ) {
        if (oldStatus == newStatus) return

        var deltaInterested = 0L
        var deltaGoing = 0L

        when (oldStatus to newStatus) {
            "none" to "interested" -> deltaInterested = 1
            "none" to "going"      -> deltaGoing = 1

            "interested" to "none" -> deltaInterested = -1
            "going"      to "none" -> deltaGoing = -1

            "interested" to "going" -> {
                deltaInterested = -1
                deltaGoing = 1
            }
            "going" to "interested" -> {
                deltaInterested = 1
                deltaGoing = -1
            }
        }

        if (deltaInterested == 0L && deltaGoing == 0L) return

        onLocalDelta?.invoke(deltaInterested, deltaGoing)

        val updates = mutableMapOf<String, Any>()

        updates["interestedCount"] = FieldValue.increment(deltaInterested)
        updates["goingCount"] = FieldValue.increment(deltaGoing)

        db.collection("events")
            .document(eventId)
            .update(updates)
            .addOnFailureListener {
            }
    }
}