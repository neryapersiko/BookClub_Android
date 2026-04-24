package com.example.bookclub.di

import android.content.Context
import com.example.bookclub.database.AppDatabase
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.repository.ImageRepository

/**
 * Minimal manual dependency provider for the project.
 * Keeps Firebase/Room construction out of UI & ViewModels.
 */
object ServiceLocator {

    @Volatile
    private var repository: BookRepository? = null

    @Volatile
    private var imageRepository: ImageRepository? = null

    fun provideRepository(context: Context): BookRepository {
        return repository ?: synchronized(this) {
            repository ?: run {
                val db = AppDatabase.getDatabase(context.applicationContext)
                BookRepository(db.postDao()).also { repository = it }
            }
        }
    }

    fun provideImageRepository(context: Context): ImageRepository {
        return imageRepository ?: synchronized(this) {
            imageRepository ?: run {
                val db = AppDatabase.getDatabase(context.applicationContext)
                ImageRepository(context.applicationContext, db.cachedImageDao()).also { imageRepository = it }
            }
        }
    }
}

