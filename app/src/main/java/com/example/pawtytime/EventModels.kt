package com.example.pawtytime

import com.google.firebase.Timestamp

data class EventDto(
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val imageUrl: String? = null,
    val createdByUid: String? = null,
    val goingCount: Long = 0,
    val interestedCount: Long = 0
) {
    fun toUi(id: String) = EventUi(
        id = id,
        title = title,
        description = description,
        dateTime = dateTime ?: Timestamp.now(),
        addressLine = addressLine,
        city = city,
        state = state,
        zip = zip,
        lat = lat ?: 0.0,
        lng = lng ?: 0.0,
        imageUrl = imageUrl,
        goingCount = goingCount,
        interestedCount = interestedCount
    )
}

data class EventUi(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp = Timestamp.now(),
    val addressLine: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val imageUrl: String? = null,
    val goingCount: Long = 0L,
    val interestedCount: Long = 0L
)