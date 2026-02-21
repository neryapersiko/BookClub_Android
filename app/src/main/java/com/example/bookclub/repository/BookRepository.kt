package com.example.bookclub.repository

import com.example.bookclub.model.Comment
import com.example.bookclub.model.Post
import com.example.bookclub.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BookRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun registerUser(user: User, pass: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(user.email, pass).await()
            val uid = result.user?.uid ?: throw Exception("User registration failed")
            val userWithId = user.copy(id = uid)
            firestore.collection("users").document(uid).set(userWithId).await()
            Result.success(userWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPost(post: Post): Result<Unit> {
        return try {
            val documentRef = firestore.collection("posts").document()
            val postWithId = post.copy(id = documentRef.id)
            documentRef.set(postWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPosts(): Result<List<Post>> {
        return try {
            val snapshot = firestore.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val posts = snapshot.toObjects(Post::class.java)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(comment: Comment): Result<Unit> {
        return try {
            val postRef = firestore.collection("posts").document(comment.postId)
            val commentRef = postRef.collection("comments").document()
            val commentWithId = comment.copy(id = commentRef.id)
            commentRef.set(commentWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
