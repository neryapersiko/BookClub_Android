package com.example.bookclub.network

import com.example.bookclub.model.BookDetails

sealed class BookSearchResult {
    data class Success(val details: BookDetails) : BookSearchResult()
    object NotFound : BookSearchResult()
    data class ServiceError(val message: String) : BookSearchResult()
}
