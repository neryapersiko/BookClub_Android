package com.example.bookclub

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bookclub.databinding.ActivityAddPostBinding
import com.example.bookclub.viewmodel.CreatePostViewModel

class AddPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPostBinding
    private val viewModel: CreatePostViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnPublish.setOnClickListener {
            val bookTitle = binding.etBookTitle.text.toString().trim()
            val content = binding.etContent.text.toString().trim()

            if (bookTitle.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.savePost(bookTitle, content)
        }
    }

    private fun observeViewModel() {
        viewModel.postSaved.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Post Published Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPublish.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_LONG).show()
            }
        }
    }
}
