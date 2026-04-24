package com.example.bookclub.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookclub.repository.BookRepository

class CreatePostViewModelFactory(
    private val repository: BookRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreatePostViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

