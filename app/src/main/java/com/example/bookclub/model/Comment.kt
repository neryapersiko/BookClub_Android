package com.example.bookclub.model

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImageUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L
)
