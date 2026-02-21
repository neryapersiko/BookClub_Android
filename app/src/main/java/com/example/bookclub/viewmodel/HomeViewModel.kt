package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Comment
import com.example.bookclub.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    init {
        observePosts()
    }

    private fun observePosts() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (snapshot != null) {
                    val postList = snapshot.toObjects(Post::class.java)
                    _posts.value = postList
                }
            }
    }

    fun toggleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.id)

        if (post.likes.contains(userId)) {
            postRef.update("likes", FieldValue.arrayRemove(userId))
        } else {
            postRef.update("likes", FieldValue.arrayUnion(userId))
        }
    }

    fun addComment(postId: String, text: String) {
        val user = auth.currentUser ?: return
        val commentRef = firestore.collection("posts").document(postId).collection("comments").document()
        
        val comment = Comment(
            id = commentRef.id,
            postId = postId,
            userId = user.uid,
            userName = user.displayName ?: "User",
            content = text,
            timestamp = System.currentTimeMillis()
        )
        
        commentRef.set(comment)
    }

    fun observeComments(postId: String, onUpdate: (List<Comment>) -> Unit) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.let {
                    onUpdate(it.toObjects(Comment::class.java))
                }
            }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid
}
