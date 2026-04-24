package com.example.bookclub.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val repository: BookRepository
) : ViewModel() {

    private val _registrationStatus = MutableLiveData<Result<Unit>?>()
    val registrationStatus: LiveData<Result<Unit>?> = _registrationStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Registers a user with email/password and an optional local image URI.
     * Signature updated to remove external URL support.
     */
    fun registerUser(name: String, email: String, pass: String, localUri: Uri?) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _registrationStatus.value = repository.registerUserWithProfileImage(
                    name = name,
                    email = email,
                    pass = pass,
                    localProfileImageUri = localUri
                )
            } catch (e: Exception) {
                _registrationStatus.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetStatus() {
        _registrationStatus.value = null
    }
}
