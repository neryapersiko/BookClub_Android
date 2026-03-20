package com.example.bookclub

import android.app.Application
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.OkHttpClient

class BookClubApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Create a custom OkHttpClient with a User-Agent interceptor
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        // Build Picasso to use the custom downloader
        val picasso = Picasso.Builder(this)
            .downloader(OkHttp3Downloader(client))
            .build()

        // Set the singleton instance so Picasso.get() uses this configuration globally
        try {
            Picasso.setSingletonInstance(picasso)
        } catch (e: IllegalStateException) {
            // Already initialized, usually happens during tests or if get() was called before this
        }
    }
}
