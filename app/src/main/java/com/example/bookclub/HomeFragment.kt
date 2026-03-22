package com.example.bookclub

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookclub.adapter.PostAdapter
import com.example.bookclub.databinding.FragmentHomeBinding
import com.example.bookclub.viewmodel.HomeViewModel
import com.example.bookclub.viewmodel.HomeViewModelFactory
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postAdapter: PostAdapter
    private val auth = FirebaseAuth.getInstance()
    
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
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun navigateToLogin() {
        findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
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
                val imageUrl = post.profileImageUrl.ifEmpty { post.userImageUrl }
                val action = HomeFragmentDirections.actionHomeFragmentToCommentsFragment(
                    postId = post.id,
                    userName = post.userName,
                    bookTitle = post.bookTitle,
                    content = post.content,
                    userImageUrl = imageUrl
                )
                findNavController().navigate(action)
            }
        )
        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        binding.btnAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addPostFragment)
        }

        binding.btnProfile.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: ""
            val action = HomeFragmentDirections.actionHomeFragmentToProfileFragment(currentUserId)
            findNavController().navigate(action)
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            navigateToLogin()
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
