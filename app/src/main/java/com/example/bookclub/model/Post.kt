package com.example.bookclub.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val likesCount: Int = 0
)
