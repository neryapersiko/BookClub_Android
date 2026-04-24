package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Comment
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val repository: BookRepository,
    private val postId: String
) : ViewModel() {
    
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _operationStatus = MutableLiveData<Result<Unit>>()
    val operationStatus: LiveData<Result<Unit>> = _operationStatus

    init {
        repository.getCommentsRealtime(postId) { commentList ->
            _comments.value = commentList
            _isLoading.postValue(false)
        }
    }

    fun sendComment(text: String) {
        viewModelScope.launch {
            _isSending.value = true
            _operationStatus.value = repository.addCommentForCurrentUser(postId, text)
            _isSending.value = false
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _isSending.value = true
            _operationStatus.value = repository.deleteComment(postId, commentId)
            _isSending.value = false
        }
    }

    fun editComment(commentId: String, newContent: String) {
        viewModelScope.launch {
            _isSending.value = true
            _operationStatus.value = repository.updateComment(postId, commentId, newContent)
            _isSending.value = false
        }
    }
    
    fun getCurrentUserId(): String? = repository.getCurrentUserId()
}
