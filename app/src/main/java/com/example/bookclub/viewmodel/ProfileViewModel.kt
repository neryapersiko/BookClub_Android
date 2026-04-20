package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _updateStatus = MutableLiveData<Result<Unit>?>()
    val updateStatus: LiveData<Result<Unit>?> = _updateStatus

    private var postsListener: ListenerRegistration? = null

    init {
        refreshUserData()
        listenToMyPosts()
    }

    /**
     * Triggers a fresh fetch of user data from Firestore/Room.
     * Called on init and when the fragment resumes to ensure the UI is up-to-date.
     */
    fun refreshUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Fetching from Firestore directly ensures we bypass any local delay, 
                // and the repository will handle syncing Room in the background.
                val document = firestore.collection("users").document(uid).get().await()
                if (document.exists()) {
                    _userName.value = document.getString("name") ?: "No Name"
                    _profileImageUrl.value = document.getString("profileImageUrl")
                } else {
                    _userName.value = "User not found"
                }
            } catch (e: Exception) {
                _userName.value = "Error: ${e.message}"
            }
        }
    }

    fun updateUserName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid).update("name", newName).await()
                _userName.value = newName
                _updateStatus.value = Result.success(Unit)
            } catch (e: Exception) {
                _updateStatus.value = Result.failure(e)
            }
        }
    }

    fun resetUpdateStatus() {
        _updateStatus.value = null
    }

    private fun listenToMyPosts() {
        val uid = auth.currentUser?.uid ?: return
        
        postsListener?.remove()
        
        postsListener = firestore.collection("posts")
            .whereEqualTo("userId", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val postList = snapshot.toObjects(Post::class.java)
                    if (_posts.value == postList) return@addSnapshotListener

                    viewModelScope.launch(Dispatchers.Default) {
                        if (_posts.value == postList) return@launch
                        
                        withContext(Dispatchers.Main) {
                            _posts.value = postList
                        }
                    }
                }
            }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("posts").document(postId).delete().await()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun updatePost(postId: String, newTitle: String, newAuthor: String, newYear: Int?, newContent: String) {
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any?>(
                    "bookTitle" to newTitle,
                    "bookAuthor" to newAuthor,
                    "content" to newContent
                )
                if (newYear != null) {
                    updates["bookPublishYear"] = newYear
                } else {
                    updates["bookPublishYear"] = null
                }
                firestore.collection("posts").document(postId).update(updates).await()
            } catch (e: Exception) {
                // Error handling
            }
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
    }
}
