package com.example.bookclub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.FragmentHomeBinding
import com.example.bookclub.ui.nav.toCommentsNavData
import com.example.bookclub.viewmodel.HomeViewModel
import com.example.bookclub.viewmodel.HomeViewModelFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postAdapter: PostAdapter
    
    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure we handle authentication before setting up the UI
        if (!viewModel.isLoggedIn()) {
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun navigateToLogin() {
        val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
        findNavController().navigate(action)
    }

    private fun setupRecyclerView() {
        val currentUserId = viewModel.getCurrentUserId()
        postAdapter = PostAdapter(
            currentUserId = currentUserId,
            onLikeClick = { post ->
                currentUserId?.let { uid ->
                    viewModel.likePost(post.id, uid)
                }
            },
            onCommentClick = { post ->
                val d = post.toCommentsNavData()
                val action = HomeFragmentDirections.actionHomeFragmentToCommentsFragment(
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
            }
        )
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun setupListeners() {
        binding.btnAddPost.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToAddPostFragment()
            findNavController().navigate(action)
        }

        binding.btnProfile.setOnClickListener {
            val currentUserId = viewModel.getCurrentUserId() ?: ""
            val action = HomeFragmentDirections.actionHomeFragmentToProfileFragment(currentUserId)
            findNavController().navigate(action)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarHome.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            val lm = binding.rvPosts.layoutManager
            val state = lm?.onSaveInstanceState()
            postAdapter.submitList(posts) {
                binding.rvPosts.post {
                    lm?.onRestoreInstanceState(state)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
