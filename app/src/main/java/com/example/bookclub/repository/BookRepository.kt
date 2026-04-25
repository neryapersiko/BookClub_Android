package com.example.bookclub.repository

import androidx.lifecycle.LiveData
import com.example.bookclub.database.PostDao
import com.example.bookclub.model.Comment
import com.example.bookclub.model.BookDetails
import com.example.bookclub.model.Post
import com.example.bookclub.model.User
import com.example.bookclub.network.BookSearchResult
import com.example.bookclub.network.GoogleBooksService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class BookRepository(
    private val postDao: PostDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    // Repository-owned scope for long-running listeners (Firestore snapshot listeners).
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun logout() {
        auth.signOut()
    }

    /**
     * Returns LiveData from Room. This is the Single Source of Truth for the UI.
     */
    fun getAllPosts(): LiveData<List<Post>> {
        return postDao.getAllPosts()
    }

    fun getPostById(postId: String): LiveData<Post?> {
        return postDao.getPostById(postId)
    }

    /**
     * Manually updates the local Room database for the current user's profile image.
     * This forces the Room LiveData to emit a new value immediately.
     */
    suspend fun updateLocalUserProfile(newImageUrl: String) = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext
        postDao.updateProfileImageForUser(uid, newImageUrl)
    }

    /**
     * Listens to Firestore in realtime and syncs the data into the Room database.
     */
    fun startRealtimeSync() {
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                repoScope.launch {
                    snapshot?.let {
                        val posts = it.toObjects(Post::class.java)
                        // Atomic upsert + delete missing IDs (prevents transient empty emissions)
                        postDao.replaceWithUpsert(posts)
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

    suspend fun registerUserWithProfileImage(
        name: String,
        email: String,
        pass: String,
        localProfileImageUri: android.net.Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("User creation failed")

            val finalImageUrl = if (localProfileImageUri != null) {
                val ref = storage.reference.child("profile_images/${uid}_${System.currentTimeMillis()}.jpg")
                ref.putFile(localProfileImageUri).await()
                ref.downloadUrl.await().toString()
            } else {
                ""
            }

            val userMap = mapOf(
                "id" to uid,
                "name" to name,
                "email" to email,
                "profileImageUrl" to finalImageUrl
            )
            firestore.collection("users").document(uid).set(userMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                Result.failure(Exception("User not found"))
            } else {
                val name = doc.getString("name") ?: "No Name"
                val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                Result.success(name to profileImageUrl)
            }
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

    suspend fun addCommentForCurrentUser(postId: String, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val user = auth.currentUser ?: throw Exception("Not authenticated")
            val (name, profileImageUrl) = getUserProfile(user.uid).getOrElse { throw it }
            val comment = Comment(
                postId = postId,
                userId = user.uid,
                userName = name,
                profileImageUrl = profileImageUrl,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            addComment(comment)
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

    suspend fun updatePostWithOptionalImage(
        postId: String,
        newTitle: String,
        newAuthor: String,
        newYear: Int?,
        newContent: String,
        newLocalImageUri: android.net.Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentUserId = auth.currentUser?.uid ?: throw Exception("Not authenticated")
            val postRef = firestore.collection("posts").document(postId)
            val snapshot = postRef.get().await()
            val post = snapshot.toObject(Post::class.java)

            if (post?.userId != currentUserId) throw Exception("Unauthorized")

            val updates = mutableMapOf<String, Any?>(
                "bookTitle" to newTitle,
                "bookAuthor" to newAuthor,
                "bookPublishYear" to newYear,
                "content" to newContent
            )

            if (newLocalImageUri != null) {
                val ref = storage.reference.child("book_images/${postId}_${System.currentTimeMillis()}.jpg")
                ref.putFile(newLocalImageUri).await()
                updates["bookImageUrl"] = ref.downloadUrl.await().toString()
            }

            postRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPostForCurrentUser(
        bookTitle: String,
        bookAuthor: String,
        bookPublishYear: Int?,
        content: String,
        bookImageUri: android.net.Uri?,
        autoFilledImageUrl: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val (name, profileImageUrl) = getUserProfile(userId).getOrElse { throw it }

            val postRef = firestore.collection("posts").document()
            val bookImageUrl = when {
                bookImageUri != null -> {
                    val ref = storage.reference.child("book_images/${postRef.id}_${System.currentTimeMillis()}.jpg")
                    ref.putFile(bookImageUri).await()
                    ref.downloadUrl.await().toString()
                }
                !autoFilledImageUrl.isNullOrEmpty() -> autoFilledImageUrl
                else -> ""
            }

            val post = Post(
                id = postRef.id,
                userId = userId,
                userName = name,
                profileImageUrl = profileImageUrl,
                bookTitle = bookTitle,
                bookAuthor = bookAuthor,
                bookPublishYear = bookPublishYear,
                bookImageUrl = bookImageUrl,
                content = content,
                timestamp = System.currentTimeMillis(),
                likedBy = emptyList(),
                likesCount = 0
            )

            postRef.set(post).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBookDetails(title: String): BookSearchResult = withContext(Dispatchers.IO) {
        GoogleBooksService.fetchBookDetails(title)
    }
}
