package com.example.bookclub.repository

import android.content.Context
import android.net.Uri
import com.example.bookclub.database.CachedImage
import com.example.bookclub.database.CachedImageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.security.MessageDigest

class ImageRepository(
    private val appContext: Context,
    private val cachedImageDao: CachedImageDao
) {
    private val mutexByKey = mutableMapOf<String, Mutex>()

    suspend fun getOrFetchLocalUri(key: String, url: String): Uri? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        val safeUrl = url.replace("http://", "https://")

        val mutex = synchronized(mutexByKey) { mutexByKey.getOrPut(key) { Mutex() } }
        mutex.withLock {
            val existing = cachedImageDao.get(key)
            if (existing != null) {
                val file = File(existing.localPath)
                val sameUrl = existing.sourceUrl == safeUrl
                if (sameUrl && file.exists() && file.length() > 0L) {
                    return@withContext versionedFileUri(file, existing.updatedAt)
                }

                // URL changed or file missing/corrupt: invalidate this entry
                runCatching { file.delete() }
                cachedImageDao.delete(key)
            }

            val bytes = downloadBytes(safeUrl) ?: return@withContext null
            val now = System.currentTimeMillis()
            val file = writeToInternalFile(key, bytes)

            cachedImageDao.upsert(
                // Note: localPath is stable for a given key; 'updatedAt' + URI versioning
                // ensures Picasso treats updates as a new resource.
                CachedImage(
                    key = key,
                    sourceUrl = safeUrl,
                    localPath = file.absolutePath,
                    updatedAt = now
                )
            )
            versionedFileUri(file, now)
        }
    }

    suspend fun invalidate(key: String) = withContext(Dispatchers.IO) {
        val mutex = synchronized(mutexByKey) { mutexByKey.getOrPut(key) { Mutex() } }
        mutex.withLock {
            val existing = cachedImageDao.get(key)
            if (existing != null) {
                runCatching { File(existing.localPath).delete() }
                cachedImageDao.delete(key)
            }
        }
    }

    private fun writeToInternalFile(key: String, bytes: ByteArray): File {
        val dir = File(appContext.filesDir, "cached-images").apply { mkdirs() }
        val name = sha1(key) + ".img"
        val file = File(dir, name)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    private fun downloadBytes(url: String): ByteArray? {
        return runCatching {
            URL(url).openStream().use { it.readBytes() }
        }.getOrNull()
    }

    private fun versionedFileUri(file: File, version: Long): Uri {
        val base = Uri.fromFile(file).toString()
        return Uri.parse("$base?v=$version")
    }

    private fun sha1(input: String): String {
        val md = MessageDigest.getInstance("SHA-1")
        val bytes = md.digest(input.toByteArray())
        return buildString(bytes.size * 2) {
            for (b in bytes) append(((b.toInt() and 0xff) + 0x100).toString(16).substring(1))
        }
    }
}

