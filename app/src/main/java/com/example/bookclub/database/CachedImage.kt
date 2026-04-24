package com.example.bookclub.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_images")
data class CachedImage(
    @PrimaryKey val key: String,
    val sourceUrl: String,
    val localPath: String,
    val updatedAt: Long
)

