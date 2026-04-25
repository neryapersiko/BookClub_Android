package com.example.bookclub.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.bookclub.model.Post

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    fun getPostById(postId: String): LiveData<Post?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<Post>)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    @Query("DELETE FROM posts WHERE id NOT IN (:ids)")
    suspend fun deletePostsNotIn(ids: List<String>)

    @Query("UPDATE posts SET profileImageUrl = :newImageUrl WHERE userId = :userId")
    suspend fun updateProfileImageForUser(userId: String, newImageUrl: String)

    @Transaction
    suspend fun replaceWithUpsert(posts: List<Post>) {
        if (posts.isEmpty()) {
            deleteAllPosts()
            return
        }
        insertPosts(posts)
        deletePostsNotIn(posts.map { it.id })
    }
}
