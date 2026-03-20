package com.example.bookclub

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bookclub.adapter.CommentAdapter
import com.example.bookclub.databinding.ActivityCommentsBinding
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.viewmodel.CommentsViewModel
import com.example.bookclub.viewmodel.CommentsViewModelFactory

class CommentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommentsBinding
    private lateinit var commentAdapter: CommentAdapter
    
    private val viewModel: CommentsViewModel by viewModels {
        val postId = intent.getStringExtra("POST_ID") ?: ""
        CommentsViewModelFactory(BookRepository(), postId)
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
        // Set the toolbar as the action bar for this activity
        setSupportActionBar(binding.toolbar)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Comments"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Handle the back arrow click in the toolbar
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun setupHeader() {
        val userName = intent.getStringExtra("USER_NAME") ?: "Anonymous User"
        val bookTitle = intent.getStringExtra("BOOK_TITLE") ?: "No Title"
        val content = intent.getStringExtra("CONTENT") ?: ""

        binding.tvHeaderUserName.text = "Posted by: $userName"
        binding.tvHeaderBookTitle.text = bookTitle
        binding.tvHeaderContent.text = content
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
