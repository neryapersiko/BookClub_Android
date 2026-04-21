package com.example.bookclub

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class BookClubApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Create a dedicated directory for the OkHttp cache (50MB)
        val cacheDirectory = File(cacheDir, "http-cache")
        val cacheSize = 50 * 1024 * 1024L // 50MB
        val cache = Cache(cacheDirectory, cacheSize)

        // Create a custom OkHttpClient with a User-Agent interceptor and Cache
        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                    .build()
                chain.proceed(newRequest)
            }
            .build()

        // Build Picasso to use the custom downloader with the persistent cache
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
