package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: BookRepository) : ViewModel() {

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    // Observe LiveData from the repository (Room database)
    private val _posts = MediatorLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    init {
        // Start syncing Firestore data into the local Room database
        repository.startRealtimeSync()

        _posts.addSource(repository.getAllPosts()) { list ->
            _posts.value = list
            // First emission means we can stop showing the initial spinner
            if (_isLoading.value == true) _isLoading.value = false
        }
    }

    fun isLoggedIn(): Boolean = repository.isLoggedIn()

    fun getCurrentUserId(): String? = repository.getCurrentUserId()

    fun logout() {
        repository.logout()
    }

    fun likePost(postId: String, userId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }
}
