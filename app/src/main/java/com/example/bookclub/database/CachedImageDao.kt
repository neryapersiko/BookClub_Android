package com.example.bookclub.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedImageDao {
    @Query("SELECT * FROM cached_images WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CachedImage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedImage)

    @Query("DELETE FROM cached_images WHERE `key` = :key")
    suspend fun delete(key: String)
}

