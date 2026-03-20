package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(private val repository: BookRepository) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    init {
        repository.getPostsRealtime { postList ->
            // Prevent redundant updates that block the Main Thread and overload Picasso
            if (_posts.value == postList) return@getPostsRealtime

            viewModelScope.launch(Dispatchers.Default) {
                // Background processing: double-check equality after thread switch
                if (_posts.value == postList) return@launch
                
                withContext(Dispatchers.Main) {
                    _posts.value = postList
                }
            }
        }
    }

    fun likePost(postId: String, userId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }
}
