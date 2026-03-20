package com.example.bookclub.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookclub.databinding.ItemCommentBinding
import com.example.bookclub.model.Comment

class CommentAdapter(
    private val currentUserId: String?,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(comment: Comment) {
            binding.tvCommenterName.text = comment.userName
            binding.tvCommentText.text = comment.content

            // Load profile image
            Glide.with(binding.root.context)
                .load(comment.profileImageUrl)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.ivCommentAuthorProfile)

            // Show Edit/Delete only for the owner
            if (comment.userId == currentUserId) {
                binding.layoutActions.visibility = View.VISIBLE
                binding.btnEditComment.setOnClickListener { onEditClick(comment) }
                binding.btnDeleteComment.setOnClickListener { onDeleteClick(comment) }
            } else {
                binding.layoutActions.visibility = View.GONE
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean = oldItem == newItem
    }
}
