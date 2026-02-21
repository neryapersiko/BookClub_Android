package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreatePostViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _postSaved = MutableLiveData<Boolean>()
    val postSaved: LiveData<Boolean> = _postSaved

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun savePost(bookTitle: String, content: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val postRef = firestore.collection("posts").document()
                val post = Post(
                    id = postRef.id,
                    userId = userId,
                    bookTitle = bookTitle,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    likes = emptyList()
                )
                postRef.set(post).await()
                _postSaved.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _postSaved.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}
