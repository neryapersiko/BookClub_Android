package com.example.bookclub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class UserViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _isLoggedOut = MutableLiveData<Boolean>()
    val isLoggedOut: LiveData<Boolean> = _isLoggedOut

    fun logout() {
        auth.signOut()
        _isLoggedOut.value = true
    }
}
