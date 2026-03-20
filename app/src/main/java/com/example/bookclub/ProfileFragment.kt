package com.example.bookclub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.FragmentProfileBinding
import com.example.bookclub.model.Post
import com.example.bookclub.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postAdapter: PostAdapter
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onLikeClick = { /* Already handled if needed */ },
            onCommentClick = { post ->
                val intent = Intent(requireContext(), CommentsActivity::class.java)
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
            layoutManager = LinearLayoutManager(requireContext())
            // Optimization for memory and performance
            setHasFixedSize(true)
            setItemViewCacheSize(10)
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvProfileName.text = name
        }

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Picasso.get()
                    .load(url)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    // Mandatory resize to prevent "Large Bitmap" crash
                    .resize(500, 500)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivProfileImage, object : Callback {
                        override fun onSuccess() {
                            Log.d("Picasso", "Profile image loaded successfully")
                        }

                        override fun onError(e: Exception?) {
                            Log.e("Picasso", "Error loading profile image: ${e?.message}")
                        }
                    })
            }
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }
    }

    private fun showDeleteConfirmation(postId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(postId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(post: Post) {
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 0)

        val etTitle = EditText(requireContext())
        etTitle.hint = "Book Title"
        etTitle.setText(post.bookTitle)
        layout.addView(etTitle)

        val etContent = EditText(requireContext())
        etContent.hint = "Content"
        etContent.setText(post.content)
        layout.addView(etContent)

        AlertDialog.Builder(requireContext())
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
