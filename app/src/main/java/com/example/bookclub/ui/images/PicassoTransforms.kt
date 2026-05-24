package com.example.bookclub.ui.images

import com.squareup.picasso.RequestCreator

/**
 * Max decode sizes for Picasso — prevents "trying to draw too large bitmap" crashes.
 */
object PicassoTransforms {

    fun RequestCreator.feedAvatar() =
        resize(120, 120).centerCrop().onlyScaleDown()

    fun RequestCreator.feedBookCover() =
        resize(400, 600).centerCrop().onlyScaleDown()

    fun RequestCreator.profileLarge() =
        resize(500, 500).centerCrop().onlyScaleDown()

    fun RequestCreator.profilePreview() =
        resize(500, 500).centerInside().onlyScaleDown()

    fun RequestCreator.commentAvatar() =
        resize(96, 96).centerCrop().onlyScaleDown()

    fun RequestCreator.commentsHeaderAvatar() =
        resize(120, 120).centerCrop().onlyScaleDown()

    fun RequestCreator.commentsHeaderCover() =
        resize(400, 600).centerCrop().onlyScaleDown()

    fun RequestCreator.bookCoverPreview() =
        resize(800, 1200).centerInside().onlyScaleDown()

    /** Fallback when no view-specific transform is passed to [CachedImageLoader]. */
    fun RequestCreator.safeDefault() =
        resize(400, 600).centerInside().onlyScaleDown()
}
