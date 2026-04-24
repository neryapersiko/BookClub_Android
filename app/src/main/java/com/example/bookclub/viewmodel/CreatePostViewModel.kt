package com.example.bookclub.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.BookDetails
import com.example.bookclub.network.BookSearchResult
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val repository: BookRepository
) : ViewModel() {

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
            when (val result = repository.fetchBookDetails(title)) {
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
                val result = repository.createPostForCurrentUser(
                    bookTitle = bookTitle,
                    bookAuthor = bookAuthor,
                    bookPublishYear = bookPublishYear,
                    content = content,
                    bookImageUri = bookImageUri,
                    autoFilledImageUrl = autoFilledImageUrl
                )
                result.onSuccess {
                    _postSaved.value = true
                }.onFailure { e ->
                    _errorMessage.value = e.message
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
}
