package com.example.bookclub.ui.images

import android.net.Uri
import android.widget.ImageView
import com.example.bookclub.di.ServiceLocator
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import com.example.bookclub.ui.images.PicassoTransforms.safeDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single place to load images through the Room-backed cache + Picasso.
 * Call sites should pass a [configure] with [PicassoTransforms]; default applies [PicassoTransforms.safeDefault].
 */
object CachedImageLoader {

    fun load(
        scope: CoroutineScope,
        imageView: ImageView,
        cacheKey: String,
        url: String,
        placeholder: Int,
        configure: (RequestCreator) -> RequestCreator = { it.safeDefault() }
    ) {
        imageView.tag = cacheKey
        scope.launch {
            val localUri: Uri? = ServiceLocator.provideImageRepository(imageView.context)
                .getOrFetchLocalUri(cacheKey, url)

            // If view got rebound, ignore.
            if (imageView.tag != cacheKey) return@launch

            val request = if (localUri != null) Picasso.get().load(localUri) else Picasso.get().load(url)
            configure(request)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(placeholder)
                .error(placeholder)
                .into(imageView)
        }
    }
}
