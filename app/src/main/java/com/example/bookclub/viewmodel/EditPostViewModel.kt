package com.example.bookclub.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.Post
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.repository.ImageRepository
import kotlinx.coroutines.launch

class EditPostViewModel(
    private val repository: BookRepository,
    private val imageRepository: ImageRepository,
    private val postId: String
) : ViewModel() {

    val post: LiveData<Post?> = repository.getPostById(postId)

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _updateSuccess = MutableLiveData(false)
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun savePost(
        newTitle: String,
        newAuthor: String,
        newYear: Int?,
        newContent: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _updateSuccess.value = false

            val result = repository.updatePostWithOptionalImage(
                postId = postId,
                newTitle = newTitle,
                newAuthor = newAuthor,
                newYear = newYear,
                newContent = newContent,
                newLocalImageUri = _selectedImageUri.value
            )

            result.onSuccess {
                if (_selectedImageUri.value != null) {
                    imageRepository.invalidate("book:$postId")
                }
                _updateSuccess.value = true
            }.onFailure { e ->
                _errorMessage.value = e.message
            }

            _isLoading.value = false
        }
    }
}

