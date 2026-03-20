package com.example.bookclub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookclub.repository.BookRepository

class CommentsViewModelFactory(
    private val repository: BookRepository,
    private val postId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommentsViewModel(repository, postId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
