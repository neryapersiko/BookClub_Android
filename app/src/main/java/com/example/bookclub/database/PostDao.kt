package com.example.bookclub.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookclub.model.Post

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    @Query("UPDATE posts SET profileImageUrl = :newImageUrl WHERE userId = :userId")
    suspend fun updateProfileImageForUser(userId: String, newImageUrl: String)
}
