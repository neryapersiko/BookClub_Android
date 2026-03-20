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

class EditProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

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
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()
                if (document.exists()) {
                    val data = mapOf(
                        "name" to document.getString("name"),
                        "profileImageUrl" to document.getString("profileImageUrl")
                    )
                    _userData.value = data
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateProfile(newName: String, newLocalUri: Uri?, newWebUrl: String?) {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>("name" to newName)
                
                if (newLocalUri != null) {
                    val uploadedUrl = uploadImage(uid, newLocalUri)
                    updates["profileImageUrl"] = uploadedUrl
                } else if (!newWebUrl.isNullOrEmpty()) {
                    updates["profileImageUrl"] = newWebUrl
                }

                firestore.collection("users").document(uid).update(updates).await()
                _updateResult.value = Result.success(Unit)
            } catch (e: Exception) {
                _updateResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun uploadImage(uid: String, uri: Uri): String {
        // Using timestamp to avoid cache issues as requested
        val ref = storage.reference.child("profile_images/${uid}_${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun resetUpdateResult() {
        _updateResult.value = null
    }
}
