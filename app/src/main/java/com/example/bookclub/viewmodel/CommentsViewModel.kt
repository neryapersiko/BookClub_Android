package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Comment
import com.example.bookclub.repository.BookRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CommentsViewModel(
    private val repository: BookRepository,
    private val postId: String
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _operationStatus = MutableLiveData<Result<Unit>>()
    val operationStatus: LiveData<Result<Unit>> = _operationStatus

    init {
        repository.getCommentsRealtime(postId) { commentList ->
            _comments.value = commentList
        }
    }

    fun sendComment(text: String) {
        val user = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                val userDoc = firestore.collection("users").document(user.uid).get().await()
                
                if (userDoc.exists()) {
                    val finalName = userDoc.getString("name") ?: "Anonymous User"
                    val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                    val comment = Comment(
                        postId = postId,
                        userId = user.uid,
                        userName = finalName,
                        profileImageUrl = profileImageUrl,
                        content = text,
                        timestamp = System.currentTimeMillis()
                    )
                    _operationStatus.value = repository.addComment(comment)
                } else {
                    _operationStatus.value = Result.failure(Exception("User profile not found"))
                }
            } catch (e: Exception) {
                _operationStatus.value = Result.failure(e)
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _operationStatus.value = repository.deleteComment(postId, commentId)
        }
    }

    fun editComment(commentId: String, newContent: String) {
        viewModelScope.launch {
            _operationStatus.value = repository.updateComment(postId, commentId, newContent)
        }
    }
    
    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
