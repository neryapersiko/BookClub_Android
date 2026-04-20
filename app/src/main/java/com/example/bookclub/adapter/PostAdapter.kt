package com.example.bookclub.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.bookclub.databinding.ItemPostBinding
import com.example.bookclub.model.Post
import com.squareup.picasso.Picasso

class PostAdapter(
    private val currentUserId: String?,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onEditClick: ((Post) -> Unit)? = null,
    private val onDeleteClick: ((Post) -> Unit)? = null
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            // User header
            binding.tvAuthorName.text = post.userName
            binding.tvLikesCount.text = post.likesCount.toString()

            // User profile image
            val profileUrl = post.profileImageUrl.ifEmpty { post.userImageUrl }
            if (profileUrl.isNotEmpty()) {
                Picasso.get()
                    .load(profileUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .resize(120, 120)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivAuthorProfile)
            } else {
                binding.ivAuthorProfile.setImageResource(android.R.drawable.ic_menu_gallery)
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
                Picasso.get()
                    .load(coverUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .resize(240, 360)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivBookCover)
            } else {
                binding.ivBookCover.setImageResource(android.R.drawable.ic_menu_gallery)
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
            val isLiked = currentUserId != null && (post.likedBy.contains(currentUserId) || post.likes?.contains(currentUserId) == true)
            if (isLiked) {
                binding.btnLike.setIconTintResource(android.R.color.holo_red_dark)
                binding.btnLike.setTextColor(Color.RED)
            } else {
                binding.btnLike.setIconTintResource(android.R.color.darker_gray)
                binding.btnLike.setTextColor(Color.BLACK)
            }

            // Click listeners
            binding.btnLike.setOnClickListener { onLikeClick(post) }
            binding.btnComment.setOnClickListener { onCommentClick(post) }
            binding.root.setOnClickListener { onCommentClick(post) }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}
