package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: BookRepository) : ViewModel() {

    // Observe LiveData from the repository (Room database)
    val posts: LiveData<List<Post>> = repository.getAllPosts()

    init {
        // Start syncing Firestore data into the local Room database
        repository.startRealtimeSync()
    }

    fun likePost(postId: String, userId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }
}
