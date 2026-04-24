package com.example.bookclub.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookclub.databinding.ItemPostBinding
import com.example.bookclub.R
import com.example.bookclub.model.Post
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.example.bookclub.ui.images.CachedImageLoader
import java.util.UUID

class PostAdapter(
    private val currentUserId: String?,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onDeleteClick: ((Post) -> Unit)? = null
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    private val imageScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
            return
        }

        val payload = payloads.lastOrNull()
        if (payload is LikePayload) {
            holder.bindLike(payload.likesCount, payload.likedBy, payload.likes)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun getItemId(position: Int): Long {
        // Stable, collision-resistant ID derived from post.id
        return UUID.nameUUIDFromBytes(getItem(position).id.toByteArray()).mostSignificantBits
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            // User header
            binding.tvAuthorName.text = post.userName
            bindLike(post.likesCount, post.likedBy, post.likes)

            // User profile image
            val profileUrl = post.profileImageUrl.ifEmpty { post.userImageUrl }
            if (profileUrl.isNotEmpty()) {
                val key = "profile:${post.userId}"
                CachedImageLoader.load(
                    scope = imageScope,
                    imageView = binding.ivAuthorProfile,
                    cacheKey = key,
                    url = profileUrl,
                    placeholder = R.drawable.avatar_default
                ) { it.resize(120, 120).centerCrop().onlyScaleDown() }
            } else {
                binding.ivAuthorProfile.setImageResource(R.drawable.avatar_default)
            }

            // Book section
            binding.tvBookTitle.text = post.bookTitle

            if (post.bookAuthor.isNotEmpty()) {
                binding.tvBookAuthor.text = "by ${post.bookAuthor}"
                binding.tvBookAuthor.visibility = View.VISIBLE
            } else {
                binding.tvBookAuthor.visibility = View.GONE
            }

            if (post.bookPublishYear != null) {
                binding.tvBookPublishYear.text = "(${post.bookPublishYear})"
                binding.tvBookPublishYear.visibility = View.VISIBLE
            } else {
                binding.tvBookPublishYear.visibility = View.GONE
            }

            // Book cover image
            if (post.bookImageUrl.isNotEmpty()) {
                val coverUrl = post.bookImageUrl.replace("http://", "https://")
                val key = "book:${post.id}"
                CachedImageLoader.load(
                    scope = imageScope,
                    imageView = binding.ivBookCover,
                    cacheKey = key,
                    url = coverUrl,
                    placeholder = R.drawable.book_cover_default
                ) { it.resize(240, 360).centerCrop().onlyScaleDown() }
            } else {
                binding.ivBookCover.setImageResource(R.drawable.book_cover_default)
            }

            // User review content
            binding.tvContent.text = post.content

            // Edit/Delete buttons (own posts only)
            if (post.userId == currentUserId && onEditClick != null && onDeleteClick != null) {
                binding.layoutPostActions.visibility = View.VISIBLE
                binding.btnEditPost.setOnClickListener { onEditClick.invoke(post) }
                binding.btnDeletePost.setOnClickListener { onDeleteClick.invoke(post) }
            } else {
                binding.layoutPostActions.visibility = View.GONE
            }

            // Like button state
            // Click listeners
            binding.btnLike.setOnClickListener { onLikeClick(post) }
            binding.btnComment.setOnClickListener { onCommentClick(post) }
            binding.root.setOnClickListener { onCommentClick(post) }
        }

        fun bindLike(likesCount: Int, likedBy: List<String>, likes: List<String>?) {
            binding.tvLikesCount.text = likesCount.toString()

            val isLiked = currentUserId != null && (likedBy.contains(currentUserId) || likes?.contains(currentUserId) == true)
            val context = binding.root.context
            if (isLiked) {
                val likeColor = context.getColor(R.color.like_red)
                binding.btnLike.setIconTintResource(R.color.like_red)
                binding.btnLike.setTextColor(likeColor)
            } else {
                val secondaryColor = context.getColor(R.color.text_secondary)
                binding.btnLike.setIconTintResource(R.color.text_secondary)
                binding.btnLike.setTextColor(secondaryColor)
            }
        }
    }

    private data class LikePayload(
        val likesCount: Int,
        val likedBy: List<String>,
        val likes: List<String>?
    )

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem

        override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
            val likeChanged =
                oldItem.likesCount != newItem.likesCount ||
                    oldItem.likedBy != newItem.likedBy ||
                    oldItem.likes != newItem.likes

            return if (likeChanged) {
                LikePayload(
                    likesCount = newItem.likesCount,
                    likedBy = newItem.likedBy,
                    likes = newItem.likes
                )
            } else {
                null
            }
        }
    }
}
