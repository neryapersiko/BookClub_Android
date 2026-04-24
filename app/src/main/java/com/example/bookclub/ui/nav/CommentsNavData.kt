package com.example.bookclub.ui.nav

import com.example.bookclub.model.Post

data class CommentsNavData(
    val postId: String,
    val userName: String,
    val bookTitle: String,
    val content: String,
    val userImageUrl: String,
    val bookAuthor: String,
    val bookPublishYear: Int,
    val bookImageUrl: String
)

fun Post.toCommentsNavData(): CommentsNavData {
    val imageUrl = profileImageUrl.ifEmpty { userImageUrl }
    return CommentsNavData(
        postId = id,
        userName = userName,
        bookTitle = bookTitle,
        content = content,
        userImageUrl = imageUrl,
        bookAuthor = bookAuthor,
        bookPublishYear = bookPublishYear ?: 0,
        bookImageUrl = bookImageUrl
    )
}

