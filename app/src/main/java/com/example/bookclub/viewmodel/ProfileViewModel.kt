package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: BookRepository,
    private val userId: String
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    private val _posts = MediatorLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _updateStatus = MutableLiveData<Result<Unit>?>()
    val updateStatus: LiveData<Result<Unit>?> = _updateStatus

    init {
        refreshUserData()
        observeMyPosts()
    }

    fun refreshUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getUserProfile(userId)
                .onSuccess { (name, imageUrl) ->
                    _userName.value = name
                    _profileImageUrl.value = imageUrl.ifEmpty { null }
                }
                .onFailure { e ->
                    _userName.value = "Error: ${e.message}"
                }
            _isLoading.value = false
        }
    }

    private fun observeMyPosts() {
        _posts.addSource(repository.getAllPosts()) { allPosts ->
            _posts.value = allPosts.filter { it.userId == userId }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _updateStatus.value = repository.deletePost(postId)
        }
    }

    fun updatePost(postId: String, newTitle: String, newAuthor: String, newYear: Int?, newContent: String) {
        viewModelScope.launch {
            _updateStatus.value = repository.updatePost(postId, newTitle, newAuthor, newYear, newContent)
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = null
    }
}
