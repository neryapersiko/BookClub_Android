package com.example.bookclub.repository

import androidx.lifecycle.LiveData
import com.example.bookclub.database.PostDao
import com.example.bookclub.model.Comment
import com.example.bookclub.model.Post
import com.example.bookclub.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class BookRepository(
    private val postDao: PostDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Returns LiveData from Room. This is the Single Source of Truth for the UI.
     */
    fun getAllPosts(): LiveData<List<Post>> {
        return postDao.getAllPosts()
    }

    /**
     * Manually updates the local Room database for the current user's profile image.
     * This forces the Room LiveData to emit a new value immediately.
     */
    fun updateLocalUserProfile(newImageUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            postDao.updateProfileImageForUser(uid, newImageUrl)
            
            // To ensure Room triggers a re-emission if the update above isn't enough,
            // we could potentially re-insert or touch the table, but the UPDATE query 
            // on an observed table should be sufficient for Room to notify observers.
        }
    }

    /**
     * Listens to Firestore in realtime and syncs the data into the Room database.
     */
    fun startRealtimeSync() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                CoroutineScope(Dispatchers.IO).launch {
                    snapshot?.let {
                        val posts = it.toObjects(Post::class.java)
                        // Using a transaction-like approach: delete then insert.
                        // Room will notify observers once the operations are complete.
                        postDao.deleteAllPosts()
                        postDao.insertPosts(posts)
                    }
                }
            }
    }

    suspend fun registerUser(user: User, pass: String): Result<User> = withContext(Dispatchers.IO) {
        return@withContext try {
            withTimeoutOrNull(5000) {
                val result = auth.createUserWithEmailAndPassword(user.email, pass).await()
                val uid = result.user?.uid ?: throw Exception("User registration failed")
                val userWithId = user.copy(id = uid)
                firestore.collection("users").document(uid).set(userWithId).await()
                Result.success(userWithId)
            } ?: Result.failure(Exception("Operation timed out. Check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            withTimeoutOrNull(5000) {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: throw Exception("Login failed")
                Result.success(uid)
            } ?: Result.failure(Exception("Login timed out. Check your connection."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPost(post: Post): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val documentRef = firestore.collection("posts").document()
            val postWithId = post.copy(id = documentRef.id)
            documentRef.set(postWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val postRef = firestore.collection("posts").document(postId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val post = snapshot.toObject(Post::class.java) ?: return@runTransaction
                
                val updatedLikedBy = post.likedBy.toMutableList()
                val newLikesCount: Int
                
                if (updatedLikedBy.contains(userId)) {
                    updatedLikedBy.remove(userId)
                    newLikesCount = (post.likesCount - 1).coerceAtLeast(0)
                } else {
                    updatedLikedBy.add(userId)
                    newLikesCount = post.likesCount + 1
                }
                
                transaction.update(postRef, "likedBy", updatedLikedBy)
                transaction.update(postRef, "likesCount", newLikesCount)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Comments Functions ---

    fun getCommentsRealtime(postId: String, onUpdate: (List<Comment>) -> Unit) {
        firestore.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                snapshot?.let {
                    val comments = it.toObjects(Comment::class.java)
                    onUpdate(comments)
                }
            }
    }

    suspend fun addComment(comment: Comment): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val commentRef = firestore.collection("posts")
                .document(comment.postId)
                .collection("comments")
                .document()
            
            val commentWithId = comment.copy(id = commentRef.id)
            commentRef.set(commentWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            val commentRef = firestore.collection("posts").document(postId).collection("comments").document(commentId)
            
            val commentSnapshot = commentRef.get().await()
            val comment = commentSnapshot.toObject(Comment::class.java)
            
            if (comment?.userId == currentUserId) {
                commentRef.delete().await()
                Result.success(Unit)
            } else {
                throw Exception("Not authorized to delete this comment")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateComment(postId: String, commentId: String, newContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            val commentRef = firestore.collection("posts").document(postId).collection("comments").document(commentId)
            
            val commentSnapshot = commentRef.get().await()
            val comment = commentSnapshot.toObject(Comment::class.java)
            
            if (comment?.userId == currentUserId) {
                commentRef.update("content", newContent).await()
                Result.success(Unit)
            } else {
                throw Exception("Not authorized to update this comment")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Profile/Post Management ---

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            val postRef = firestore.collection("posts").document(postId)
            val snapshot = postRef.get().await()
            val post = snapshot.toObject(Post::class.java)
            
            if (post?.userId == currentUserId) {
                postRef.delete().await()
                Result.success(Unit)
            } else {
                throw Exception("Unauthorized")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, newTitle: String, newAuthor: String, newYear: Int?, newContent: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            val postRef = firestore.collection("posts").document(postId)
            val snapshot = postRef.get().await()
            val post = snapshot.toObject(Post::class.java)

            if (post?.userId == currentUserId) {
                val updates = mutableMapOf<String, Any?>(
                    "bookTitle" to newTitle,
                    "bookAuthor" to newAuthor,
                    "bookPublishYear" to newYear,
                    "content" to newContent
                )
                postRef.update(updates).await()
                Result.success(Unit)
            } else {
                throw Exception("Unauthorized")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
