package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: BookRepository) : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    init {
        repository.getPostsRealtime { postList ->
            _posts.value = postList
        }
    }

    fun likePost(postId: String, userId: String) {
        viewModelScope.launch {
            repository.toggleLike(postId, userId)
        }
    }
}
