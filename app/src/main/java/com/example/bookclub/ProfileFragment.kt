package com.example.bookclub

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.FragmentProfileBinding
import com.example.bookclub.di.ServiceLocator
import com.example.bookclub.model.Post
import com.example.bookclub.ui.nav.toCommentsNavData
import com.example.bookclub.ui.toolbar.bindBack
import com.example.bookclub.viewmodel.ProfileViewModel
import com.example.bookclub.viewmodel.ProfileViewModelFactory
import com.squareup.picasso.Callback
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postAdapter: PostAdapter
    private val args: ProfileFragmentArgs by navArgs()
    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            repository = ServiceLocator.provideRepository(requireContext()),
            userId = args.userId
        )
    }

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
            currentUserId = ServiceLocator.provideRepository(requireContext()).getCurrentUserId(),
            onLikeClick = { /* Already handled if needed */ },
            onCommentClick = { post ->
                val d = post.toCommentsNavData()
                val action = ProfileFragmentDirections.actionProfileFragmentToCommentsFragment(
                    postId = d.postId,
                    userName = d.userName,
                    bookTitle = d.bookTitle,
                    content = d.content,
                    userImageUrl = d.userImageUrl,
                    bookAuthor = d.bookAuthor,
                    bookPublishYear = d.bookPublishYear,
                    bookImageUrl = d.bookImageUrl
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
        binding.toolbar.bindBack(findNavController())

        binding.btnEditProfile.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarProfile.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnEditProfile.isEnabled = !loading
        }

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
        val key = "profile:${args.userId}"
        viewLifecycleOwner.lifecycleScope.launch {
            val localUri = ServiceLocator.provideImageRepository(requireContext())
                .getOrFetchLocalUri(key = key, url = url)

            if (localUri != null) {
                Picasso.get()
                    .load(localUri)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .placeholder(R.drawable.avatar_default)
                    .error(R.drawable.avatar_default)
                    .resize(500, 500)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivProfileImage, object : Callback {
                        override fun onSuccess() {
                            Log.d("Picasso", "Profile image loaded from cache")
                        }

                        override fun onError(e: Exception?) {
                            Log.e("Picasso", "Error loading cached image: ${e?.message}")
                        }
                    })
            } else {
                Picasso.get()
                    .load(url)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .placeholder(R.drawable.avatar_default)
                    .error(R.drawable.avatar_default)
                    .resize(500, 500)
                    .centerCrop()
                    .onlyScaleDown()
                    .into(binding.ivProfileImage)
            }
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

        val etAuthor = EditText(requireContext())
        etAuthor.hint = "Book Author"
        etAuthor.setText(post.bookAuthor)
        layout.addView(etAuthor)

        val etYear = EditText(requireContext())
        etYear.hint = "Publish Year"
        etYear.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        if (post.bookPublishYear != null) {
            etYear.setText(post.bookPublishYear.toString())
        }
        layout.addView(etYear)

        val etContent = EditText(requireContext())
        etContent.hint = "Content"
        etContent.setText(post.content)
        layout.addView(etContent)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val newTitle = etTitle.text.toString().trim()
                val newAuthor = etAuthor.text.toString().trim()
                val newYear = etYear.text.toString().trim().toIntOrNull()
                val newContent = etContent.text.toString().trim()
                if (newTitle.isNotEmpty() && newContent.isNotEmpty()) {
                    viewModel.updatePost(post.id, newTitle, newAuthor, newYear, newContent)
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
