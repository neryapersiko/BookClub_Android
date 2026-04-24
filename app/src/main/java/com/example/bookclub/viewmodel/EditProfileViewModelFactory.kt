package com.example.bookclub.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.bookclub.di.ServiceLocator

class EditProfileViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            val repository = ServiceLocator.provideRepository(context)
            val imageRepository = ServiceLocator.provideImageRepository(context)
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(repository, imageRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
