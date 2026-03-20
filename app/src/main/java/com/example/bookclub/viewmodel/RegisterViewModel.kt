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

    fun registerUser(name: String, email: String, pass: String, localUri: Uri?, webUrl: String?) {
        if (localUri == null && webUrl.isNullOrEmpty()) {
            _registrationStatus.value = Result.failure(Exception("Please provide a profile image"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // 1. Create Auth User
                val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = authResult.user?.uid ?: throw Exception("User creation failed")

                // 2. Determine Profile Image URL
                val finalImageUrl = if (localUri != null) {
                    uploadImage(uid, localUri)
                } else {
                    webUrl!!
                }

                // 3. Save to Firestore
                val userMap = mapOf(
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
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun resetStatus() {
        _registrationStatus.value = null
    }
}
