package com.example.pawtytime

data class Post(
    val id: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,

    // Pet-focused fields
    val petName: String = "",
    val petPhotoUrl: String? = null,

    val photoUrl: String? = null,
    val caption: String = "",
    val likeCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

