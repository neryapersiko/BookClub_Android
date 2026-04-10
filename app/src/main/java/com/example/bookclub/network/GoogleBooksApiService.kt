package com.example.bookclub.network

import retrofit2.http.GET
import retrofit2.http.Query

interface GoogleBooksApiService {

    @GET("volumes")
    suspend fun searchBooks(@Query("q") query: String): GoogleBooksResponse
}
