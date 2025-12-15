package com.example.pawtytime

data class Post(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,

    val petOwnerUid: String = "",
    val petId: String = "",

    // Pet-focused fields
    val petName: String = "",
    val petPhotoUrl: String? = null,

    val photoUrl: String? = null,
    val caption: String = "",

    // used Long + likedBy so we can track who liked it
    val likeCount: Long = 0L,
    val likedBy: Map<String, Boolean> = emptyMap(),

    val createdAt: Long = System.currentTimeMillis()
)
