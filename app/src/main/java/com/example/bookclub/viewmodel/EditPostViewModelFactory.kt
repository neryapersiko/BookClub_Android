package com.example.bookclub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.repository.ImageRepository

class EditPostViewModelFactory(
    private val repository: BookRepository,
    private val imageRepository: ImageRepository,
    private val postId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditPostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditPostViewModel(repository, imageRepository, postId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

