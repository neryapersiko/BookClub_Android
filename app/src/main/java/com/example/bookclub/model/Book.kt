package com.example.bookclub.model

data class Book(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val pageCount: Int = 0
)
