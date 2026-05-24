package com.example.bookclub.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.repository.ImageRepository
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val repository: BookRepository,
    private val imageRepository: ImageRepository
) : ViewModel() {

    private val _userData = MutableLiveData<Map<String, String?>>()
    val userData: LiveData<Map<String, String?>> = _userData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _updateResult = MutableLiveData<Result<Unit>?>()
    val updateResult: LiveData<Result<Unit>?> = _updateResult

    init {
        fetchCurrentUserData()
    }

    private fun fetchCurrentUserData() {
        viewModelScope.launch {
            repository.getCurrentUserProfile()
                .onSuccess { data -> _userData.value = data }
        }
    }

    fun updateProfile(newName: String, newLocalUri: Uri?) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val uploadedUrl = repository
                    .updateCurrentUserProfile(name = newName, newLocalUri = newLocalUri)
                    .getOrElse { throw it }

                val currentImageUrl = uploadedUrl ?: _userData.value?.get("profileImageUrl")

                _userData.value = mapOf(
                    "name" to newName,
                    "profileImageUrl" to currentImageUrl
                )

                currentImageUrl?.let {
                    repository.updateLocalUserProfile(it)
                }

                repository.getCurrentUserId()?.let { uid ->
                    imageRepository.invalidate("profile:$uid")
                }

                _updateResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }
}
