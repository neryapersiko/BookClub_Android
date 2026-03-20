package com.example.bookclub

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.ActivityProfileBinding
import com.example.bookclub.model.Post
import com.example.bookclub.viewmodel.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var postAdapter: PostAdapter
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Profile"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupListeners() {
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onLikeClick = { /* Already handled in MainActivity if needed */ },
            onCommentClick = { post ->
                val intent = Intent(this, CommentsActivity::class.java)
                intent.putExtra("POST_ID", post.id)
                intent.putExtra("USER_NAME", post.userName)
                intent.putExtra("BOOK_TITLE", post.bookTitle)
                intent.putExtra("CONTENT", post.content)
                startActivity(intent)
            },
            onEditClick = { post -> showEditDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmation(post.id) }
        )
        binding.rvMyPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(this@ProfileActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(this) { name ->
            binding.tvProfileName.text = name
        }

        viewModel.profileImageUrl.observe(this) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivProfileImage)
            }
        }

        viewModel.posts.observe(this) { posts ->
            postAdapter.submitList(posts)
        }
    }

    private fun showDeleteConfirmation(postId: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(postId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(post: Post) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)

        val etTitle = EditText(this)
        etTitle.hint = "Book Title"
        etTitle.setText(post.bookTitle)
        layout.addView(etTitle)

        val etContent = EditText(this)
        etContent.hint = "Content"
        etContent.setText(post.content)
        layout.addView(etContent)

        AlertDialog.Builder(this)
            .setTitle("Edit Post")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newContent = etContent.text.toString().trim()
                if (newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                    viewModel.updatePost(post.id, newTitle, newContent)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
