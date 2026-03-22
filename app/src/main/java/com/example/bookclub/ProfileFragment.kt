package com.example.bookclub

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
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.FragmentProfileBinding
import com.example.bookclub.model.Post
import com.example.bookclub.viewmodel.ProfileViewModel
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso

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

    override fun onResume() {
        super.onResume()
        viewModel.refreshUserData()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            currentUserId = viewModel.getCurrentUserId(),
            onLikeClick = { /* Already handled if needed */ },
            onCommentClick = { post ->
                val imageUrl = post.profileImageUrl.ifEmpty { post.userImageUrl }
                val action = ProfileFragmentDirections.actionProfileFragmentToCommentsFragment(
                    postId = post.id,
                    userName = post.userName,
                    bookTitle = post.bookTitle,
                    content = post.content,
                    userImageUrl = imageUrl
                )
                findNavController().navigate(action)
            },
            onEditClick = { post -> showEditDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmation(post.id) }
        )
        binding.rvMyPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
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
                loadProfileImage(url)
            }
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }
    }

    private fun loadProfileImage(url: String) {
        Picasso.get().invalidate(url)
        Picasso.get()
            .load(url)
            .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
            .networkPolicy(NetworkPolicy.NO_CACHE)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .resize(500, 500)
            .centerCrop()
            .onlyScaleDown()
            .into(binding.ivProfileImage, object : Callback {
                override fun onSuccess() {
                    Log.d("Picasso", "Profile image refreshed successfully")
                }

                override fun onError(e: Exception?) {
                    Log.e("Picasso", "Error refreshing profile image: ${e?.message}")
                }
            })
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
