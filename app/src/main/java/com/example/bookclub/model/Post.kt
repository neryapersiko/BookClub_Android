package com.example.bookclub.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val userImageUrl: String = "", // Added for Firestore compatibility
    val bookId: String = "",
    val bookTitle: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val likes: List<String>? = null, // Added for Firestore compatibility
    val likesCount: Int = 0
)
