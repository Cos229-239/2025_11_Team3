package com.example.pawtytime

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val text: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String? = null,
    val createdAt: Timestamp? = null,
    val likeCount: Long = 0L,
    val likedBy: Map<String, Boolean> = emptyMap(),
    val parentId: String? = null  // null = top-level, non-null = reply
)