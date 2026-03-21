package com.example.bookclub

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bookclub.adapter.CommentAdapter
import com.example.bookclub.database.AppDatabase
import com.example.bookclub.databinding.ActivityCommentsBinding
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.viewmodel.CommentsViewModel
import com.example.bookclub.viewmodel.CommentsViewModelFactory
import com.squareup.picasso.Picasso

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var commentAdapter: CommentAdapter
    
    private val viewModel: CommentsViewModel by viewModels {
        val postId = intent.getStringExtra("POST_ID") ?: ""
        val database = AppDatabase.getDatabase(this)
        CommentsViewModelFactory(BookRepository(database.postDao()), postId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
        setupHeader()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupActionBar() {
        setSupportActionBar(binding.toolbar)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Comments"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupHeader() {
        val userName = intent.getStringExtra("USER_NAME") ?: "Anonymous User"
        val bookTitle = intent.getStringExtra("BOOK_TITLE") ?: "No Title"
        val content = intent.getStringExtra("CONTENT") ?: ""
        val userImageUrl = intent.getStringExtra("USER_IMAGE_URL") ?: ""

        binding.tvHeaderBookTitle.text = bookTitle
        binding.tvHeaderContent.text = content

        if (userImageUrl.isNotEmpty()) {
            Picasso.get()
                .load(userImageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .resize(120, 120)
                .centerCrop()
                .into(binding.ivHeaderProfile)
        } else {
            binding.ivHeaderProfile.setImageResource(android.R.drawable.ic_menu_gallery)
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onEditClick = { comment -> showEditDialog(comment.id, comment.content) },
            onDeleteClick = { comment -> viewModel.deleteComment(comment.id) }
        )
        binding.rvComments.adapter = commentAdapter
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendComment(text)
                binding.etComment.text.clear()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.comments.observe(this) { list ->
            commentAdapter.submitList(list)
            if (list.isNotEmpty()) {
                binding.rvComments.scrollToPosition(list.size - 1)
            }
        }
        
        viewModel.operationStatus.observe(this) { result ->
            result.onFailure {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(commentId: String, currentText: String) {
        val editText = EditText(this)
        editText.setText(currentText)
        
        AlertDialog.Builder(this)
            .setTitle("Edit Comment")
            .setView(editText)
            .setPositiveButton("Update") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    viewModel.editComment(commentId, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
