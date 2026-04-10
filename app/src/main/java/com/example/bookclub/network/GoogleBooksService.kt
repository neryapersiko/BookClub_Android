package com.example.bookclub.network

import com.example.bookclub.model.BookDetails
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleBooksService {

    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    private val api: GoogleBooksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }

    suspend fun fetchBookDetails(title: String): BookDetails? {
        return try {
            val response = api.searchBooks(title)
            val volumeInfo = response.items?.firstOrNull()?.volumeInfo ?: return null

            val author = volumeInfo.authors?.firstOrNull() ?: ""
            val publishYear = volumeInfo.publishedDate
                ?.take(4)
                ?.toIntOrNull()
            val imageUrl = volumeInfo.imageLinks?.thumbnail ?: ""

            if (author.isEmpty() && publishYear == null && imageUrl.isEmpty()) {
                return null
            }

            BookDetails(
                author = author,
                publishYear = publishYear,
                imageUrl = imageUrl
            )
        } catch (e: Exception) {
            null
        }
    }
}
