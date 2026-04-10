package com.example.bookclub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.bookclub.database.Converters
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(tableName = "posts")
@TypeConverters(Converters::class)
data class Post(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val profileImageUrl: String = "",
    val userImageUrl: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val bookPublishYear: Int? = null,
    val bookImageUrl: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val likes: List<String>? = null,
    val likesCount: Int = 0
)
