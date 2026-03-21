package com.example.bookclub.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
                // 1. Create Auth User
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = authResult.user?.uid ?: throw Exception("User creation failed")

                // 2. Upload Image if provided, otherwise use an empty string or a default
                val finalImageUrl = if (localUri != null) {
                    uploadImage(uid, localUri)
                } else {
                    "" // Or a specific default placeholder URL
                }

                // 3. Save User Data to Firestore
                val userMap = mapOf(
                    "id" to uid,
                    "name" to name,
                    "email" to email,
                    "profileImageUrl" to finalImageUrl
                )
                firestore.collection("users").document(uid).set(userMap).await()

                _registrationStatus.value = Result.success(Unit)
            } catch (e: Exception) {
                _registrationStatus.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadImage(uid: String, uri: Uri): String {
        // Use unique filename with timestamp to avoid cache issues
        val ref = storage.reference.child("profile_images/${uid}_${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun resetStatus() {
        _registrationStatus.value = null
    }
}
