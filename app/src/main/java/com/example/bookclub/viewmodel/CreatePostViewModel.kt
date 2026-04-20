package com.example.bookclub.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.BookDetails
import com.example.bookclub.model.Post
import com.example.bookclub.network.BookSearchResult
import com.example.bookclub.network.GoogleBooksService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CreatePostViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    private val _postSaved = MutableLiveData<Boolean>()
    val postSaved: LiveData<Boolean> = _postSaved

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _bookDetails = MutableLiveData<BookDetails?>()
    val bookDetails: LiveData<BookDetails?> = _bookDetails

    private val _isAutoFillLoading = MutableLiveData<Boolean>()
    val isAutoFillLoading: LiveData<Boolean> = _isAutoFillLoading

    private val _autoFillError = MutableLiveData<String?>()
    val autoFillError: LiveData<String?> = _autoFillError

    var autoFilledImageUrl: String? = null
        private set

    fun fetchBookDetails(title: String) {
        viewModelScope.launch {
            _isAutoFillLoading.value = true
            _autoFillError.value = null
            when (val result = GoogleBooksService.fetchBookDetails(title)) {
                is BookSearchResult.Success -> {
                    _bookDetails.value = result.details
                    autoFilledImageUrl = result.details.imageUrl.ifEmpty { null }
                }
                is BookSearchResult.NotFound -> {
                    _autoFillError.value = "No book found for \"$title\""
                }
                is BookSearchResult.ServiceError -> {
                    _autoFillError.value = "Book search service is currently unavailable. Please try again later."
                }
            }
            _isAutoFillLoading.value = false
        }
    }

    fun clearBookImage() {
        autoFilledImageUrl = null
    }

    fun savePost(
        bookTitle: String,
        bookAuthor: String,
        bookPublishYear: Int?,
        content: String,
        bookImageUri: Uri?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userId = auth.currentUser?.uid

                if (userId != null) {
                    val userDoc = firestore.collection("users").document(userId).get().await()

                    if (userDoc.exists()) {
                        val fetchedName = userDoc.getString("name") ?: "Anonymous"
                        val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                        val postRef = firestore.collection("posts").document()

                        val bookImageUrl = when {
                            bookImageUri != null -> uploadBookImage(postRef.id, bookImageUri)
                            !autoFilledImageUrl.isNullOrEmpty() -> autoFilledImageUrl!!
                            else -> ""
                        }

                        val post = Post(
                            id = postRef.id,
                            userId = userId,
                            userName = fetchedName,
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
                        _postSaved.value = true
                    } else {
                        _errorMessage.value = "User profile not found"
                        _postSaved.value = false
                    }
                } else {
                    _errorMessage.value = "Not logged in"
                    _postSaved.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _postSaved.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadBookImage(postId: String, uri: Uri): String {
        val ref = storage.reference.child("book_images/${postId}_${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
