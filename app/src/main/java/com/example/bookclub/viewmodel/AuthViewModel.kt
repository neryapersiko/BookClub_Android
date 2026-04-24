package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookclub.model.User
import com.example.bookclub.repository.BookRepository
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

class AuthViewModel(private val repository: BookRepository) : ViewModel() {

    private val _uiState = MutableLiveData(AuthUiState())
    val uiState: LiveData<AuthUiState> = _uiState

    /**
     * Registers a new user with the provided name, email, and password.
     */
    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null) ?: AuthUiState(isLoading = true)
            
            val user = User(name = name, email = email)
            val result = repository.registerUser(user, pass)
            
            result.onSuccess {
                _uiState.value = (_uiState.value ?: AuthUiState()).copy(isLoading = false, isLoggedIn = true)
            }.onFailure { exception ->
                _uiState.value = (_uiState.value ?: AuthUiState()).copy(isLoading = false, errorMessage = exception.message)
            }
        }
    }

    /**
     * Authenticates a user with email and password.
     */
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null) ?: AuthUiState(isLoading = true)
            
            val result = repository.loginUser(email, pass)
            
            result.onSuccess {
                _uiState.value = (_uiState.value ?: AuthUiState()).copy(isLoading = false, isLoggedIn = true)
            }.onFailure { exception ->
                _uiState.value = (_uiState.value ?: AuthUiState()).copy(isLoading = false, errorMessage = exception.message)
            }
        }
    }

    /**
     * Resets the error message in the UI state.
     */
    fun clearError() {
        _uiState.value = (_uiState.value ?: AuthUiState()).copy(errorMessage = null)
    }
}
