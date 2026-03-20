package com.example.bookclub

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.ActivityMainBinding
import com.example.bookclub.repository.BookRepository
import com.example.bookclub.viewmodel.HomeViewModel
import com.example.bookclub.viewmodel.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()
    
    // Initialize ViewModel using Factory
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(BookRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Logout logic - check if user is logged in
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid
        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onLikeClick = { post ->
                currentUserId?.let { uid ->
                    viewModel.likePost(post.id, uid)
                }
            },
            onCommentClick = { post ->
                val intent = Intent(this, CommentsActivity::class.java)
                intent.putExtra("POST_ID", post.id)
                intent.putExtra("USER_NAME", post.userName)
                intent.putExtra("BOOK_TITLE", post.bookTitle)
                intent.putExtra("CONTENT", post.content)
                startActivity(intent)
            }
        )
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    private fun setupListeners() {
        binding.btnAddPost.setOnClickListener {
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
        }

        binding.btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(this) { posts ->
            postAdapter.submitList(posts)
        }
    }
}
