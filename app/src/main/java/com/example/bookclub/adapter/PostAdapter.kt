package com.example.bookclub.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.example.bookclub.databinding.ItemPostBinding
import com.example.bookclub.model.Post

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
            binding.tvBookTitle.text = post.bookTitle
            binding.tvAuthorName.text = post.userName
            binding.tvContent.text = post.content
            binding.tvLikesCount.text = post.likesCount.toString()

            // Safe image URL selection
            val imageUrl = post.profileImageUrl.ifEmpty { post.userImageUrl }
            
            if (imageUrl.isNotEmpty()) {
                // Attempt to load from Network/Cache normally
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.stat_notify_error)
                    .resize(120, 120)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivAuthorProfile, object : com.squareup.picasso.Callback {
                        override fun onSuccess() {
                            // Image loaded successfully
                        }

                        override fun onError(e: Exception?) {
                            // If network load fails, try loading strictly from local cache
                            Picasso.get()
                                .load(imageUrl)
                                .networkPolicy(com.squareup.picasso.NetworkPolicy.OFFLINE)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .resize(120, 120)
                                .centerCrop()
                                .into(binding.ivAuthorProfile)
                        }
                    })
            } else {
                binding.ivAuthorProfile.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            
            if (post.userId == currentUserId && onEditClick != null && onDeleteClick != null) {
                binding.layoutPostActions.visibility = View.VISIBLE
                binding.btnEditPost.setOnClickListener { onEditClick.invoke(post) }
                binding.btnDeletePost.setOnClickListener { onDeleteClick.invoke(post) }
            } else {
                binding.layoutPostActions.visibility = View.GONE
            }

            val isLiked = currentUserId != null && (post.likedBy.contains(currentUserId) || post.likes?.contains(currentUserId) == true)
            if (isLiked) {
                binding.btnLike.setIconTintResource(android.R.color.holo_red_dark)
                binding.btnLike.setTextColor(Color.RED)
            } else {
                binding.btnLike.setIconTintResource(android.R.color.darker_gray)
                binding.btnLike.setTextColor(Color.BLACK)
            }

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
